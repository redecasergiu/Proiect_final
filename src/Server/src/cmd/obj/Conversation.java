package cmd.obj;

import java.io.Serializable;

/**
 * Conversation DETAILS
 */
public class Conversation implements Serializable{
    private final int conversationId;
    private final String name;

    public Conversation(int convId, String name) {
        this.conversationId = convId;
        this.name = name;
    }

    public int getConversationId() {
        return conversationId;
    }

    public String getName() {
        return name;
    }
    
}
