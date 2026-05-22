package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessageDatabase {
    private static final String DB_URL = "jdbc:sqlite:mesaje.db";
    private static final int MAX_PROFILE_IMAGE_BASE64_LENGTH = 2_100_000;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("SQLite JDBC driver is missing from the runtime classpath. Run with Maven or add sqlite-jdbc to the run configuration.");
        }
    }

    public static void initDatabase() {
        String messagesSql = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sender TEXT NOT NULL," +
                "message TEXT NOT NULL," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String profilesSql = "CREATE TABLE IF NOT EXISTS profiles (" +
                "username TEXT PRIMARY KEY," +
                "image_base64 TEXT NOT NULL," +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(messagesSql);
            stmt.execute(profilesSql);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize message database", e);
        }
    }

    public static void saveMessage(String sender, String message) {
        String sql = "INSERT INTO messages(sender, message) VALUES(?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAllMessages() {
        List<String> messages = new ArrayList<>();
        String sql = "SELECT sender, message FROM messages ORDER BY id ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                messages.add("__MSG__|" + escape(rs.getString("sender")) + "|" + escape(rs.getString("message")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return messages;
    }

    public static boolean saveProfileImage(String username, String imageBase64) {
        if (!isValidImageBase64(imageBase64)) {
            return false;
        }

        String sql = "INSERT INTO profiles(username, image_base64, updated_at) VALUES(?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(username) DO UPDATE SET image_base64 = excluded.image_base64, updated_at = CURRENT_TIMESTAMP";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, imageBase64);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Map<String, String> getAllProfileImages() {
        Map<String, String> profiles = new LinkedHashMap<>();
        String sql = "SELECT username, image_base64 FROM profiles ORDER BY username ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String imageBase64 = rs.getString("image_base64");

                if (isValidImageBase64(imageBase64)) {
                    profiles.put(rs.getString("username"), imageBase64);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return profiles;
    }

    private static boolean isValidImageBase64(String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank() || imageBase64.length() > MAX_PROFILE_IMAGE_BASE64_LENGTH) {
            return false;
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            return isPng(imageBytes) || isJpeg(imageBytes);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isPng(byte[] imageBytes) {
        byte[] signature = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

        if (imageBytes.length < signature.length) {
            return false;
        }

        for (int i = 0; i < signature.length; i++) {
            if (imageBytes[i] != signature[i]) {
                return false;
            }
        }

        return true;
    }

    private static boolean isJpeg(byte[] imageBytes) {
        return imageBytes.length >= 3
                && (imageBytes[0] & 0xFF) == 0xFF
                && (imageBytes[1] & 0xFF) == 0xD8
                && (imageBytes[2] & 0xFF) == 0xFF;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }
}
