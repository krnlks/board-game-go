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
    
    public void run() {
        try {
            System.out.println("after init");
            client = socket.accept();
            System.out.println("server: Client accepted!");
            model.setChanged1();
            System.out.println("a");
            model.notifyObservers2(UpdateMessages.CLIENT_CONNECTED);
            System.out.println("view notified (choose)");
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

        System.out.println("server: data available...?");
        // Sind Daten verfügbar?
        while (stream.available() == 0);
        System.out.println("server: data available!");

        // Ankommende Daten lesen und ausgeben
        return stream.read();
    }

    public void init() throws IOException {
        // Erzeugen der Socket/binden an Port/Wartestellung
        socket = new ServerSocket(SERVER_PORT);
        // Ab hier ist der Server "scharf" geschaltet
        // und wartet auf Verbindungen von Clients
        // System.out.println("Warten auf Verbindungen ...");

        // im Aufruf der Methode accept() verharrt die
        // Server-Anwendung solange, bis eine Verbindungs-
        // anforderung eines Client eingegangen ist.
        // Ist dies der Fall, so wird die Anforderung akzeptiert
    }
    
    public void terminate() throws IOException{
        // Verbindung beenden
        client.close();
        // Server-Socket schließen
        socket.close();
    }
}
