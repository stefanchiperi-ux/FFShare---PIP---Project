package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Client {
    private String host = "localhost";
    private int port = 5000;
    private String username;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private boolean running = false;

    private Consumer<String> onMessageReceived;
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

    public void connect() throws IOException {
        this.socket = new Socket(host, port);

        this.out = new PrintWriter(socket.getOutputStream(), true);

        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
        );

        this.running = true;

        System.out.println("Connected to server");

        out.println(username);

        startListening();
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                String message;

                while (running && (message = in.readLine()) != null) {
                    System.out.println("Message received: " + message);
                    handleReceivedMessage(message);
                }

            } catch (IOException e) {
                if (running) {
                    System.out.println("Disconnected from server.");
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

    public void sendMessage(String text) {
        if (out != null) {
            out.println(text);
        }
    }

    public void sendProfileImage(String imageBase64) {
        if (out != null) {
            out.println("__SET_PROFILE__|" + imageBase64);
        }
    }

    public void close() throws IOException {
        running = false;

        if (socket != null) {
            socket.close();
        }
    }
}
