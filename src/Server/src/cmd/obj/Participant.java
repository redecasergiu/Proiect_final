package cmd.obj;

import java.io.Serializable;

public class Participant implements Serializable {

    private final String name;

    public Participant(String p) {
        this.name = p;
    }

    public String getName() {
        return name;
    }
}
