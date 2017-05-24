package client;

import cmd.cs.CreateConversation;
import cmd.cs.Credentials;
import cmd.cs.RecordMessage;
import cmd.general.SimplestInstruction;
import cmd.general.UpdateParticipantsList;
import cmd.obj.Message;
import cmd.sc.AccessCard;
import cmd.sc.GetMessages;
import cmd.sc.UpdateMessages;
import cmd.sc.UpdateConversationList;
import gui.MainW;
import gui.Window;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class Client {

    private static final String SERVERADDRESS = "localhost";
    private static final int PORT = 9001;

    private ObjectInputStream in;
    private ObjectOutputStream out;
    private SimplestInstruction ri; //interpretation of the output as an instruction

    private Window w;   //main window

    public Client() {
        Window.setClient(this);
    }

    /**
     * @return the server address
     */
    private String getServerAddress() {
        return SERVERADDRESS;
    }

    /**
     * Prompt for and return the desired screen name.
     *
     * @return array of strings containing the username and the password
     */
    @SuppressWarnings("empty-statement")
    private Object[] getCredentials() throws InterruptedException {
        Object[] options = new Boolean[]{true, false};
        return new Object[]{
            JOptionPane.showInputDialog(
            null,
            "Username:",
            "Login",
            JOptionPane.PLAIN_MESSAGE),
            JOptionPane.showInputDialog(
            null,
            "Password:",
            "Login",
            JOptionPane.PLAIN_MESSAGE),
            JOptionPane.showInputDialog(
            null,
            "Registration:",
            "Login",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            false)
        };
    }

    protected void run() throws IOException {

        //connect to the server
        String serverAddress = getServerAddress();
        try {
            Socket socket = new Socket(serverAddress, PORT);
            out = new ObjectOutputStream(socket.getOutputStream());  //message sent to the server
            in = new ObjectInputStream(socket.getInputStream());  //received message from server

            MainW mw;
            while (true) {
                ri = (SimplestInstruction) in.readObject(); //received instruction
                switch (ri.getCmd()) {
                    case "SUBMITCREDENTIALS":
                        Object[] credentials = getCredentials();
                        Credentials c = new Credentials((String) credentials[0], (String) credentials[1], (boolean) credentials[2]);
                        out.writeObject(c);
                        break;
                    case "ACCESSGRANTED":   //open the main window
                        AccessCard ac = (AccessCard) ri;
                        w = new MainW(null);
                        w.setVisible(true);
                        w.setTitle("YChat - " + ac.getUser());
                        display(ac.getGreeting());
                        break;
                    case "GETMESSAGES":
                        GetMessages gm = (GetMessages) ri;
                        mw = (MainW) w;
                        mw.receiveMessages(gm.getConversationId(), gm.getMessages());
                        break;
                    case "UPDATECONVERSATIONLIST":
                        UpdateConversationList ucs = (UpdateConversationList) ri;                         
                        mw = (MainW) w;
                        mw.updateConversations(ucs.getConversations());
                        break;
                    case "UPDATEMESSAGES":
                        UpdateMessages um = (UpdateMessages) ri;
                        mw = (MainW) w;
                        mw.updateMessages(um.getConversationId(), um.getUserName()+": "+um.getLastMessage()+"\n");
                        break;
                    case "UPDATECONVERSATION":
                        System.out.println("UPDATECONVERSATION");
                        break;
                    case "UPDATEPARTICIPANTSLIST":
                        UpdateParticipantsList<String> upPs = (UpdateParticipantsList<String>) ri;
                        mw = (MainW) w; // main window
                        mw.updateParticipants(upPs.getConversationId(), upPs.getParticipants());
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            display("error");
            for (StackTraceElement s : e.getStackTrace())
                System.out.println(s.toString());
            System.out.println(e.getMessage());
            //System.exit(42);
        }
    }

    @Deprecated
    public void sendMessage(int conversationId, String message) throws IOException {
        out.writeObject(new RecordMessage(conversationId, message));
    }

    public void sendMessage(int conversationId, Message msg) throws IOException{
        out.writeObject(new RecordMessage(conversationId, msg));
    }
    
    public void createConversation(String convName) throws IOException{
        out.writeObject(new CreateConversation(convName));
    }

    private void display(String message) {
        JOptionPane.showMessageDialog(null, message);
    }
    
    public void updateParticipants(int conversationId, List<String> newParticipants){
        try {
            out.writeObject(new UpdateParticipantsList<>(conversationId, newParticipants));
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void getMessages(int conversationId){
        try {
            out.writeObject(new SimplestInstruction("GETMESSAGES", conversationId));
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
