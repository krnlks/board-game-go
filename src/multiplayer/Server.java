package multiplayer;

import game.Model;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.SwingWorker;

/**
 * @author Lukas Kern
 */
public class Server extends LAN_Conn{
    //Port of the server application
    public static final int SERVER_PORT = 10001;
    
    private ServerSocket socket;
    private Socket client;
    
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
            client = socket.accept();
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
     * Sends a draw via a stream
     */
    public void send(int b) throws IOException{
        System.out.println("Server: Going to send draw...");
        client.getOutputStream().write(b);
        System.out.println("Server: Sent draw");
    }
    
    /**
     * Receives a draw via a stream  
     */
    public int receive() throws IOException{
        //Use stream for reading
        InputStream stream = client.getInputStream();

        System.out.println("Server: Data available...?");
        //Is data available?
        while (stream.available() == 0);
        System.out.println("Server: Data available!");

        //Read and return incoming data
        return stream.read();
    }

    /**
     * Creates the {@code socket} and binds it to {@code SERVER_PORT}
     * 
     * @throws IOException
     */
    public void init() throws IOException {
    	System.out.println("The server (White)\n------------------\n");
    	socket = new ServerSocket(SERVER_PORT);
    }
    
    /**
     * Closes the connection and the {@code socket}
     */
    public void terminate() throws IOException{
        client.close();
        socket.close();
    }
}
