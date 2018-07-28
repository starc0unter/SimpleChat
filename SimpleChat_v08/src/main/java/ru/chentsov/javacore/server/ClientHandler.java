package ru.chentsov.javacore.server;

import ru.chentsov.javacore.server.service.BaseAuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static ru.chentsov.javacore.ChatConstants.*;

/**
 * @author Evgenii Chentsov
 */
public final class ClientHandler {

    private Server server;
    private Socket socket;
    private String nickName;
    private DataInputStream in;
    private DataOutputStream out;

    public ClientHandler(final Server server, final Socket socket) {
        this.socket = socket;
        this.server = server;
        this.nickName = "";

        //registering each user in a new thread
        try {
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            final Thread messageHandlerThread = new Thread(this::handleMessages);
            messageHandlerThread.start();
        } catch (IOException e) {
            throw new RuntimeException ("Issues during account creation occurred" );
        }
    }

    public String getNickName() {
        return nickName;
    }

    private void handleMessages() {
        try {
            if (handleClientRequest()) getMessages();
        }  catch (SocketException | EOFException e) {
            System.out.println("Session has been aborted");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            server.unsubscribe(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method handles Client authorization. In case of authorization type,
     * client can create new account or use an old one.
     * @return true if user has been successfully authorized or false otherwise
     * @throws IOException
     */
    private boolean handleClientRequest() throws IOException {
        final int timeoutInSeconds = 120;
        final long startAuthTime = new Date().getTime();
        while (true) {
            if (in.available() > 0) {
                final String message = in.readUTF();
                if (message.startsWith(AUTHORIZE_FLAG)) {
                    if (authorizeClient(message)) return true;
                } else if (message.startsWith(REGISTER_FLAG)) {
                    if (registerClient(message)) return true;
                }
            }

            final long endAuthTime = new Date().getTime();
            if ((endAuthTime - startAuthTime) / 1000 > timeoutInSeconds) {
                System.out.println("closing socket");
                sendMessage(AUTHORIZE_TIMEOUT);
                return false;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * a single attempt of Client authorization
     * @param message service message that contains authorization data
     * @return true is attempt was successful and false otherwise
     */
    private boolean authorizeClient(final String message) {
        final int expectedTokensAmount = 3;
        final String[] authData = message.split("\\s");
        if (authData.length != expectedTokensAmount) {
            sendMessage("Please fill all the fields (no spaces)");
            return false;
        }

        final String login = authData[1];
        final String password = authData[2];
        final String nickName = server.getAuthService().getNickByLoginPass(login, password);
        if (nickName != null) {
            if (!server.isNickNameBusy(nickName)) {
                sendMessage(AUTHORIZE_OK_FLAG + nickName);
                this.nickName = nickName;
                server.subscribe(this);
                return true;
            } else sendMessage("Account is already in use");
        } else sendMessage("Wrong login/password pair");

        return false;
    }

    /**
     * a single attempt of Client registration
     * @param message service message that contains authorization data
     * @return true is attempt was successful and false otherwise
     */
    private boolean registerClient(final String message) {
        final int expectedTokensAmount = 4;
        final String[] authData = message.split("\\s");
        if (authData.length != expectedTokensAmount) {
            sendMessage("Please fill all the fields (no spaces)");
            return false;
        }

        final String nickName = authData[3];
        final String login = authData[1];
        final String password = authData[2];
        if (server.getAuthService() instanceof BaseAuthService) {
            final BaseAuthService authService = ((BaseAuthService) server.getAuthService());
            final boolean containsLogin = authService.containsLogin(login);
            final boolean containsNickName = authService.containsNickName(nickName);

            if (!containsLogin) {
                if (!containsNickName) {
                    authService.addEntry(login, password, nickName);
                    sendMessage(REGISTER_OK_FLAG + nickName);
                    this.nickName = nickName;
                    server.subscribe(this);
                    return true;
                } else sendMessage("Nickname is already in use");
            } else sendMessage("Login is already in use");
        }

        return false;
    }

    public String getCurrentDate() {
        final SimpleDateFormat messageDateFormat = new SimpleDateFormat("HH:mm");
        return  "|" + messageDateFormat.format(new Date()) + "| ";
    }

    private void getMessages() throws IOException{
        while (true) {
            final String messageText = in.readUTF();
            if (messageText.startsWith(COMMAND_SYMBOL)) {
                System.out.println(FROM + nickName + ": " + messageText);
                if (TERMINATE_CONNECTION_FLAG.equals(messageText)) break;
                if (messageText.startsWith(PRIVATE_MESSAGE_FLAG)) {
                    final String[] parts = messageText.split("\\s");
                    final String nickName = parts[1];
                    final String message = messageText.substring(PRIVATE_MESSAGE_FLAG.length()
                            + nickName.length() + 1);
                    server.sendMessageToClient(nickName, this, message);
                }
            } else server.broadcastMessage(getCurrentDate() + nickName + ": " + messageText);
        }
    }

    public void sendMessage(final String message) {
        try {
            out.writeUTF(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
