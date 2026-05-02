package client;

import java.io.IOException;
import java.util.Scanner;

import core.Session;

public class ClientMain {

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Username: ");
        String username = scanner.nextLine();

        while (username.isBlank()) {
            System.out.print("Username invalid. Introdu username: ");
            username = scanner.nextLine();
        }

        Client client = new Client(username);
        Session.setClient(client);

        client.connect();

        System.out.println("Scrie mesaje. Scrie /exit ca să ieși.");

        while (true) {
            String text = scanner.nextLine();

            if (text.equalsIgnoreCase("/exit")) {
                break;
            }

            client.sendMessage(text);
        }

        client.close();
        scanner.close();
    }
}