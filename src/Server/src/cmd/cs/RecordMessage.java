package cmd.cs;

import cmd.general.SimplestInstruction;
import cmd.obj.Message;


public class RecordMessage extends SimplestInstruction{
    private final static String instruction = "RECORDMESSAGE";
    private final int conversationId;
    private final String message;
    
    public RecordMessage(int conversationId, String message){
        super(instruction);
        this.conversationId = conversationId;
        this.message = message;
    }
    
    public RecordMessage(int conversationId, Message msg){
        super(instruction);
        this.conversationId = conversationId;
        this.message = msg.getContent();
    }
    
    public int getConversationId(){
        return this.conversationId;
    }
    
    public String getMessage(){
        return this.message;
    }
}
