package interfata_drive;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML
    private Label userNameLabel; // Acesta va fi ID-ul din Scene Builder

    public void setUserData(String fullName) {
        userNameLabel.setText(fullName);
    }
}