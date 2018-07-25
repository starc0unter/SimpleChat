package ru.chentsov.javacore.client;

import ru.chentsov.javacore.ChatConstants;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public final class ClientWindow extends JFrame {

    private final static String DEFAULT_MESSAGE_TEXT = "Write a message... ";

    private final JTextField sendMessageField = new JTextField();
    private final JTextArea viewMessageArea = new JTextArea();
    private final JButton sendMessageButton = new JButton("SEND");
    private final JPanel sendMessagePanel = new JPanel(new BorderLayout ());
    private final JScrollPane viewMessageScroll = new JScrollPane(viewMessageArea);

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private AuthorizationWindow authWindow;

    void init(final String serverAddress, final int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            in = new DataInputStream(socket.getInputStream ());
            out = new DataOutputStream(socket.getOutputStream ());
            startAuthorization();
        } catch (IOException e) {
            System.out.println("Cannot establish connection, exiting...");
            System.exit(1);
        }

        setTitle("client");
        setBounds(600,300, 500, 500);
        setMinimumSize(new Dimension(400, 300));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        addViewMessageArea();
        addSendMessagePanel();
        addWindowListener(getWindowClosingAdapter());

        final Thread messageReaderThread = new Thread(this::getMessages);
        messageReaderThread.setDaemon(true);
        messageReaderThread.start();
        setVisible(true);
    }

    private void startAuthorization() {
        authWindow = new AuthorizationWindow(in, out);
        while (!authWindow.isAuthorized()); //do nothing
        authWindow.setVisible(false);
    }

    private void addViewMessageArea() {
        viewMessageArea.setEditable(false);
        viewMessageArea.setLineWrap(true);
        DefaultCaret caret = (DefaultCaret) viewMessageArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        add(viewMessageScroll,BorderLayout.CENTER);
    }

    private void addSendMessagePanel() {
        add(sendMessagePanel, BorderLayout.SOUTH);
        sendMessagePanel.add(sendMessageButton, BorderLayout.EAST);
        sendMessagePanel.add(sendMessageField, BorderLayout.CENTER);
        sendMessageField.addFocusListener(getSendMessageFieldFocusListener());
        sendMessageField.addActionListener(e -> sendMessage());
        sendMessageField.setText(DEFAULT_MESSAGE_TEXT);

        sendMessageButton.addActionListener((e) -> {
            boolean hasEmptyMessage = sendMessageField.getText().trim().isEmpty();
            boolean hasDefaultMessage = sendMessageField.getText().equals(DEFAULT_MESSAGE_TEXT);
            if (!hasEmptyMessage && !hasDefaultMessage) {
                sendMessage();
                sendMessageField.grabFocus();
            }
        });
    }

    private WindowAdapter getWindowClosingAdapter() {
        return new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                try {
                    out.writeUTF(ChatConstants.TERMINATE_CONNECTION_FLAG);
                    out.flush();
                    socket.close();
                    out.close();
                    in.close();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        };
    }

    private void getMessages() {
        try {
            while (true) {
                final String message = in.readUTF();
                if (message.equalsIgnoreCase(ChatConstants.TERMINATE_CONNECTION_FLAG)) break;
                viewMessageArea.append(message + "\n");
            }
        } catch (Exception e) {
            System.out.println("Interrupted connection..");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            authWindow.setAuthorized(false);
        }
    }

    private void sendMessage() {
        try {
            out.writeUTF(sendMessageField.getText());
            out.flush();
            sendMessageField.setText("");
        } catch (IOException e) {
            System.out.println("Cannot send message");
        }
    }

    private FocusListener getSendMessageFieldFocusListener() {
        return new FocusListener() {
            @Override
            public void focusGained(final FocusEvent e) {
                if (DEFAULT_MESSAGE_TEXT.equals(sendMessageField.getText()))  sendMessageField.setText("");
            }

            @Override
            public void focusLost(final FocusEvent e) {
                if (sendMessageField.getText().isEmpty()) sendMessageField.setText(DEFAULT_MESSAGE_TEXT);
            }
        };
    }

}
