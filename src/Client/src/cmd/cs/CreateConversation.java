package cmd.cs;

import cmd.general.SimplestInstruction;

public class CreateConversation extends SimplestInstruction {

    private final static String instruction = "CREATECONVERSATION";
    private String convName;    //conversation name

    public CreateConversation(String name) {
        super(instruction);
        this.convName = name;
    }

    public String getConvName() {
        return convName;
    }

}
