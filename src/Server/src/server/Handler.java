package server;

import cmd.cs.CreateConversation;
import cmd.sc.AccessCard;
import cmd.cs.Credentials;
import cmd.sc.GetMessages;
import cmd.cs.RecordMessage;
import cmd.general.SimplestInstruction;
import cmd.general.TransferFile;
import cmd.general.UpdateParticipantsList;
import cmd.sc.UpdateMessages;
import cmd.sc.UpdateConversationList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static server.DBManager.*;

class Handler extends Thread {

    protected static final List<Handler> hs;    //authenticated users <token, handler>
    protected static final int TKLEN = 90;  //the length of the token
    private static final Random RANDOM = new SecureRandom();
    private static final String FILES_FOLDER = "./_files/";

    static {
        hs = new ArrayList<>();
    }

    //authentication fields
    private Authentication auth;

    //connection fields
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public Handler(Socket socket) {
        this.socket = socket;
        this.auth = null;
    }

    /**
     * @return id of the user related to this handler
     */
    public int getUid() {
        return this.auth.getUserId();
    }

    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());  //message sent to the server
            in = new ObjectInputStream(socket.getInputStream());  //received message from server
            SimplestInstruction ri; //received instruction

            //check user credentials
            String pass, name;
            int uid;
            while (true) {
                out.writeObject(new Credentials(null, null, false));
                ri = (SimplestInstruction) in.readObject();
                if (!ri.getCmd().equals("SUBMITCREDENTIALS")) {
                    continue;
                }
                Credentials c = (Credentials) ri;
                name = c.getUser();
                pass = c.getPass();
                if (name == null || pass == null || name.length() == 0) {
                    continue;
                }

                if (c.isRegistration()) {
                    uid = dbRegister(name, pass);
                } else {
                    uid = dbCheckCredentials(name, pass);
                }

                if ((uid) < 0) {
                    continue;
                }

                break;
            }

            //initialize client app
            out.writeObject(new AccessCard(name, "Hi, " + name + "!")); //say hi
            this.auth = new Authentication(name, uid);

            out.writeObject(new UpdateConversationList(dbGetConversations(auth.getUserId())));  //refresh conversations list
            out.writeObject(new SimplestInstruction("REFRESHCONTACTS", dbGetContacts(this.getUid()))); //refresh participants list

            //talk with the authenticated client
            while (true) {
                ri = (SimplestInstruction) in.readObject();
                if (ri == null) {
                    return;
                }
                exec(ri);    //interpret the user action
            }
        } catch (ClassNotFoundException | SQLException | IOException ex) {
            //Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getLocalizedMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    private void exec(SimplestInstruction ri) throws SQLException, IOException {
        String cmd = ri.getCmd();

        int convId;
        String filePath;
        Set<Integer> userIdsToBeNotified;

        switch (cmd) {
            case "ADDCONTACT":
                String name = (String) ri.getParam();
                List<String> contacts = dbAddContact(this.getUid(), name);
                out.writeObject(new SimplestInstruction("REFRESHCONTACTS", contacts));
                break;
            case "SUBMITCREDENTIALS":   //not used
                Credentials c = (Credentials) ri;
                break;
            case "RECORDMESSAGE":
                recordMessage(ri);
                break;
            case "STOREFILE":   //the user sends a file to server
                storeFile((TransferFile)ri);
                break;
            case "GETFILE": //the user requests a file
                sendFile(ri);
                break;
            case "GETMESSAGES": //the user asks for the messages of a conversation
                convId = (int) ri.getParam();    //conversation id
                GetMessages oobjget = new GetMessages(convId, dbGetMessages(convId));
                userIdsToBeNotified = dbGetUsers(convId); //get the users to be notified
                notifyChange(userIdsToBeNotified, oobjget);
                break;
            case "CREATECONVERSATION":
                CreateConversation cc = (CreateConversation) ri;
                int id = dbCreateConversation(auth.getUserId(), cc.getConvName());
                if (id != -1) {  //if the conversation was successfully created
                    UpdateConversationList cvs = new UpdateConversationList(dbGetConversations(auth.getUserId()));
                    out.writeObject(cvs);
                }
                break;
            case "UPDATEPARTICIPANTSLIST": //update the participants list, then send it to the client
                UpdateParticipantsList<String> upl = (UpdateParticipantsList<String>) ri;
                convId = upl.getConversationId();
                for (String p : upl.getParticipants()) { //get the new participants
                    dbAddParticipant(convId, p);
                }
                out.writeObject(new UpdateParticipantsList<>(convId, dbGetParticipants(convId)));
                break;
        }
    }

    /**
     * @param length string length
     * @return a random string
     */
    static String genRandStr(int length) {
        String letters = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM1234567890";
        String pw = "";
        for (int i = 0; i < length; i++) {
            int index = (int) (RANDOM.nextDouble() * letters.length());
            pw += letters.substring(index, index + 1);
        }
        return pw;
    }

    //Do not edit
    protected static String genRandStr() {
        return genRandStr(Handler.TKLEN);
    }

    @Deprecated
    private void updateConversation(String content) {
        //to be continued
    }

    private void notifyChange(Set<Integer> userIdsToBeNotified, SimplestInstruction i) {
        for (Handler h : Handler.hs) { //update conversation messages for the required users (those who can see the conversation of the message)
            if (userIdsToBeNotified.contains(h.getUid())) {
                try {
                    h.sendInstructionToClient(i);
                } catch (IOException ex) {
                    Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private void sendInstructionToClient(SimplestInstruction i) throws IOException {
        out.writeObject(i);
    }

    private void recordMessage(SimplestInstruction ri) throws SQLException {
        RecordMessage rm = (RecordMessage) ri;
        String lastMessage = rm.getMessage();
        int convId = rm.getConversationId();
        Set<Integer> userIdsToBeNotified = dbStoreMessage(convId, this.auth.getUserId(), lastMessage); //register the message in the db
        //String content = dbGetMessages(rm.getConversationId()); //get conversation content
        SimplestInstruction si = new UpdateMessages(convId, this.auth.getUserName(), lastMessage);
        notifyChange(userIdsToBeNotified, si);
    }

    /**
     * Store a file
     *
     * @param tf
     * @throws FileNotFoundException
     */
    private void storeFile(TransferFile tf) throws FileNotFoundException, SQLException, IOException {
        String token = genRandStr();
        String filePath = FILES_FOLDER + token + tf.getFileName();
        try (FileOutputStream fos = new FileOutputStream(filePath)) {   //store the file
            fos.write(tf.getContent());
        }
        recordMessage(new RecordMessage(tf.getConversationId(), "<FILE TOKEN> " + token)); //send a message to the participants with the token
    }

    private void sendFile(SimplestInstruction ri) {
        String token = (String) ri.getParam();
        File folder = new File(FILES_FOLDER);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            File f = listOfFiles[i];
            if (f.isFile()) {
                if (f.getName().substring(0,90).equals(token)){
                    try {
                        Path path = Paths.get(f.getAbsolutePath());
                        byte[] content;
                        content = Files.readAllBytes(path); //read file content
                        out.writeObject(new TransferFile(this.getUid(), f, content, false));
                    } catch (IOException ex) {
                        Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }
}
