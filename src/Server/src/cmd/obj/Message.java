package cmd.obj;

import java.io.Serializable;

public class Message implements Serializable{

    private final String content;
    private final int conversationId;
    private final String owner; //the user who send the messages

    public Message(int id, String content) {
        this.conversationId = id;
        this.content = content;
        this.owner = null;
    }
    
    public Message(int id, String owner, String content) {
        this.conversationId = id;
        this.content = content;
        this.owner = owner;
    }

    public String getContent() {
        return content;
    }

    public int getConversationId() {
        return conversationId;
    }

    public String getOwner() {
        return owner;
    }

}
