package multiplayer;

import game.Model;

import java.io.IOException;

/**
 * interface which represents the lan connection and which is
 * to be implemented e.g. by a server/client 
 * @author Lukas Kern
 */
public abstract class LAN_Conn extends Thread {
    
    protected Model model;
    public enum LAN { SERVER, CLIENT };
    
    /**
     * @param b the message to be sent 
     * @throws IOException
     */
    public abstract void send(int b) throws IOException;
    /**
     * @return the received message or -1 in case of an error
     * @throws IOException 
     */
    public abstract int receive() throws IOException;
    /**
     * establishes the LAN connection
     * @throws IOException
     */
    public abstract void init() throws IOException;
    /**
     * @throws IOException
     */
    public abstract void terminate() throws IOException;
}
