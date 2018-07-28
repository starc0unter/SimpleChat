package ru.chentsov.javacore.client;

import javax.swing.*;
import java.awt.*;
import java.io.*;

import static ru.chentsov.javacore.ChatConstants.*;


public final class AuthorizationWindow extends JFrame {

    private final JLabel infoLabel = new JLabel("Please authorize to enter the chat");
    private final JLabel loginLabel = new JLabel("Login:  ");
    private final JLabel passwordLabel = new JLabel("Password:  ");
    private final JLabel nickNameLabel = new JLabel("Nickname: ");
    private final JTextField loginField = new JTextField();
    private final JTextField nicknameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPanel nickNamePanel = new JPanel();
    private final JPanel loginPanel = new JPanel();
    private final JPanel passwordPanel = new JPanel();
    private final JPanel buttonPanel = new JPanel();
    private final JButton okButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Exit");
    private final JCheckBox newClientCheckBox = new JCheckBox("New Client");
    private final Dimension buttonSize = new Dimension(100, 30);

    private Session session;
    private String nickName;
    private volatile boolean isAuthorized = false;

    AuthorizationWindow(final Session session) {
        this.session = session;

        setTitle("Auth");
        setBounds(600,300, 250, 250);
        setResizable(false);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        final GridLayout mainLayout = new GridLayout(6, 1);
        setLayout(mainLayout);

        setUpInfoPanel();
        setUpAuthPanel();
        setUpButtonPanel();

        add(infoLabel);
        add(newClientCheckBox);
        add(nickNamePanel);
        add(loginPanel);
        add(passwordPanel);
        add(buttonPanel);

        setVisible(true);
    }

    public String getNickName() {
        return nickName;
    }

    void setAuthorized(final boolean authorized) {
        isAuthorized = authorized;
    }

    boolean isAuthorized() {
        return isAuthorized;
    }

    private void setUpInfoPanel() {
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        newClientCheckBox.setHorizontalAlignment(SwingConstants.RIGHT);
        newClientCheckBox.addChangeListener((e) -> {
            if (!newClientCheckBox.isSelected()) nicknameField.setEditable(false);
            else nicknameField.setEditable(true);
        });
    }

    private void setUpAuthPanel() {
        nickNamePanel.setLayout(new GridLayout(1, 2));
        nickNameLabel.setHorizontalAlignment(JLabel.RIGHT);
        nicknameField.setEditable(false);
        nickNamePanel.add(nickNameLabel);
        nickNamePanel.add(nicknameField);

        loginPanel.setLayout(new GridLayout(1, 2));
        loginLabel.setHorizontalAlignment(JLabel.RIGHT);
        loginPanel.add(loginLabel);
        loginPanel.add(loginField);

        passwordPanel.setLayout(new GridLayout(1, 2));
        passwordLabel.setHorizontalAlignment(JLabel.RIGHT);
        passwordPanel.add(passwordLabel);
        passwordPanel.add(passwordField);
    }

    private void setUpButtonPanel() {
        cancelButton.setPreferredSize(buttonSize);
        cancelButton.addActionListener((e) -> System.exit(0));
        okButton.setPreferredSize(buttonSize);
        okButton.addActionListener((e) -> new Thread(() -> onOKClick(session.getIn(), session.getOut())).start());
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
    }

    //in and out cannot be final here - if server closes the socket, we need
    //to re-establish connection here
    private void onOKClick(DataInputStream in, DataOutputStream out) {
        okButton.setEnabled(false);
        newClientCheckBox.setEnabled(false);

        try {
            final boolean hasInputData = in.available() > 0;
            if (hasInputData && in.readUTF().startsWith(AUTHORIZE_TIMEOUT)) {
                session.closeConnection();
                session.openConnection();
                in = session.getIn();
                out = session.getOut();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (newClientCheckBox.isSelected()) {
            infoLabel.setText("Registering...");
            tryAuthorize(in, out, REGISTER_FLAG, REGISTER_OK_FLAG);
        } else {
            infoLabel.setText("Authorizing...");
            tryAuthorize(in, out, AUTHORIZE_FLAG, AUTHORIZE_OK_FLAG);
        }
    }

    private void tryAuthorize(final DataInputStream in, final DataOutputStream out,
                              final String actionFlag, final String actionSuccessFlag) {
        try {
            out.writeUTF(actionFlag + loginField.getText()
                    + " " + String.valueOf(passwordField.getPassword())
                    + " " + nicknameField.getText());
            out.flush();
            clearFields();
            while (true) {
                final String infoMessage = in.readUTF();
                if (infoMessage.startsWith(actionSuccessFlag)) {
                    nickName = infoMessage.split("\\s")[1];
                    setAuthorized(true);
                    break;
                }
                infoLabel.setText(infoMessage);
                okButton.setEnabled(true);
                newClientCheckBox.setEnabled(true);
            }
        } catch (IOException e) {
            infoLabel.setText("Failed to connect");
            okButton.setEnabled(true);
            newClientCheckBox.setEnabled(true);
        }
    }

    private void clearFields() {
        nicknameField.setText("");
        loginField.setText("");
        passwordField.setText("");
    }

}
