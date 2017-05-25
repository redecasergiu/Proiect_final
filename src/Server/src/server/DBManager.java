package server;

import cmd.obj.Conversation;
import cmd.obj.Message;
import java.security.MessageDigest;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static server.Server.conn;

public class DBManager {


    /**
     * @param base input string
     * @return hashed string
     * @link https://stackoverflow.com/a/11009612
     */
    protected static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Create a new conversation in the database.
     *
     * @param uid user id
     * @return the created conversation id or -1 in case of error
     */
    protected static int dbCreateConversation(int uid, String convName) throws SQLException {
        try (CallableStatement stmt = conn.prepareCall("select createConversation(?,?) as 'id';")) {
            stmt.setInt(1, uid);
            stmt.setString(2, convName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
                return -1;
            }
        }
    }


    /**
     * @param user username
     * @param pass plain password
     * @return user id (from the db) or -1 if the username already exists
     */
    protected static int dbRegister(String user, String pass) throws SQLException {
        String salt = Handler.genRandStr(256);
        String hpass = sha256(pass + salt);
        try (CallableStatement stmt = conn.prepareCall("select registerUser(?,?,?) as 'id';")) {
            stmt.setString(1, user);
            stmt.setString(2, hpass);
            stmt.setString(3, salt);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return (rs.getInt("id"));
                }
                return -1;
            }
        }
    }

    /**
     * @param user
     * @param pass
     * @return user id or -1 if the credentials are false
     * @throws SQLException
     */
    protected static int dbCheckCredentials(String user, String pass) throws SQLException {
        String salt, hpass;
        int id;
        try (CallableStatement stmt = conn.prepareCall("call getCredentials(?);")) {
            stmt.setString(1, user);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    id = rs.getInt("id");
                    hpass = rs.getString("hpass");
                    salt = rs.getString("salt");
                    if (hpass.equals(sha256(pass + salt))) {
                        return id;
                    }
                }
                return -1;
            }
        }
    }

    /**
     * Get conversations of a user.
     *
     * @param uid
     * @return
     */
    protected static List<Conversation> dbGetConversations(int uid) throws SQLException {
        List<Conversation> res = new ArrayList<>();
        try (CallableStatement stmt = conn.prepareCall("call getConversations(?);")) {
            stmt.setInt(1, uid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    res.add(new Conversation(rs.getInt("id"), rs.getString("name")));
                }
            }
        }
        return res;
    }

    /**
     * Store a message in the database
     *
     * @param conversationId
     * @param userId
     * @param content
     * @return the ids of the users who can see the conversation (this is used
     * to notify them)
     * @throws SQLException
     */
    protected static Set<Integer> dbStoreMessage(int conversationId, int userId, String content) throws SQLException {
        Set<Integer> set = new HashSet<>();
        try (CallableStatement stmt = conn.prepareCall("call storeMessage(?,?,?);")) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, userId);
            stmt.setString(3, content);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getInt("userid"));
                }
            }
        }
        return set;
    }

    /**
     * Get messages of a conversation in the form of a string.
     *
     * @param conversationId
     * @return the conversation
     * @throws SQLException
     */
    protected static String dbGetMessagesString(int conversationId) throws SQLException {
        String res = "";
        try (CallableStatement stmt = conn.prepareCall("call getMessages(?);")) {
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    String content = rs.getString("content");
                    res += username + ": " + content + "\n";
                }
            }
        }
        return res.substring(0, res.length() - 1);    //remove the last '\n'
    }
    
    
    /**
     * Get messages of a conversation.
     *
     * @param conversationId
     * @return a list of messages
     * @throws SQLException
     */
    protected static List<Message> dbGetMessages(int conversationId) throws SQLException {
        List<Message> res = new ArrayList<>();
        try (CallableStatement stmt = conn.prepareCall("call getMessages(?);")) {
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    String content = rs.getString("content");
                    res.add(new Message(conversationId, username, content));
                }
            }
        }
        return res;
    }
    
    
    /**
     * @param conversationId
     * @return the participants of a conversation ids
     * @throws SQLException 
     */
    protected static Set<Integer> dbGetUsers(int conversationId) throws SQLException {
        Set<Integer> res = new HashSet<>();
        try (CallableStatement stmt = conn.prepareCall("call getConversationUsers(?);")) {
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int uid = rs.getInt("userid");
                    res.add(uid);
                }
            }
        }
        return res;
    }

    /**
     * Add a participant to a conversation
     *
     * @param conversationId id of the conversation
     * @param participantName name of the participant
     * @return the conversation
     * @throws SQLException
     */
    protected static void dbAddParticipant(int conversationId, String participantName) throws SQLException {
        try (CallableStatement stmt = conn.prepareCall("call addParticipant(?,?);")) {
            stmt.setInt(1, conversationId);
            stmt.setString(2, participantName);
            stmt.executeQuery();
        }
    }

    /**
     * Get the participants of a conversation.
     *
     * @param conversationId
     * @return the participants list of strings
     * @throws SQLException
     */
    protected static List<String> dbGetParticipants(int conversationId) throws SQLException {
        List<String> res = new ArrayList<>();
        try (CallableStatement stmt = conn.prepareCall("call getParticipants(?);")) {
            stmt.setInt(1, conversationId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    res.add(username);
                }
            }
        }
        return res;
    }
    
    
    /**
     * Add a contact to the contactlist if the contact exists in the database.
     *
     * @param requestorId
     * @param name name of the wanted contact
     * @return the contacts of the requestor
     * @throws SQLException
     */
    protected static List<String> dbAddContact(int requestorId, String name) throws SQLException {
        List<String> res = new ArrayList<>();
        try (CallableStatement stmt = conn.prepareCall("call addContact(?,?);")) {
            stmt.setInt(1, requestorId);
            stmt.setString(2, name);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    res.add(username);
                }
            }
        }
        return res;
    }

    
    /**
     * Add a contact to the contactlist if the contact exists in the database.
     *
     * @param requestorId
     * @return the contacts of the requestor
     * @throws SQLException
     */
    protected static List<String> dbGetContacts(int requestorId) throws SQLException {
        List<String> res = new ArrayList<>();
        try (CallableStatement stmt = conn.prepareCall("call getContacts(?);")) {
            stmt.setInt(1, requestorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    res.add(username);
                }
            }
        }
        return res;
    }
}
