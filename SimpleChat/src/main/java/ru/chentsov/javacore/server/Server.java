package ru.chentsov.javacore.server;

import ru.chentsov.javacore.server.service.AuthService;
import ru.chentsov.javacore.server.service.BaseAuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Evgenii Chentsov
 */
public final class Server {

    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AuthService authService = new BaseAuthService();

    public void init(final int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            authService.start();
            Socket socket = null;
            //looking for the clients
            while (true) {
                System.out.println("Server is awaiting connections");
                socket = serverSocket.accept();
                System.out.println("A client has connected to the server");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            authService.stop();
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isNickNameBusy(final String nickName) {
        for (ClientHandler client : clients) {
            if (client.getNickName().equals(nickName)) return true;
        }
        return false;
    }

    public synchronized void sendMessageToSingleClient (final String nickName, final String message) {
        for (ClientHandler client : clients) {
            if (client.getNickName().equals(nickName)) client.sendMsg(message);
        }
    }

    public synchronized void broadcastMsg(final String message) {
        for (ClientHandler client : clients) {
            client.sendMsg(message);
        }
    }

    public synchronized void unsubscribe(final ClientHandler client) {
        clients.remove(client);
    }

    public synchronized void subscribe(final ClientHandler client) {
        clients.add(client);
    }

}
