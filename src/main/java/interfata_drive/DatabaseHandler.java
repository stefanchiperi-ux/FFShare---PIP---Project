package interfata_drive;

import java.nio.file.Path;
import java.sql.*;

public class DatabaseHandler {
    private static final String DEFAULT_URL = "jdbc:sqlite:utilizatori.db";
    private static String databaseUrl = DEFAULT_URL;

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    static void useDatabaseForTesting(Path databasePath) {
        databaseUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
    }

    static void resetDatabaseForTesting() {
        databaseUrl = DEFAULT_URL;
    }

    public static void initDatabase() {
        // Creăm tabelul cu coloana suplimentară 'fullname'
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

    public static boolean registerUser(String fullname, String user, String pass) {
        // Ordinea trebuie să coincidă cu ordinea din INSERT
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
        return null; // Returnează null dacă datele sunt greșite
    }
}
