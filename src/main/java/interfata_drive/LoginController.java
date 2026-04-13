package interfata_drive;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class LoginController {

    @FXML
    private TextField userField;

    @FXML
    private PasswordField passField;

    @FXML
    private Button loginBtn;

    @FXML
    public void initialize() {
        // Logica pentru butonul de Conectare
        loginBtn.setOnAction(event -> {
            String username = userField.getText();
            String password = passField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Eroare", "Te rugăm să completezi ambele câmpuri!");
            } else {
                System.out.println("Logare încercată pentru: " + username);
                
                // Exemplu simplu de verificare
                if (username.equals("admin") && password.equals("1234")) {
                    System.out.println("Succes! Bine ai venit, " + username);
                } else {
                    showAlert("Eroare", "Utilizator sau parolă incorectă!");
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}