package cmd.cs;

import cmd.general.SimplestInstruction;

public class Credentials extends SimplestInstruction {

    private final static String instruction = "SUBMITCREDENTIALS";
    private final String user, pass;
    private final boolean register;

    
    public Credentials(String user, String pass, boolean register) {
        super(instruction);
        this.user = user;
        this.pass = pass;
        this.register = register;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }
    
    /**
     * @return true if the user wants to register a new account
     */
    public boolean isRegistration(){
        return this.register;
    }
}
