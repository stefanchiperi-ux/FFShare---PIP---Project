package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
	private String host = "localhost";
    private int port = 5000;
    private String username;

    private Socket socket;
    private PrintWriter out;

    public Client(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    public Client(String username) {
        this.username = username;
    }

    public void connect() throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);

        System.out.println("Connected to server");

        out.println(username); 
    }

    public void sendMessage(String text) {
        if (out != null) {
            out.println(text); 
        }
    }

    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}
