package ru.chentsov.javacore.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * @author Evgenii Chentsov
 */
public final class Session {

    private String serverAddress;
    private int serverPort;
    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;

    public Session(final String serverAddress, final int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public DataInputStream getIn() {
        return in;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public Socket getSocket() {
        return socket;
    }

    void openConnection() throws IOException {
        socket = new Socket(serverAddress, serverPort);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    void closeConnection() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

}
