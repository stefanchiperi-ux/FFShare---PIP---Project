package interfata_drive;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {

    @FXML private TextField nameField; // fx:id="nameField" în Scene Builder
    @FXML private TextField userField; // fx:id="userField"
    @FXML private PasswordField passField; // fx:id="passField"
    @FXML private Button loginBtn; // fx:id="loginBtn"
    @FXML private Button toggleBtn; // fx:id="toggleBtn" (Butonul sub cel de Login)

    private boolean isRegisterMode = false;

    @FXML
    public void initialize() {
        // La început suntem pe modul "Conectare", deci ascundem câmpul de nume
        nameField.setVisible(false);
        nameField.setManaged(false);

        // Setăm acțiunea pentru butonul principal
        loginBtn.setOnAction(event -> {
            if (isRegisterMode) {
                handleRegister();
            } else {
                handleLogin();
            }
        });

        // Setăm acțiunea pentru butonul de schimbare mod
        toggleBtn.setOnAction(event -> toggleMode());
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        
        if (isRegisterMode) {
            nameField.setVisible(true);
            nameField.setManaged(true);
            loginBtn.setText("Înregistrare");
            toggleBtn.setText("Ai deja cont? Conectează-te");
        } else {
            nameField.setVisible(false);
            nameField.setManaged(false);
            loginBtn.setText("Conectare");
            toggleBtn.setText("Creează cont nou");
        }
    }

    private void handleLogin() {
        String user = userField.getText();
        String pass = passField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Eroare", "Te rugăm să completezi utilizatorul și parola!");
            return;
        }

        if (DatabaseHandler.validateLogin(user, pass)) {
            System.out.println("Autentificare reușită!");
            incarcaDashboard();
        } else {
            showAlert("Eroare", "Utilizator sau parolă incorectă!");
        }
    }

    private void handleRegister() {
        String name = nameField.getText();
        String user = userField.getText();
        String pass = passField.getText();

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            showAlert("Eroare", "Toate câmpurile sunt obligatorii pentru înregistrare!");
            return;
        }
        
        if (name.length() < 6 || pass.length() < 6) {
        	showAlert("Eroare", "Username-ul si parola trebuie sa contina minim 6 caractere!");
        	return;
        }
        
        if (!pass.matches(".*\\d.*") || !pass.matches(".*[^a-zA-Z0-9].*")) {
        	showAlert("Eroare", "Parola trebuie sa contina minim un caracter special si minim o cifra!");
        	return;
        }
        
        if (DatabaseHandler.registerUser(name, user, pass)) {
            showAlert("Succes", "Cont creat cu succes! Acum te poți loga.");
            toggleMode(); // Trecem automat înapoi la Login
        } else {
            showAlert("Eroare", "Utilizatorul există deja!");
        }
    }

    private void incarcaDashboard() {
        try {
            // Încarcă Dashboard.fxml (asigură-te că e în src/main/resources/interfata_drive/)
            var resource = getClass().getResource("/interfata_drive/Dashboard.fxml");
            if (resource == null) throw new IOException("Dashboard.fxml nu a fost găsit!");

            Parent root = FXMLLoader.load(resource);
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FFShare - Bine ai venit");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Eroare", "Nu s-a putut încărca ecranul de bun venit.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}