package multiplayer;

import game.Model;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends LAN_Conn{
    // Port der Serveranwendung
    public static final int SERVER_PORT = 10001;
    
    private ServerSocket socket;
    private Socket client;
    
    public Server(Model model) {
        this.model = model;
    }
    
    /**
     * Der Server wartet auf Verbindungen von Clients (accept)
     */
    public void run() {
        try {
            System.out.println("Server: after init");
            client = socket.accept();
            System.out.println("Server: Client accepted!");
            model.setChanged1();
            System.out.println("Server: a");
            model.notifyObservers2(UpdateMessages.CLIENT_CONNECTED);
            System.out.println("Server: view notified (choose)");
            model.receive();
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    public void send(int b) throws IOException{
        client.getOutputStream().write(b);
    }
    
    public int receive() throws IOException{
        // Datenstrom zum Lesen verwenden
        InputStream stream = client.getInputStream();

        System.out.println("Server: data available...?");
        // Sind Daten verfuegbar?
        while (stream.available() == 0);
        System.out.println("Server: data available!");

        // Ankommende Daten lesen und ausgeben
        return stream.read();
    }

    /**
     * Erzeugen des Sockets (binden an Port)
     * @throws IOException
     */
    public void init() throws IOException {
    	System.out.println("The server\n----------\n");
    	socket = new ServerSocket(SERVER_PORT);
    }
    
    /**
     * Verbindung beenden und ServerSocket schließen
     */
    public void terminate() throws IOException{
        client.close();
        socket.close();
    }
}
