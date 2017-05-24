package server;

import cmd.cs.CreateConversation;
import cmd.sc.AccessCard;
import cmd.cs.Credentials;
import cmd.sc.GetMessages;
import cmd.cs.RecordMessage;
import cmd.general.SimplestInstruction;
import cmd.general.UpdateParticipantsList;
import cmd.sc.UpdateMessages;
import cmd.sc.UpdateConversationList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static server.DBManager.*;

class Handler extends Thread {

    protected static final List<Handler> hs;    //authenticated users <token, handler>
    protected static final int TKLEN = 90;  //the length of the token

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
            UpdateConversationList cvs = new UpdateConversationList(dbGetConversations(auth.getUserId()));
            out.writeObject(cvs);   //refresh conversations list

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
        Set<Integer> userIdsToBeNotified;
        switch (cmd) {
            case "SUBMITCREDENTIALS":   //not used
                Credentials c = (Credentials) ri;
                break;
            case "RECORDMESSAGE":
                RecordMessage rm = (RecordMessage) ri;
                String lastMessage = rm.getMessage();
                convId = rm.getConversationId();
                userIdsToBeNotified = dbStoreMessage(convId, this.auth.getUserId(), lastMessage); //register the message in the db
                //String content = dbGetMessages(rm.getConversationId()); //get conversation content
                SimplestInstruction si = new UpdateMessages(convId, this.auth.getUserName(), lastMessage);
                notifyChange(userIdsToBeNotified, si);
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

    private void sendInstructionToClient(SimplestInstruction i) throws IOException{
        out.writeObject(i);
    }
}
