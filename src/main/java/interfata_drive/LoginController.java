package interfata_drive;

import client.Client;
import core.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controleaza ecranul de autentificare si inregistrare.
 */
public class LoginController {

    @FXML private TextField nameField;
    @FXML private TextField userField;
    @FXML private PasswordField passField;
    @FXML private Button loginBtn;
    @FXML private Button toggleBtn;

    private boolean isRegisterMode = false;

    /**
     * Pregateste evenimentele pentru butoanele din formular.
     */
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

    /**
     * Schimba formularul intre login si inregistrare.
     */
    private void toggleMode() {
        isRegisterMode = !isRegisterMode;

        if (isRegisterMode) {
            nameField.setVisible(true);
            nameField.setManaged(true);
            loginBtn.setText("Inregistrare");
            toggleBtn.setText("Ai deja cont? Conecteaza-te");
        } else {
            nameField.setVisible(false);
            nameField.setManaged(false);
            loginBtn.setText("Conectare");
            toggleBtn.setText("Creeaza cont nou");
        }
    }

    /**
     * Verifica datele si conecteaza utilizatorul.
     */
    private void handleLogin() {
        String user = userField.getText();
        String pass = passField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert("Eroare", "Username-ul si parola sunt obligatorii!");
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
                    showAlert("Eroare", "Utilizator sau parola incorecta!");
                });
                return;
            }

            Client client = new Client(fullName);

            try {
                client.connect();

                Session.setClient(client);

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

    /**
     * Verifica datele si creeaza un cont nou.
     */
    private void handleRegister() {
        String name = nameField.getText();
        String user = userField.getText();
        String pass = passField.getText();

        if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            showAlert("Eroare", "Toate campurile sunt obligatorii pentru inregistrare!");
            return;
        }

        if (user.length() < 6 || pass.length() < 6) {
            showAlert("Eroare", "Username-ul si parola trebuie sa contina minim 6 caractere!");
            return;
        }

        if (!pass.matches(".*\\d.*") || !pass.matches(".*[^a-zA-Z0-9].*")) {
            showAlert("Eroare", "Parola trebuie sa contina minim o cifra si minim un caracter special!");
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
                    showAlert("Succes", "Cont creat cu succes! Acum te poti loga.");
                    toggleMode();
                } else {
                    showAlert("Eroare", "Utilizatorul exista deja!");
                }
            });
        });

        registerThread.setDaemon(true);
        registerThread.start();
    }

    /**
     * Incarca ecranul principal dupa autentificare.
     *
     * @param fullName numele complet al utilizatorului
     */
    private void incarcaDashboard(String fullName) {
        try {
            var resource = getClass().getResource("/interfata_drive/Dashboard.fxml");

            if (resource == null) {
                throw new IOException("Dashboard.fxml nu a fost gasit!");
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
            showAlert("Eroare", "Nu s-a putut incarca Dashboard-ul.");
        }
    }

    /**
     * Afiseaza un mesaj simplu pentru utilizator.
     *
     * @param title titlul ferestrei
     * @param message mesajul afisat
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
