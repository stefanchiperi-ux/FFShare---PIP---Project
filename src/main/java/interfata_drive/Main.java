package interfata_drive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Pregătim baza de date
        DatabaseHandler.initDatabase();

        try {
            // 2. Încărcăm fișierul de design
            var resource = getClass().getResource("/interfata_drive/LoginDesign.fxml");
            if (resource == null) {
                throw new RuntimeException("Eroare: LoginDesign.fxml nu a fost găsit în resurse!");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            // 3. Afișăm fereastra
            Scene scene = new Scene(root);
            primaryStage.setTitle("FFShare - Autentificare");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}