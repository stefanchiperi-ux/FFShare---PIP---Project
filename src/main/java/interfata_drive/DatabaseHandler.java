package interfata_drive;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestioneaza baza de date pentru utilizatori.
 */
public class DatabaseHandler {
    private static final String DEFAULT_URL = "jdbc:sqlite:utilizatori.db";
    private static String databaseUrl = DEFAULT_URL;

    /**
     * Creeaza o conexiune la baza de date curenta.
     *
     * @return conexiunea SQL
     * @throws SQLException daca baza de date nu poate fi deschisa
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    /**
     * Schimba baza de date folosita in teste.
     *
     * @param databasePath calea bazei de date temporare
     */
    static void useDatabaseForTesting(Path databasePath) {
        databaseUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    /**
     * Reseteaza baza de date dupa teste.
     */
    static void resetDatabaseForTesting() {
        databaseUrl = DEFAULT_URL;
    }

    /**
     * Creeaza tabelul de utilizatori daca nu exista.
     */
    public static void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "fullname TEXT," +
                     "username TEXT UNIQUE NOT NULL," +
                     "password TEXT NOT NULL);";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inregistreaza un utilizator nou.
     *
     * @param fullname numele complet al utilizatorului
     * @param user username-ul ales
     * @param pass parola aleasa
     * @return true daca utilizatorul a fost salvat
     */
    public static boolean registerUser(String fullname, String user, String pass) {
        String sql = "INSERT INTO users(fullname, username, password) VALUES(?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullname);
            pstmt.setString(2, user);
            pstmt.setString(3, pass);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Cauta numele complet dupa username si parola.
     *
     * @param user username-ul introdus
     * @param pass parola introdusa
     * @return numele complet sau null daca datele sunt gresite
     */
    public static String getFullName(String user, String pass) {
        String sql = "SELECT fullname FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("fullname");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
