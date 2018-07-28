package ru.chentsov.javacore.server;

import ru.chentsov.javacore.server.service.AuthService;
import ru.chentsov.javacore.server.service.BaseAuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static ru.chentsov.javacore.ChatConstants.*;

/**
 * @author Evgenii Chentsov
 */
public final class Server {

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
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

    public boolean isNickNameBusy(final String nickName) {
        for (ClientHandler client : clients) {
            if (client.getNickName().equals(nickName)) return true;
        }

        return false;
    }

    public void sendMessageToClient(final String toNickName, final ClientHandler from, final String message) {
        for (ClientHandler client : clients) {
            if (client.getNickName().equals(toNickName)) {
                from.sendMessage(from.getCurrentDate() + PRIVATE_TO + client.getNickName() + ": " + message);
                client.sendMessage(from.getCurrentDate() + PRIVATE_FROM + from.getNickName() + ": " + message);
                return;
            }
        }
        from.sendMessage(NO_SUCH_USER_IN_ROOM);
    }

    public synchronized void broadcastMessage(final String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void broadcastClientList() {
        final StringBuilder clientList = new StringBuilder(CLIENT_LIST_FLAG);
        for (ClientHandler client : clients) {
            clientList.append(" ").append(client.getNickName());
        }
        broadcastMessage(clientList.toString());
    }

    public void unsubscribe(final ClientHandler client) {
        clients.remove(client);
        if (!client.getNickName().isEmpty()) {
            broadcastMessage(client.getNickName() + CHAT_LEFT);
            broadcastClientList();
        }
    }

    public void subscribe(final ClientHandler client) {
        clients.add(client);
        broadcastMessage(client.getNickName() + CHAT_ENTERED);
        broadcastClientList();
    }

}
