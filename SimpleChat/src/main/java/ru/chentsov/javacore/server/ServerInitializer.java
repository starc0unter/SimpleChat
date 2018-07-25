package ru.chentsov.javacore.server;

/**
 * @author Evgenii Chentsov
 */
public final class ServerInitializer {

    public static void main(String[] args) {
        final Server chatServer = new Server();
        chatServer.init(8189);
    }

}
