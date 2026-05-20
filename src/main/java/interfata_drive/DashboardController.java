package interfata_drive;

import ai.ApiKeyStore;
import ai.GroqAiService;
import core.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Path;

import java.io.IOException;

public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label chatStatusLabel;
    @FXML private TextField writeMessageField;
    @FXML private Button sendBtn;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesBox;
    @FXML private Button addFileBtn;

    private final ApiKeyStore apiKeyStore = new ApiKeyStore();
    private final GroqAiService groqAiService = new GroqAiService();

    @FXML
    public void initialize() {
        if (Session.getClient() != null) {
            Session.getClient().setOnMessageReceived(message -> {
                Platform.runLater(() -> addMessage(message, MessageSender.FRIEND));
            });
        }

        sendBtn.setOnAction(event -> sendMessageAction());
        addFileBtn.setOnAction(event -> handleAddFileAction());
        writeMessageField.setOnAction(event -> sendMessageAction());

        chatMessagesBox.heightProperty().addListener((obs, oldValue, newValue) -> {
            chatScrollPane.setVvalue(1.0);
        });

        updateApiStatus();
        addMessage("Salut! Scrie /ai urmat de intrebare pentru asistent sau /api key cheia_ta_groq pentru conectare.", MessageSender.AI);
    }

    public void setUserData(String fullName) {
        userNameLabel.setText(fullName);
    }

    private void sendMessageAction() {
        String message = writeMessageField.getText();

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        message = message.trim();
        writeMessageField.clear();

        if (message.toLowerCase().startsWith("/api key")) {
            handleApiKeyCommand(message);
            return;
        }

        if (message.toLowerCase().startsWith("/ai")) {
            handleAiCommand(message);
            return;
        }

        addMessage(message, MessageSender.USER);
        sendFriendMessage(message);
    }

    private void handleApiKeyCommand(String command) {
        String apiKey = command.replaceFirst("(?i)^/api\\s+key", "").trim();

        if (apiKey.isEmpty()) {
            addMessage("Scrie comanda asa: /api key cheia_ta_groq", MessageSender.AI);
            return;
        }

        try {
            apiKeyStore.saveApiKey(apiKey);
            updateApiStatus();
            addMessage("Cheia API a fost salvata local.", MessageSender.AI);
        } catch (IOException e) {
            addMessage("Nu am putut salva cheia API: " + e.getMessage(), MessageSender.AI);
        }
    }

    private void handleAiCommand(String command) {
        String prompt = command.replaceFirst("(?i)^/ai", "").trim();

        if (prompt.isEmpty()) {
            addMessage("Scrie o intrebare dupa /ai.", MessageSender.AI);
            return;
        }

        addMessage(prompt, MessageSender.USER);
        addMessage("Ma gandesc...", MessageSender.AI);

        Thread aiThread = new Thread(() -> {
            try {
                String apiKey = apiKeyStore.loadApiKey();

                if (apiKey == null) {
                    Platform.runLater(() -> addMessage("Nu ai setat cheia API(Groq). Foloseste /api key ....", MessageSender.AI));
                    return;
                }

                String response = groqAiService.ask(apiKey, prompt);
                Platform.runLater(() -> addMessage(response, MessageSender.AI));
            } catch (Exception e) {
                Platform.runLater(() -> addMessage("Nu am putut contacta asistentul AI: " + e.getMessage(), MessageSender.AI));
            }
        });

        aiThread.setDaemon(true);
        aiThread.start();
    }

    private void sendFriendMessage(String message) {
        if (Session.getClient() == null || !Session.getClient().isConnected()) {
            addMessage("Mod offline: mesajul nu a fost trimis catre server.", MessageSender.SYSTEM);
            return;
        }

        Thread sendThread = new Thread(() -> {
            try {
                Session.getClient().sendMessage(message);
            } catch (Exception e) {
                Platform.runLater(() -> addMessage("Eroare: mesajul nu a putut fi trimis.", MessageSender.SYSTEM));
            }
        });

        sendThread.setDaemon(true);
        sendThread.start();
    }

    private void updateApiStatus() {
        try {
            String apiKey = apiKeyStore.loadApiKey();
            chatStatusLabel.setText(apiKey == null ? "AI key not set" : "AI ready");
        } catch (IOException e) {
            chatStatusLabel.setText("AI key unavailable");
        }
    }

    public void addMessage(String message) {
        addMessage(message, MessageSender.FRIEND);
    }

    private void addMessage(String message, MessageSender sender) {
        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);
        row.setPadding(new Insets(2, 0, 2, 0));
        row.setAlignment(sender == MessageSender.USER ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Text text = new Text(formatMessage(message, sender));
        text.setFill(sender == MessageSender.USER ? Color.WHITE : Color.web("#1F2937"));
        text.setStyle("-fx-font-size: 13px;");

        TextFlow bubble = new TextFlow(text);
        bubble.setMaxWidth(260);
        bubble.setPadding(new Insets(10, 13, 10, 13));
        bubble.setStyle(getBubbleStyle(sender));

        row.getChildren().add(bubble);
        chatMessagesBox.getChildren().add(row);
    }

    private String formatMessage(String message, MessageSender sender) {
        return switch (sender) {
            case AI -> "AI: " + message;
            case SYSTEM -> message;
            default -> message;
        };
    }

    private String getBubbleStyle(MessageSender sender) {
        return switch (sender) {
            case USER -> "-fx-background-color: #2962FF; -fx-background-radius: 18 18 4 18;";
            case AI -> "-fx-background-color: white; -fx-border-color: #BBD4FF; -fx-border-radius: 18 18 18 4; -fx-background-radius: 18 18 18 4;";
            case SYSTEM -> "-fx-background-color: #EAF2FF; -fx-background-radius: 14; -fx-border-color: #D7E3F8; -fx-border-radius: 14;";
            case FRIEND -> "-fx-background-color: white; -fx-background-radius: 18 18 18 4; -fx-border-color: #E3EAF5; -fx-border-radius: 18 18 18 4;";
        };
    }

    private enum MessageSender {
        USER,
        FRIEND,
        AI,
        SYSTEM
    }
    
    
    private void handleAddFileAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Alege fișier");

        File selectedFile = fileChooser.showOpenDialog(addFileBtn.getScene().getWindow());

        if (selectedFile == null) {
            return;
        }

        Path filePath = selectedFile.toPath().toAbsolutePath().normalize();
        String fileName = filePath.getFileName().toString();

        if (Session.getCurrentUser() != null) {
            Session.getCurrentUser().addFile(filePath);
        }

        addMessage("Fișier adăugat: " + fileName, MessageSender.SYSTEM);
    }
}
