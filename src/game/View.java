package game;

import java.awt.*;
import java.awt.event.*;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import multiplayer.UpdateMessages;

/* Ideas for improvement:
 * - Don't call Model methods directly, as in if (model.estbl_LanComm() == 0){ ...
 */

//TODO [med] Adjust size of playing field depending on chosen board dimension (and size of screen, if possible). I.e., if possible, choose a dynamic size instead of static like currently 
//TODO [low] Add labels with letters and number as coordinates around of the playing field (or only above and left of it)
//TODO Use a loop and a map for the intersection-ImageIcons

/**
 * @author Lukas Kern
 */
public class View implements Observer{
    private static final long serialVersionUID = 1L;
    
    Model model;
    int dim = Constants.BOARD_DIM;
    
    JDialog choose; //choose: host or join
    JButton host;
    JButton join;
    JTextField server_addr;
    JLabel conn_info_host;
    JLabel conn_info_client;
    JDialog waiting;
    
    JFrame gameWindow;
    JSplitPane splitPane;
    
    JPanel boardPanel;     //Holds all the UI components
    BoardButton[][] board; //The UI counterpart of 'board' in Model; has a button for each intersection

    //Inner intersections
    ImageIcon intersct = new ImageIcon("icons/Intersct.jpg"); //Intersection without a stone on it
    ImageIcon intersct_B = new ImageIcon("icons/Intersct_B.jpg"); //with black stone on it
    ImageIcon intersct_BL = new ImageIcon("icons/Intersct_BL.jpg"); //...that was put last.
    ImageIcon intersct_W = new ImageIcon("icons/Intersct_W.jpg"); //with white stone on it
    ImageIcon intersct_WL = new ImageIcon("icons/Intersct_WL.jpg"); //...that was put last.
    
    //BOARD CORNERS
    //Top left
    ImageIcon intersct_crnTL = new ImageIcon("icons/Intersct_crnTL.jpg");
    ImageIcon intersct_crnTL_B = new ImageIcon("icons/Intersct_crnTL_B.jpg");
    ImageIcon intersct_crnTL_BL = new ImageIcon("icons/Intersct_crnTL_BL.jpg");
    ImageIcon intersct_crnTL_W = new ImageIcon("icons/Intersct_crnTL_W.jpg");
    ImageIcon intersct_crnTL_WL = new ImageIcon("icons/Intersct_crnTL_WL.jpg");
    //Top right
    ImageIcon intersct_crnTR = new ImageIcon("icons/Intersct_crnTR.jpg");
    ImageIcon intersct_crnTR_B = new ImageIcon("icons/Intersct_crnTR_B.jpg");
    ImageIcon intersct_crnTR_BL = new ImageIcon("icons/Intersct_crnTR_BL.jpg");
    ImageIcon intersct_crnTR_W = new ImageIcon("icons/Intersct_crnTR_W.jpg");
    ImageIcon intersct_crnTR_WL = new ImageIcon("icons/Intersct_crnTR_WL.jpg");
    //Bottom left
    ImageIcon intersct_crnBL = new ImageIcon("icons/Intersct_crnBL.jpg");
    ImageIcon intersct_crnBL_B = new ImageIcon("icons/Intersct_crnBL_B.jpg");
    ImageIcon intersct_crnBL_BL = new ImageIcon("icons/Intersct_crnBL_BL.jpg");
    ImageIcon intersct_crnBL_W = new ImageIcon("icons/Intersct_crnBL_W.jpg");
    ImageIcon intersct_crnBL_WL = new ImageIcon("icons/Intersct_crnBL_WL.jpg");
    //Bottom right
    ImageIcon intersct_crnBR = new ImageIcon("icons/Intersct_crnBR.jpg");
    ImageIcon intersct_crnBR_B = new ImageIcon("icons/Intersct_crnBR_B.jpg");
    ImageIcon intersct_crnBR_BL = new ImageIcon("icons/Intersct_crnBR_BL.jpg");
    ImageIcon intersct_crnBR_W = new ImageIcon("icons/Intersct_crnBR_W.jpg");
    ImageIcon intersct_crnBR_WL = new ImageIcon("icons/Intersct_crnBR_WL.jpg");
    
    //BOARD EDGES
    //Top
    ImageIcon intersct_edgT = new ImageIcon("icons/Intersct_edgT.jpg");
    ImageIcon intersct_edgT_B = new ImageIcon("icons/Intersct_edgT_B.jpg");
    ImageIcon intersct_edgT_BL = new ImageIcon("icons/Intersct_edgT_BL.jpg");
    ImageIcon intersct_edgT_W = new ImageIcon("icons/Intersct_edgT_W.jpg");
    ImageIcon intersct_edgT_WL = new ImageIcon("icons/Intersct_edgT_WL.jpg");
    //Left
    ImageIcon intersct_edgL = new ImageIcon("icons/Intersct_edgL.jpg");
    ImageIcon intersct_edgL_B = new ImageIcon("icons/Intersct_edgL_B.jpg");
    ImageIcon intersct_edgL_BL = new ImageIcon("icons/Intersct_edgL_BL.jpg");
    ImageIcon intersct_edgL_W = new ImageIcon("icons/Intersct_edgL_W.jpg");
    ImageIcon intersct_edgL_WL = new ImageIcon("icons/Intersct_edgL_WL.jpg");
    //Right
    ImageIcon intersct_edgR = new ImageIcon("icons/Intersct_edgR.jpg");
    ImageIcon intersct_edgR_B = new ImageIcon("icons/Intersct_edgR_B.jpg");
    ImageIcon intersct_edgR_BL = new ImageIcon("icons/Intersct_edgR_BL.jpg");
    ImageIcon intersct_edgR_W = new ImageIcon("icons/Intersct_edgR_W.jpg");
    ImageIcon intersct_edgR_WL = new ImageIcon("icons/Intersct_edgR_WL.jpg");
    //Bottom
    ImageIcon intersct_edgB = new ImageIcon("icons/Intersct_edgB.jpg");
    ImageIcon intersct_edgB_B = new ImageIcon("icons/Intersct_edgB_B.jpg");
    ImageIcon intersct_edgB_BL = new ImageIcon("icons/Intersct_edgB_BL.jpg");
    ImageIcon intersct_edgB_W = new ImageIcon("icons/Intersct_edgB_W.jpg");
    ImageIcon intersct_edgB_WL = new ImageIcon("icons/Intersct_edgB_WL.jpg");

    
    JPanel scorePanel;
    JLabel ter_B;  //Territory
    JLabel ter_W;
    JLabel pris_B;  //Prisoners
    JLabel pris_W;
    JLabel scr_B;  //Score
    JLabel scr_W;
    JLabel phase;   //Displays the information whose turn it is
    String blackTurn = "Black begins";
    String whiteTurn = "";
    JButton pass;
    JButton undo;
    
    public View(Model model){
        this.model = model;
        this.model.addObserver(this);
        init();
        choose.setVisible(true);
    }//View constructor
    
    
    /**
     * Creates and initializes all the UI components
     */
    private void init(){
        gameWindow = new JFrame("Go");
        splitPane = new JSplitPane (JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerSize(0);
        
            boardPanel = new JPanel();
            boardPanel.setLayout( new GridLayout (dim,dim) );
            boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            
                board = new BoardButton[dim][dim];
                
                //TODO Remove redundancy or incorporate setting corner and edge icons here
                //TODO Also, redundant vs what is done in Model
                //Set up field buttons and add them to the board
                //Also set center icons
                for (int y=0; y<dim; y++){
                    for (int x=0; x<dim; x++){
                        board[y][x] = new BoardButton(intersct, y, x);
                        board[y][x].addActionListener( new BoardButtonActionListener() );
                        board[y][x].setBorder(BorderFactory.createEmptyBorder());
                        boardPanel.add(board[y][x]);      
                    }//for
                }//for
                
                //Set corner icons
                board[0][0].setIcon(intersct_crnTL);
                board[0][dim-1].setIcon(intersct_crnTR);
                board[dim-1][0].setIcon(intersct_crnBL);
                board[dim-1][dim-1].setIcon(intersct_crnBR);
                
                
                //Set edge icons
                for (int x=1; x<dim-1; x++){
                    board[0][x].setIcon(intersct_edgT);
                }
                for (int y=1; y<dim-1; y++){
                    board[y][0].setIcon(intersct_edgL);
                }
                for (int y=1; y<dim-1; y++){
                    board[y][dim-1].setIcon(intersct_edgR);
                }
                for (int x=1; x<dim-1; x++){
                    board[dim-1][x].setIcon(intersct_edgB);
                }
                
        splitPane.add(boardPanel);
            
            scorePanel = new JPanel();
            scorePanel.setLayout( new GridLayout (5,3) );
            
                ter_B = new JLabel();
                ter_W = new JLabel();
                pris_B = new JLabel();
                pris_W = new JLabel();
                scr_B = new JLabel();
                scr_W = new JLabel();
                phase = new JLabel(blackTurn);
                pass = new JButton("Pass");
                pass.addActionListener( new PassButtonActionListener() );
//                undo = new JButton("Undo");
//                undo.addActionListener( new UndoButtonActionListener() );
            
            scorePanel.add(new JLabel());           //Upper left corner of the scorePanel is empty
            Font font = new Font(new JLabel().getFont().getFontName(), Font.BOLD, 16);
            JLabel black = new JLabel("Black"); 
            black.setFont(font);
            scorePanel.add(black);
            
            JLabel white = new JLabel("White"); 
            white.setForeground(Color.WHITE);
            white.setFont(font);
            scorePanel.add(white);
            
            scorePanel.add(new JLabel("Territory"));
            scorePanel.add(ter_B);
            scorePanel.add(ter_W);
            
            scorePanel.add(new JLabel("Prisoners"));
            scorePanel.add(pris_B);
            scorePanel.add(pris_W);
            
            scorePanel.add(new JLabel("Score"));
            scorePanel.add(scr_B);
            scorePanel.add(scr_W);
            
            scorePanel.add(phase);           //lower left corner of the scorePanel is empty
            scorePanel.add(pass);
//            scorePanel.add(undo);
            
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
        host = new JButton("Host (White)");
        conn_info_host = new JLabel("");
        host.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	//TODO Remove dependency between player color (black/white) and LAN role (server/client). First choice: Host or join? Then: color? Also modify Model.setLAN_Role(). 
                gameWindow.setTitle("Go - White");
                //TODO Both for choosing host or join and (after that) choosing stone color: Make it possible to change decision
                conn_info_host.setText("Waiting...");
                model.setLAN_Role("");
                if (model.estbl_LanConn() != 0){
                	System.out.println("View: hostOrJoin: Starting server failed");
                    System.exit(1);
                }
            }
        });
        join = new JButton("Join (Black):");
        join.setHorizontalAlignment( SwingConstants.CENTER );
        server_addr = new JTextField();
        //TODO Outsource this address to development branch and create release with empty text field
        server_addr.setText("127.0.0.1");  //TODO Remove this line at the end
        conn_info_client = new JLabel("");
        ActionListener srvAddrList = new ActionListener() { //Create a listener for the server address
            public void actionPerformed(ActionEvent e) {
                gameWindow.setTitle("Go - Black");
                model.setLAN_Role(server_addr.getText());
                if (model.estbl_LanConn() == 0){
                    System.out.println("View: hostOrJoin: estbl_LanConn() successful");
                    choose.dispose();
                    blackTurn = "My turn";
                    updateScorePanel();
                    whiteTurn = "White's turn";
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
    
    
    private ImageIcon getBoardButtonIcon(int y, int x){
        //TODO Not very urgent: I should not access the Model directly as I do here
        IS is = model.getIntersection(y, x);
        IS.Orient orient= is.getOrient();
        IS.State state = is.getState();
        boolean wasPutLast = is.wasPutLast();
        
        if (state.equals(IS.State.E)){            //Empty
            if (orient.equals(IS.Orient.C)){     //Center
                return intersct;
            }else if(orient.equals(IS.Orient.T)){ //Top
                return intersct_edgT;
            }else if(orient.equals(IS.Orient.L)){ //Left
                return intersct_edgL;
            }else if(orient.equals(IS.Orient.R)){ //Right
                return intersct_edgR;
            }else if(orient.equals(IS.Orient.B)){ //Bottom
                return intersct_edgB;
            }else if(orient.equals(IS.Orient.TL)){ //Top left
                return intersct_crnTL;
            }else if(orient.equals(IS.Orient.TR)){ //Top right
                return intersct_crnTR;
            }else if(orient.equals(IS.Orient.BL)){ //Bottom left
                return intersct_crnBL;
            }else{                                 //Bottom right
                return intersct_crnBR;
            }
        }else if (state.equals(IS.State.B)){      //Black
            if (wasPutLast){                        //Stone was put last                        
                if (orient.equals(IS.Orient.C)){     //Center
                    return intersct_BL;
                }else if(orient.equals(IS.Orient.T)){ //Top
                    return intersct_edgT_BL;
                }else if(orient.equals(IS.Orient.L)){ //Left
                    return intersct_edgL_BL;
                }else if(orient.equals(IS.Orient.R)){ //Right
                    return intersct_edgR_BL;
                }else if(orient.equals(IS.Orient.B)){ //Bottom
                    return intersct_edgB_BL;
                }else if(orient.equals(IS.Orient.TL)){ //Top left
                    return intersct_crnTL_BL;
                }else if(orient.equals(IS.Orient.TR)){ //Top right
                    return intersct_crnTR_BL;
                }else if(orient.equals(IS.Orient.BL)){ //Bottom left
                    return intersct_crnBL_BL;
                }else{                                 //Bottom right
                    return intersct_crnBR_BL;
                }
            }else{                                   //Stone wasn't put last
                if (orient.equals(IS.Orient.C)){     //Center
                    return intersct_B;
                }else if(orient.equals(IS.Orient.T)){ //Top
                    return intersct_edgT_B;
                }else if(orient.equals(IS.Orient.L)){ //Left
                    return intersct_edgL_B;
                }else if(orient.equals(IS.Orient.R)){ //Right
                    return intersct_edgR_B;
                }else if(orient.equals(IS.Orient.B)){ //Bottom
                    return intersct_edgB_B;
                }else if(orient.equals(IS.Orient.TL)){ //Top left
                    return intersct_crnTL_B;
                }else if(orient.equals(IS.Orient.TR)){ //Top right
                    return intersct_crnTR_B;
                }else if(orient.equals(IS.Orient.BL)){ //Bottom left
                    return intersct_crnBL_B;
                }else{                                 //Bottom right
                    return intersct_crnBR_B;
                }
            }
        }else{                                          //White
            if (wasPutLast){                            //Stone was put last
                if (orient.equals(IS.Orient.C)){     //Center
                    return intersct_WL;
                }else if(orient.equals(IS.Orient.T)){ //Top
                    return intersct_edgT_WL;
                }else if(orient.equals(IS.Orient.L)){ //Left
                    return intersct_edgL_WL;
                }else if(orient.equals(IS.Orient.R)){ //Right
                    return intersct_edgR_WL;
                }else if(orient.equals(IS.Orient.B)){ //Bottom
                    return intersct_edgB_WL;
                }else if(orient.equals(IS.Orient.TL)){ //Top left
                    return intersct_crnTL_WL;
                }else if(orient.equals(IS.Orient.TR)){ //Top right
                    return intersct_crnTR_WL;
                }else if(orient.equals(IS.Orient.BL)){ //Bottom left
                    return intersct_crnBL_WL;
                }else{                                //Bottom right
                    return intersct_crnBR_WL;
                }
            }else{                                      //Stone wasn't put last
                if (orient.equals(IS.Orient.C)){     //Center
                    return intersct_W;
                }else if(orient.equals(IS.Orient.T)){ //Top
                    return intersct_edgT_W;
                }else if(orient.equals(IS.Orient.L)){ //Left
                    return intersct_edgL_W;
                }else if(orient.equals(IS.Orient.R)){ //Right
                    return intersct_edgR_W;
                }else if(orient.equals(IS.Orient.B)){ //Bottom
                    return intersct_edgB_W;
                }else if(orient.equals(IS.Orient.TL)){ //Top left
                    return intersct_crnTL_W;
                }else if(orient.equals(IS.Orient.TR)){ //Top right
                    return intersct_crnTR_W;
                }else if(orient.equals(IS.Orient.BL)){ //Bottom left
                    return intersct_crnBL_W;
                }else{                                //Bottom right
                    return intersct_crnBR_W;
                }
            }
        }
    }//getFieldButtonIcon
    
    //TODO Not very urgent: Is there a way without setting EVERY icon anew after a draw? 
    private void updateBoard(){
        for (int y=0; y<dim; y++) {
        	for (int x=0; x<dim; x++) {
        	    board[y][x].setIcon(getBoardButtonIcon(y, x));
        	}
        }
    }//updatePlayingField

    private void displayResults(){
        String result;
        int scrB = model.getScr_B();    //Black's score
        int scrW = model.getScr_W();    //White's score
        
        if (scrB > scrW){
            result = String.format("Black wins with %d to %d points!", scrB, scrW);
        }else if (scrW > scrB){
            result = String.format("White wins with %d to %d points!", scrW, scrB);
        }else{
            result = String.format("Draw!", scrW, scrB);
        }
        JOptionPane.showMessageDialog(gameWindow, result);
    }//displayResults
    
    private void recv_wait(){
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
    }//recv_wait
    
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
            whiteTurn = "My turn"; //I got here because I'm the server. That also means that I'm White.
            blackTurn = "Black's turn";
            updateScorePanel(); //Update score panel so that it says this instead of "Black begins"
            choose.dispose();
            System.out.println("View: update: Disposed choose");
            waiting.setVisible(true);
        }else if (arg1.equals(UpdateMessages.RECVD_PASS)){
            System.out.println("View: update: RECVD_PASS: " + Thread.currentThread().getName());
            waiting.setVisible(false);
            updateScorePanel();    
        }else if (arg1.equals(UpdateMessages.RECVD_DOUBLEPASS)){
            System.out.println("View: update: RECVD_DOUBLEPASS: " + Thread.currentThread().getName());
            waiting.setVisible(false);
            updateScorePanel();
            displayResults();
            gameWindow.dispose();
        }else if (arg1.equals(UpdateMessages.RECVD_MOVE)){
            System.out.println("View: update: RECVD_MOVE: " + Thread.currentThread().getName());
            waiting.setVisible(false);
            updateBoard();
            updateScorePanel();
        }else{
        	System.out.println("View: update: What.");
        }
    }//update
    
    /**
     * The Go board consists of {@code dim*dim} {@code IS} intersections of which
     * each has its own button
     * 
     * @see IS
     */
    private class BoardButton extends JButton{
        
        private static final long serialVersionUID = 1L;
        
        int y;     //Position in the boardButtons[][]
        int x;     //Position in the boardButtons[][]
        
        public BoardButton(Icon icon, int y, int x) {
            super(icon);
            this.y = y;
            this.x = x;
        }//BoardButton constructor
    }//BoardButton

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
    
    private class BoardButtonActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            BoardButton bb = (BoardButton) e.getSource();
            if (model.isEmptyIntersection(bb.y, bb.x)){
                if (model.isNoSuicide(bb.y, bb.x) ){
                    System.out.println("\nView: BBAL: Draw #" + model.getGamecnt());
                    model.processMove(bb.y, bb.x);
                    if (model.getGamecnt() > 9 && model.areBoardsEqual(model.getBoard(), model.getBoard_m2())){
                        phase.setText("A stone that struck an opposing stone cannot be struck right afterwards!");
                        //TODO: Not sure if it's a good idea to use the same logic for really undoing a move and in this situation
                        model.undo();
                    }else{
                        updateBoard();
                        updateScorePanel();
                        System.out.println("View: BBAL: Updated score panel.");
                        //This is the event dispatch thread
                        System.out.println("View: BBAL: Going to send draw to opponent...");
                        model.send((bb.y*dim) + bb.x); //something in [0,dim*dim-1]
                        System.out.println("View: BBAL: Sent draw to opponent.");
                        System.out.println("View: BBAL: Going to wait for opponent's draw...");
                        recv_wait();
                    }
                }else{
                    //TODO Make another label / text field for these notifications
                    phase.setText("That would be suicide!");
                }
            }else{
                phase.setText("Please choose an empty intersection!");
            }
        }//actionPerformed
    }//FieldButtonActionListener
    
    //TODO Replace these model calls with one call... Gross! 
    private class PassButtonActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            if ( !model.isDoublePass() ){                       //It's a regular pass (no double pass)
                model.pass();
                updateScorePanel(); 
                model.send(Constants.SEND_PASS);
                System.out.println("View: PBAL: Sent pass to opponent.");
                System.out.println("View: PBAL: Going to wait for opponent's draw...");
                recv_wait();
            }else{                                              //Both players passed, game is over
                model.pass();
                updateScorePanel();
                displayResults();
                model.send(Constants.SEND_DOUBLEPASS);
                gameWindow.dispose();
            }
        }//actionPerformed
    }//PassButtonActionListener
    
    //TODO Implement a phase for deciding on a local draw (including an undo button) and add a 'Send' button
    //TODO Start implementing MVC cleaner: For now, just call model.undo() (still dirty but hey). Then let the model notify the View, and if, for instance, the move was already undone, display a notification on the board. Do this for pass button and everywhere else, too. 
    private class UndoButtonActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            if (model.getGamecnt() > 1){
                if (model.areBoardsEqual(model.getBoard(), model.getBoard_m1())){
                    phase.setText("You can only undo one move!");
                }else{
                  model.undo();
                  updateBoard();
                  updateScorePanel();                                                        
                }
            }else{
                phase.setText("There's no move that can be undone!");
            }
        }//actionPerformed
    }//UndoButtonActionListener

}//View