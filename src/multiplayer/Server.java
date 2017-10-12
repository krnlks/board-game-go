package multiplayer;

import game.Model;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

import javax.swing.SwingWorker;

/**
 * @author Lukas Kern
 */
public class Server extends LAN_Conn{
    //Port of the server application
    public static final int SERVER_PORT = 10001;
    
    private ServerSocket serverSocket;
    
    public Server(Model model) {
        this.model = model;
    }
    
    /**
     * Uses {@link ServerSocket#accept} to wait for a client who wishes to connect. 
     * 
     * @see ServerSocket#accept
     */
    public void run() {
        try {
            socket = serverSocket.accept();
            isConnected = true;
            System.out.println("The server (White)\n------------------\n");
            System.out.println("Server: Client connected!");
            //TODO Ouch
            model.setChanged1();
            //Schedule a SwingWorker for execution on a worker thread because it can take some time until the opponent
            //makes his draw.
            SwingWorker worker = new SwingWorker(){
                @Override
                protected Object doInBackground() throws Exception {
                    model.receive();
                    return null;
                }
            };
            worker.execute();
            //Causes View#update and thus to waiting.setVisible(true)
            model.notifyObservers2(UpdateMessages.CLIENT_CONNECTED);
            System.out.println("Server: View notified");
        } catch (SocketException se) {
            System.out.println("Socket closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }//run

    /**
     * Creates the {@code socket} and binds it to {@code SERVER_PORT}
     * 
     * @throws IOException
     */
    public void init(String not_used) throws IOException {
        serverSocket = new ServerSocket(SERVER_PORT);
    }
    
    /**
     * Closes the connection and the {@code socket}
     */
    public void terminate() throws IOException{
    	if (serverSocket != null)
    		serverSocket.close();
        super.terminate();
    }
}
