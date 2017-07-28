package multiplayer;

import game.Model;

import java.io.IOException;
import java.net.ServerSocket;

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
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }//run

    /**
     * Creates the {@code socket} and binds it to {@code SERVER_PORT}
     * 
     * @throws IOException
     */
    public void init() throws IOException {
    	System.out.println("The server (White)\n------------------\n");
    	serverSocket = new ServerSocket(SERVER_PORT);
    }
    
    /**
     * Closes the connection and the {@code socket}
     */
    public void terminate() throws IOException{
        super.terminate();
        serverSocket.close();
    }
}
