package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a simple multi-client chat server.
 * <p>
 * The server listens for incoming client connections on a specified port.
 * Each connected client is handled on a separate thread, allowing multiple
 * clients to communicate with the server at the same time.
 * </p>
 * <p>
 * Messages received from one client are broadcast to all other connected
 * clients.
 * </p>
 */
public class Server {

    /**
     * The port on which the server listens for incoming client connections.
     */
    private final int port;

    /**
     * Thread-safe list containing all currently connected clients.
     * <p>
     * A {@link CopyOnWriteArrayList} is used because clients may be added or
     * removed while the server is broadcasting messages.
     * </p>
     */
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /**
     * Creates a new server that listens on the specified port.
     *
     * @param port the port number used by the server
     */
    Server(int port) {
        this.port = port;
    }

    /**
     * Starts the server and waits for incoming client connections.
     * <p>
     * For each accepted client connection, a new thread is created in order to
     * handle that client independently.
     * </p>
     */
    void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started at port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new Thread(() -> {
                    handleClient(clientSocket);
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a client to the list of connected clients.
     *
     * @param client the client handler to be added
     */
    private void addClient(ClientHandler client) {
        clients.add(client);
    }

    /**
     * Removes a client from the list of connected clients.
     *
     * @param client the client handler to be removed
     */
    private void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    /**
     * Searches for a connected client by username.
     *
     * @param username the username of the client to search for
     * @return the matching {@link ClientHandler}, or {@code null} if no client
     *         with the given username is connected
     */
    private ClientHandler findClient(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }

        return null;
    }

    /**
     * Sends a message from one client to all other connected clients.
     * <p>
     * The sender does not receive their own message back from the server.
     * </p>
     *
     * @param senderUsername the username of the client who sent the message
     * @param message the message to be broadcast
     */
    private void broadcast(String senderUsername, String message) {
        String fullMessage = senderUsername + ": " + message;

        for (ClientHandler client : clients) {
            if (!client.getUsername().equals(senderUsername)) {
                client.send(fullMessage);
            }
        }
    }

    /**
     * Handles communication with a connected client.
     * <p>
     * This method reads the username sent by the client, checks whether the
     * username is valid and unique, then continuously reads messages from the
     * client and broadcasts them to the other connected clients.
     * </p>
     * <p>
     * When the client disconnects or an error occurs, the client is removed from
     * the list of connected clients and the socket is closed.
     * </p>
     *
     * @param clientSocket the socket associated with the connected client
     */
    private void handleClient(Socket clientSocket) {
        ClientHandler client = null;

        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
            );

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String username = in.readLine();

            if (username == null || username.isBlank()) {
                clientSocket.close();
                return;
            }

            username = username.trim();

            if (findClient(username) != null) {
                out.println("Username already used.");
                clientSocket.close();
                return;
            }

            client = new ClientHandler(username, clientSocket, out);
            addClient(client);

            System.out.println(username + " connected");
            broadcast("SERVER", username + " connected");

            String message;

            while ((message = in.readLine()) != null) {
                System.out.println(username + ": " + message);
                broadcast(username, message);
            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (client != null) {
                removeClient(client);

                System.out.println(client.getUsername() + " disconnected");
                broadcast("SERVER", client.getUsername() + " disconnected");
            }

            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Represents a connected client.
     * <p>
     * This class stores the client's username, socket, and output stream.
     * It is used by the server to send messages to a specific client.
     * </p>
     */
    private static class ClientHandler {

        /**
         * The username of the connected client.
         */
        private final String username;

        /**
         * The socket associated with the client connection.
         */
        private final Socket socket;

        /**
         * Output stream used to send messages to the client.
         */
        private final PrintWriter out;

        /**
         * Creates a new client handler for a connected client.
         *
         * @param username the username of the client
         * @param socket the socket associated with the client
         * @param out the output stream used to send messages to the client
         */
        public ClientHandler(String username, Socket socket, PrintWriter out) {
            this.username = username;
            this.socket = socket;
            this.out = out;
        }

        /**
         * Returns the username of the client.
         *
         * @return the client's username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Returns the socket associated with the client.
         *
         * @return the client's socket
         */
        public Socket getSocket() {
            return socket;
        }

        /**
         * Sends a message to the client.
         *
         * @param message the message to be sent
         */
        public void send(String message) {
            out.println(message);
        }
    }
}