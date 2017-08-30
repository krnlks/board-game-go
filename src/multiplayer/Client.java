package multiplayer;

import game.Model;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Lukas Kern
 */
public class Client extends LAN_Conn{
    public Client(Model model){
        this.model = model;
    }//Client constructor
    
    /**
     * Creates the {@code socket} and connects it to {@code SERVER_PORT} on host with address {@code server_addr}
     * @throws IOException
     */
    public void init(String server_addr) throws IOException {
        socket = new Socket(server_addr, Server.SERVER_PORT);
        System.out.println("The client (Black)\n------------------\n");
        System.out.println("Client: Connected to server: "
                + socket.getRemoteSocketAddress());
        isConnected = true;
    }//init
}