package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Client {

    private String host = "172.20.10.11";
    private int port = 5000;
    private String username;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private boolean running = false;

    private Consumer<String> onMessageReceived;
    private Consumer<File> onFileReceived;
    private Consumer<List<String>> onFileListReceived;
    private final List<String> pendingMessages = new ArrayList<>();

    private final Path downloadDirectory = Paths.get("received_files");

    public Client(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    public Client(String username) {
        this.username = username;
    }

    public synchronized void setOnMessageReceived(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;

        if (this.onMessageReceived != null && !pendingMessages.isEmpty()) {
            for (String message : pendingMessages) {
                this.onMessageReceived.accept(message);
            }
            pendingMessages.clear();
        }
    }

    public void setOnFileReceived(Consumer<File> onFileReceived) {
        this.onFileReceived = onFileReceived;
    }

    public void setOnFileListReceived(Consumer<List<String>> onFileListReceived) {
        this.onFileListReceived = onFileListReceived;
    }

    public void connect() throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), 1500);

        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());

        this.running = true;

        System.out.println("Connected to server");

        out.writeUTF(username);
        out.flush();

        startListening();
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (running) {
                    String type = in.readUTF();

                    if (type.equals("MESSAGE")) {
                        String message = in.readUTF();
                        System.out.println("Message received: " + message);
                        handleReceivedMessage(message);
                    } else if (type.equals("FILE")) {
                        String fileName = in.readUTF();
                        long fileSize = in.readLong();

                        File receivedFile = receiveFile(fileName, fileSize);
                        System.out.println("File received: " + receivedFile.getAbsolutePath());

                        if (onFileReceived != null) {
                            onFileReceived.accept(receivedFile);
                        }

                        handleReceivedMessage("__SERVER__|Fisier descarcat in: " + receivedFile.getAbsolutePath());
                    } else if (type.equals("FILES_LIST")) {
                        int count = in.readInt();
                        List<String> files = new ArrayList<>();

                        for (int i = 0; i < count; i++) {
                            files.add(in.readUTF());
                        }

                        System.out.println("File list received from server.");

                        if (onFileListReceived != null) {
                            onFileListReceived.accept(files);
                        }
                    } else {
                        System.out.println("Unknown response from server: " + type);
                    }
                }
            } catch (EOFException e) {
                if (running) {
                    System.out.println("Server closed the connection.");
                }
            } catch (IOException e) {
                if (running) {
                    System.out.println("Disconnected from server.");
                    e.printStackTrace();
                }
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private synchronized void handleReceivedMessage(String message) {
        if (onMessageReceived != null) {
            onMessageReceived.accept(message);
        } else {
            pendingMessages.add(message);
        }
    }

    public synchronized void sendMessage(String text) {
        if (out == null) {
            return;
        }

        try {
            out.writeUTF("MESSAGE");
            out.writeUTF(text);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendProfileImage(String imageBase64) {
        if (out == null) {
            return;
        }

        try {
            out.writeUTF("SET_PROFILE");
            out.writeUTF(imageBase64);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendFile(File file) {
        if (out == null || file == null || !file.exists() || !file.isFile()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            out.writeUTF("FILE");
            out.writeUTF(file.getName());
            out.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();

            System.out.println("File sent: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void requestFileList() {
        if (out == null) {
            return;
        }

        try {
            out.writeUTF("LIST_FILES");
            out.flush();
            System.out.println("File list request sent to server.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void requestFileDownload(String filePath) {
        if (out == null || filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            out.writeUTF("DOWNLOAD_FILE");
            out.writeUTF(filePath);
            out.flush();
            System.out.println("File download request sent to server: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File receiveFile(String fileName, long fileSize) throws IOException {
        Files.createDirectories(downloadDirectory);

        String safeFileName = Paths.get(fileName).getFileName().toString();
        Path outputPath = getUniquePath(downloadDirectory.resolve(safeFileName));

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
        
        
        System.out.println("Saving downloaded file to: " + outputPath.toAbsolutePath());

        return outputPath.toFile();
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

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null;
    }

    public void close() throws IOException {
        running = false;

        if (socket != null) {
            socket.close();
        }
    }
    
    
}
