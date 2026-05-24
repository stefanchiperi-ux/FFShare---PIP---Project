package core;

import client.Client;

/**
 * Pastreaza clientul conectat pentru toata aplicatia.
 */
public class Session {
    private static Client client;

    /**
     * Seteaza clientul curent.
     *
     * @param client clientul conectat
     */
    public static void setClient(Client client) {
        Session.client = client;
    }

    /**
     * Intoarce clientul curent al sesiunii.
     *
     * @return clientul conectat sau null
     */
    public static Client getClient() {
        return client;
    }
}
