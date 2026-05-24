package server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerFileStorageTest {

    @TempDir
    Path tempDir;

    private Server server;

    @BeforeEach
    void setUp() {
        server = new Server(0, tempDir);
    }

    @Test
    void getServerFilesListReturnsRelativePathsWithForwardSlashes() throws IOException {
        Path userDir = Files.createDirectories(tempDir.resolve("ana"));
        Files.writeString(userDir.resolve("raport.txt"), "continut");
        Files.createDirectories(tempDir.resolve("empty"));

        assertEquals(List.of("ana/raport.txt"), server.getServerFilesList());
    }

    @Test
    void resolveServerFileAcceptsExistingRegularFileInsideServerDirectory() throws IOException {
        Path file = Files.createDirectories(tempDir.resolve("ana")).resolve("raport.txt");
        Files.writeString(file, "continut");

        assertEquals(file.toAbsolutePath().normalize(), server.resolveServerFile("ana/raport.txt"));
    }

    @Test
    void resolveServerFileRejectsMissingDirectoriesAndBlankRequests() throws IOException {
        Files.createDirectories(tempDir.resolve("ana"));

        assertNull(server.resolveServerFile("ana"));
        assertNull(server.resolveServerFile(""));
        assertNull(server.resolveServerFile(null));
        assertNull(server.resolveServerFile("missing.txt"));
    }

    @Test
    void resolveServerFileRejectsPathTraversalAndAbsolutePathsOutsideServerDirectory() throws IOException {
        Path outsideFile = tempDir.resolveSibling("outside-download-test.txt");
        Files.writeString(outsideFile, "secret");

        try {
            assertNull(server.resolveServerFile("../" + outsideFile.getFileName()));
            assertNull(server.resolveServerFile(outsideFile.toString()));
        } finally {
            Files.deleteIfExists(outsideFile);
        }
    }
}
