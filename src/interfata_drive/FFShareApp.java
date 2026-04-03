package interfata_drive;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class FFShareApp extends Application {

    // Elemente de UI pe care trebuie să le modificăm dinamic
    private Label pageTitleLabel;
    private FlowPane filesGrid;
    private VBox chatPanel;
    private VBox chatMessagesBox;
    private ScrollPane chatScrollPane;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F8F9FA; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // 1. Meniul Lateral (Stânga)
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // 2. Zona Centrală (Fișiere)
        VBox centerArea = createCenterArea();
        root.setCenter(centerArea);

        // 3. Panoul de Chat (Dreapta)
        chatPanel = createChatPanel();
        chatPanel.setVisible(false); // Ascuns by default
        chatPanel.setManaged(false); // Nu ocupă spațiu când e ascuns
        root.setRight(chatPanel);

        Scene scene = new Scene(root, 1100, 700);
        primaryStage.setTitle("FFShare - Conversie JavaFX");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(30, 20, 20, 20));
        sidebar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E0E0E0; -fx-border-width: 0 1 0 0;");
        sidebar.setPrefWidth(220);

        // Logo
        Label logo = new Label("FFShare");
        logo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        logo.setTextFill(Color.web("#5C6BC0"));

        // Buton Trimite Mesaj (Deschide/Închide Chat)
        Button btnSendMessage = new Button("Trimite mesaj");
        btnSendMessage.setMaxWidth(Double.MAX_VALUE);
        btnSendMessage.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px; -fx-background-radius: 5px;");
        btnSendMessage.setCursor(javafx.scene.Cursor.HAND);
        
        // Logica pentru deschidere/închidere chat
        btnSendMessage.setOnAction(e -> {
            boolean isNowVisible = !chatPanel.isVisible();
            chatPanel.setVisible(isNowVisible);
            chatPanel.setManaged(isNowVisible);
        });

        VBox menuButtons = new VBox(5);
        
        // Crearea butoanelor de navigație
        Button btnMyFiles = createMenuButton("Fișierele mele");
        Button btnShared = createMenuButton("Partajate cu mine");
        Button btnRecent = createMenuButton("Recente");
        Button btnFavs = createMenuButton("Favorite");
        Button btnTrash = createMenuButton("Coș de gunoi");

        menuButtons.getChildren().addAll(btnMyFiles, btnShared, btnRecent, btnFavs, btnTrash);
        sidebar.getChildren().addAll(logo, btnSendMessage, menuButtons);

        return sidebar;
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.BASELINE_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333333; -fx-padding: 10px 15px; -fx-font-size: 14px;");
        btn.setCursor(javafx.scene.Cursor.HAND);

        // Efect de hover
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #F0F2F5; -fx-text-fill: #333333; -fx-padding: 10px 15px; -fx-font-size: 14px; -fx-background-radius: 5px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333333; -fx-padding: 10px 15px; -fx-font-size: 14px;"));

        // Logica la click: schimbă titlul paginii
        btn.setOnAction(e -> pageTitleLabel.setText(text));

        return btn;
    }

    private VBox createCenterArea() {
        VBox center = new VBox(20);
        center.setPadding(new Insets(30));

        // Header (Titlu + Search)
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        pageTitleLabel = new Label("Fișierele mele");
        pageTitleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchBox = new TextField();
        searchBox.setPromptText("Caută în fișiere...");
        searchBox.setPrefWidth(250);
        searchBox.setStyle("-fx-background-radius: 20px; -fx-padding: 8px 15px; -fx-border-color: #E0E0E0; -fx-border-radius: 20px; -fx-background-color: white;");

        header.getChildren().addAll(pageTitleLabel, spacer, searchBox);

        // Grid de fișiere
        filesGrid = new FlowPane(15, 15);
        
        // Adăugăm câteva fișiere mock
        filesGrid.getChildren().addAll(
            createFileCard("Documente", "12 fișiere • Azi"),
            createFileCard("Poze Vacanță", "48 fișiere • Ieri"),
            createFileCard("Proiect_Final.pdf", "2.4 MB • 2 Apr"),
            createFileCard("Prezentare.pptx", "5.1 MB • 1 Apr"),
            createFileCard("Video_Tutorial.mp4", "124 MB • 29 Mar"),
            createFileCard("Screenshot.png", "890 KB • 28 Mar"),
            createFileCard("Date_Importante.xlsx", "1.2 MB • 25 Mar")
        );

        ScrollPane scrollPane = new ScrollPane(filesGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: #F8F9FA;");
        scrollPane.setBorder(Border.EMPTY);

        center.getChildren().addAll(header, scrollPane);
        return center;
    }

    private VBox createFileCard(String title, String details) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        card.setPrefSize(200, 120);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        Label detailsLabel = new Label(details);
        detailsLabel.setTextFill(Color.GRAY);
        detailsLabel.setFont(Font.font("Segoe UI", 12));

        card.getChildren().addAll(titleLabel, detailsLabel);
        
        // Efect de hover pe carduri
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 3); -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);"));

        return card;
    }

    private VBox createChatPanel() {
        VBox chat = new VBox();
        chat.setPrefWidth(300);
        chat.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 0 1;");

        // Header Chat
        VBox chatHeader = new VBox(2);
        chatHeader.setPadding(new Insets(15));
        chatHeader.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
        
        Label chatName = new Label("Andrei Popescu");
        chatName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label chatStatus = new Label("Online");
        chatStatus.setTextFill(Color.web("#2E7D32")); // Verde
        chatHeader.getChildren().addAll(chatName, chatStatus);

        // Zona de Mesaje
        chatMessagesBox = new VBox(15);
        chatMessagesBox.setPadding(new Insets(15));
        
        // Mesaje inițiale
        addMessageToChat("Andrei", "Hey! Am trimis documentele pe care le-ai cerut.", "10:30", false);
        addMessageToChat("Tu", "Mulțumesc! Le verific acum.", "10:32", true);

        chatScrollPane = new ScrollPane(chatMessagesBox);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-background-color: transparent; -fx-background: white;");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        // Input Box (Jos)
        HBox inputBox = new HBox(10);
        inputBox.setPadding(new Insets(15));
        inputBox.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 1 0 0 0;");
        
        TextField messageInput = new TextField();
        messageInput.setPromptText("Scrie un mesaj...");
        messageInput.setStyle("-fx-background-radius: 15px; -fx-padding: 8px 12px;");
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        
        Button btnSend = new Button("Trimite");
        btnSend.setStyle("-fx-background-color: #5C6BC0; -fx-text-fill: white; -fx-background-radius: 15px;");
        btnSend.setCursor(javafx.scene.Cursor.HAND);

        // Logica pentru trimiterea mesajului
        Runnable sendMessageAction = () -> {
            String text = messageInput.getText().trim();
            if (!text.isEmpty()) {
                addMessageToChat("Tu", text, "Acum", true);
                messageInput.clear();
                // Scroll automat la ultimul mesaj
                chatScrollPane.layout();
                chatScrollPane.setVvalue(1.0);
            }
        };

        btnSend.setOnAction(e -> sendMessageAction.run());
        messageInput.setOnAction(e -> sendMessageAction.run()); // Funcționează și la apăsarea tastei Enter

        inputBox.getChildren().addAll(messageInput, btnSend);
        chat.getChildren().addAll(chatHeader, chatScrollPane, inputBox);

        return chat;
    }

    private void addMessageToChat(String sender, String text, String time, boolean isMe) {
        VBox messageContainer = new VBox(3);
        
        Label senderLabel = new Label(sender + " (" + time + ")");
        senderLabel.setFont(Font.font("Segoe UI", 10));
        senderLabel.setTextFill(Color.GRAY);
        
        Label messageText = new Label(text);
        messageText.setWrapText(true);
        messageText.setPadding(new Insets(8, 12, 8, 12));
        messageText.setMaxWidth(220);
        
        if (isMe) {
            messageContainer.setAlignment(Pos.CENTER_RIGHT);
            messageText.setStyle("-fx-background-color: #E8EAF6; -fx-background-radius: 10px; -fx-text-fill: black;");
        } else {
            messageContainer.setAlignment(Pos.CENTER_LEFT);
            messageText.setStyle("-fx-background-color: #F1F3F4; -fx-background-radius: 10px; -fx-text-fill: black;");
        }
        
        messageContainer.getChildren().addAll(senderLabel, messageText);
        chatMessagesBox.getChildren().add(messageContainer);
    }
}