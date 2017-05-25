package cmd.general;

import java.io.File;

public class TransferFile extends SimplestInstruction {

    private final static String instruction1 = "STOREFILE";
    private final static String instruction2 = "RETRIEVEFILE";
    private final File file;
    private final byte[] content;
    private final int conversationId;

    /**
     * @param conversationId the id of the conversation where the file was
     * posted
     * @param file
     * @param store if true the file should be stored (by the server) else it
     * will be retrieved (by the client)
     */
    public TransferFile(int conversationId, File file, byte[] content, boolean store) {
        super(store ? instruction1 : instruction2);
        this.file = file;
        this.content = content;
        this.conversationId = conversationId;
    }

    public File getFile(){
        return this.file;
    }
   
    public int getConversationId() {
        return conversationId;
    }
    
    
    public String getFileName(){
        return this.file.getName();
    }

    public byte[] getContent() {
        return content;
    }
}
