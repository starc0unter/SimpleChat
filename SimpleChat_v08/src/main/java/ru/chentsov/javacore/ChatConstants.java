package ru.chentsov.javacore;

/**
 * @author Evgenii Chentsov
 */
public final class ChatConstants {

    public static final String TERMINATE_CONNECTION_FLAG = "/end ";
    public static final String PRIVATE_MESSAGE_FLAG = "/w ";
    public static final String AUTHORIZE_OK_FLAG = "/authOK ";
    public static final String AUTHORIZE_FLAG = "/auth ";
    public static final String AUTHORIZE_TIMEOUT = "/authTimeout ";
    public static final String REGISTER_FLAG = "/newClient ";
    public static final String REGISTER_OK_FLAG = "/newClientOK ";
    public static final String CLIENT_LIST_FLAG = "/clients";
    public static final String CHAT_ENTERED = " entered the chat";
    public static final String CHAT_LEFT = " left the chat";
    public static final String FROM = "from ";
    public static final String PRIVATE_FROM = "private from ";
    public static final String PRIVATE_TO = "private to ";
    public static final String NO_SUCH_USER_IN_ROOM = "There is no such user in the room";
    public static final String COMMAND_SYMBOL = "/";

    private ChatConstants() {};

}
