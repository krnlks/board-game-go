package game;

import java.io.IOException;
import java.util.Arrays;
import java.util.Observable;

import multiplayer.Client;
import multiplayer.LAN_Conn;
import multiplayer.Server;
import multiplayer.UpdateMessages;

/* Ideas for improvement:
 * - Global variables
 * - markRegion() returns true/false -> not what you would expect 
 * - Return value of markRegion is never used -> OK to "abuse" it for recursion?
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
	//TODO: Maybe move the 4 board states to one IS[][][]
	/**
	 * The state of the board represented by the state of its intersections.
	 * (0,0) is the top left corner of the board.
	 */
	private IS[][] board;
	/** The previous ("minus 1") state of the board. board is set to this when a move is undone. */
	private IS[][] board_m1;
	/**
	 * The second last ("minus 2") state of the board.
	 * Compared to board to detect illegal move in "ko"-situation.
	 */
	private IS[][] board_m2;
	/**
	 * The third last ("minus 3") state of the board.
	 * Used as a backup for board_m2 when a locally processed move is undone because it's invalid.
	 */
	private IS[][] board_m3;
	//TODO: Convert to IS[] lastStones so that boardToString() can mark previous last stones correctly
	private IS lastStone;                     //Intersection that the last stone was put on
	private IS lastStone_backup;                     //back up of last in for when a locally processed move must be undone
	int[][] mark;                            //Used to mark regions of each player
	boolean blackGroup;
	boolean whiteGroup;
	int currentGroup = 0;							  //How many different regions exist
	int dim = Constants.BOARD_DIM;           //Board dimension (Beginner = 9, Professional = 19)
	private boolean isMyTurn;
	private boolean isGameOver = false;
	
	public Model(){
		gameCnt = 1;                    //The game starts at draw #1
		//Create boards with empty intersections
		board = new IS[dim][dim];
		board_m1 = new IS[dim][dim];
		board_m2 = new IS[dim][dim];
		board_m3 = new IS[dim][dim];
		
		initBoard(board);
		initBoard(board_m1);
		initBoard(board_m2);
		initBoard(board_m3);
        
        lastStone = board[0][0]; //Initialize in order to prevent null pointer exception in processMove
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
	        // TODO Auto-generated catch block
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
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    isMyTurn = true;
	}//receive
	
    /**
     * Starts the {@link LAN_Conn} and thereby establishes the LAN connection 
     * 
     * @return 0 in case of success; -1 if an IOException has occurred
     * and re-initialization is necessary
     */
    public int estbl_LanConn() {
        try{
            this.lan.init();
            this.lan.start();
            return 0;
        }catch (Exception e) {
            return -1;
        }
    }//estbl_LanConn   
	
	/**
	 * Creates a {@link Server} or a {@link Client} depending on {@code server_address} and assigns it to {@code lan}. Also sets {@code player}
	 * 
	 * @param server_address empty string if this shall be the server,
	 * or the address of the remote server if this shall be the client
	 */
	public void setLANRole(String server_address){
	    if (this.lan == null){
	        if (server_address.equals("")){
	            this.lan = new Server(this);
	            this.player = Player.WHITE;
	        }else{
	            this.lan = new Client(this, server_address);
	            this.player = Player.BLACK;
	            isMyTurn = true;
	        }
	    }
	}//setLAN_Role
	
	
	public int getGamecnt() {
	    return gameCnt;
	}//getGamecnt
	
    public IS getIntersection(int y, int x) {
        return board[y][x];
    }// getIntersection
    
    public IS getIntersectionB4(int y, int x){
        return board_m1[y][x];
    }//getCpyIntersection
 
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

    
    /**
	 * Get both players' territory on the board
     */
	public void getTerritory(){
		mark = new int[dim][dim];                 //Create a copy of the board to mark empty intersections
		ter_B = 0;
	    ter_W = 0;
	    currentGroup = 0;						  //Number of current region (1-81)
	    for (int y=0; y < dim; y++){     
	        for (int x=0; x < dim; x++){           //Make sure that every intersection on the board has been tested
	            
	            blackGroup = false;               //Reset region marker
	            whiteGroup = false;               //Reset region marker
	            currentGroup++;				   //Mark intersections of an empty region in isEmptyRegion() as a specific region
	            markGroup(y, x);
	            
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
	}//getTerritory

	
	/**
	 * TODO Complete description
	 * <br> TODO Redundant with findKillOppGroups and hasGroupLiberty???
	 * 
	 * <p> Mark groups of empty intersections and find out whether they belong to Black or White.
	 * <br> > 0: belongs to a group, -1: black stone, -2: white stone.
	 * 
	 * @param y
	 * @param x
	 */
	public void markGroup(int y, int x){        
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
	        markGroup(y, x-1);                //Look west
	        markGroup(y-1, x);                //north
	        markGroup(y, x+1);                //east
	        markGroup(y+1, x);                //south
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
     * @return true if the move is no suicide and has been performed, false if it would be suicide and therefore hasn't been performed
     * @see #findKillOppGroups
     * @see #isSuicide
     */
    public boolean processMove(int y, int x) {
        //Backup in case the draw is undone
        backupBoardStates();
        backupPrisoners();

        printBoards(); //for debugging
        
        //TODO Bad semantics, state <> player
        board[y][x].setState(getCurrentPlayer());                           //Put player's stone on empty intersection
        if (!findKillOppGroups(y, x, getCurrentPlayer())                    //Search for opponent groups to be removed
                && isSuicide(y, x)){
            //TODO: Don't set and then unset again (use marker instead)
            board[y][x].setState(IS.State.E);
            restoreBoardStates();
            restorePrisoners();
            return false;
        }

        lastStone.wasNotPutLast();                //Remove indicator
        lastStone_backup = lastStone;
        lastStone = board[y][x];
       
        gameCnt++;                                                      //Game counter is set to next player's turn
        return true;
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
     * @return true if an opponent group was removed, false if not
     */
    public boolean findKillOppGroups(int y, int x, IS.State playerColor){
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
        boolean hasKilledGroup = false;
        for (int i = 0; i < 4; i++){
            if (yAdj[i] < 0 || yAdj[i] >= dim || xAdj[i] < 0 || xAdj[i] >= dim
                    || !board[yAdj[i]][xAdj[i]].getState().equals(getOpponent(playerColor))){
                continue;
            }else{
                int [][] mark = new int[dim][dim];                                          //Mark what we find
                mark[y][x] = 2; //mark our stone as an opponent's stone for hasGroupLiberty
                if (!hasGroupLiberty(yAdj[i], xAdj[i], yAdj[i], xAdj[i], getOpponent(playerColor), mark)){  //If the opponent group doesn't have a liberty,
                    for (int l = 0; l < dim; l++) {                                             //remove it.
                        for (int k = 0; k < dim; k++) {
                            if (mark[l][k] == 1) {
                                board[l][k].setState(IS.State.E);
                                if (getCurrentPlayer().equals(IS.State.W)) {        //White's move
                                    pris_W++;                                       //White captures the removed black stone
                                } else {                                            //Black's move
                                    pris_B++;                                       //Black captures the removed white stone
                                }// if                               
                            }//if
                        }//for
                    }//for
                    hasKilledGroup = true;
                }
            }
        }
        return hasKilledGroup;
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
        int[][] mark = new int[dim][dim];                                       //Mark what we find
        return !hasGroupLiberty(y, x, y, x, getCurrentPlayer(), mark);          //If the stone's group has a liberty, it isn't suicide
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
            
            lastStone.wasNotPutLast();
            lastStone = lastStone_backup;
            lastStone.wasPutLast();
        }
    }//undo
	

	private void backupBoardStates(){
	    cpyBoard(board_m2, board_m3);
	    cpyBoard(board_m1, board_m2);
	    cpyBoard(board, board_m1);
	}
	
	private void restoreBoardStates(){
        cpyBoard(board_m1, board);                          
        cpyBoard(board_m2, board_m1);                           
        cpyBoard(board_m3, board_m2);   
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
     * Returns true if the player is trying to put his stone on an empty intersection.
     * 
     * @param y 
     * @param x
     * @return true if the player is trying to put his stone on an empty intersection
     */
	public boolean isEmptyIntersection(int y, int x){
	    if (board[y][x].getState() == IS.State.E) {    
	        return true;
	    }else{
	        return false;
	    }
	}//isEmptyIntersection
	
	
	/**
	 * TODO Complete description
	 * <br><br>
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
    }//cpyField

	//TODO Guess it's not OK to work around a 'protected' qualifier like this...
    public void setChanged1() {
        setChanged();
    }//setChanged1
    
    public void notifyObservers2(Object updateMessage){
        notifyObservers(updateMessage);
    }//notifyObservers2
    
    /**
     * 
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
                + ",\ntmp_4_ko=\n" + boardToString(board_m2, false) + ",\nboardCpy=\n" + Arrays.toString(mark)
                + ",\nblackRegion=" + blackGroup + ", whiteRegion=" + whiteGroup + ", currentRegion=" + currentGroup
                + ", dim=" + dim + "]\n";
    }
    
    public IS[][] getBoard() {
        return board;
    }//getFields

    public IS[][] getBoard_m1() {
        return board_m1;
    }//getFields_b4
    
    public IS[][] getBoard_m2(){
        return board_m2;
    }//getTmp_4_ko

    public int getTer_B() {
        return ter_B;
    }//getTer_B

    public int getTer_W() {
        return ter_W;
    }//getTer_W

    public int getPris_B() {
        return pris_B;
    }//getPris_B

    public int getPris_W() {
        return pris_W;
    }//getPris_W

    public int getScr_B(){
        return pris_B + ter_B;
    }//getScr_B
    
    public int getScr_W(){
        return pris_W + ter_W;
    }//getScr_W
    
    public boolean isMyTurn(){
        return isMyTurn;
    }
    
    public boolean isGameOver(){
        return isGameOver;
    }
    
    private void printBoards() {
        System.out.println("board_m2:");
        System.out.println(boardToString(board_m2, false) + "\n");

        System.out.println("board_m1:");
        System.out.println(boardToString(board_m1, false) + "\n");
        
        System.out.println("board:");
        System.out.println(boardToString(board, false) + "\n");
    }

}//Model