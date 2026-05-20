package server;

public class ServerMain {

	public static void main(String[] args) {
		Server chatServer = new Server(5000);
        FileServer fileServer = new FileServer(5001);

        new Thread(() -> chatServer.start()).start();
        new Thread(() -> fileServer.start()).start();
	}

}
