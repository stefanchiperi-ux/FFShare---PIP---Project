package interfata_drive;

import javafx.application.Platform;
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

import client.Client;
import core.Session;
import core.User;

public class LoginController {

    @FXML private TextField nameField;
    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private Button loginBtn;
    @FXML private Button toggleBtn;

    private boolean isRegisterMode = false;

    @FXML
    public void initialize() {
        nameField.setVisible(false);
        nameField.setManaged(false);

        loginBtn.setOnAction(event -> {
            if (isRegisterMode) {
                handleRegister();
            } else {
                handleLogin();
            }
        });

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
            showAlert("Eroare", "Username-ul și parola sunt obligatorii!");
            return;
        }

        loginBtn.setDisable(true);
        toggleBtn.setDisable(true);

        Thread loginThread = new Thread(() -> {
            String fullName = DatabaseHandler.getFullName(user, pass);

            if (fullName == null) {
                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    toggleBtn.setDisable(false);
                    showAlert("Eroare", "Utilizator sau parolă incorectă!");
                });
                return;
            }

            Client client = new Client(fullName);

            try {
                client.connect();

                Session.setClient(client);
                Session.setCurrentUser(new User(fullName));

                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    toggleBtn.setDisable(false);
                    incarcaDashboard(fullName);
                });

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    loginBtn.setDisable(false);
                    toggleBtn.setDisable(false);
                    showAlert("Eroare", "Nu s-a putut conecta la server!");
                });
            }
        });

        loginThread.setDaemon(true);
        loginThread.start();
    }

    private void handleRegister() {
        String name = nameField.getText();
        String user = userField.getText();
        String pass = passField.getText();

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            showAlert("Eroare", "Toate câmpurile sunt obligatorii pentru înregistrare!");
            return;
        }

        if (user.length() < 6 || pass.length() < 6) {
            showAlert("Eroare", "Username-ul și parola trebuie să conțină minim 6 caractere!");
            return;
        }

        if (!pass.matches(".*\\d.*") || !pass.matches(".*[^a-zA-Z0-9].*")) {
            showAlert("Eroare", "Parola trebuie să conțină minim o cifră și minim un caracter special!");
            return;
        }

        loginBtn.setDisable(true);
        toggleBtn.setDisable(true);

        Thread registerThread = new Thread(() -> {
            boolean success = DatabaseHandler.registerUser(name, user, pass);

            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                toggleBtn.setDisable(false);

                if (success) {
                    showAlert("Succes", "Cont creat cu succes! Acum te poți loga.");
                    toggleMode();
                } else {
                    showAlert("Eroare", "Utilizatorul există deja!");
                }
            });
        });

        registerThread.setDaemon(true);
        registerThread.start();
    }

    private void incarcaDashboard(String fullName) {
        try {
            var resource = getClass().getResource("/interfata_drive/Dashboard.fxml");

            if (resource == null) {
                throw new IOException("Dashboard.fxml nu a fost găsit!");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();
            dashboardController.setUserData(fullName);

            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("FFShare - Bine ai venit, " + fullName);
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Eroare", "Nu s-a putut încărca Dashboard-ul.");
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