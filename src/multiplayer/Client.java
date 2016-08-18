package multiplayer;

import game.Model;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * @author Lukas Kern
 */
public class Client extends LAN_Conn{
    private String server_addr;
    private Socket socket;
    
    public Client(Model model, String server_addr){
        this.server_addr = server_addr;
        this.model = model;
    }//Client constructor
    
    public void run(){}

    /**
     * Sends a draw via a stream
     */
    public void send(int b) throws IOException {
        System.out.println("Client: Going to send draw...");
        socket.getOutputStream().write(b);
        System.out.println("Client: Sent draw");
    }
    
    /**
     * Receives a draw via a stream  
     */
    public int receive() throws IOException {
        //Use stream for reading
        InputStream stream = socket.getInputStream();
        
        //Is data available?
        while (stream.available() == 0);
        
        //Read and return incoming data
        return stream.read();
    }
    
    /**
     * Creates the {@code socket} and connects it to {@code SERVER_PORT} on host with address {@code server_addr}
     * @throws IOException
     */
    public void init() throws Exception {
    	System.out.println("The client (Black)\n------------------\n");
        socket = new Socket(server_addr, Server.SERVER_PORT);
        System.out.println("Client: Connected to server: "
                + socket.getRemoteSocketAddress());
    }//init

    /**
     * Closes the {@code socket}
     */
    public void terminate() throws IOException {
        socket.close();
    }
}