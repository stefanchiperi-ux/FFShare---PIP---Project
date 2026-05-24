package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Client {

    private static final int MAX_PROFILE_IMAGE_PAYLOAD_BYTES = 2_100_000;

    private String host = "172.20.10.9";
    private int port = 5000;
    private String username;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private boolean running = false;

    private Consumer<String> onMessageReceived;
    private Consumer<List<String>> onFileListReceived;
    private final List<String> pendingMessages = new ArrayList<>();

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
                    } else if (type.equals("PROFILE")) {
                        String profileUsername = in.readUTF();
                        byte[] profilePayload = readPayload(MAX_PROFILE_IMAGE_PAYLOAD_BYTES);
                        String imageBase64 = new String(profilePayload, StandardCharsets.UTF_8);

                        handleReceivedMessage("__PROFILE__|" + escape(profileUsername) + "|" + imageBase64);
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

        byte[] profilePayload = imageBase64.getBytes(StandardCharsets.UTF_8);

        if (profilePayload.length <= 0 || profilePayload.length > MAX_PROFILE_IMAGE_PAYLOAD_BYTES) {
            System.out.println("Profile image is too large to send.");
            return;
        }

        try {
            out.writeUTF("SET_PROFILE");
            out.writeInt(profilePayload.length);
            out.write(profilePayload);
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

    private byte[] readPayload(int maxSize) throws IOException {
        int payloadSize = in.readInt();

        if (payloadSize <= 0 || payloadSize > maxSize) {
            throw new IOException("Invalid payload size: " + payloadSize);
        }

        byte[] payload = new byte[payloadSize];
        in.readFully(payload);
        return payload;
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

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }
}
