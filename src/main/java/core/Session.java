package core;

import client.Client;

public class Session {
    private static Client client;

    public static void setClient(Client client) {
        Session.client = client;
    }

    public static Client getClient() {
        return client;
    }
}
