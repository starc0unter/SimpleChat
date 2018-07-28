package ru.chentsov.javacore.client;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.Arrays;

import static ru.chentsov.javacore.ChatConstants.*;

public final class ClientWindow extends JFrame {

    private final static String DEFAULT_MESSAGE_TEXT = "Write a message... ";

    private final GridBagLayout viewAreaLayout = new GridBagLayout();
    private final JPanel viewAreaPanel = new JPanel(viewAreaLayout);
    private final JTextArea viewMessageArea = new JTextArea();
    private final JScrollPane viewMessageScroll = new JScrollPane(viewMessageArea);
    private final JTextField sendMessageField = new JTextField();
    private final JButton sendMessageButton = new JButton(" SEND ");
    private final JPanel sendMessagePanel = new JPanel(new BorderLayout());
    private final DefaultListModel<String> infoClientListModel = new DefaultListModel<>();
    private final JList<String> infoClientList = new JList<>(infoClientListModel);
    private final JScrollPane infoClientScroll = new JScrollPane(infoClientList);

    private Session session;
    private AuthorizationWindow authWindow;
    private String nickName;

    public ClientWindow(final String serverAddress, final int serverPort) {
        session = new Session(serverAddress, serverPort);

        setTitle("client");
        setBounds(600,300, 500, 500);
        setMinimumSize(new Dimension(400, 300));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        addViewArea();
        addSendMessagePanel();
        addWindowListener(getWindowClosingAdapter());
    }

    void init() {
        try {
            session.openConnection();
            startAuthorization();
            nickName = authWindow.getNickName();
            this.setTitle("User: " + nickName);
        } catch (IOException e) {
            System.out.println("Cannot establish connection, exiting...");
            System.exit(1);
        }

        final Thread messageReaderThread = new Thread(() -> getMessages(session.getIn()));
        messageReaderThread.setDaemon(true);
        messageReaderThread.start();
    }

    private void startAuthorization() {
        this.setVisible(false);
        authWindow = new AuthorizationWindow(session);
        while (!authWindow.isAuthorized()); //do nothing
        nickName = authWindow.getNickName();
        authWindow.setVisible(false);
        this.setVisible(true);
    }

    private void addViewArea() {
        addViewMessageScroll();
        addInfoClientScroll();
        add(viewAreaPanel, BorderLayout.CENTER);

        final GridBagConstraints viewAreaConstraints = new GridBagConstraints();
        viewAreaConstraints.fill = GridBagConstraints.BOTH;
        viewAreaConstraints.gridx = 0;
        viewAreaConstraints.weightx = 1;
        viewAreaConstraints.weighty = 1;
        viewAreaConstraints.ipadx = 340;

        final GridBagConstraints infoAreaConstraints = new GridBagConstraints();
        infoAreaConstraints.fill = GridBagConstraints.VERTICAL;
        infoAreaConstraints.gridx = 1;
        infoAreaConstraints.weightx = 0;
        infoAreaConstraints.weighty = 1;
        infoAreaConstraints.ipadx = 50;

        viewAreaPanel.add(viewMessageScroll, viewAreaConstraints);
        viewAreaPanel.add(infoClientScroll, infoAreaConstraints);
    }

    private void addViewMessageScroll() {
        viewMessageArea.setEditable(false);
        viewMessageArea.setLineWrap(true);
        final DefaultCaret caret = (DefaultCaret) viewMessageArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    private void addInfoClientScroll() {
        infoClientList.setLayoutOrientation(JList.VERTICAL);
        infoClientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        infoClientList.addListSelectionListener(getListSelectionListener());
        infoClientScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        infoClientScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    private void addSendMessagePanel() {
        add(sendMessagePanel, BorderLayout.SOUTH);
        sendMessagePanel.add(sendMessageButton, BorderLayout.EAST);
        sendMessagePanel.add(sendMessageField, BorderLayout.CENTER);
        sendMessageField.addFocusListener(getSendMessageFieldFocusListener());
        sendMessageField.addActionListener(e -> sendMessage(session.getOut()));
        sendMessageField.setText(DEFAULT_MESSAGE_TEXT);

        sendMessageButton.addActionListener((e) -> {
            final boolean hasEmptyMessage = sendMessageField.getText().trim().isEmpty();
            final boolean hasDefaultMessage = sendMessageField.getText().equals(DEFAULT_MESSAGE_TEXT);
            if (!hasEmptyMessage && !hasDefaultMessage) {
                sendMessage(session.getOut());
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
                session.getOut().writeUTF(TERMINATE_CONNECTION_FLAG);
                session.getOut().flush();
                session.closeConnection();
            } catch (SocketException se) {
                System.out.println("Socket has already been closed");
            } catch (IOException exc) {
                exc.printStackTrace();
            }
            }
        };
    }

    private void getMessages(final DataInputStream in) {
        try {
            while (true) {
                final String message = in.readUTF();
                if (message.startsWith(COMMAND_SYMBOL)) {
                    if (TERMINATE_CONNECTION_FLAG.equalsIgnoreCase(message)) break;
                    if (message.startsWith(CLIENT_LIST_FLAG)) updateClientList(message);
                } else viewMessageArea.append(message + "\n");
            }
        } catch (Exception e) {
            System.out.println("Interrupting connection..");
        } finally {
            try {
                nickName = "";
                authWindow.setAuthorized(false);
                session.closeConnection();
                System.out.println("Session closed successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateClientList(final String message) {
        final String[] tokens = message.split("\\s");
        final String[] nickNames = Arrays.copyOfRange(tokens, 1, tokens.length);
        infoClientListModel.removeAllElements();
        for (int i = 0; i < nickNames.length; i++) {
            infoClientListModel.add(i, nickNames[i]);
        }
        infoClientList.repaint();
    }

    private ListSelectionListener getListSelectionListener() {
        return new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()){
                    final JList source = (JList)event.getSource();
                    final String nickName = source.getSelectedValue().toString();
                    sendMessageField.grabFocus();
                    sendMessageField.setText(PRIVATE_MESSAGE_FLAG + nickName + " ");
                }
            }
        };
    }

    private void sendMessage(final DataOutputStream out) {
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
