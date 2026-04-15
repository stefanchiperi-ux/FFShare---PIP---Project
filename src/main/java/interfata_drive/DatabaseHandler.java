package interfata_drive;

import java.sql.*;

public class DatabaseHandler {
    private static final String URL = "jdbc:sqlite:utilizatori.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
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
            // Dacă ajunge aici, SQLite a dat eroare (cel mai des din cauza UNIQUE pe username)
            e.printStackTrace(); // Verifică consola pentru detalii specifice
            return false;
        }
    }

    public static boolean validateLogin(String user, String pass) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // Returnează true dacă s-a găsit o potrivire
        } catch (SQLException e) {
            return false;
        }
    }
}