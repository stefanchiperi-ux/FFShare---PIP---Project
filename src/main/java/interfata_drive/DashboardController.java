package interfata_drive;

import ai.ApiKeyStore;
import ai.GroqAiService;
import core.Session;
import core.UserFile;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private TextField writeMessageField;
    @FXML private Button sendBtn;
    @FXML private ListView<ChatMessage> messageList;
    @FXML private Button addFileBtn;
    @FXML private FlowPane filesFlowPane;
    @FXML private Button filesBtn;
    @FXML private Button profileBtn;
    @FXML private VBox filesPage;
    @FXML private VBox profilePage;
    @FXML private Label profileNameLabel;
    @FXML private ImageView profileImageView;
    @FXML private Button chooseProfileImageBtn;

    private String currentUser;
    private final Map<String, Image> profileImages = new HashMap<>();
    private final ApiKeyStore apiKeyStore = new ApiKeyStore();
    private final GroqAiService groqAiService = new GroqAiService();

    @FXML
    public void initialize() {
        messageList.setCellFactory(listView -> new ChatMessageCell());

        if (Session.getClient() != null) {
            Session.getClient().setOnMessageReceived(message -> Platform.runLater(() -> handleServerMessage(message)));
        }
        
        Session.getClient().setOnFileListReceived(files ->
        Platform.runLater(() -> refreshFilesView(files)));
        Session.getClient().requestFileList();

        sendBtn.setOnAction(event -> sendMessageAction());
        addFileBtn.setOnAction(event -> handleAddFileAction());
        writeMessageField.setOnAction(event -> sendMessageAction());

        filesBtn.setOnAction(event -> showFilesPage());
        profileBtn.setOnAction(event -> showProfilePage());
        chooseProfileImageBtn.setOnAction(event -> chooseProfileImage());

        showFilesPage();
        refreshFilesView();
        messageList.getItems().add(ChatMessage.server("Scrie /ai urmat de intrebare pentru asistent sau /api key cheia_ta_groq pentru conectare."));
    }

    public void setUserData(String fullName) {
        this.currentUser = fullName;
        userNameLabel.setText(fullName);
        profileNameLabel.setText(fullName);
    }

    private void showFilesPage() {
        filesPage.setVisible(true);
        filesPage.setManaged(true);
        profilePage.setVisible(false);
        profilePage.setManaged(false);
        filesBtn.setStyle(activeMenuStyle());
        profileBtn.setStyle(inactiveMenuStyle());
    }

    private void showProfilePage() {
        filesPage.setVisible(false);
        filesPage.setManaged(false);
        profilePage.setVisible(true);
        profilePage.setManaged(true);
        filesBtn.setStyle(inactiveMenuStyle());
        profileBtn.setStyle(activeMenuStyle());
    }

    private String activeMenuStyle() {
        return "-fx-background-color: #2962FF; -fx-text-fill: white; -fx-background-radius: 8;";
    }

    private String inactiveMenuStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #607D8B;";
    }

    private void chooseProfileImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Alege poza de profil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagini PNG/JPG", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(chooseProfileImageBtn.getScene().getWindow());

        if (selectedFile == null) {
            return;
        }

        try {
            byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());

            if (imageBytes.length > 1_500_000) {
                showAlert("Eroare", "Imaginea este prea mare. Alege o poza sub 1.5 MB.");
                return;
            }

            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
            Image image = new Image(new ByteArrayInputStream(imageBytes));

            profileImages.put(currentUser, image);
            profileImageView.setImage(image);
            messageList.refresh();

            if (Session.getClient() != null && Session.getClient().isConnected()) {
                Session.getClient().sendProfileImage(imageBase64);
            }

            showAlert("Succes", "Poza de profil a fost salvata.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Eroare", "Nu s-a putut incarca poza de profil.");
        }
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

        if (Session.getClient() == null || !Session.getClient().isConnected()) {
            messageList.getItems().add(ChatMessage.server("Mod offline: mesajul nu a fost trimis catre server."));
            return;
        }

        String messageToSend = message;
        Thread sendThread = new Thread(() -> {
            try {
                Session.getClient().sendMessage(messageToSend);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> messageList.getItems().add(ChatMessage.server("Eroare: mesajul nu a putut fi trimis.")));
            }
        });

        sendThread.setDaemon(true);
        sendThread.start();
    }

    private void handleApiKeyCommand(String command) {
        String apiKey = command.replaceFirst("(?i)^/api\\s+key", "").trim();

        if (apiKey.isEmpty()) {
            messageList.getItems().add(ChatMessage.server("Scrie comanda asa: /api key cheia_ta_groq"));
            return;
        }

        try {
            apiKeyStore.saveApiKey(apiKey);
            messageList.getItems().add(ChatMessage.server("Cheia API a fost salvata local."));
        } catch (IOException e) {
            messageList.getItems().add(ChatMessage.server("Nu am putut salva cheia API: " + e.getMessage()));
        }
    }

    private void handleAiCommand(String command) {
        String prompt = command.replaceFirst("(?i)^/ai", "").trim();

        if (prompt.isEmpty()) {
            messageList.getItems().add(ChatMessage.server("Scrie o intrebare dupa /ai."));
            return;
        }

        messageList.getItems().add(new ChatMessage(currentUser, prompt, false));
        messageList.getItems().add(ChatMessage.server("AI: Ma gandesc..."));

        Thread aiThread = new Thread(() -> {
            try {
                String apiKey = apiKeyStore.loadApiKey();

                if (apiKey == null) {
                    Platform.runLater(() -> messageList.getItems().add(ChatMessage.server("Nu ai setat cheia API(Groq). Foloseste /api key ....")));
                    return;
                }

                String response = groqAiService.ask(apiKey, prompt);
                Platform.runLater(() -> messageList.getItems().add(ChatMessage.server("AI: " + response)));
            } catch (Exception e) {
                Platform.runLater(() -> messageList.getItems().add(ChatMessage.server("Nu am putut contacta asistentul AI: " + e.getMessage())));
            }
        });

        aiThread.setDaemon(true);
        aiThread.start();
    }

    private void handleServerMessage(String rawMessage) {
        if (rawMessage == null) {
            return;
        }

        if (rawMessage.startsWith("__PROFILE__|")) {
            String[] parts = splitProtocol(rawMessage, 3);
            if (parts.length == 3) {
                String username = unescape(parts[1]);
                String imageBase64 = parts[2];
                saveProfileImageInMemory(username, imageBase64);
            }
            return;
        }

        if (rawMessage.startsWith("__MSG__|")) {
            String[] parts = splitProtocol(rawMessage, 3);
            if (parts.length == 3) {
                messageList.getItems().add(new ChatMessage(unescape(parts[1]), unescape(parts[2]), false));
            }
            return;
        }

        if (rawMessage.startsWith("__SERVER__|")) {
            String[] parts = splitProtocol(rawMessage, 2);
            if (parts.length == 2) {
                messageList.getItems().add(ChatMessage.server(unescape(parts[1])));
            }
            return;
        }

        int separatorIndex = rawMessage.indexOf(": ");
        if (separatorIndex > 0) {
            String sender = rawMessage.substring(0, separatorIndex);
            String text = rawMessage.substring(separatorIndex + 2);
            messageList.getItems().add(new ChatMessage(sender, text, false));
        } else {
            messageList.getItems().add(ChatMessage.server(rawMessage));
        }
    }

    private void saveProfileImageInMemory(String username, String imageBase64) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            profileImages.put(username, image);

            if (username.equals(currentUser)) {
                profileImageView.setImage(image);
            }

            messageList.refresh();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] splitProtocol(String rawMessage, int limit) {
        return rawMessage.split("\\|", limit);
    }

    private String unescape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (escaping) {
                if (ch == 'p') {
                    result.append('|');
                } else if (ch == 'n') {
                    result.append('\n');
                } else {
                    result.append(ch);
                }
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else {
                result.append(ch);
            }
        }

        if (escaping) {
            result.append('\\');
        }

        return result.toString();
    }

    private void handleAddFileAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Alege fisier");

        File selectedFile = fileChooser.showOpenDialog(addFileBtn.getScene().getWindow());

        if (selectedFile == null) {
            return;
        }

        Path filePath = selectedFile.toPath().toAbsolutePath().normalize();
        String fileName = filePath.getFileName().toString();

        if (Session.getCurrentUser() != null) {
            Session.getCurrentUser().addFile(filePath);
            Session.getClient().sendFile(selectedFile);
            Session.getClient().requestFileList();
            refreshFilesView();
        }

        if (Session.getClient() != null && Session.getClient().isConnected()) {
            Thread uploadThread = new Thread(() -> Session.getClient().sendFile(selectedFile));
            uploadThread.setDaemon(true);
            uploadThread.start();
        }

        messageList.getItems().add(ChatMessage.server("Fisier adaugat: " + fileName));
    }

    private void refreshFilesView() {
        filesFlowPane.getChildren().clear();

        if (Session.getCurrentUser() == null) {
            return;
        }

        for (UserFile userFile : Session.getCurrentUser().getFiles()) {
            filesFlowPane.getChildren().add(createFileCard(userFile));
        }
    }

    private VBox createFileCard(UserFile file) {
        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 42px;");

        Label nameLabel = new Label(file.getName());
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(130);
        nameLabel.setStyle("-fx-text-fill: #37474F; -fx-font-size: 13px;");

        VBox fileCard = new VBox(8);
        fileCard.setAlignment(Pos.CENTER);
        fileCard.setPrefWidth(150);
        fileCard.setPrefHeight(130);
        fileCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);"
        );

        fileCard.getChildren().addAll(iconLabel, nameLabel);
        return fileCard;
    }
    
    private VBox createFileCard(String fileName) {
        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 42px;");

        Label nameLabel = new Label(fileName);
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(130);
        nameLabel.setStyle("-fx-text-fill: #37474F; -fx-font-size: 13px;");

        VBox fileCard = new VBox(8);
        fileCard.setAlignment(Pos.CENTER);
        fileCard.setPrefWidth(150);
        fileCard.setPrefHeight(130);
        fileCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);"
        );

        fileCard.getChildren().addAll(iconLabel, nameLabel);
        return fileCard;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class ChatMessageCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            if (item.serverMessage) {
                Label serverLabel = new Label("SERVER: " + item.text);
                serverLabel.setTextFill(Color.web("#546E7A"));
                serverLabel.setWrapText(true);
                serverLabel.setStyle("-fx-font-size: 12px; -fx-font-style: italic;");
                setGraphic(serverLabel);
                setText(null);
                return;
            }

            HBox row = new HBox(10);
            row.setAlignment(Pos.TOP_LEFT);
            row.setPadding(new Insets(6, 8, 6, 8));

            VBox avatarBox = new VBox();
            avatarBox.setAlignment(Pos.CENTER);
            avatarBox.getChildren().add(createAvatar(item.sender));

            Label senderLabel = new Label(item.sender);
            senderLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            senderLabel.setTextFill(Color.web("#263238"));

            Label textLabel = new Label(item.text);
            textLabel.setWrapText(true);
            textLabel.setTextFill(Color.web("#263238"));
            textLabel.setMaxWidth(210);

            VBox content = new VBox(2, senderLabel, textLabel);
            row.getChildren().addAll(avatarBox, content);

            setGraphic(row);
            setText(null);
        }

        private javafx.scene.Node createAvatar(String username) {
            Image image = profileImages.get(username);

            if (image != null && !image.isError()) {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(34);
                imageView.setFitHeight(34);
                imageView.setPreserveRatio(false);
                imageView.setClip(new Circle(17, 17, 17));
                return imageView;
            }

            Label initials = new Label(getInitial(username));
            initials.setMinSize(34, 34);
            initials.setMaxSize(34, 34);
            initials.setAlignment(Pos.CENTER);
            initials.setTextFill(Color.WHITE);
            initials.setFont(Font.font("System", FontWeight.BOLD, 13));
            initials.setStyle("-fx-background-color: #2962FF; -fx-background-radius: 17;");
            return initials;
        }

        private String getInitial(String username) {
            if (username == null || username.isBlank()) {
                return "?";
            }
            return username.trim().substring(0, 1).toUpperCase();
        }
    }

    public static class ChatMessage {
        private final String sender;
        private final String text;
        private final boolean serverMessage;

        public ChatMessage(String sender, String text, boolean serverMessage) {
            this.sender = sender;
            this.text = text;
            this.serverMessage = serverMessage;
        }

        public static ChatMessage server(String text) {
            return new ChatMessage("SERVER", text, true);
        }
    }
    
    
    private void refreshFilesView(List<String> files) {
        filesFlowPane.getChildren().clear();

        for (String file : files) {
            filesFlowPane.getChildren().add(createFileCard(file));
        }
    }
}
