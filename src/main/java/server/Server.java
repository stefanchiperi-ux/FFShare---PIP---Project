package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class Server {

    private static final int MAX_PROFILE_IMAGE_PAYLOAD_BYTES = 2_100_000;

    private final int port;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final Path serverFilesDirectory = Paths.get("server_files");

    Server(int port) {
        this.port = port;
    }

    void start() {
        MessageDatabase.initDatabase();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            Files.createDirectories(serverFilesDirectory);

            System.out.println("Server started at port " + port);
            System.out.println("Files will be saved in: " + serverFilesDirectory.toAbsolutePath());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addClient(ClientHandler client) {
        clients.add(client);
    }

    private void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    private ClientHandler findClient(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    private void broadcastMessage(String senderUsername, String message) {
        String fullMessage = "__MSG__|" + escape(senderUsername) + "|" + escape(message);
        for (ClientHandler client : clients) {
            client.sendMessage(fullMessage);
        }
    }

    private void broadcastServerMessage(String message) {
        String fullMessage = "__SERVER__|" + escape(message);
        for (ClientHandler client : clients) {
            client.sendMessage(fullMessage);
        }
    }

    private void broadcastProfile(String username, String imageBase64) {
        for (ClientHandler client : clients) {
            client.sendProfile(username, imageBase64);
        }
    }

    private void handleClient(Socket clientSocket) {
        ClientHandler client = null;

        try {
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            String username = in.readUTF();

            if (username == null || username.isBlank()) {
                clientSocket.close();
                return;
            }

            username = username.trim();

            if (findClient(username) != null) {
                out.writeUTF("MESSAGE");
                out.writeUTF("__SERVER__|Username already used.");
                out.flush();
                clientSocket.close();
                return;
            }

            client = new ClientHandler(username, out);
            addClient(client);

            System.out.println(username + " connected");

            for (Map.Entry<String, String> profile : MessageDatabase.getAllProfileImages().entrySet()) {
                client.sendProfile(profile.getKey(), profile.getValue());
            }

            for (String oldMessage : MessageDatabase.getAllMessages()) {
                client.sendMessage(oldMessage);
            }

            broadcastServerMessage(username + " connected");

            while (true) {
                String type = in.readUTF();

                if (type.equals("MESSAGE")) {
                    String message = in.readUTF();

                    System.out.println(username + ": " + message);
                    MessageDatabase.saveMessage(username, message);
                    broadcastMessage(username, message);
                } else if (type.equals("SET_PROFILE")) {
                    String imageBase64 = readProfilePayload(in);

                    if (MessageDatabase.saveProfileImage(username, imageBase64)) {
                        broadcastProfile(username, imageBase64);
                    } else {
                        client.sendMessage("__SERVER__|Poza de profil nu este valida.");
                    }
                } else if (type.equals("FILE")) {
                    String fileName = in.readUTF();
                    long fileSize = in.readLong();

                    Path savedFile = saveFileOnServer(in, username, fileName, fileSize);

                    System.out.println(username + " uploaded file: " + savedFile.toAbsolutePath());
                    client.sendMessage("__SERVER__|File uploaded successfully: " + savedFile.getFileName());
                    broadcastServerMessage(username + " uploaded a file on the server: " + savedFile.getFileName());
                    broadcastFileList();
                } else if (type.equals("LIST_FILES")) {
                    List<String> files = getServerFilesList();

                    System.out.println(username + " requested file list");
                    client.sendFileList(files);
                } else if (type.equals("DOWNLOAD")) {
                    String requestedFile = in.readUTF();
                    Path fileToDownload = resolveServerFile(requestedFile);

                    System.out.println(username + " requested file download: " + requestedFile);

                    if (fileToDownload == null) {
                        client.sendDownloadError(requestedFile, "Fisierul nu a fost gasit pe server.");
                    } else {
                        client.sendFileDownload(requestedFile, fileToDownload);
                    }
                } else {
                    System.out.println("Unknown packet type from " + username + ": " + type);
                }
            }
        } catch (EOFException e) {
            System.out.println("Client disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (client != null) {
                removeClient(client);
                System.out.println(client.getUsername() + " disconnected");
                broadcastServerMessage(client.getUsername() + " disconnected");
            }

            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String readProfilePayload(DataInputStream in) throws IOException {
        int payloadSize = in.readInt();

        if (payloadSize <= 0 || payloadSize > MAX_PROFILE_IMAGE_PAYLOAD_BYTES) {
            throw new IOException("Invalid profile image size: " + payloadSize);
        }

        byte[] payload = new byte[payloadSize];
        in.readFully(payload);
        return new String(payload, StandardCharsets.UTF_8);
    }

    private Path saveFileOnServer(
            DataInputStream in,
            String username,
            String fileName,
            long fileSize
    ) throws IOException {
        Path userDirectory = serverFilesDirectory.resolve(username);
        Files.createDirectories(userDirectory);

        String safeFileName = Paths.get(fileName).getFileName().toString();
        Path outputPath = getUniquePath(userDirectory.resolve(safeFileName));

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            byte[] buffer = new byte[4096];
            long remainingBytes = fileSize;

            while (remainingBytes > 0) {
                int bytesToRead = (int) Math.min(buffer.length, remainingBytes);
                int bytesRead = in.read(buffer, 0, bytesToRead);

                if (bytesRead == -1) {
                    throw new EOFException("Connection closed before file was fully received.");
                }

                fos.write(buffer, 0, bytesRead);
                remainingBytes -= bytesRead;
            }
        }

        return outputPath;
    }

    private List<String> getServerFilesList() throws IOException {
        List<String> fileNames = new ArrayList<>();

        if (!Files.exists(serverFilesDirectory)) {
            return fileNames;
        }

        try (Stream<Path> paths = Files.walk(serverFilesDirectory)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String relativePath = serverFilesDirectory.relativize(path).toString().replace("\\", "/");
                fileNames.add(relativePath);
            });
        }

        return fileNames;
    }

    private Path resolveServerFile(String requestedFile) throws IOException {
        if (requestedFile == null || requestedFile.isBlank()) {
            return null;
        }

        Path serverRoot = serverFilesDirectory.toAbsolutePath().normalize();
        Path requestedPath = serverRoot.resolve(requestedFile).normalize();

        if (!requestedPath.startsWith(serverRoot) || !Files.isRegularFile(requestedPath)) {
            return null;
        }

        return requestedPath;
    }

    private Path getUniquePath(Path originalPath) {
        if (!Files.exists(originalPath)) {
            return originalPath;
        }

        String fileName = originalPath.getFileName().toString();
        String name;
        String extension;

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0) {
            name = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        } else {
            name = fileName;
            extension = "";
        }

        Path parent = originalPath.getParent();
        int counter = 1;

        while (true) {
            Path newPath = parent.resolve(name + "_" + counter + extension);

            if (!Files.exists(newPath)) {
                return newPath;
            }

            counter++;
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }

    private static class ClientHandler {

        private final String username;
        private final DataOutputStream out;

        public ClientHandler(String username, DataOutputStream out) {
            this.username = username;
            this.out = out;
        }

        public String getUsername() {
            return username;
        }

        public synchronized void sendMessage(String message) {
            try {
                out.writeUTF("MESSAGE");
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public synchronized void sendProfile(String username, String imageBase64) {
            byte[] profilePayload = imageBase64.getBytes(StandardCharsets.UTF_8);

            if (profilePayload.length <= 0 || profilePayload.length > MAX_PROFILE_IMAGE_PAYLOAD_BYTES) {
                return;
            }

            try {
                out.writeUTF("PROFILE");
                out.writeUTF(username);
                out.writeInt(profilePayload.length);
                out.write(profilePayload);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public synchronized void sendFileDownload(String requestedFile, Path file) {
            try (InputStream input = Files.newInputStream(file)) {
                out.writeUTF("FILE_DOWNLOAD");
                out.writeUTF(requestedFile);
                out.writeUTF(file.getFileName().toString());
                out.writeLong(Files.size(file));

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public synchronized void sendDownloadError(String requestedFile, String message) {
            try {
                out.writeUTF("DOWNLOAD_ERROR");
                out.writeUTF(requestedFile == null ? "" : requestedFile);
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public synchronized void sendFileList(List<String> files) {
            try {
                out.writeUTF("FILES_LIST");
                out.writeInt(files.size());

                for (String file : files) {
                    out.writeUTF(file);
                }

                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void broadcastFileList() throws IOException {
    	List<String> files = getServerFilesList();

        for (ClientHandler client : clients) {
            client.sendFileList(files);
        }
    }
}
