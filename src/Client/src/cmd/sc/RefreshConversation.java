package cmd.sc;

import cmd.general.SimplestInstruction;

public class RefreshConversation extends SimplestInstruction {

    private final static String instruction = "REFRESHCONVERSATION";
    private final int conversationId;
    private final String content;

    public RefreshConversation(int id, String content) {
        super(instruction);
        this.conversationId = id;
        this.content = content;
    }

    public int getConversationId() {
        return conversationId;
    }

    public String getContent() {
        return content;
    }
}
