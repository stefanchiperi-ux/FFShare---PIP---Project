package interfata_drive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Încarcă fișierul FXML pe care l-ai creat
            FXMLLoader loader = new FXMLLoader(getClass().getResource("MainDesign.fxml"));
            Parent root = loader.load();

            // Creăm scena folosind design-ul încărcat
            Scene scene = new Scene(root);

            primaryStage.setTitle("FFShare - Autentificare");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false); // Îl ținem fix pentru a păstra designul curat
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("EROARE: Nu s-a putut încărca MainDesign.fxml. Verifică dacă fișierul este în același folder cu această clasă!");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}