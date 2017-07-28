package multiplayer;

import game.Model;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Lukas Kern
 */
public class Client extends LAN_Conn{
    private String server_addr;
    
    public Client(Model model, String server_addr){
        this.server_addr = server_addr;
        this.model = model;
    }//Client constructor
    
    /**
     * Creates the {@code socket} and connects it to {@code SERVER_PORT} on host with address {@code server_addr}
     * 
     * @throws IOException
     */
    public void init() throws Exception {
    	System.out.println("The client (Black)\n------------------\n");
        socket = new Socket(server_addr, Server.SERVER_PORT);
        System.out.println("Client: Connected to server: "
                + socket.getRemoteSocketAddress());
    }//init
}