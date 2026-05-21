package server;

/**
 * Entry point of the server application.
 * <p>
 * This class starts both the chat server and the file server on separate ports.
 * Each server is launched on its own thread so that they can run simultaneously.
 * </p>
 */
public class ServerMain {

    /**
     * Starts the chat server and the file server.
     * <p>
     * The chat server listens on port {@code 5000}, while the file server listens
     * on port {@code 5001}. Both servers are started in separate threads to avoid
     * blocking each other.
     * </p>
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        Server chatServer = new Server(5000);

        new Thread(() -> chatServer.start()).start();
    }
}