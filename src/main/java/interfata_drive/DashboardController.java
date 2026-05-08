package interfata_drive;

import core.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class DashboardController {

    @FXML private Label userNameLabel; // Acesta va fi ID-ul din Scene Builder
    @FXML private TextField writeMessageField; // fx:id="nameField" în Scene Builder
    @FXML private Button sendBtn; // 
    @FXML private ListView<String> messageList;
    
    @FXML
    public void initialize() {
    	Session.getClient().setOnMessageReceived(message -> {
            addMessage(message);
        });

        sendBtn.setOnAction(event -> {
        	sendMessageAction();
        });

    }

    public void setUserData(String fullName) {
        userNameLabel.setText(fullName);
    }
    
    private void sendMessageAction() {
    	String message = writeMessageField.getText();
    	Session.getClient().sendMessage(message);
    }
    
    public void addMessage(String message) {
        messageList.getItems().add(message);
    }
}