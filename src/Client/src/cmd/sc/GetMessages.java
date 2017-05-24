package cmd.sc;

import cmd.general.SimplestInstruction;
import cmd.obj.Message;
import java.util.List;

/**
 * The client requests to receive the messages of a conversation
 */
public class GetMessages extends SimplestInstruction{
    private final static String instruction = "GETMESSAGES";
    private final int conversationId;
    private final List<Message> messages;
    
    public GetMessages(int conversationId, List<Message> messages){
        super(instruction);
        this.conversationId = conversationId;
        this.messages = messages;
    }
    
    public int getConversationId(){
        return this.conversationId;
    }
    
    public List<Message> getMessages(){
        return this.messages;
    }
}
