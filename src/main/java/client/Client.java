package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;


public class Client {
    private String host = "172.20.10.11";
    private int port = 5000;
    private String username;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private boolean running = false;

    // Functia care va fi apelata cand primim mesaj de la server
    private Consumer<String> onMessageReceived;

    public Client(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    public Client(String username) {
        this.username = username;
    }

    public void setOnMessageReceived(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void connect() throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(host, port), 1500);

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
        new Thread(() -> {
            try {
                String message;

                while (running && (message = in.readLine()) != null) {
                    System.out.println("Message received: " + message);

                    if (onMessageReceived != null) {
                        onMessageReceived.accept(message);
                    }
                }

            } catch (IOException e) {
                if (running) {
                    System.out.println("Disconnected from server.");
                }
            }
        }).start();
    }

    public void sendMessage(String text) {
        if (out != null) {
            out.println(text);
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
