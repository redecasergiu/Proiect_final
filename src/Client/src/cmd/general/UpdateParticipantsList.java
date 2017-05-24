package cmd.general;

import java.util.List;

public class UpdateParticipantsList<T> extends SimplestInstruction {

    private final static String instruction = "UPDATEPARTICIPANTSLIST";
    private final int conversationId;
    private final List<T> participants;

    public UpdateParticipantsList(int id, List<T> ps) {
        super(instruction);
        this.participants = ps;
        this.conversationId = id;
    }

    public List<T> getParticipants() {
        return participants;
    }

    public int getConversationId() {
        return conversationId;
    }

}
