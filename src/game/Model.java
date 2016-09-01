package game;

import java.io.IOException;
import java.util.Arrays;
import java.util.Observable;

import multiplayer.Client;
import multiplayer.LAN_Conn;
import multiplayer.Server;
import multiplayer.UpdateMessages;

/* Ideas for improvement:
 * - isNoSuicide (loops)
 * - Global variables
 * - markRegion() returns true/false -> not what you would expect 
 * - Return value of markRegion is never used -> OK to "abuse" it for recursion? 
 * - Rekursionen können bestimmt noch zusammengefasst werden
 */

//TODO Change KO back to "ko" - it's not KO but the name of a rule in Go 
//TODO Look at more complex methods in Model and write good and clear description

//TODO Sometimes I'm using ==, sometimes .equals. Fix that!

/**
 * The data and logic for the game. In Go Black plays against White. 
 * The game is played on a square board that consists of intersections that are either empty
 * or have a black or white stone upon them.
 * 
 * @author Lukas Kern
 */
public class Model extends Observable{
    
    private LAN_Conn lan;       //Server or client
    private Player player;         //Black or White
    private int ter_B;                    //Territory occupied by Black
    private int ter_W;                    //Territory occupied by White
    private int pris_B = 0;                    //Conquered opponent stones of Black
    private int pris_W = 0;                    //Conquered opponent stones of White
    private int pris_B_b4 = pris_B;
    private int pris_W_b4 = pris_W;
    
    /**
     * The number of the current draw, incremented by each player in each of their turns. Used to determine whose turn it is.
     * 
     * <p>TODO Also used for something else (like comparing board states)? Maybe there's a better solution. 
     */
	private int gamecnt;
	/**
	 * The state of the board represented by the state of its intersections.
	 * (0,0) is the top left corner of the board.
	 */
	private IS[][] board;
	private IS[][] board_b4;			 	//The preceding state of the board
	private IS[][] tmp_4_ko;				//Saves the state of intersections_b4 while testing on illegal move in KO-situation
	int[][] mark;                            //Used to mark regions of each player
	boolean blackRegion;
	boolean whiteRegion;
	int currentRegion = 0;							  //How many different regions exist
	int dim = Constants.BOARD_DIM;           //Board dimension (Beginner = 9, Professional = 19)
	
	//TODO Quite some things done in constructor. Outsource creation of the intersections to an init() method?
	public Model(){
		gamecnt = 1;                    //The game starts at draw #1
		board = new IS[dim][dim];		//Create board with empty intersections
		board_b4 = new IS[dim][dim];	//Copy of the board for undoing moves
		tmp_4_ko = new IS[dim][dim];	//New KO-test board
		
		IS is;                          //Used for initializing intersections
		//TODO Redundant vs what is done in View?
		//Create center intersections
		for (int y=1; y < dim-1; y++){
			for (int x=1; x < dim-1; x++){
			    is = new IS(IS.Orient.C);
				board[y][x] = is;
			}
		}
		
        
        //Create corner intersections
        is = new IS(IS.Orient.TL);
        board[0][0] = is;
        is = new IS(IS.Orient.TR);
        board[0][dim-1] = is;
        is = new IS(IS.Orient.BL);
        board[dim-1][0] = is;
        is = new IS(IS.Orient.BR);
        board[dim-1][dim-1] = is;
        
        
        //Create edge intersections
        for (int x=1; x<dim-1; x++){ //Top
            is = new IS(IS.Orient.T);
            board[0][x] = is;
        }
        for (int y=1; y<dim-1; y++){ //Left
            is = new IS(IS.Orient.L);
            board[y][0] = is;
        }
        for (int y=1; y<dim-1; y++){ //Right
            is = new IS(IS.Orient.R);
            board[y][dim-1] = is;
        }
        for (int x=1; x<dim-1; x++){ //Bottom
            is = new IS(IS.Orient.B);
            board[dim-1][x] = is;
        }
        
        cpyBoard(board, board_b4);
		
	}//Model constructor
	
	public void send(int b){
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
            System.out.println("Model: receive: Return from receive");
            if (recv == -1){
                //TODO Passing: Carry out opponent's pass draw locally
            }else{
                int y = recv / dim;
                int x = recv - dim*y;
                this.processMove(y, x);
            }
            setChanged();
            notifyObservers(UpdateMessages.DRAW_RECVD);
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
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
	public void setLAN_Role(String server_address){
	    if (server_address.equals("")){
	        this.lan = new Server(this);
	        this.player = Player.WHITE;
	    }else{
	        this.lan = new Client(this, server_address);
	        this.player = Player.BLACK;
	    }
	}//setLAN_Role
	
	/**
	 * Returns true if it's the calling player's turn.
	 * 
	 * @return true if it's the calling player's turn.
	 */
	public boolean isMyTurn(){
	    if ( player.equals(Player.BLACK) && getCurrentPlayer().equals(IS.State.B)
          || player.equals(Player.WHITE) && getCurrentPlayer().equals(IS.State.W)){
	        return true;
	    }else{
	        return false;
	    }
	}//isMyTurn
	
	public Player getPlayer(){
	    return player;
	}//getPlayer
	
	public int getGamecnt() {
	    return gamecnt;
	}//getGamecnt
	
    public IS getIntersection(int y, int x) {
        return board[y][x];
    }// getIntersection
    
    public IS getIntersectionB4(int y, int x){
        return board_b4[y][x];
    }//getCpyIntersection
    
    //TODO This doesn't return the Player but the "color of the stone on an intersection"(?!?!) -> Either change return type to Player or create Enum Stone that doesn't contain E (empty)  
    private IS.State getCurrentPlayer(){       //Getting the color of the player whose turn it is
        if (gamecnt % 2 == 0){
            return IS.State.W;
        }else{
            return IS.State.B;
        }
    }//getPlayer
    
    //TODO Currently this doesn't get the opponent but the state of the intersection (which could also be empty but let's hope it's never). Either change return type to Player(color) or change the way it is called/used!
    private IS.State getOpponent(IS.State state){       //Gets the color of the specified color's opponent
        if (state == IS.State.W){
            return IS.State.B;
        }else if(state == IS.State.B){
            return IS.State.W;
        }else{
            System.out.println("This should not have happened.");
            return null;
        }
    }//getOpponent

    public IS[][] getIntersections() {
        return board;
    }//getFields

    public IS[][] getIntersections_b4() {
        return board_b4;
    }//getFields_b4
    
    public IS[][] getTmp_4_ko(){
    	return tmp_4_ko;
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

    
    /**
	 * Get both players' territory on the board
     */
	public void getTerritory(){
		mark = new int[dim][dim];                 //Create a copy of the Go board to mark empty intersections
		ter_B = 0;
	    ter_W = 0;
	    currentRegion = 0;						  //Number of current region (1-81)
	    for (int y=0; y < dim; y++){     
	        for (int x=0; x < dim; x++){           //Make sure that every intersection on the board has been tested
	            
	            blackRegion = false;               //Reset region marker
	            whiteRegion = false;               //Reset region marker
	            currentRegion++;				   //Mark intersections of an empty region in isEmptyRegion() as a specific region
	            markRegion(y, x);
	            
	            int cnt = 0;
	            for (int[] ia : mark){
	                for (int val : ia){
	                    if (val == currentRegion){
	                        cnt++;
	                    }//if
	                }//for
	            }//for
	            if (blackRegion && !whiteRegion){      	 //Test whether the region marked before is Black's territory
	                ter_B += cnt;
	            }else if (whiteRegion && !blackRegion){      //Test whether the region marked before is White's territory
	                ter_W += cnt;            	
	            }//if
	        }//for
	    }//for
	}//getTerritory

	
	/**
	 * TODO Complete description
	 * <br><br>
	 * Returns true if ???
	 * <p> Mark empty regions and find out whether they are a region of Black or White 
	 * 
	 * @param y
	 * @param x
	 * @return true if ???
	 */
	public boolean markRegion(int y, int x){        
		if (y < 0 || y >= dim || x < 0 || x >= dim){
	        return false;	                        //Reached border of game table
		}else if ( mark[y][x] > 0 ){
	    	return false;        					//Found already marked field ( >0 = a region, -1 = black stone, -2 = white stone)
		}else if ( mark[y][x] == -1 ){
			blackRegion = true;
			return false;
		}else if ( mark[y][x] == -2 ){
			whiteRegion = true;
			return false;
	    }else if ( board[y][x].getState().equals(IS.State.B) ){    
	        blackRegion = true;
	        mark[y][x] = -1;
	        return false;                          //Found black field -> border of empty region!
	    }else if ( board[y][x].getState().equals(IS.State.W) ){
            whiteRegion = true;
            mark[y][x] = -2;
            return false;                          //Found white field -> border of empty region!
	    }else{
	        
	        mark[y][x] = currentRegion;        //Mark the empty intersection as part of the current region
	        
	        if (   ( markRegion(y, x-1) )           //Look west
	             ||( markRegion(y-1, x) )           //north
	             ||( markRegion(y, x+1) )           //east
	             ||( markRegion(y+1, x) ) ) {       //south
	            
	            return true;   
	        }//if     
	        return false;
	    }//if
	}//isEmptyRegion

	
	
	/**
	 * TODO Complete description
	 * <br><br>
	 * Returns true if the move is no suicide
	 * 
	 * @param i
	 * @param j
	 * @return true if the move is no suicide
	 */
	//TODO Change parameter names of this and similar methods from i, j to y, x
    public boolean isNoSuicide(int i, int j) {
        int [][] mark = new int[dim][dim];										//Mark what we find
        if (hasRegionFreedom(i, j, i, j, getCurrentPlayer(), mark)) {				//If the stone's region has a freedom, it isn't suicide
        	return true;
        } else {																//Is it really suicide?
        																		//New board matrix for marking intersections from the view of the
        	int [][] tmp = new int[dim][dim];										//current player's opponent 
        																		//Mark the current intersection with the color of
        	tmp[i][j] = 1;														//the stone that shall be put 
        	for (int y=0; y < dim; y++){
        	    for (int x=0; x < dim; x++){
        			if (mark[y][x] == 1){									//If it's an adjacent opponent stone
        				if (!hasRegionFreedom(y, x, y, x, getOpponent(getCurrentPlayer()), tmp)){	//If a surrounding opponent region has no freedom,
        					return true;													//it's no suicide
        				}
        			}
                	for (int k=0; k < dim; k++){
                		for (int l=0; l < dim; l++){
                			if (tmp[k][l] == 2){	//If the intersection has already been marked:
                				tmp[k][l] = 0;		//Clear it for hasRegionFreedom
                			}
                		}
                	}
        		}
        	}
        	return false;
        }//if
    }//isNoSuicide
    
    
	//TODO Highly redundant with lookAround
    /**
     * TODO Complete description
	 * <br><br>
	 * Returns true if the region of stone {@code [sx][sy]} of player {@code [color]} has a freedom
	 * <p> ??? 
     * 
     * @param sx x-coordinate of the intersection on which a stone was placed (the starting intersection)
     * @param sy y-coordinate of the intersection on which a stone was placed
     * @param xNow x-coordinate of the current intersection that has been reached by the recursion
     * @param yNow y-coordinate of the current intersection that has been reached by the recursion
     * @param playerColor a player's color ({@code IS.State.B} or {@code IS.State.W}). Never use {@code IS.State.E}! Type Player or PlayerColor would be more appropriate but IS.State is more compatible
     * @param mark a matrix to mark what we find
     * @return
     */
	public boolean hasRegionFreedom(int sy, int sx, int yNow, int xNow, IS.State playerColor, int[][] mark){
	    if (yNow < 0 || yNow >= dim || xNow < 0 || xNow>= dim){
	        return false;                                //reached border of game table
	    }else if (mark[yNow][xNow] == 1 || mark[yNow][xNow] == 2) {
	    	return false;                                // already been here
	    }else if (board[yNow][xNow].getState().equals(getOpponent(playerColor))){
	    	mark[yNow][xNow] = 1;					 //found adjacent stone of opponent color
	    	return false;
	    }else if (board[yNow][xNow].getState().equals(IS.State.E) && (sy != yNow || sx != xNow)){
	        return true;                                 //found a freedom which is not the starting intersection
        }else{

            mark[yNow][xNow] = 2;					 //found adjacent stone of this color
            if (    (hasRegionFreedom(sy, sx, yNow, xNow-1, playerColor, mark))     // look west, north, east, south
                 || (hasRegionFreedom(sy, sx, yNow-1, xNow, playerColor, mark))
                 || (hasRegionFreedom(sy, sx, yNow, xNow+1, playerColor, mark)) 
                 || (hasRegionFreedom(sy, sx, yNow+1, xNow, playerColor, mark))) {

				return true;
            }// if
			return false;           
        }//if
    }//hasRegionFreedom
	
	
	//TODO Change from empty corner/edge icon to its counterpart w/ stone and vice versa.
	/**
	 * TODO Complete description
	 * <br><br>
	 * Executes the move and tests for ko.
	 * Returns the field in its state after the preceding move 
	 * 
	 * @param y
	 * @param x
	 * @see #lookAround
	 */
	public void processMove(int y, int x) {
		//Save state of prisoners for eventual undoing
		this.pris_W_b4 = this.pris_W;
		this.pris_B_b4 = this.pris_B;
        
		if (!areBoardsEqual(board_b4, board)){
			cpyBoard(board_b4, tmp_4_ko);								//Save a copy of board_b4 for testing on illegal move in "ko"-situation
		}
		
        cpyBoard(board, board_b4);									//Save the board state so that it can be undone later
        //TODO Bad logic, state <> player
        board[y][x].setState(getCurrentPlayer());					        //Put player's stone on empty intersection
        int [][] lookBoard = new int [dim][dim];
        lookAround(y, x, y, x, getCurrentPlayer(), lookBoard);					//Search for opponent regions to be removed							
        gamecnt++;														//Game counter is set to next player's turn
    }// processMove
    
    
	//TODO Complete description
	//TODO Change data type of playerColor to something more appropriate (like Player or Player.Color)
	//TODO lookAround is a really bad name for a method. Change to lookForRegion or merge with hasRegionFreedom (they are similar)
    /**
     * <br><br>
     * Central method for processing a move
     * Returns true if ???
     * <p> The current player's stone "looks around".
     * If it finds an opponent stone, the latter marks its region and finds out whether it has a freedom.
     * If its region has no freedom, the region is removed.
     * <p> For {@code lookBoard} and {@code mark}: 0 means "found empty intersection",
     * 1 means "found adjacent stone in the color {@code playerColor}",
     * 2 means "found adjacent stone in the opposite color of {@code playerColor}" 
     * 
     * @param sx x-coordinate of the intersection on which a stone was placed (the starting intersection)
     * @param sy y-coordinate of the intersection on which a stone was placed
     * @param xNow x-coordinate of the current intersection that has been reached by the recursion
     * @param yNow y-coordinate of the current intersection that has been reached by the recursion
     * @param playerColor a player's color ({@code IS.State.B} or {@code IS.State.W}). Never use {@code IS.State.E}! Type Player or PlayerColor would be more appropriate but IS.State is more compatible 
     * @param lookBoard a matrix to mark what we find
     * @return true if ???
     * @see #processMove
     */
    public boolean lookAround(int sy, int sx, int yNow, int xNow, IS.State playerColor, int[][] lookBoard){ //Looking for what??
	    if (yNow < 0 || yNow >= dim || xNow < 0 || xNow>= dim){
	        return false;
	    }else if (lookBoard[yNow][xNow] == 1 || lookBoard[yNow][xNow] == 2) {
	    	return false;                                //Already been here
	    }else if (board[yNow][xNow].getState().equals(IS.State.E)){                     //Found an empty intersection
	    	return false;
	    }else if (board[yNow][xNow].getState().equals(getOpponent(playerColor))){				//Found adjacent stone of opponent color
	     	int [][] mark = new int[dim][dim];										    //Mark what we find
	     	mark[sy][sx] = 1;
	    	if (!hasRegionFreedom(yNow, xNow, yNow, xNow, getOpponent(playerColor), mark)){	//The opponent region has no freedom and is removed
	    	    for (int y = 0; y < dim; y++) {
	    	        for (int x = 0; x < dim; x++) {
                        if (mark[y][x] == 2) {
                            board[y][x].setState(IS.State.E);
                            mark[y][x] = 0;               
							if (gamecnt % 2 == 0) { 							//White's move
								pris_W++; 										//White captures the removed black stone
							} else {											//Black's move
								pris_B++; 										//Black captures the removed white stone
							}// if                               
                        }//if
                    }//for
                }//for
	    	}else{														//The opponent region has a freedom and is unmarked														
	    	    for (int y = 0; y < dim; y++) {                         //so that a region of this color can "move through" these intersections
	    	        for (int x = 0; x < dim; x++) {				
                		if (mark[y][x] == 2) {
                			mark[y][x] = 0;
                		}
                	}
                }
	    	}
	    	return false;
		}else if (board[yNow][xNow].getState().equals(playerColor)) {
			lookBoard[yNow][xNow] = 1; 											//Found adjacent stone of this color
			
            if (    (lookAround(sy, sx, yNow, xNow-1, playerColor, lookBoard))     						//Look west, north, east, south
                 || (lookAround(sy, sx, yNow-1, xNow, playerColor, lookBoard))
                 || (lookAround(sy, sx, yNow, xNow+1, playerColor, lookBoard)) 
                 || (lookAround(sy, sx, yNow+1, xNow, playerColor, lookBoard))) {

   				return true; //Can never be reached?
            }// if
            return false;
		}//if
	    return false;
    }//lookAround

	
    /**
     * TODO Write description
     * <br><br>
     */
    public void undoMove(){
		if (gamecnt > 1 ){							//If it's the first move, there's nothing to be undone
		    
			cpyBoard(board_b4, board);			//Copy array fields before -> fields				
			
			if (getCurrentPlayer().equals(IS.State.B)){			//Depending on the player whose turn it was, his latest prisoners are undone
				this.pris_W = this.pris_W_b4;							
			}else{
				this.pris_B = this.pris_B_b4;				
			}
			gamecnt--;								//Set game count to last turn
		}
	}//undoMove
	

    /**
     * TODO Improve description
     * <br><br>
     * Returns true if two {@code IS[][]} board states are equal with respect to the values of the boards' {@code IS} intersections.
	 * 
	 * <p> Used to assure that the undo button is hit only once in a row.
	 * If {@code board} and {@code board_b4} are already equal, it's not allowed to undo your move.
	 * Used to find out whether it is a KO-situation.
	 * If {@code board} and {@code tmp_4_ko} are equal, the move reproduces the preceding state of the board and therefore isn't allowed.
     * 
     * @param one one {@code IS[][]} board state
     * @param other the other
     * @return true if two {@code IS[][]} board states are equal with respect to the values of the boards' {@code IS} intersections. 
     * @see IS
     */
    public boolean areBoardsEqual(IS[][] one, IS[][] other){
        for (int y=0; y < dim; y++){
            for (int x=0; x < dim; x++){
                if ( !one[y][x].equals(other[y][x]) ){
                    return false;
                }
            }
        }
        return true;
    }//areBoardsEqual
    
    
    //TODO Passing: Display waiting dialog after passing (instead of "Please wait your turn")
    /**
     * Pass a draw
     */
    public void pass() {
    	cpyBoard(board, board_b4);
        if (this.gamecnt % 2 == 0) { //White passes
            this.pris_B_b4 = this.pris_B;
            this.pris_B++; //and black receives a prisoner point

        } else { //Black passes
            this.pris_W_b4 = this.pris_W;
            this.pris_W++; //and white receives a prisoner point
        }
        this.gamecnt++;
    }//pass
    
    
    /**
     * Returns true if the draw that is being processed is a double pass.
     * 
     * @return true if the draw that is being processed is a double pass
     */
    public boolean isDoublePass(){
    	int pris_tmp;
    	int pris_tmp_b4;
    	
    	if (getCurrentPlayer().equals(IS.State.B)){
    		pris_tmp = pris_B;
    		pris_tmp_b4 = pris_B_b4;
    	}else{
    		pris_tmp = pris_W;
    		pris_tmp_b4 = pris_W_b4;
    	}//if
    	
    	if (pris_tmp == pris_tmp_b4 + 1){
    		return true;
    	}else{
    		return false;
    	}
    }//isDoublePass
    
    
    
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
                dest[y][x] = src[y][x];
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
    
    private String boardToString(IS[][]board){
        StringBuffer results = new StringBuffer();
        String separator = " ";

        for (int y = 0; y < dim; ++y){
            if (y > 0)
                results.append('\n');
            for (int x = 0; x < dim; ++x){
              results.append(board[y][x]).append(separator);
          }
        }
        return results.toString();
    }

    @Override
    public String toString() {
        return "\nModel [lan=" + lan + ", player=" + player + ", ter_B=" + ter_B + ", ter_W=" + ter_W + ", pris_B="
                + pris_B + ", pris_W=" + pris_W + ", pris_B_b4=" + pris_B_b4 + ", pris_W_b4=" + pris_W_b4 + ", gamecnt="
                + gamecnt + ",\nboard=\n" + boardToString(board) + ",\nboard_b4=\n" + boardToString(board_b4)
                + ",\ntmp_4_ko=\n" + boardToString(tmp_4_ko) + ",\nboardCpy=\n" + Arrays.toString(mark)
                + ",\nblackRegion=" + blackRegion + ", whiteRegion=" + whiteRegion + ", currentRegion=" + currentRegion
                + ", dim=" + dim + "]\n";
    }

}//Model