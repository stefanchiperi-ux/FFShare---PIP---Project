package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;

public class FileServer {
    private final int port;
    private final Path uploadFolder = Paths.get("received_files");

    public FileServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            Files.createDirectories(uploadFolder);

            System.out.println("File server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();

                new Thread(() -> {
                    handleFileUpload(socket);
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleFileUpload(Socket socket) {
        try (
                DataInputStream in = new DataInputStream(socket.getInputStream())
        ) {
            String fileName = in.readUTF();
            long fileSize = in.readLong();

            Path filePath = uploadFolder.resolve(fileName);

            try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;

                while (totalRead < fileSize) {
                    int bytesToRead = (int) Math.min(buffer.length, fileSize - totalRead);
                    int bytesRead = in.read(buffer, 0, bytesToRead);

                    if (bytesRead == -1) {
                        break;
                    }

                    fileOut.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }
            }

            System.out.println("Received file: " + fileName + " (" + fileSize + " bytes)");

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}