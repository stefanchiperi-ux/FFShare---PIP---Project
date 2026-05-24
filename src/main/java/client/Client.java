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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Clientul care comunica cu serverul aplicatiei.
 */
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
    private final Map<String, File> pendingDownloads = new ConcurrentHashMap<>();

    /**
     * Creeaza un client cu adresa si port personalizate.
     *
     * @param host adresa serverului
     * @param port portul serverului
     * @param username numele utilizatorului
     */
    public Client(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    /**
     * Creeaza un client cu setarile implicite de conectare.
     *
     * @param username numele utilizatorului
     */
    public Client(String username) {
        this.username = username;
    }

    /**
     * Seteaza actiunea apelata cand se primeste un mesaj.
     *
     * @param onMessageReceived actiunea pentru mesajele primite
     */
    public synchronized void setOnMessageReceived(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;

        if (this.onMessageReceived != null && !pendingMessages.isEmpty()) {
            for (String message : pendingMessages) {
                this.onMessageReceived.accept(message);
            }
            pendingMessages.clear();
        }
    }

    /**
     * Seteaza actiunea apelata cand se primeste lista de fisiere.
     *
     * @param onFileListReceived actiunea pentru lista de fisiere
     */
    public void setOnFileListReceived(Consumer<List<String>> onFileListReceived) {
        this.onFileListReceived = onFileListReceived;
    }

    /**
     * Deschide conexiunea cu serverul.
     *
     * @throws IOException daca serverul nu poate fi accesat
     */
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

    /**
     * Porneste firul care asculta raspunsurile serverului.
     */
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
                    } else if (type.equals("FILE_DOWNLOAD")) {
                        receiveFileDownload();
                    } else if (type.equals("DOWNLOAD_ERROR")) {
                        String filePath = in.readUTF();
                        String errorMessage = in.readUTF();
                        pendingDownloads.remove(filePath);
                        handleReceivedMessage("__SERVER__|" + errorMessage);
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

    /**
     * Trimite mesajul catre interfata sau il pastreaza temporar.
     *
     * @param message mesajul primit
     */
    private synchronized void handleReceivedMessage(String message) {
        if (onMessageReceived != null) {
            onMessageReceived.accept(message);
        } else {
            pendingMessages.add(message);
        }
    }

    /**
     * Trimite un mesaj text catre server.
     *
     * @param text mesajul scris de utilizator
     */
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

    /**
     * Trimite poza de profil catre server.
     *
     * @param imageBase64 imaginea codata Base64
     */
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

    /**
     * Trimite un fisier catre server.
     *
     * @param file fisierul ales de utilizator
     */
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

    /**
     * Cere serverului descarcarea unui fisier.
     *
     * @param filePath calea fisierului pe server
     * @param destination locul unde se salveaza fisierul
     */
    public synchronized void requestFileDownload(String filePath, File destination) {
        if (out == null || filePath == null || filePath.isBlank() || destination == null) {
            return;
        }

        pendingDownloads.put(filePath, destination);

        try {
            out.writeUTF("DOWNLOAD");
            out.writeUTF(filePath);
            out.flush();
            System.out.println("File download request sent: " + filePath);
        } catch (IOException e) {
            pendingDownloads.remove(filePath);
            handleReceivedMessage("__SERVER__|Nu am putut cere descarcarea fisierului: " + e.getMessage());
        }
    }

    /**
     * Cere serverului lista fisierelor disponibile.
     */
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

    /**
     * Citeste un pachet de bytes de la server.
     *
     * @param maxSize dimensiunea maxima acceptata
     * @return continutul citit
     * @throws IOException daca dimensiunea sau citirea esueaza
     */
    private byte[] readPayload(int maxSize) throws IOException {
        int payloadSize = in.readInt();

        if (payloadSize <= 0 || payloadSize > maxSize) {
            throw new IOException("Invalid payload size: " + payloadSize);
        }

        byte[] payload = new byte[payloadSize];
        in.readFully(payload);
        return payload;
    }

    /**
     * Primeste si salveaza fisierul descarcat.
     *
     * @throws IOException daca fisierul nu poate fi primit
     */
    private void receiveFileDownload() throws IOException {
        String filePath = in.readUTF();
        String fileName = in.readUTF();
        long fileSize = in.readLong();

        if (fileSize < 0) {
            throw new IOException("Invalid file download size: " + fileSize);
        }

        File destination = pendingDownloads.remove(filePath);

        if (destination == null) {
            discardBytes(fileSize);
            handleReceivedMessage("__SERVER__|Nu am gasit destinatia pentru descarcare: " + fileName);
            return;
        }

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            discardBytes(fileSize);
            handleReceivedMessage("__SERVER__|Nu am putut crea folderul de descarcare.");
            return;
        }

        IOException writeError = null;
        FileOutputStream fos = null;

        try {
            try {
                fos = new FileOutputStream(destination);
            } catch (IOException e) {
                writeError = e;
            }

            long remainingBytes = fileSize;
            byte[] buffer = new byte[4096];

            while (remainingBytes > 0) {
                int bytesToRead = (int) Math.min(buffer.length, remainingBytes);
                int bytesRead = in.read(buffer, 0, bytesToRead);

                if (bytesRead == -1) {
                    throw new EOFException("Connection closed before file was fully downloaded.");
                }

                if (fos != null && writeError == null) {
                    try {
                        fos.write(buffer, 0, bytesRead);
                    } catch (IOException e) {
                        writeError = e;
                    }
                }

                remainingBytes -= bytesRead;
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    if (writeError == null) {
                        writeError = e;
                    }
                }
            }
        }

        if (writeError != null) {
            handleReceivedMessage("__SERVER__|Nu am putut salva fisierul: " + writeError.getMessage());
            return;
        }

        handleReceivedMessage("__SERVER__|Fisier descarcat: " + destination.getAbsolutePath());
    }

    /**
     * Arunca bytes primiti cand fisierul nu poate fi salvat.
     *
     * @param bytesToDiscard numarul de bytes de ignorat
     * @throws IOException daca citirea este intrerupta
     */
    private void discardBytes(long bytesToDiscard) throws IOException {
        byte[] buffer = new byte[4096];
        long remainingBytes = bytesToDiscard;

        while (remainingBytes > 0) {
            int bytesToRead = (int) Math.min(buffer.length, remainingBytes);
            int bytesRead = in.read(buffer, 0, bytesToRead);

            if (bytesRead == -1) {
                throw new EOFException("Connection closed before payload was fully discarded.");
            }

            remainingBytes -= bytesRead;
        }
    }

    /**
     * Verifica daca exista conexiune activa cu serverul.
     *
     * @return true daca clientul este conectat
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null;
    }

    /**
     * Inchide conexiunea cu serverul.
     *
     * @throws IOException daca socketul nu poate fi inchis
     */
    public void close() throws IOException {
        running = false;

        if (socket != null) {
            socket.close();
        }
    }

    /**
     * Escapeaza textul folosit in protocolul aplicatiei.
     *
     * @param value textul original
     * @return textul pregatit pentru trimitere
     */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }
}
