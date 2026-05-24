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

/**
 * Teste pentru fisierele salvate de server.
 */
class ServerFileStorageTest {

    @TempDir
    Path tempDir;

    private Server server;

    @BeforeEach
    /**
     * Creeaza un server de test cu folder temporar.
     */
    void setUp() {
        server = new Server(0, tempDir);
    }

    @Test
    /**
     * Verifica lista de fisiere cu cai relative.
     *
     * @throws IOException daca fisierele de test nu pot fi create
     */
    void getServerFilesListReturnsRelativePathsWithForwardSlashes() throws IOException {
        Path userDir = Files.createDirectories(tempDir.resolve("ana"));
        Files.writeString(userDir.resolve("raport.txt"), "continut");
        Files.createDirectories(tempDir.resolve("empty"));

        assertEquals(List.of("ana/raport.txt"), server.getServerFilesList());
    }

    @Test
    /**
     * Verifica rezolvarea unui fisier existent in folderul serverului.
     *
     * @throws IOException daca fisierul de test nu poate fi creat
     */
    void resolveServerFileAcceptsExistingRegularFileInsideServerDirectory() throws IOException {
        Path file = Files.createDirectories(tempDir.resolve("ana")).resolve("raport.txt");
        Files.writeString(file, "continut");

        assertEquals(file.toAbsolutePath().normalize(), server.resolveServerFile("ana/raport.txt"));
    }

    @Test
    /**
     * Verifica respingerea cererilor goale sau inexistente.
     *
     * @throws IOException daca folderul de test nu poate fi creat
     */
    void resolveServerFileRejectsMissingDirectoriesAndBlankRequests() throws IOException {
        Files.createDirectories(tempDir.resolve("ana"));

        assertNull(server.resolveServerFile("ana"));
        assertNull(server.resolveServerFile(""));
        assertNull(server.resolveServerFile(null));
        assertNull(server.resolveServerFile("missing.txt"));
    }

    @Test
    /**
     * Verifica blocarea cailor din afara folderului serverului.
     *
     * @throws IOException daca fisierul de test nu poate fi creat
     */
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
