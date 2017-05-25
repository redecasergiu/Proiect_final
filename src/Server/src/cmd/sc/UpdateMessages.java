package cmd.sc;

import cmd.general.SimplestInstruction;

/**
 * Add another message to the conversation
 */
public class UpdateMessages extends SimplestInstruction {

    private final static String instruction = "UPDATEMESSAGES";
    private final int conversationId;
    private final String lastMessage;
    private final String userName;

    public UpdateMessages(int id, String userName, String message) {
        super(instruction);
        this.conversationId = id;
        this.lastMessage = message;
        this.userName = userName;
    }

    public int getConversationId() {
        return conversationId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getUserName() {
        return userName;
    }

}
