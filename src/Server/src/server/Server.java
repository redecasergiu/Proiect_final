package server;

import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final int PORT = 9001;   //the listened port
    protected static Connection conn; //db conn

    public static void main(String[] args) throws Exception {
        try {
            Server.conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chatdb?user=root&password=root");
        } catch (Exception ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getMessage());
        }

        System.out.println("Server started.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                Handler h = new Handler(listener.accept());
                Handler.hs.add(h);
                h.start();
            }
        } finally {
            listener.close();
        }
    }

}
