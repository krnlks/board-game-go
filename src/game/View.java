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


/**
 * @author Lukas Kern
 */
public class View implements Observer{
    
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
    }
    
    private class BoardButtonActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            BoardButton bb = (BoardButton) e.getSource();
            if (model.isEmptyIntersection(bb.y, bb.x)){
                if (model.isNoSuicide(bb.y, bb.x) ){
                    System.out.println("\nView: BBAL: Draw #" + model.getGamecnt());
                    model.processMove(bb.y, bb.x);
                    if ( (model.areBoardsEqual(model.getBoard(), model.getBoard_ko())) && (model.getGamecnt() > 8) ){
                        phase.setText("A stone that struck an opposing stone cannot be struck right afterwards!");
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
            if ( !model.isDoublePass() ){
                model.pass();
                updateScorePanel(); 
                model.send(Constants.SEND_PASS);
                System.out.println("View: PBAL: Sent pass to opponent.");
                System.out.println("View: PBAL: Going to wait for opponent's draw...");
                recv_wait();
            }else{												//Both players passed, game is over
                model.pass();
                updateScorePanel();
                String result;
                int scrB = model.getScr_B();	//Black's score
                int scrW = model.getScr_W();	//White's score
                
                if (scrB > scrW){
                    result = String.format("Black wins with %d to %d points!", scrB, scrW);
                }else if (scrW > scrB){
                    result = String.format("White wins with %d to %d points!", scrW, scrB);
                }else{
                    result = String.format("Draw!", scrW, scrB);
                }
                JOptionPane.showMessageDialog(gameWindow, result);
                model.send(Constants.SEND_PASS);
                gameWindow.dispose();
                }
        }//actionPerformed
    }//PassButtonActionListener
    
    //TODO Implement a phase for deciding on a local draw (including an undo button) and add a 'Send' button
    //TODO Start implementing MVC cleaner: For now, just call model.undo() (still dirty but hey). Then let the model notify the View, and if, for instance, the move was already undone, display a notification on the board. Do this for pass button and everywhere else, too. 
    private class UndoButtonActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            if (model.getGamecnt() > 1){
                if (model.areBoardsEqual(model.getBoard(), model.getBoard_b4())){
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
    
    JPanel board; //The Go board / table
    BoardButton[][] boardButtons;

    //Inner intersections
    ImageIcon intersct = new ImageIcon("icons/Intersct.jpg");
    ImageIcon intersct_B = new ImageIcon("icons/Intersct_B.jpg");
    ImageIcon intersct_W = new ImageIcon("icons/Intersct_W.jpg");
    
    //Board corners
    ImageIcon intersct_crnTL = new ImageIcon("icons/Intersct_crnTL.jpg");
    ImageIcon intersct_crnTL_B = new ImageIcon("icons/Intersct_crnTL_B.jpg");
    ImageIcon intersct_crnTL_W = new ImageIcon("icons/Intersct_crnTL_W.jpg");

    ImageIcon intersct_crnTR = new ImageIcon("icons/Intersct_crnTR.jpg");
    ImageIcon intersct_crnTR_B = new ImageIcon("icons/Intersct_crnTR_B.jpg");
    ImageIcon intersct_crnTR_W = new ImageIcon("icons/Intersct_crnTR_W.jpg");

    ImageIcon intersct_crnBL = new ImageIcon("icons/Intersct_crnBL.jpg");
    ImageIcon intersct_crnBL_B = new ImageIcon("icons/Intersct_crnBL_B.jpg");
    ImageIcon intersct_crnBL_W = new ImageIcon("icons/Intersct_crnBL_W.jpg");
    
    ImageIcon intersct_crnBR = new ImageIcon("icons/Intersct_crnBR.jpg");
    ImageIcon intersct_crnBR_B = new ImageIcon("icons/Intersct_crnBR_B.jpg");
    ImageIcon intersct_crnBR_W = new ImageIcon("icons/Intersct_crnBR_W.jpg");
    
    //Board edges
    ImageIcon intersct_edgT = new ImageIcon("icons/Intersct_edgT.jpg");
    ImageIcon intersct_edgT_B = new ImageIcon("icons/Intersct_edgT_B.jpg");
    ImageIcon intersct_edgT_W = new ImageIcon("icons/Intersct_edgT_W.jpg");

    ImageIcon intersct_edgL = new ImageIcon("icons/Intersct_edgL.jpg");
    ImageIcon intersct_edgL_B = new ImageIcon("icons/Intersct_edgL_B.jpg");
    ImageIcon intersct_edgL_W = new ImageIcon("icons/Intersct_edgL_W.jpg");
    
    ImageIcon intersct_edgR = new ImageIcon("icons/Intersct_edgR.jpg");
    ImageIcon intersct_edgR_B = new ImageIcon("icons/Intersct_edgR_B.jpg");
    ImageIcon intersct_edgR_W = new ImageIcon("icons/Intersct_edgR_W.jpg");

    ImageIcon intersct_edgB = new ImageIcon("icons/Intersct_edgB.jpg");
    ImageIcon intersct_edgB_B = new ImageIcon("icons/Intersct_edgB_B.jpg");
    ImageIcon intersct_edgB_W = new ImageIcon("icons/Intersct_edgB_W.jpg");

    
    JPanel scorePanel;
    JLabel ter_B;  //Territory
    JLabel ter_W;
    JLabel pris_B;  //Prisoners
    JLabel pris_W;
    JLabel scr_B;  //Score
    JLabel scr_W;
    JLabel phase;   //Displays the information whose turn it is
    String blackTurn = "Black's turn";
    String whiteTurn = "White's turn";
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
        
            board = new JPanel();
            board.setLayout( new GridLayout (dim,dim) );
            board.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            
                boardButtons = new BoardButton[dim][dim];
                
                //TODO Remove redundancy or incorporate setting corner and edge icons here
                //TODO Also, redundant vs what is done in Model
                //Set up field buttons and add them to the board
                //Also set center icons
                for (int y=0; y<dim; y++){
                    for (int x=0; x<dim; x++){
                        boardButtons[y][x] = new BoardButton(intersct, y, x);
                        boardButtons[y][x].addActionListener( new BoardButtonActionListener() );
                        boardButtons[y][x].setBorder(BorderFactory.createEmptyBorder());
                        board.add(boardButtons[y][x]);      
                    }//for
                }//for
                
                //Set corner icons
                boardButtons[0][0].setIcon(intersct_crnTL);
                boardButtons[0][dim-1].setIcon(intersct_crnTR);
                boardButtons[dim-1][0].setIcon(intersct_crnBL);
                boardButtons[dim-1][dim-1].setIcon(intersct_crnBR);
                
                
                //Set edge icons
                for (int x=1; x<dim-1; x++){
                    boardButtons[0][x].setIcon(intersct_edgT);
                }
                for (int y=1; y<dim-1; y++){
                    boardButtons[y][0].setIcon(intersct_edgL);
                }
                for (int y=1; y<dim-1; y++){
                    boardButtons[y][dim-1].setIcon(intersct_edgR);
                }
                for (int x=1; x<dim-1; x++){
                    boardButtons[dim-1][x].setIcon(intersct_edgB);
                }
                
        splitPane.add(board);
            
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
                undo = new JButton("Undo");
                undo.addActionListener( new UndoButtonActionListener() );
            
            scorePanel.add(new JLabel());           //Upper left corner of the scorePanel is empty
            scorePanel.add(new JLabel("Black"));
            scorePanel.add(new JLabel("White"));
            
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
        host = new JButton("Host (White)");
        conn_info_host = new JLabel("");
        host.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	gameWindow.setTitle("Go - White");
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
        server_addr.setText("192.168.178.25");  //TODO Remove this line at the end
        conn_info_client = new JLabel("");
        ActionListener srvAddrList = new ActionListener() { //Create a listener for the server address
            public void actionPerformed(ActionEvent e) {
                gameWindow.setTitle("Go - Black");
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
    
    
    private ImageIcon getBoardButtonIcon(int y, int x){
        //TODO I should not access the Model directly as I do here
        IS is = model.getIntersection(y, x);
        IS.Orient orient= is.getOrient();
        IS.State state = is.getState();
        
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
        }else{                                    //White
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
    }//getFieldButtonIcon
    
    //TODO Not very urgent: Is there a way without setting EVERY icon anew after a draw? 
    private void updateBoard(){
        for (int y=0; y<dim; y++) {
        	for (int x=0; x<dim; x++) {
        	    boardButtons[y][x].setIcon(getBoardButtonIcon(y, x));
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
            waiting.setVisible(true);
        }else if (arg1.equals(UpdateMessages.DRAW_RECVD)){
            System.out.println("View: update: DRAW_RECVD: " + Thread.currentThread().getName());
            waiting.setVisible(false);
            updateBoard();
            updateScorePanel();
        }else{
        	System.out.println("View: update: What.");
        }
    }//update

}//View