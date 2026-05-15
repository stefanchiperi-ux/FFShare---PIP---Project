package interfata_drive;

import core.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private TextField writeMessageField;
    @FXML private Button sendBtn;
    @FXML private ListView<String> messageList;

    @FXML
    public void initialize() {

        if (Session.getClient() != null) {
            Session.getClient().setOnMessageReceived(message -> {
                Platform.runLater(() -> addMessage(message));
            });
        }

        sendBtn.setOnAction(event -> sendMessageAction());
    }

    public void setUserData(String fullName) {
        userNameLabel.setText(fullName);
    }

    private void sendMessageAction() {
        String message = writeMessageField.getText();

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        writeMessageField.clear();

        Thread sendThread = new Thread(() -> {
            try {
                Session.getClient().sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    addMessage("Eroare: mesajul nu a putut fi trimis.");
                });
            }
        });

        sendThread.setDaemon(true);
        sendThread.start();

        addMessage("Eu: " + message);
    }

    public void addMessage(String message) {
        messageList.getItems().add(message);
    }
}