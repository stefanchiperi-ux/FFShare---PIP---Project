package interfata_drive;

import ai.ApiKeyStore;
import ai.GroqAiService;
import core.Session;
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import java.util.List;

/**
 * Controleaza ecranul principal al aplicatiei.
 */
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
    @FXML private TextField searchField;

    private String currentUser;
    private final Map<String, Image> profileImages = new HashMap<>();
    private final ApiKeyStore apiKeyStore = new ApiKeyStore();
    private final GroqAiService groqAiService = new GroqAiService();
    private List<String> allFiles = new ArrayList<>();

    @FXML
    /**
     * Pregateste lista de mesaje, butoanele si lista de fisiere.
     */
    public void initialize() {
        messageList.setCellFactory(listView -> new ChatMessageCell());

        if (Session.getClient() != null) {
            Session.getClient().setOnMessageReceived(message -> Platform.runLater(() -> handleServerMessage(message)));
            Session.getClient().setOnFileListReceived(files -> Platform.runLater(() -> {
                allFiles = new ArrayList<>(files);
                refreshFilesView();
            }));
            Session.getClient().requestFileList();
        }

        sendBtn.setOnAction(event -> sendMessageAction());
        addFileBtn.setOnAction(event -> handleAddFileAction());
        writeMessageField.setOnAction(event -> sendMessageAction());

        filesBtn.setOnAction(event -> showFilesPage());
        profileBtn.setOnAction(event -> showProfilePage());
        chooseProfileImageBtn.setOnAction(event -> chooseProfileImage());
        searchField.textProperty().addListener((obs, oldText, newText) -> refreshFilesView());

        showFilesPage();
        refreshFilesView();
        messageList.getItems().add(ChatMessage.server("Scrie /ai urmat de intrebare pentru asistent sau /api key cheia_ta_groq pentru conectare."));
    }

    /**
     * Seteaza datele utilizatorului logat in interfata.
     *
     * @param fullName numele complet al utilizatorului
     */
    public void setUserData(String fullName) {
        this.currentUser = fullName;
        userNameLabel.setText(fullName);
        profileNameLabel.setText(fullName);
    }

    /**
     * Afiseaza pagina cu fisiere.
     */
    private void showFilesPage() {
        filesPage.setVisible(true);
        filesPage.setManaged(true);
        profilePage.setVisible(false);
        profilePage.setManaged(false);
        filesBtn.setStyle(activeMenuStyle());
        profileBtn.setStyle(inactiveMenuStyle());
        searchField.setVisible(true);
        searchField.setManaged(true);
    }

    /**
     * Afiseaza pagina de profil.
     */
    private void showProfilePage() {
        filesPage.setVisible(false);
        filesPage.setManaged(false);
        profilePage.setVisible(true);
        profilePage.setManaged(true);
        filesBtn.setStyle(inactiveMenuStyle());
        profileBtn.setStyle(activeMenuStyle());
        searchField.setVisible(false);
        searchField.setManaged(false);
    }

    /**
     * Intoarce stilul pentru butonul activ din meniu.
     *
     * @return stil CSS pentru buton activ
     */
    private String activeMenuStyle() {
        return "-fx-background-color: #2962FF; -fx-text-fill: white; -fx-background-radius: 8;";
    }

    /**
     * Intoarce stilul pentru butonul inactiv din meniu.
     *
     * @return stil CSS pentru buton inactiv
     */
    private String inactiveMenuStyle() {
        return "-fx-background-color: transparent; -fx-text-fill: #607D8B;";
    }

    /**
     * Permite alegerea si trimiterea pozei de profil.
     */
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

    /**
     * Proceseaza mesajul scris si il trimite sau ruleaza o comanda.
     */
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

    /**
     * Salveaza cheia API primita prin comanda din chat.
     *
     * @param command comanda completa scrisa de utilizator
     */
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

    /**
     * Trimite intrebarea catre asistentul AI.
     *
     * @param command comanda completa cu intrebarea
     */
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

    /**
     * Interpreteaza mesajele primite de la server.
     *
     * @param rawMessage mesajul in formatul protocolului
     */
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

    /**
     * Salveaza temporar poza de profil primita.
     *
     * @param username utilizatorul pozei
     * @param imageBase64 imaginea codata Base64
     */
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

    /**
     * Imparte un mesaj primit dupa separatorul protocolului.
     *
     * @param rawMessage mesajul complet
     * @param limit numarul maxim de parti
     * @return partile mesajului
     */
    private String[] splitProtocol(String rawMessage, int limit) {
        return rawMessage.split("\\|", limit);
    }

    /**
     * Decodeaza textul primit prin protocol.
     *
     * @param value textul escapat
     * @return textul normal
     */
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

    /**
     * Alege si trimite un fisier catre server.
     */
    private void handleAddFileAction() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Alege fisier");

        File selectedFile = fileChooser.showOpenDialog(addFileBtn.getScene().getWindow());

        if (selectedFile == null) {
            return;
        }

        String fileName = selectedFile.getName();

        if (Session.getClient() == null || !Session.getClient().isConnected()) {
            showAlert("Eroare", "Nu exista conexiune la server pentru incarcarea fisierului.");
            return;
        }

        showFilesPage();
        addFileBtn.setDisable(true);
        messageList.getItems().add(ChatMessage.server("Se incarca fisierul: " + fileName));

        Thread uploadThread = new Thread(() -> {
            try {
                Session.getClient().sendFile(selectedFile);
                Session.getClient().requestFileList();
            } finally {
                Platform.runLater(() -> addFileBtn.setDisable(false));
            }
        });
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    /**
     * Cere descarcarea unui fisier selectat.
     *
     * @param filePath calea fisierului pe server
     */
    private void handleDownloadFileAction(String filePath) {
        if (Session.getClient() == null || !Session.getClient().isConnected()) {
            showAlert("Eroare", "Nu exista conexiune la server pentru descarcarea fisierului.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salveaza fisierul");
        fileChooser.setInitialFileName(getDisplayFileName(filePath));

        File destination = fileChooser.showSaveDialog(filesFlowPane.getScene().getWindow());

        if (destination == null) {
            return;
        }

        Session.getClient().requestFileDownload(filePath, destination);
        messageList.getItems().add(ChatMessage.server("Se descarca fisierul: " + getDisplayFileName(filePath)));
    }

    /**
     * Reface lista vizuala de fisiere dupa cautare sau actualizare.
     */
    private void refreshFilesView() {
        filesFlowPane.getChildren().clear();

        String query = normalizeSearchText(searchField == null ? "" : searchField.getText());
        boolean hasVisibleFiles = false;

        for (String file : allFiles) {
            if (matchesSearch(file, query)) {
                filesFlowPane.getChildren().add(createFileCard(file));
                hasVisibleFiles = true;
            }
        }

        if (!hasVisibleFiles) {
            filesFlowPane.getChildren().add(createEmptyFilesLabel(query));
        }
    }

    /**
     * Verifica daca un fisier corespunde cautarii.
     *
     * @param filePath calea fisierului
     * @param query textul cautat
     * @return true daca fisierul trebuie afisat
     */
    private boolean matchesSearch(String filePath, String query) {
        if (query.isEmpty()) {
            return true;
        }

        return normalizeSearchText(filePath).contains(query)
                || normalizeSearchText(getDisplayFileName(filePath)).contains(query);
    }

    /**
     * Normalizeaza textul folosit pentru cautare.
     *
     * @param value textul original
     * @return textul cu litere mici si fara spatii la margini
     */
    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Creeaza mesajul afisat cand nu exista fisiere.
     *
     * @param query textul cautat
     * @return eticheta pentru stare goala
     */
    private Label createEmptyFilesLabel(String query) {
        String text = query.isEmpty() ? "Nu exista fisiere incarcate." : "Nu s-au gasit fisiere pentru cautarea introdusa.";
        Label emptyLabel = new Label(text);
        emptyLabel.setTextFill(Color.web("#607D8B"));
        emptyLabel.setStyle("-fx-font-size: 13px;");
        return emptyLabel;
    }


    /**
     * Creeaza cardul pentru un fisier din lista.
     *
     * @param filePath calea fisierului
     * @return cardul vizual al fisierului
     */
    private VBox createFileCard(String filePath) {
        Label iconLabel = new Label("📄");
        iconLabel.setStyle("-fx-font-size: 42px;");

        Label nameLabel = new Label(getDisplayFileName(filePath));
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(130);
        nameLabel.setStyle("-fx-text-fill: #37474F; -fx-font-size: 13px;");

        Button downloadBtn = new Button("Descarca");
        downloadBtn.setPrefWidth(110);
        downloadBtn.setStyle(
                "-fx-background-color: #2962FF;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 14;" +
                "-fx-font-size: 12px;" +
                "-fx-cursor: hand;"
        );
        downloadBtn.setOnAction(event -> handleDownloadFileAction(filePath));

        VBox fileCard = new VBox(8);
        fileCard.setAlignment(Pos.CENTER);
        fileCard.setPrefWidth(150);
        fileCard.setPrefHeight(165);
        fileCard.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);"
        );

        fileCard.getChildren().addAll(iconLabel, nameLabel, downloadBtn);
        return fileCard;
    }

    /**
     * Extrage numele fisierului din cale.
     *
     * @param filePath calea completa sau relativa
     * @return numele afisat in interfata
     */
    private String getDisplayFileName(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return "Fisier fara nume";
        }

        int slashIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return slashIndex >= 0 ? filePath.substring(slashIndex + 1) : filePath;
    }

    /**
     * Afiseaza o alerta simpla pentru utilizator.
     *
     * @param title titlul alertei
     * @param message mesajul alertei
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Celula custom pentru afisarea mesajelor din chat.
     */
    private class ChatMessageCell extends ListCell<ChatMessage> {
        @Override
        /**
         * Actualizeaza continutul unei celule din lista.
         *
         * @param item mesajul afisat
         * @param empty true daca celula este goala
         */
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

        /**
         * Creeaza avatarul pentru un utilizator.
         *
         * @param username numele utilizatorului
         * @return nodul grafic pentru avatar
         */
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

        /**
         * Calculeaza initiala afisata in avatar.
         *
         * @param username numele utilizatorului
         * @return prima litera sau semnul intrebarii
         */
        private String getInitial(String username) {
            if (username == null || username.isBlank()) {
                return "?";
            }
            return username.trim().substring(0, 1).toUpperCase();
        }
    }

    /**
     * Model simplu pentru un mesaj din chat.
     */
    public static class ChatMessage {
        private final String sender;
        private final String text;
        private final boolean serverMessage;

        /**
         * Creeaza un mesaj de chat.
         *
         * @param sender expeditorul mesajului
         * @param text continutul mesajului
         * @param serverMessage true daca mesajul este de sistem
         */
        public ChatMessage(String sender, String text, boolean serverMessage) {
            this.sender = sender;
            this.text = text;
            this.serverMessage = serverMessage;
        }

        /**
         * Creeaza un mesaj trimis de server.
         *
         * @param text textul mesajului
         * @return mesajul de server
         */
        public static ChatMessage server(String text) {
            return new ChatMessage("SERVER", text, true);
        }
    }
}
