package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	int port;
	Server(int port){
		this.port = port;
	}
	
	void start() {
		try(ServerSocket serverSocket = new ServerSocket(port)){
			System.out.println("Server started at port " + port);
			
			
			
			while(true){
				Socket clientSocket = serverSocket.accept();
				
				new Thread(() -> {
					handleClient(clientSocket);
				}).start();
				
				
			}
			
		} catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	
	void clientConnect() {
		
	}
	
	private void handleClient(Socket clientSocket) {
	    try {
	        BufferedReader in = new BufferedReader(
	                new InputStreamReader(clientSocket.getInputStream())
	        );

	        String username = in.readLine();
	        System.out.println(username + " connected");

	        String message;

	        while ((message = in.readLine()) != null) {
	            System.out.println(username + ": " + message);
	        }

	        System.out.println(username + " disconnected");

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

}
