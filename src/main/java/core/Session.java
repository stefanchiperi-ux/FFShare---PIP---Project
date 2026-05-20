package core;

import client.Client;

public class Session {
    private static Client client;
    private static User currentUser;

    public static void setClient(Client client) {
        Session.client = client;
    }

    public static Client getClient() {
        return client;
    }
    
    public static void setCurrentUser(User user) {
    	Session.currentUser = user;
    }
    
    public static User getCurrentUser() {
    	return currentUser;
    }
}
