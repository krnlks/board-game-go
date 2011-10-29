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
        try {
            init();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void send(int b) throws IOException {
        // Senden der Nachricht ueber einen Stream
        socket.getOutputStream().write(b);
    }
    
    public int receive() throws IOException {
        // Datenstrom zum Lesen verwenden
        InputStream stream = socket.getInputStream();
        
        // Sind Daten verfügbar?
        while (stream.available() == 0);
        
        // Ankommende Daten lesen und ausgeben
        return stream.read();
    }
    
    public void init() throws UnknownHostException, IOException {
        // Erzeugen der Socket und Aufbau der Verbindung
        socket = new Socket(server_addr, SERVER_PORT);
        System.out.println("Verbunden mit Server: "
                + socket.getRemoteSocketAddress());
    }

    public void terminate() throws IOException {
        // Beenden der Kommunikationsverbindung
        socket.close();
    }
}