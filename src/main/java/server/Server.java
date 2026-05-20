package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    Server(int port) {
        this.port = port;
    }

    void start() {
        MessageDatabase.initDatabase();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started at port " + port);

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
            client.send(fullMessage);
        }
    }

    private void broadcastServerMessage(String message) {
        String fullMessage = "__SERVER__|" + escape(message);
        for (ClientHandler client : clients) {
            client.send(fullMessage);
        }
    }

    private void broadcastProfile(String username, String imageBase64) {
        String profileMessage = "__PROFILE__|" + escape(username) + "|" + imageBase64;
        for (ClientHandler client : clients) {
            client.send(profileMessage);
        }
    }

    private void handleClient(Socket clientSocket) {
        ClientHandler client = null;

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String username = in.readLine();

            if (username == null || username.isBlank()) {
                clientSocket.close();
                return;
            }

            username = username.trim();

            if (findClient(username) != null) {
                out.println("__SERVER__|Username already used.");
                clientSocket.close();
                return;
            }

            client = new ClientHandler(username, clientSocket, out);
            addClient(client);

            System.out.println(username + " connected");

            for (Map.Entry<String, String> profile : MessageDatabase.getAllProfileImages().entrySet()) {
                out.println("__PROFILE__|" + escape(profile.getKey()) + "|" + profile.getValue());
            }

            for (String oldMessage : MessageDatabase.getAllMessages()) {
                out.println(oldMessage);
            }

            broadcastServerMessage(username + " connected");

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("__SET_PROFILE__|")) {
                    String imageBase64 = message.substring("__SET_PROFILE__|".length());
                    MessageDatabase.saveProfileImage(username, imageBase64);
                    broadcastProfile(username, imageBase64);
                    continue;
                }

                System.out.println(username + ": " + message);
                MessageDatabase.saveMessage(username, message);
                broadcastMessage(username, message);
            }

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

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("|", "\\p").replace("\n", "\\n").replace("\r", "");
    }

    private static class ClientHandler {
        private final String username;
        private final Socket socket;
        private final PrintWriter out;

        public ClientHandler(String username, Socket socket, PrintWriter out) {
            this.username = username;
            this.socket = socket;
            this.out = out;
        }

        public String getUsername() {
            return username;
        }

        public Socket getSocket() {
            return socket;
        }

        public void send(String message) {
            out.println(message);
        }
    }
}
