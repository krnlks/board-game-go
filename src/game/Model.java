package game;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import multiplayer.Client;
import multiplayer.LAN_Conn;
import multiplayer.Server;
import multiplayer.UpdateMessages;

/* Ideas for improvement:
 * - Global variables
 */

//TODO Look at more complex methods in Model and write good and clear description

//TODO At the beginning, provide options: local or distributed multiplayer (and at some point maybe singleplayer)

//TODO Set player (Black / White) at the beginning because it won't change and it will be used again and again throughout the game. This might also help get rid of getCurrentPlayer() / getOpponent() hassle. Then, only relevant information will be whose turn it is.

/**
 * The data and logic for the game. In Go Black plays against White. 
 * The game is played on a square board that consists of intersections that are either empty
 * or have a black or white stone upon them.
 * 
 * @author Lukas Kern
 */
public class Model extends Observable{
    
    private LAN_Conn lan;       //Server or client
    private int ter_B;                    //Territory occupied by Black
    private int ter_W;                    //Territory occupied by White
    private int pris_B = 0;                    //Conquered opponent stones of Black
    private int pris_W = 0;                    //Conquered opponent stones of White
    private int pris_B_b4 = pris_B;
    private int pris_W_b4 = pris_W;
    private Player player;
    
    /**
     * The number of the current draw, incremented by each player in each of their turns. Used to determine whose turn it is.
     * 
     * <p> TODO Also used for something else (like comparing board states)? Maybe there's a better solution. 
     */
	private int gameCnt;
	//TODO: Maybe move the different board states to one IS[][][]
	/**
	 * The state of the board represented by the state of its intersections.
	 * (0,0) is the top left corner of the board.
	 */
	private IS[][] board;
	/** The previous ("minus 1") state of the board. board is set to this when a move is undone. */
	private IS[][] board_m1;
	//TODO: Convert to IS[] lastStones so that boardToString() can mark previous last stones correctly
	private Position playedLast = new Position(0, 0); //Position where the last stone was put
	private Position played2ndLast = new Position(-1, -1); //used e.g. when a locally processed move must be undone
	private Position killedLast = new Position(-1, -1);
	private int lastKillCount = 0;
	boolean blackGroup;
	boolean whiteGroup;
	int currentGroup = 0;							  //How many different regions exist
	int dim = Constants.BOARD_DIM;           //Board dimension (Beginner = 9, Professional = 19)
	private boolean isMyTurn;
	private boolean isGameOver = false;
	
	public Model(){
		gameCnt = 1;                    //The game starts at draw #1
		//Create board with empty intersections
		board = new IS[dim][dim];
		board_m1 = new IS[dim][dim];
		
		initBoard(board);
		initBoard(board_m1);
	}//Model constructor
	
	private void initBoard(IS[][] board){
	    IS is;                          //Used for initializing intersections
        //TODO Redundant vs what is done in View?
        //Create center intersections
        for (int y=1; y < dim-1; y++){
            for (int x=1; x < dim-1; x++){
                is = new IS(IS.Type.C);
                board[y][x] = is;
            }
        }
        
        //Create corner intersections
        is = new IS(IS.Type.CRN_TL);
        board[0][0] = is;
        is = new IS(IS.Type.CRN_TR);
        board[0][dim-1] = is;
        is = new IS(IS.Type.CRN_BL);
        board[dim-1][0] = is;
        is = new IS(IS.Type.CRN_BR);
        board[dim-1][dim-1] = is;
        
        //Create edge intersections
        for (int x = 1; x < dim-1; x++){ //Top
            is = new IS(IS.Type.E_T);
            board[0][x] = is;
        }
        for (int y = 1; y < dim-1; y++){ //Left
            is = new IS(IS.Type.E_L);
            board[y][0] = is;
        }
        for (int y = 1; y < dim-1; y++){ //Right
            is = new IS(IS.Type.E_R);
            board[y][dim-1] = is;
        }
        for (int x = 1; x < dim-1; x++){ //Bottom
            is = new IS(IS.Type.E_B);
            board[dim-1][x] = is;
        }
	}
	
	public void send(int b){
	    isMyTurn = false;
	    try {
	        System.out.println("Model: send: Going to send draw...");
            lan.send(b); 
            System.out.println("Model: send: Sent draw");
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}//send
	
	public void receive(){
	    try {
	        System.out.println("Model: receive: Call to receive");
            int recv = lan.receive();
            System.out.println("Model: receive: Return from receive. recv: " + recv);
            if (recv == Constants.SEND_PASS){
                System.out.println("Model: receive: Received pass");
                pass();
                setChanged();
                notifyObservers(UpdateMessages.RECVD_PASS);
            }else if (recv == Constants.SEND_DOUBLEPASS){
                System.out.println("Model: receive: Received double pass");
                pass();
                isGameOver = true;
                setChanged();
                notifyObservers(UpdateMessages.RECVD_DOUBLEPASS);
            }else if (recv == Constants.SEND_QUIT){
                System.out.println("Model: receive: Received quit");
                isGameOver = true;
                setChanged();
                notifyObservers(UpdateMessages.RECVD_QUIT);
            }else{
                int y = recv / dim;
                int x = recv - dim*y;
                processMove(y, x);
                setChanged();
                notifyObservers(UpdateMessages.RECVD_MOVE);
            }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } catch (InterruptedException e) {
			e.printStackTrace();
		}
	    isMyTurn = true;
	}//receive
	
	
	/**
	 * Creates a {@link Server} or a {@link Client} depending on whether 
	 * {@code server_address} is an empty string or not 
	 * and assigns it to {@code lan}. Also sets {@code player}.
	 * 
	 * @param server_address empty string if this shall be the server,
	 * or the address of the remote server if this shall be the client
	 * 
	 * Also starts {@code lan} and thereby establishes the LAN connection 
     * 
     * @return 0 if the server/client was created successfully and,
     * if this is the client, if it successfully connected to the server 
     * <br> -1 if an IOException occurred
     * <br> -2 if {@code lan} is already connected
     * @see LAN_Conn
     * @see Client
     * @see Server
	 */
	public int setLANRole(String server_address){
	    try {
            if (this.lan != null){
                if (this.lan.isConnected())
                    return -2;
                lan.terminate();
            }
            
            if (server_address.equals("")){
                this.lan = new Server(this);
                this.player = Player.WHITE;
                isMyTurn = false;
            }else{
                this.lan = new Client(this);
                this.player = Player.BLACK;
                isMyTurn = true;
            }
            this.lan.init(server_address); 
            this.lan.start();
            return 0;
        }catch (IOException e) { //from lan.terminate() or lan.init()
            return -1;
        }
	}//setLAN_Role

    
    /**
	 * Calculate both players' territory on the board
     */
	public void calcTerritory(){
		int[][] mark = new int[dim][dim];         //Used to mark regions of each player
												  //Create a copy of the board to mark empty intersections
		ter_B = 0;
	    ter_W = 0;
	    currentGroup = 0;						  //Number of current region (1-81)
	    for (int y=0; y < dim; y++){     
	        for (int x=0; x < dim; x++){           //Make sure that every intersection on the board has been tested
	            
	            blackGroup = false;               //Reset region marker
	            whiteGroup = false;               //Reset region marker
	            currentGroup++;				   //Mark intersections of an empty region in isEmptyRegion() as a specific region
	            
	            markGroup(y, x, mark);
	            
	            int cnt = 0;
	            for (int[] ia : mark){
	                for (int val : ia){
	                    if (val == currentGroup){
	                        cnt++;
	                    }//if
	                }//for
	            }//for
	            if (blackGroup && !whiteGroup){      	 //Test whether the region marked before is Black's territory
	                ter_B += cnt;
	            }else if (whiteGroup && !blackGroup){      //Test whether the region marked before is White's territory
	                ter_W += cnt;            	
	            }//if
	        }//for
	    }//for
	}//calcTerritory

	
	/**
	 * TODO Complete description
	 * <br> TODO Redundant with findKillOppGroups and hasGroupLiberty???
	 * 
	 * <p> Mark groups of empty intersections and find out whether they belong to Black or White.
	 * <br> > 0: belongs to a group, -1: black stone, -2: white stone.
	 * 
	 * @param y
	 * @param x
	 * @param mark 
	 */
	public void markGroup(int y, int x, int[][] mark){        
		if (   y < 0 || y >= dim || x < 0 || x >= dim //Reached border of game table
		    || mark[y][x] > 0 ){                      //Found already marked intersection
		    return;
		}else if ( mark[y][x] == -1 ){              //TODO ???
			blackGroup = true;
		}else if ( mark[y][x] == -2 ){              //TODO ???
			whiteGroup = true;
	    }else if ( board[y][x].getState().equals(IS.State.B) ){ //Found black stone -> border of empty region!    
	        blackGroup = true;
	        mark[y][x] = -1;
	    }else if ( board[y][x].getState().equals(IS.State.W) ){ //Found white stone -> border of empty region!
            whiteGroup = true;
            mark[y][x] = -2;
	    }else{
	        mark[y][x] = currentGroup;        //Mark the empty intersection as part of the current region
	        markGroup(y, x-1, mark);                //Look west
	        markGroup(y-1, x, mark);                //north
	        markGroup(y, x+1, mark);                //east
	        markGroup(y+1, x, mark);                //south
	    }//if
	}
	
	
    /**
     * TODO Change from empty corner/edge icon to its counterpart with stone and vice versa. <p>
     * 
     * Performs a non-pass move, i.e.
     * <br> places the stone,
     * <br> finds adjacent opponent groups and removes them if this stone is taking their last liberty,
     * <br> if no enemy group was removed, determines whether this move is suicide (if an enemy group was removed, this move can't be suicide), 
     * <br> and if yes, rolls back the changes that this move has made.
     * 
     * @param y y-coordinate of the intersection on which the stone is being placed
     * @param x x-coordinate of the intersection on which the stone is being placed
     * @return 
     * @see #findKillOppGroups
     * @see #isSuicide
     */
    public MoveReturn processMove(int y, int x) {
//        //Backup in case the move is undone
//        backupBoardStates();
//        backupPrisoners();
        
        printBoards(); //for debugging
        
        //TODO Bad semantics, state <> player
        MoveReturn mr = findKillOppGroups(y, x, getCurrentPlayer());
        if (mr.equals(MoveReturn.KO) || mr.equals(MoveReturn.SUICIDE))
    		return mr;
        
        board[playedLast.getY()][playedLast.getX()].wasNotPutLast(); //Remove indicator
        played2ndLast.set(playedLast);
        playedLast.set(y, x);
        
        gameCnt++;  //Game counter is set to next player's turn
        return MoveReturn.OK;
    }// processMove

    
	/**
     * TODO Change data type of playerColor to something more appropriate (like Player or Player.Color)
     * <br> TODO: Maybe decouple this and other methods by introducing a method that returns groups (including info if they're black/white) <p>
     * 
     * Called after a stone was placed. Finds adjacent opponent groups of stones
     * and removes them if this stone took their last liberty.
     * In more detail: If an opponent stone is found, the latter determines its group and finds out whether it has a liberty.
     * If the group does not have a liberty it is removed.
     * 
     * <p> {@code mark}:
     * <br> contains zeros by default ("empty intersections"). hasGroupLiberty marks groups that are the opponent from the
     * perspective of this player with 1. If the marked group doesn't have a liberty, its stones are removed.
     * 
     * @param y y-coordinate of the intersection on which a stone was placed
     * @param x x-coordinate of the intersection on which a stone was placed
     * @param playerColor a player's color ({@code IS.State.B} or {@code IS.State.W}). Never use {@code IS.State.E}! Type Player or PlayerColor would be more appropriate but IS.State is more compatible 
     * @see #processMove
     * @see #hasGroupLiberty
     * @return {@link MoveReturn} OK if the move is valid, KO if it's invalid due to ko-situation, SUICIDE if it would be suicide
     */
    public MoveReturn findKillOppGroups(int y, int x, IS.State playerColor){
    	Position killed = new Position(-1, -1);
    	int killCount = 0;
    	
    	board[y][x].setState(getCurrentPlayer());      //Put player's stone on empty intersection
    	Position[] adj = getAdjPositions(y, x);
        for (Position pos : adj) {
            if (board[pos.getY()][pos.getX()].getState().equals(getOpponent(playerColor))){
                int [][] mark = new int[dim][dim];                                          //Mark what we find
                mark[y][x] = 2; //mark our stone as an opponent's stone for hasGroupLiberty
                if (!hasGroupLiberty(pos.getY(), pos.getX(), pos.getY(), pos.getX(), getOpponent(playerColor), mark)){  //If the opponent group doesn't have a liberty,
                    for (int l = 0; l < dim; l++) {                                             //remove it.
                        for (int k = 0; k < dim; k++) {
                            if (mark[l][k] == 1) {
                				board[l][k].setEmpty();
                				killed.set(l, k);
                				killCount++;
                            }//if
                        }//for
                    }//for
                }
            }
        }
        if (killCount == 0 && isSuicide(y, x)){
        	board[y][x].setEmpty();
            return MoveReturn.SUICIDE;
        }
        if (killCount == 1 && lastKillCount == 1
        		&& killedLast.equals(new Position(y, x))){
    		board[y][x].setEmpty();
    		board[killed.getY()][killed.getX()].setState(getOpponent(getCurrentPlayer()));
    		return MoveReturn.KO;
    	}
        if (killCount > 0){
        	if (getCurrentPlayer().equals(IS.State.W)) {        //White's move
        		pris_W += killCount;                                       //White captures the removed black stone
        	} else {                                            //Black's move
        		pris_B += killCount;                                       //Black captures the removed white stone
        	}
        	if (killCount == 1){ 
        		killedLast.set(killed);
        	}else{ //if more than one stone was killed, next round can't be ko
        		killedLast.set(-1, -1);
        	}
        }
        lastKillCount = killCount;
        return MoveReturn.OK;
    }
    
    
    /**
     * TODO Complete description
     * <br> TODO Redundant with markRegion???
     *
     * <p> Returns true if the group of stone {@code [yStart][xStart]} of player {@code playerColor} has a liberty.
     * Also stores found stones (both of {@code playerColor} and of their opponent) in {@code mark}. 
     * 
     * @param yStart y-coordinate of the intersection on which a stone was placed
     * @param xStart x-coordinate of the intersection on which a stone was placed
     * @param yNow y-coordinate of the current intersection that has been reached by the recursion
     * @param xNow x-coordinate of the current intersection that has been reached by the recursion
     * @param playerColor a player's color ({@code IS.State.B} or {@code IS.State.W}). Never use {@code IS.State.E}! Type Player or PlayerColor would be more appropriate but IS.State is more compatible
     * @param mark stores found stones (both of {@code playerColor} and of their opponent) in {@code}
     * @return true if the group of stone {@code [yStart][xStart]} of player {@code playerColor} has a liberty
     * 
     * @see #findKillOppGroups
     * @see #isSuicide
     */
    public boolean hasGroupLiberty(int yStart, int xStart, int yNow, int xNow, IS.State playerColor, int[][] mark){
        if (yNow < 0 || yNow >= dim || xNow < 0 || xNow >= dim         //Reached border of the board
                || mark[yNow][xNow] == 1 || mark[yNow][xNow] == 2){    //Already been here 
            return false;                                
        }else if (board[yNow][xNow].getState().equals(getOpponent(playerColor))){ //Found adjacent stone of opponent color
            mark[yNow][xNow] = 2;                    
            return false;
        }else if (board[yNow][xNow].getState().equals(IS.State.E) && (yStart != yNow || xStart != xNow)){
            return true;                                 //Found a liberty which is not the starting intersection
        }else{                                           //Found adjacent stone of this color

            mark[yNow][xNow] = 1;                    
            if (    (hasGroupLiberty(yStart, xStart, yNow, xNow-1, playerColor, mark))     // look west, north, east, south
                 || (hasGroupLiberty(yStart, xStart, yNow-1, xNow, playerColor, mark))
                 || (hasGroupLiberty(yStart, xStart, yNow, xNow+1, playerColor, mark)) 
                 || (hasGroupLiberty(yStart, xStart, yNow+1, xNow, playerColor, mark))) {

                return true;
            }// if
            return false;           
        }//if
    }
    
	
	/**
	 * Returns true if the move is suicide, false if it isn't.
	 * 
	 * @param y y-coordinate of the intersection on which a stone shall be placed 
	 * @param x x-coordinate of the intersection on which a stone shall be placed
	 * @return true if the move is suicide, false if it isn't
	 * 
	 * @see #findKillOppGroups
	 * @see #hasGroupLiberty
	 */
    public boolean isSuicide(int y, int x) {
        return !hasGroupLiberty(y, x, y, x, getCurrentPlayer(), new int[dim][dim]);
    }//isSuicide
    

    /** 
     * Pass a draw
     * @return true if double-pass, false if not 
     */
    public boolean pass() {
        backupBoardStates(); 
        int pris_tmp, pris_tmp_b4;
        if (getCurrentPlayer().equals(IS.State.B)) { //White passes
            pris_tmp = pris_B;
            pris_tmp_b4 = pris_B_b4;
            this.pris_W_b4 = this.pris_W;
            this.pris_W++; //and White receives a prisoner point
        } else { //Black passes
            pris_tmp = pris_W;
            pris_tmp_b4 = pris_W_b4;
            this.pris_B_b4 = this.pris_B;
            this.pris_B++; //and Black receives a prisoner point
        }
        this.gameCnt++;
        //TODO: Why does this mess up the score panel and prevents waiting dialog from being shown?
//        send(Constants.SEND_PASS);
        
        if (pris_tmp == pris_tmp_b4 + 1){
            isGameOver = true;
        }
        return isGameOver;
    }//pass
    
    
    /** Undoes the last draw */
    public void undo(){
        if (gameCnt > 1 ){                          //If it's the first move, there's nothing to be undone
            
            restoreBoardStates();            
            restorePrisoners();
            
            gameCnt--;                              //Set game count to last turn
            
            board[playedLast.getY()][playedLast.getX()].wasNotPutLast();
            playedLast.set(played2ndLast);
            board[playedLast.getY()][playedLast.getX()].wasPutLast();
        }
    }//undo
	

	private void backupBoardStates(){
	    cpyBoard(board, board_m1);
	}
	
	private void restoreBoardStates(){
        cpyBoard(board_m1, board);                          
	}
	
	private void backupPrisoners(){
        if (getCurrentPlayer().equals(IS.State.B)) {
            this.pris_W_b4 = this.pris_W;
        } else {
            this.pris_B_b4 = this.pris_B;
        }
	}
	
	private void restorePrisoners(){
        if (getCurrentPlayer().equals(IS.State.B)){         //Depending on the player whose turn it was, his latest prisoners are undone
            this.pris_W = this.pris_W_b4;                           
        }else{
            this.pris_B = this.pris_B_b4;               
        }
	}
	

    /**
     * TODO Improve description <p>
     * 
     * Returns true if two {@code IS[][]} board states are equal with respect to the values of the boards' {@code IS} intersections.
	 * 
	 * <p> Used to assure that the undo button is hit only once in a row.
	 * If {@code board} and {@code board_m1} are already equal, it's not allowed to undo your move.
	 * Used to find out whether it's a ko-situation.
	 * If {@code board} and {@code board_m2} are equal, the move reproduces the preceding state of the board and therefore isn't allowed.
     * 
     * @param one one {@code IS[][]} board state
     * @param other the other {@code IS[][]} board state
     * @return true if two {@code IS[][]} board states are equal with respect to the values of the boards' {@code IS} intersections. 
     * @see IS
     */
    public boolean areBoardsEqual(IS[][] one, IS[][] other){
        for (int y = 0; y < dim; y++){
            for (int x = 0; x < dim; x++){
                //TODO: implement and use equals instead of comparing states? Also in cpyBoard()
                if ( one[y][x].getState() != other[y][x].getState() ){
                    return false;
                }
            }
        }
        return true;
    }//areBoardsEqual
    
    
    /**
     * Returns true if board[y][x] is an empty intersection.
     * 
     * @param y 
     * @param x
     * @return true if board[y][x] is an empty intersection
     */
	public boolean isEmptyIntersection(int y, int x){
	    return (board[y][x].getState() == IS.State.E);    
	}//isEmptyIntersection
	
	
	/** @return all positions adjacent to (y,x) (less than 4
	 * if (y,x) is at the board rim or in a corner) */
	public Position[] getAdjPositions(int y, int x){
        List<Position> adj = new ArrayList<Position>();
		int []yAdj = new int[4];
        yAdj[0] = y;
        yAdj[1] = y-1;
        yAdj[2] = y;
        yAdj[3] = y+1;
        int []xAdj = new int[4];
        xAdj[0] = x-1;
        xAdj[1] = x;
        xAdj[2] = x+1;
        xAdj[3] = x;
        for (int i = 0; i < 4; i++){
            if (yAdj[i] < 0 || yAdj[i] >= dim || xAdj[i] < 0 || xAdj[i] >= dim)
                continue;
            adj.add(new Position(yAdj[i], xAdj[i]));
        }
        return adj.toArray(new Position[adj.size()]);
	}
	
	
	/**
	 * TODO Complete description <p>
	 * 
	 * Copy the whole board. Used while either processing or undoing a move as well as in initialization 
	 * 
	 * @param src
	 * @param dest
	 */
	public void cpyBoard(IS[][] src, IS[][] dest){
        for (int y=0; y < dim; y++){
            for (int x=0; x < dim; x++){
                if (dest[y][x].getState() != src[y][x].getState())
                    dest[y][x].setState(src[y][x].getState());
            }            
        }
    }

	//TODO Guess it's not OK to work around a 'protected' qualifier like this...
    public void setChanged1() {
        setChanged();
    }//setChanged1
    
    public void notifyObservers2(Object updateMessage){
        notifyObservers(updateMessage);
    }//notifyObservers2
    
    /**
     * @param board
     * @param orientations whether to include the intersections' orientations or not
     * @return
     */
    public String boardToString(IS[][]board, boolean orientations){
        StringBuffer results = new StringBuffer();
        String separator = " ";

        for (int y = 0; y < dim; ++y){
            if (y > 0)
                results.append('\n');
            for (int x = 0; x < dim; ++x){
                results.append(board[y][x].toString(orientations)).append(separator);
            }
        }
        return results.toString();
    }

    @Override
    public String toString() {
        return "\nModel [lan=" + lan + ", ter_B=" + ter_B + ", ter_W=" + ter_W + ", pris_B="
                + pris_B + ", pris_W=" + pris_W + ", pris_B_b4=" + pris_B_b4 + ", pris_W_b4=" + pris_W_b4 + ", gamecnt="
                + gameCnt + ",\nboard=\n" + boardToString(board, false) + ",\nboard_b4=\n" + boardToString(board_m1, false)
                + ",\ntmp_4_ko=\n" /*+ boardToString(board_m2, false)*/
                + ",\nblackGroup=" + blackGroup + ", whiteGroup=" + whiteGroup + ", currentRegion=" + currentGroup
                + ", dim=" + dim + "]\n";
    }
    
    public IS[][] getBoard() {
        return board;
    }

    public IS[][] getBoard_m1() {
        return board_m1;
    }
    
    public int getTer_B() {
        return ter_B;
    }

    public int getTer_W() {
        return ter_W;
    }

    public int getPris_B() {
        return pris_B;
    }

    public int getPris_W() {
        return pris_W;
    }

    public int getScr_B(){
        return pris_B + ter_B;
    }
    
    public int getScr_W(){
        return pris_W + ter_W;
    }
    
    public boolean isMyTurn(){
        return isMyTurn;
    }
    
    public boolean isGameOver(){
        return isGameOver;
    }
    
	public int getGamecnt() {
	    return gameCnt;
	}
	
    public IS getIntersection(int y, int x) {
        return board[y][x];
    }// getIntersection
 
    public Player getMyPlayer(){
        return player;
    }
    
    //Can only be set once
    public void setMyPlayer(Player player){
        this.player = this.player == null ? player : this.player;
    }
    
    //TODO This doesn't return the Player but the "color of the stone on an intersection"(?!?!) -> Either change return type to Player or create Enum Stone that doesn't contain E (empty)  
    public IS.State getCurrentPlayer(){       //Getting the color of the player whose turn it is
        if (gameCnt % 2 == 0){
            return IS.State.W;
        }else{
            return IS.State.B;
        }
    }//getPlayer
    
    //TODO Currently this doesn't get the opponent but the state of the intersection (which could also be empty but let's hope it's never). Either change return type to Player(color) or change the way it is called/used!
    public IS.State getOpponent(IS.State state){       //Gets the color of the specified color's opponent
        if (state == IS.State.W){
            return IS.State.B;
        }else if(state == IS.State.B){
            return IS.State.W;
        }else{
            System.out.println("This should not have happened.");
            return null;
        }
    }//getOpponent
    
    private void printBoards() {
//        System.out.println("board_m2:");
//        System.out.println(boardToString(board_m2, false) + "\n");

//        System.out.println("board_m1:");
//        System.out.println(boardToString(board_m1, false) + "\n");
        
        System.out.println("board:");
        System.out.println(boardToString(board, false) + "\n");
    }
    
    public enum MoveReturn {
    	OK, SUICIDE, KO
    }
}//Model