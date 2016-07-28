package multiplayer;

import game.Model;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

public class Client extends LAN_Conn{
    // Port der Serveranwendung
    public static final int SERVER_PORT = 10001;
    private String server_addr;
    private Socket socket;
    
    public Client(Model model, String server_addr){
        this.server_addr = server_addr;
        this.model = model;
    }
    
    public void run(){
//        try {
//            init();
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
    
    public void send(int b) throws IOException {
        System.out.println("Client: Sende Zug...");
        // Senden der Nachricht ueber einen Stream
        socket.getOutputStream().write(b);
        System.out.println("Client: Zug gesendet");
    }
    
    public int receive() throws IOException {
        // Datenstrom zum Lesen verwenden
        InputStream stream = socket.getInputStream();
        
        // Sind Daten verfügbar?
        while (stream.available() == 0);
        
        // Ankommende Daten lesen und ausgeben
        return stream.read();
    }
    
    public void init() throws Exception {
    	System.out.println("The client\n----------\n");
    	// Erzeugen des Socket und Aufbau der Verbindung
        socket = new Socket(server_addr, SERVER_PORT);
        System.out.println("Client: Verbunden mit Server: "
                + socket.getRemoteSocketAddress());
    }

    public void terminate() throws IOException {
        // Beenden der Kommunikationsverbindung
        socket.close();
    }
}