package client;

import java.io.File;
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

        client.setOnMessageReceived(message -> {
            System.out.println("\n[Mesaj primit] " + message);
            System.out.print("> ");
        });

        client.setOnFileReceived(file -> {
            System.out.println("\n[Fișier primit] " + file.getAbsolutePath());
            System.out.print("> ");
        });

        client.setOnFileListReceived(files -> {
            System.out.println("\n[Fișiere pe server]");

            if (files.isEmpty()) {
                System.out.println("Nu există fișiere pe server.");
            } else {
                for (String file : files) {
                    System.out.println("- " + file);
                }
            }

            System.out.print("> ");
        });

        client.connect();

        System.out.println("Client conectat.");
        System.out.println("Comenzi disponibile:");
        System.out.println("  mesaj normal");
        System.out.println("  /file cale/catre/fisier.txt");
        System.out.println("  /files");
        System.out.println("  /exit");
        System.out.println();

        while (true) {
            System.out.print("> ");
            String text = scanner.nextLine();

            if (text.equalsIgnoreCase("/exit")) {
                break;
            }

            if (text.equalsIgnoreCase("/files")) {
                client.requestFileList();
                continue;
            }

            if (text.startsWith("/file ")) {
                String path = text.substring("/file ".length()).trim();

                if (path.isBlank()) {
                    System.out.println("Trebuie să introduci calea către fișier.");
                    continue;
                }

                File file = new File(path);

                if (!file.exists()) {
                    System.out.println("Fișierul nu există: " + path);
                    continue;
                }

                if (!file.isFile()) {
                    System.out.println("Calea nu este un fișier valid: " + path);
                    continue;
                }

                client.sendFile(file);
                System.out.println("Fișier trimis: " + file.getName());
                continue;
            }

            if (text.isBlank()) {
                continue;
            }

            client.sendMessage(text);
        }

        client.close();
        scanner.close();

        System.out.println("Client închis.");
    }
}