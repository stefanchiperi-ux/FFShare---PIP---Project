package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Represents a client that connects to the chat server.
 * <p>
 * This class manages the connection to the server, sends messages to the server,
 * receives messages from the server, and notifies the application when a new
 * message is received.
 * </p>
 */
public class Client {

    /**
     * The server host address.
     */
    private String host = "localhost";

    /**
     * The server port used for the chat connection.
     */
    private int port = 5000;

    /**
     * The username used by this client when connecting to the server.
     */
    private String username;

    /**
     * The socket used to communicate with the server.
     */
    private Socket socket;

    /**
     * Output stream used to send messages to the server.
     */
    private PrintWriter out;

    /**
     * Input stream used to read messages received from the server.
     */
    private BufferedReader in;

    /**
     * Indicates whether the client is currently running and listening for messages.
     */
    private boolean running = false;

    /**
     * Callback function that is executed when a message is received from the server.
     */
    private Consumer<String> onMessageReceived;

    /**
     * Creates a new client with a custom host, port, and username.
     *
     * @param host the server host address
     * @param port the server port
     * @param username the username used by the client
     */
    public Client(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    /**
     * Creates a new client using the default host and port.
     * <p>
     * The default host is {@code localhost}, and the default port is {@code 5000}.
     * </p>
     *
     * @param username the username used by the client
     */
    public Client(String username) {
        this.username = username;
    }

    /**
     * Sets the callback function that will be called when a message is received
     * from the server.
     *
     * @param onMessageReceived the function used to handle received messages
     */
    public void setOnMessageReceived(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    /**
     * Connects the client to the server.
     * <p>
     * This method creates a socket connection, initializes the input and output
     * streams, sends the username to the server, and starts listening for incoming
     * messages.
     * </p>
     *
     * @throws IOException if the connection to the server cannot be established
     */
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

    /**
     * Starts a separate thread that listens for messages from the server.
     * <p>
     * When a message is received, it is printed to the console and passed to the
     * {@code onMessageReceived} callback, if one has been set.
     * </p>
     */
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

    /**
     * Sends a text message to the server.
     * <p>
     * The message is sent only if the output stream has been initialized.
     * </p>
     *
     * @param text the message text to be sent
     */
    public void sendMessage(String text) {
        if (out != null) {
            out.println(text);
        }
    }

    /**
     * Checks whether the client is currently connected to the server.
     *
     * @return {@code true} if the socket exists, is connected, is not closed,
     *         and the output stream is available; {@code false} otherwise
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && out != null;
    }

    /**
     * Closes the connection to the server.
     * <p>
     * This method stops the listening loop and closes the socket if it exists.
     * </p>
     *
     * @throws IOException if an error occurs while closing the socket
     */
    public void close() throws IOException {
        running = false;

        if (socket != null) {
            socket.close();
        }
    }
}