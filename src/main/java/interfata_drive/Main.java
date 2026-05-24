package interfata_drive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clasa principala pentru aplicatia JavaFX.
 */
public class Main extends Application {

    /**
     * Initializeaza baza de date si afiseaza fereastra de login.
     *
     * @param primaryStage fereastra principala a aplicatiei
     */
    @Override
    public void start(Stage primaryStage) {
        DatabaseHandler.initDatabase();

        try {
            var resource = getClass().getResource("/interfata_drive/LoginDesign.fxml");
            if (resource == null) {
                throw new RuntimeException("Eroare: LoginDesign.fxml nu a fost gasit in resurse!");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle("FFShare - Autentificare");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Porneste aplicatia JavaFX.
     *
     * @param args argumentele primite din linia de comanda
     */
    public static void main(String[] args) {
        launch(args);
    }
}
