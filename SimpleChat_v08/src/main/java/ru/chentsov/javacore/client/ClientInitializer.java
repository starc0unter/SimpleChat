package ru.chentsov.javacore.client;

/**
 * @author Evgenii Chentsov
 */
public final class ClientInitializer {

    public static void main(String[] args) {
        final ClientWindow client = new ClientWindow("localhost", 8189);
        client.init();
    }

}
