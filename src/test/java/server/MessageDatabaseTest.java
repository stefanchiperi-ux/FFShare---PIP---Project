package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageDatabaseTest {

    private static final String PNG_BASE64 = Base64.getEncoder().encodeToString(new byte[] {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    });

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MessageDatabase.useDatabaseForTesting(tempDir.resolve("messages.db"));
        MessageDatabase.initDatabase();
    }

    @AfterEach
    void tearDown() {
        MessageDatabase.resetDatabaseForTesting();
    }

    @Test
    void saveMessageReturnsEscapedMessagesInInsertionOrder() {
        MessageDatabase.saveMessage("Ana", "Salut | lume");
        MessageDatabase.saveMessage("Bob", "Linia 1\nLinia 2");

        assertEquals(List.of(
                "__MSG__|Ana|Salut \\p lume",
                "__MSG__|Bob|Linia 1\\nLinia 2"
        ), MessageDatabase.getAllMessages());
    }

    @Test
    void profileImagesAcceptValidPngBase64AndRejectInvalidPayloads() {
        assertTrue(MessageDatabase.saveProfileImage("Ana", PNG_BASE64));
        assertFalse(MessageDatabase.saveProfileImage("Bob", "not-base64"));

        assertEquals(Map.of("Ana", PNG_BASE64), MessageDatabase.getAllProfileImages());
    }
}
