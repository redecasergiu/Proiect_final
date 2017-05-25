package cmd.general;

import java.io.Serializable;

public class SimplestInstruction implements Serializable {

    private final String cmd;     //command
    private final Object param; //additional parameter to be sent for settings

    public SimplestInstruction(String cmd) {
        this.cmd = cmd;
        this.param = null;
    }
    
    public SimplestInstruction(String cmd, Object o) {
        this.cmd = cmd;
        this.param = o;
    }

    public String getCmd() {
        return cmd;
    }

    public Object getParam() {
        return param;
    }
    
}
