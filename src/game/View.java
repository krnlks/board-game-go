package game;

import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import multiplayer.UpdateMessages;

/* Verbesserungsideen:
 * - teilweise werden hier Model-Methoden direkt aufgerufen, z.b. if (model.estbl_LanComm() == 0){ ... 
 */

public class View implements Observer{
    
    private class FieldButton extends JButton{
        
        private static final long serialVersionUID = 1L;
        
        int y;     //Position in the fieldButtons[][]
        int x;     //Position in the fieldButtons[][]
        
        public FieldButton(Icon icon, int y, int x) {
            super(icon);
            this.y = y;
            this.x = x;
        }//FieldButton constructor
    }//FieldButton

    private class GameWindowListener implements WindowListener{
        public void windowClosing(WindowEvent e) {
            //TODO Send Quit to opponent
            gameWindow.dispose();
        }
        public void windowActivated(WindowEvent e) {
        }
        public void windowClosed(WindowEvent e) {
        }
        public void windowDeactivated(WindowEvent e) {
        }
        public void windowDeiconified(WindowEvent e) {
        }
        public void windowIconified(WindowEvent e) {
        }
        public void windowOpened(WindowEvent e) {
        }
    }//GameWindowListener
    
    private class FieldButtonActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            if (model.isMyTurn()){
                FieldButton fb = (FieldButton) e.getSource();
                if (model.isEmptyIntersection(fb.y, fb.x)){
                    if (model.isNoSuicide(fb.y, fb.x) ){
                        model.processMove(fb.y, fb.x);
                        if ( (model.areEqualFields(model.getFields(), model.getTmp_4_ko())) && (model.getGamecnt() > 8) ){
                            JOptionPane.showMessageDialog(gameWindow, "Ein Stein, der gerade einen Stein geschlagen hat, darf nicht sofort zurueckgeschlagen werden!");
                            model.undoMove();
                        }else{
                        	updatePlayingField();
                        	updateScorePanel();
                            System.out.println("View: FBAL: Updated score panel.");
                            printThreadInfo(); //Is event dispatch thread? (yes)
                            System.out.println("View: FBAL: Going to send draw to opponent...");
                            model.send((fb.y*9) + fb.x); //something in [0,80]
                            System.out.println("View: FBAL: Sent draw to opponent.");
                            System.out.println("View: FBAL: Going to wait for opponent's draw...");
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
                            //Meant to hinder player from entering input until it's his turn again
                            //Called at the end because it blocks until setVisible(false) is called after receiving the opponent's draw
                            //Blocks when it stands alone and won't become visible when called from inside SwingUtilities.invokeLater()
                            waiting.setVisible(true); 
                        }
                    }else{
                        JOptionPane.showMessageDialog(gameWindow, "Das waere Selbstmord!");
                    }
                }else{
                    JOptionPane.showMessageDialog(gameWindow, "Bitte auf ein freies Feld setzen!");
                }
            }else{
                JOptionPane.showMessageDialog(gameWindow, "Bitte warten Sie bis Sie an der Reihe sind.");
            }
        }//actionPerformed
    }//FieldButtonActionListener
    
    private class PassButtonActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            if (model.isMyTurn()){
                if ( !model.isDoublePass() ){
                    model.pass();
                    updateScorePanel(); 
                    model.send(-1);
                }else{												//HIER IST DAS SPIEL VORBEI!
                    model.pass();
                    updateScorePanel();
                    String result;
                    int scrB = model.getScr_B();	//Black's score
                    int scrW = model.getScr_W();	//White's score
                    
                    if (scrB > scrW){
                        result = String.format("Schwarz gewinnt mit %d zu %d Punkten!", scrB, scrW);
                    }else if (scrW > scrB){
                        result = String.format("Weiss gewinnt mit %d zu %d Punkten!", scrW, scrB);
                    }else{
                        result = String.format("Das Spiel endet unentschieden mit %d zu %d Punkten!", scrW, scrB);
                    }
                    JOptionPane.showMessageDialog(gameWindow, result);
                    model.send(-1);
                    gameWindow.dispose();
                }
            }else{
                JOptionPane.showMessageDialog(gameWindow, "Bitte warten Sie bis Sie an der Reihe sind.\n");
            }
        }//actionPerformed
    }//PassButtonActionListener
    
    private class UndoButtonActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            if (model.getGamecnt() > 1){
                if (model.areEqualFields(model.getFields(), model.getFields_b4())){
                	JOptionPane.showMessageDialog(gameWindow, "Es darf nur ein Zug zurueckgenommen werden!");
                }else{
                	model.undoMove();
                	updatePlayingField();
                	updateScorePanel();                                                        
                }
            }else{
                JOptionPane.showMessageDialog(gameWindow, "Es gibt keinen Zug, der zurueckgenommen werden kann!");
            }
        }//actionPerformed
    }//UndoButtonActionListener
    
    

    private static final long serialVersionUID = 1L;
    
    Model model;
    
    JDialog choose; //choose: host or join
    JButton host;
    JButton join;
    JTextField server_addr;
    JLabel conn_info_host;
    JLabel conn_info_client;
    JDialog waiting;
    
    JFrame gameWindow;
    JSplitPane splitPane;
    
    JPanel playingField;
    FieldButton[][] fieldButtons;
    ImageIcon intersct_E = new ImageIcon("icons/Intersct.JPG");
    ImageIcon intersct_B = new ImageIcon("icons/Intersct_S.JPG");
    ImageIcon intersct_W = new ImageIcon("icons/Intersct_W.JPG");
   
    JPanel scorePanel;
    JLabel ter_B;  //territory
    JLabel ter_W;
    JLabel pris_B;  //prisoners
    JLabel pris_W;
    JLabel scr_B;  //score
    JLabel scr_W;
    JLabel phase;   //displays the information whose turn it is
    String blackTurn = "Schwarz ist dran.";
    String whiteTurn = "Weiss ist dran.";
    JButton pass;
    JButton undo;
    
    public View(Model model){
        this.model = model;
        this.model.addObserver(this);
        init();
        choose.setVisible(true);
    }//View constructor
    
    private void printThreadInfo(){
    	System.out.println("View: Thread: " + Thread.currentThread().getName() + ". Event dispatch thread: " + 
    			javax.swing.SwingUtilities.isEventDispatchThread());
    }//printThreadInfo
    
    /**
     * Creates and initializes all the UI components
     */
    private void init(){
        gameWindow = new JFrame("Go");
        splitPane = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerSize(0);
        
            playingField = new JPanel();
            playingField.setLayout( new GridLayout (9,9) );
            playingField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            
                fieldButtons = new FieldButton[9][9];
                for (int y=0; y<=8; y++){
                    for (int x=0; x<=8; x++){
                        fieldButtons[y][x] = new FieldButton(intersct_E, y, x);
                        fieldButtons[y][x].addActionListener( new FieldButtonActionListener() );
                        fieldButtons[y][x].setBorder(BorderFactory.createEmptyBorder());
                        playingField.add(fieldButtons[y][x]);      
                    }//for
                }//for
        splitPane.add(playingField);
            
            scorePanel = new JPanel();
            scorePanel.setLayout( new GridLayout (5,3) );
            
                ter_B = new JLabel();
                ter_W = new JLabel();
                pris_B = new JLabel();
                pris_W = new JLabel();
                scr_B = new JLabel();
                scr_W = new JLabel();
                phase = new JLabel(blackTurn);
                pass = new JButton("Passen");
                pass.addActionListener( new PassButtonActionListener() );
                undo = new JButton("Zug zuruecknehmen");
                undo.addActionListener( new UndoButtonActionListener() );
            
            scorePanel.add(new JLabel());           //Upper left corner of the scorePanel is empty
            scorePanel.add(new JLabel("Schwarz"));
            scorePanel.add(new JLabel("Weiss"));
            
            scorePanel.add(new JLabel("Gebiet"));
            scorePanel.add(ter_B);
            scorePanel.add(ter_W);
            
            scorePanel.add(new JLabel("Gefangene"));
            scorePanel.add(pris_B);
            scorePanel.add(pris_W);
            
            scorePanel.add(new JLabel("Punkte"));
            scorePanel.add(scr_B);
            scorePanel.add(scr_W);
            
            scorePanel.add(phase);           //lower left corner of the scorePanel is empty
            scorePanel.add(pass);
            scorePanel.add(undo);
            
        splitPane.add(scorePanel);
        
        gameWindow.add(splitPane);
        gameWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        gameWindow.setSize(1350, 730);
        gameWindow.setResizable(false);
        gameWindow.addWindowListener(new GameWindowListener());
        gameWindow.setVisible(true);
        
        waiting = new JDialog(gameWindow, true);
        waiting.add(new JLabel("Waiting for opponent...", SwingConstants.CENTER));
        waiting.setSize(200, 100);
        waiting.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        
        hostOrJoin();
    }//init
    
    /**
     * Creates a JDialog to choose for either host or join
     */
    private void hostOrJoin() {
        GridLayout gl1 = new GridLayout(3, 1);
        GridLayout gl2 = new GridLayout(1, 2);
        JPanel jp1 = new JPanel(gl2);
        JPanel jp2 = new JPanel(gl2);
        JPanel jp3 = new JPanel(gl2);
        this.choose = new JDialog(gameWindow, true);
        choose.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        choose.setLayout(gl1);
        host = new JButton("Host (Weiss)");
        conn_info_host = new JLabel("");
        host.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	gameWindow.setTitle("Go - Weiß");
                conn_info_host.setText("Waiting..."); //TODO Guess I have to work with an extra thread for LAN_Conn for updating this etc
                model.setLAN_Role("");
                if (model.estbl_LanConn() != 0){
                	System.out.println("View: hostOrJoin: Starting server failed");
                    System.exit(1);
                }
            }
        });
        join = new JButton("Join (Schwarz):");
        join.setHorizontalAlignment( SwingConstants.CENTER );
        server_addr = new JTextField();
        server_addr.setText("192.168.178.25");  //TODO Remove this line at the end
        conn_info_client = new JLabel("");
        ActionListener srvAddrList = new ActionListener() { //Create a listener for the server address
            public void actionPerformed(ActionEvent e) {
                gameWindow.setTitle("Go - Schwarz");
                model.setLAN_Role(server_addr.getText());
                if (model.estbl_LanConn() == 0){
                    System.out.println("View: hostOrJoin: estbl_LanConn() successful");
                    choose.dispose();
                }else{
                    server_addr.setText("");
                    conn_info_client.setText("Invalid Host");
                }
            }
        }; 
        server_addr.addActionListener(srvAddrList); //Add the listener both to the text field containing the address
        join.addActionListener(srvAddrList); //and to the button
        jp1.add(host);
        jp1.add(conn_info_host);
        choose.add(jp1);
        jp2.add(join);
        jp2.add(server_addr);
        choose.add(jp2);
        jp3.add(new JLabel());
        jp3.add(conn_info_client);
        choose.add(jp3);
        
        choose.pack();
        //choose.setSize(200, 100);
        choose.setResizable(false);
        choose.addWindowFocusListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                server_addr.requestFocusInWindow();
            }
        });
    }//hostOrJoin

    public ImageIcon getFieldButtonIcon(int i, int j){
        IS buf = model.getIntersection(i, j);
        if (buf.equals(IS.E)){
            return intersct_E;
        }else if (buf.equals(IS.B)){
            return intersct_B;
        }else {
            return intersct_W;
        }
    }//getFieldButtonIcon
    
    public void updatePlayingField(){
        for (int i=0; i<fieldButtons.length; i++) {
        	for (int j=0; j<fieldButtons.length; j++) {
        		fieldButtons[i][j].setIcon(getFieldButtonIcon(i, j));
        	}
        }
    }//updatePlayingField
    
    public void updateScorePanel(){
        model.getTerritory();
        ter_B.setText(String.format("%d", model.getTer_B()));
        ter_W.setText(String.format("%d", model.getTer_W()));
        
        pris_B.setText(String.format("%d", model.getPris_B()));
        pris_W.setText(String.format("%d", model.getPris_W()));
        
        scr_B.setText(String.format("%d", model.getScr_B()));
        scr_W.setText(String.format("%d", model.getScr_W()));
        
        if (model.getGamecnt() % 2 == 0 ){
            phase.setText(whiteTurn);
        }else{
            phase.setText(blackTurn);
        }//if
    }//updateScorePanel

    @Override
    public void update(Observable arg0, Object arg1) {
        if (arg1.equals(UpdateMessages.CLIENT_CONNECTED)){
            System.out.println("View: update: Going to dispose choose and make waiting visible");
            choose.dispose();
            System.out.println("View: update: Disposed choose");
            System.out.println("View: update: " + Thread.currentThread().getName() + ". Event dispatch thread: " + 
                    javax.swing.SwingUtilities.isEventDispatchThread());
            waiting.setVisible(true);
        }else if (arg1.equals(UpdateMessages.OPPONENT)){
            waiting.setVisible(false);
            updatePlayingField();
            updateScorePanel();
        }else{
        	System.out.println("View: update: What.");
        }
    }//update

}//View