package cmd.sc;

import cmd.general.SimplestInstruction;
import cmd.obj.Conversation;
import java.util.List;

public class UpdateConversationList extends SimplestInstruction {

    private final static String instruction = "UPDATECONVERSATIONLIST";
    private final List<Conversation> conversations;

    public UpdateConversationList() {
        super(instruction);
        this.conversations = null;
    }
    
    public UpdateConversationList(List<Conversation> conversations) {
        super(instruction);
        this.conversations = conversations;
    }

    public void addConersation(Conversation c) {
        this.conversations.add(c);
    }

    public List<Conversation> getConversations() {
        return conversations;
    }
}
