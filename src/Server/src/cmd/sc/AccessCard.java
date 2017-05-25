package cmd.sc;

import cmd.general.SimplestInstruction;


public class AccessCard extends SimplestInstruction{
    private final static String instruction = "ACCESSGRANTED";
    private String user;
    private String greeting;
    
    public AccessCard(String name){
        super(instruction);
        this.user = name;
    }
    
    public AccessCard(String name, String greeting){
        super(instruction);
        this.user = name;
        this.greeting = greeting;
    }
    
    public String getUser(){
        return this.user;
    }
    
    public void setUser(String user){
        this.user = user;
    }
    
    public String getGreeting(){
        return this.greeting;
    }
}
