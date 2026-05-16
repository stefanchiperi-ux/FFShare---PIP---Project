package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;

    // Lista cu toti clientii conectati
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    Server(int port) {
        this.port = port;
    }

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

    // Adauga un client in lista
    private void addClient(ClientHandler client) {
        clients.add(client);
    }

    // Sterge un client din lista
    private void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // Gaseste un client dupa username
    private ClientHandler findClient(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }

        return null;
    }

    // Trimite mesajul primit tuturor clientilor conectati
    private void broadcast(String senderUsername, String message) {
        String fullMessage = senderUsername + ": " + message;

        for (ClientHandler client : clients) {
        	if (!client.getUsername().equals(senderUsername)) {
                client.send(fullMessage);
            }
        }
    }

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