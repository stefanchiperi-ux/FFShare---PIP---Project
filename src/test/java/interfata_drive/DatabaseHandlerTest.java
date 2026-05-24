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

/**
 * Teste pentru baza de date a utilizatorilor.
 */
class DatabaseHandlerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    /**
     * Pregateste o baza de date temporara pentru fiecare test.
     */
    void setUp() {
        DatabaseHandler.useDatabaseForTesting(tempDir.resolve("users.db"));
        DatabaseHandler.initDatabase();
    }

    @AfterEach
    /**
     * Reseteaza configuratia bazei de date dupa test.
     */
    void tearDown() {
        DatabaseHandler.resetDatabaseForTesting();
    }

    @Test
    /**
     * Verifica salvarea numelui complet pentru un cont valid.
     */
    void registerUserPersistsFullNameForValidCredentials() {
        assertTrue(DatabaseHandler.registerUser("Ion Popescu", "ion123", "Pass!1"));

        assertEquals("Ion Popescu", DatabaseHandler.getFullName("ion123", "Pass!1"));
    }

    @Test
    /**
     * Verifica respingerea username-ului duplicat si a parolei gresite.
     */
    void duplicateUsernameIsRejectedAndWrongPasswordDoesNotAuthenticate() {
        assertTrue(DatabaseHandler.registerUser("Ana Rusu", "anarusu", "Pass!1"));

        assertFalse(DatabaseHandler.registerUser("Alta Ana", "anarusu", "Other!1"));
        assertNull(DatabaseHandler.getFullName("anarusu", "gresit"));
        assertEquals("Ana Rusu", DatabaseHandler.getFullName("anarusu", "Pass!1"));
    }
}
