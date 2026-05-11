package client;

import java.io.*;
import java.net.Socket;

public class FileClient {
    private final String host;
    private final int port;

    public FileClient(String host, int port) {
        this.host = host.trim();
        this.port = port;
    }

    public void sendFile(File file) throws IOException {
        try (
                Socket socket = new Socket(host, port);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                FileInputStream fileIn = new FileInputStream(file)
        ) {
            out.writeUTF(file.getName());
            out.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            out.flush();

            System.out.println("File sent: " + file.getName());
        }
    }
}