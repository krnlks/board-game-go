package multiplayer;

import game.Model;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Interface which represents the LAN connection and which is
 * to be implemented e.g. by a server/client 
 * 
 * @author Lukas Kern
 */
public abstract class LAN_Conn extends Thread {
    Model model;
    boolean isConnected = false;
    Socket socket;
    
    /**
     * @param b the message to be sent 
     * @throws IOException
     */
    public void send(int b) throws IOException{
        System.out.println("LAN_Conn: Going to send draw...");
        socket.getOutputStream().write(b);
        System.out.println("LAN_Conn: Sent draw");
    }
  
    /**
     * @return the received message or -1 in case of an error
     * @throws IOException 
     * @throws InterruptedException 
     */
    public int receive() throws IOException, InterruptedException{
        InputStream stream = socket.getInputStream();
        
        //Is data available?
        while (stream.available() == 0){
        	Thread.sleep(100);
        }
    	//Read and return incoming data
    	return stream.read();
    }

    /**
     * Establishes the LAN connection
     * 
     * @throws IOException
     */
    public abstract void init(String server_addr) throws IOException;

    /**
     * Closes the {@code socket}
     * 
     * @throws IOException
     */
    public void terminate() throws IOException {
        if (socket != null)
            socket.close();
    }

    public boolean isConnected() {
        return isConnected;
    }
}
