package interfata_drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DatabaseHandlerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        DatabaseHandler.useDatabaseForTesting(tempDir.resolve("users.db"));
        DatabaseHandler.initDatabase();
    }

    @AfterEach
    void tearDown() {
        DatabaseHandler.resetDatabaseForTesting();
    }

    @Test
    void registerUserPersistsFullNameForValidCredentials() {
        assertTrue(DatabaseHandler.registerUser("Ion Popescu", "ion123", "Pass!1"));

        assertEquals("Ion Popescu", DatabaseHandler.getFullName("ion123", "Pass!1"));
    }

    @Test
    void duplicateUsernameIsRejectedAndWrongPasswordDoesNotAuthenticate() {
        assertTrue(DatabaseHandler.registerUser("Ana Rusu", "anarusu", "Pass!1"));

        assertFalse(DatabaseHandler.registerUser("Alta Ana", "anarusu", "Other!1"));
        assertNull(DatabaseHandler.getFullName("anarusu", "gresit"));
        assertEquals("Ana Rusu", DatabaseHandler.getFullName("anarusu", "Pass!1"));
    }
}
