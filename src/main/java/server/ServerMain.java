package server;

public class ServerMain {

    /**
     * Starts the chat server.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        Server chatServer = new Server(5000);
        new Thread(chatServer::start).start();
    }
}
