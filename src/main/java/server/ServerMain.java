package server;

public class ServerMain {
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Server(port).start();
    }
}
