package cmd.general;

@Deprecated
public class TransferFile2 extends SimplestInstruction {

    private final static String instruction1 = "STOREFILE";
    private final static String instruction2 = "RETRIEVEFILE";
    private final byte[] content;
    private final String fileName;
    private final int conversationId;

    /**
     * @param conversationId the id of the conversation where the file was
     * posted
     * @param fileName
     * @param content
     * @param store if true the file should be stored (by the server) else it
     * will be retrieved (by the client)
     */
    public TransferFile2(int conversationId, String fileName, byte[] content, boolean store) {
        super(store ? instruction1 : instruction2);
        this.content = content;
        this.fileName = fileName;
        this.conversationId = conversationId;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }

    public int getConversationId() {
        return conversationId;
    }
}
