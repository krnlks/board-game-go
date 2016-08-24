package game;

import java.io.IOException;
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

/**
 * The data and logic for the game. In Go Black plays against White. 
 * The game is played on a square board that consists of intersections that are either empty
 * or have a black or white stone upon them.
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
	int[][] boardCpy;                       //TODO Describe purpose!
	boolean blackRegion;
	boolean whiteRegion;
	int currentRegion = 0;							  //How many different regions exist
	int dim = Constants.BOARD_DIM;           //Board dimension (Beginner = 9, Professional = 19)
	
	public Model(){
		gamecnt = 1;                    //The game starts at draw #1
		board = new IS[dim][dim];		//Create board with empty intersections
		board_b4 = new IS[dim][dim];	//Copy of the board for undoing moves
		tmp_4_ko = new IS[dim][dim];	//New KO-test board
		
		for (int y=0; y < dim; y++){
			for (int x=0; x < dim; x++){
				board[y][x] = IS.E;
				board_b4[y][x] = IS.E;
			}
		}
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
	 * @return true if it's the calling player's turn.
	 */
	public boolean isMyTurn(){
	    if ( player.equals(Player.BLACK) && getCurrentPlayer().equals(IS.B)
          || player.equals(Player.WHITE) && getCurrentPlayer().equals(IS.W)){
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
    
    public IS getCpyIntersection(int y, int x){
        return board_b4[y][x];
    }//getCpyIntersection
    
    //TODO This doesn't return the Player but the "color of the stone on an intersection"(?!?!) -> Either change return type to Player or create Enum Stone that doesn't contain E (empty)  
    private IS getCurrentPlayer(){       //Getting the color of the player whose turn it is
        if (gamecnt % 2 == 0){
            return IS.W;
        }else{
            return IS.B;
        }
    }//getPlayer
    
    private IS getOpponent(IS color){       //getting the color of the specified color's opponent
        if (color == IS.W){
            return IS.B;
        }else{
            return IS.W;
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
		boardCpy = new int[dim][dim];                 //Create a copy of the Go board to mark empty intersections
		ter_B = 0;
	    ter_W = 0;
	    currentRegion = 0;						  //Number of current region (1-81)
	    for (int i=0; i < dim; i++){     
	        for (int j=0; j < dim; j++){           //Make sure that every intersection on the board has been tested
	            
	            blackRegion = false;               //Reset region marker
	            whiteRegion = false;               //Reset region marker
	            currentRegion++;				   //Mark intersections of an empty region in isEmptyRegion() as a specific region
	            markRegion(i, j);
	            
	            int cnt = 0;
	            for (int[] ia : boardCpy){
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
	 * @param y
	 * @param x
	 * @return true if ???
	 */
	public boolean markRegion(int y, int x){        
		if (y < 0 || y >= dim || x < 0 || x >= dim){
	        return false;	                        //Reached border of game table
		}else if ( boardCpy[y][x] > 0 ){
	    	return false;        					//Found already marked field ( >0 = a region, -1 = black stone, -2 = white stone)
		}else if ( boardCpy[y][x] == -1 ){
			blackRegion = true;
			return false;
		}else if ( boardCpy[y][x] == -2 ){
			whiteRegion = true;
			return false;
	    }else if ( board[y][x].equals(IS.B) ){    
	        blackRegion = true;
	        boardCpy[y][x] = -1;
	        return false;                          //Found black field -> border of empty region!
	    }else if ( board[y][x].equals(IS.W) ){
            whiteRegion = true;
            boardCpy[y][x] = -2;
            return false;                          //Found white field -> border of empty region!
	    }else{
	        
	        boardCpy[y][x] = currentRegion;        //Mark the empty intersection as part of the current region
	        
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
	 * @param i
	 * @param j
	 * @return true if the move is no suicide
	 */
	//TODO Change parameter names of this and similar methods from i, j to y, x
    public boolean isNoSuicide(int i, int j) {
        int [][] boardCpy = new int[dim][dim];										//Make new board copy
        if (hasRegionFreedom(i, j, i, j, getCurrentPlayer(), boardCpy)) {				//If the stone's region has a freedom, it isn't suicide
        	return true;
        } else {																//Is it really suicide?
        																		//New board matrix for marking intersections from the view of the
        	int [][] tmp = new int[dim][dim];										//current player's opponent 
        																		//Mark the current intersection with the color of
        	tmp[i][j] = 1;														//the stone that shall be put 
        	for (int x=0; x < dim; x++){
        		for (int y=0; y < dim; y++){
        			if (boardCpy[x][y] == 1){									//If it's an adjacent opponent stone
        				if (!hasRegionFreedom(x, y, x, y, getOpponent(getCurrentPlayer()), tmp)){	//If a surrounding opponent region has no freedom,
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
    
    
	
    /**
     * TODO Complete description
	 * <br><br>
	 * Returns true if the region of stone {@code [sx][sy]} of player {@code [color]} has a freedom
	 * <p> ??? 
     * @param sx x-coordinate of the starting intersection
     * @param sy y-coordinate of the starting intersection
     * @param xNow
     * @param yNow
     * @param color
     * @param boardCpy
     * @return
     */
    //TODO Change parameter order from x,y to y,x so that it matches the for loops that are used when iterating over the board
	public boolean hasRegionFreedom(int sx, int sy, int xNow, int yNow, IS color, int[][] boardCpy){
	    if (xNow < 0 || xNow>= dim || yNow < 0 || yNow >= dim){
	        return false;                                //reached border of game table
	    }else if (boardCpy[xNow][yNow] == 1 || boardCpy[xNow][yNow] == 2) {
	    	return false;                                // already been here
	    }else if (board[xNow][yNow].equals(getOpponent(color))){
	    	boardCpy[xNow][yNow] = 1;					 //found adjacent stone of opponent color
	    	return false;
	    }else if (board[xNow][yNow].equals(IS.E) && (sx != xNow || sy != yNow)){
	        return true;                                 //found a freedom which is not the starting intersection
        }else{

            boardCpy[xNow][yNow] = 2;					 //found adjacent stone of this color
            if (    (hasRegionFreedom(sx, sy, xNow, yNow-1, color, boardCpy))     // look west, north, east, south
                 || (hasRegionFreedom(sx, sy, xNow-1, yNow, color, boardCpy))
                 || (hasRegionFreedom(sx, sy, xNow, yNow+1, color, boardCpy)) 
                 || (hasRegionFreedom(sx, sy, xNow+1, yNow, color, boardCpy))) {

				return true;
            }// if
			return false;           
        }//if
    }//hasRegionFreedom
	
	
	/**
	 * TODO Complete description
	 * <br><br>
	 * Executes the move and tests for ko.
	 * Returns the field in its state after the preceding move 
	 * @param y
	 * @param x
	 */
	public void processMove(int y, int x) {
		//Save state of prisoners for eventual undoing
		this.pris_W_b4 = this.pris_W;
		this.pris_B_b4 = this.pris_B;
        
		if (!areBoardsEqual(board_b4, board)){
			cpyBoard(board_b4, tmp_4_ko);								//Save a copy of board_b4 for testing on illegal move in "ko"-situation
		}
		
        cpyBoard(board, board_b4);									//Save the board state so that it can be undone later
        board[y][x] = getCurrentPlayer();										//Put player's stone on empty intersection
        int [][] lookField = new int [dim][dim];
        lookAround(y, x, y, x, getCurrentPlayer(), lookField);					//Search for opponent regions to be removed							
        gamecnt++;														//Game counter is set to next player's turn
    }// processMove
    
    
    
    /**
     * TODO Complete description
     * <br><br>
     * Returns true if ???
     * <p> The current player's stone "looks around".
     * If it finds an opponent stone, the latter marks its region and finds out whether it has a freedom.
     * If its region has no freedom, the region is removed.
     * @param sx
     * @param sy
     * @param xNow
     * @param yNow
     * @param color
     * @param lookBoard
     * @return true if ???
     */
    public boolean lookAround(int sx, int sy, int xNow, int yNow, IS color, int[][] lookBoard){ //"looking board" or what?
	    if (xNow < 0 || xNow>= dim || yNow < 0 || yNow >= dim){
	        return false;
	    }else if (lookBoard[xNow][yNow] == 1 || lookBoard[xNow][yNow] == 2) {
	    	return false;                                //Already been here
	    }else if (board[xNow][yNow].equals(IS.E)){
	    	return false;
	    }else if (board[xNow][yNow].equals(getOpponent(color))){				//Found adjacent stone of opponent color
	     	int [][] tmp = new int[9][9];										//New field copy to process the move
	     	tmp[sx][sy] = 1;
	    	if (!hasRegionFreedom(xNow, yNow, xNow, yNow, getOpponent(color), tmp)){	//The opponent region has no freedom and is removed
                for (int x = 0; x < dim; x++) {
                    for (int y = 0; y < dim; y++) {
                        if (tmp[x][y] == 2) {
                            board[x][y] = IS.E;
                            tmp[x][y] = 0;               
							if (gamecnt % 2 == 0) { 							//White's move
								pris_W++; 										//White captures the removed black stone
							} else {											//Black's move
								pris_B++; 										//Black captures the removed white stone
							}// if                               
                        }//if
                    }//for
                }//for
	    	}else{														//The opponent region has a freedom and is unmarked														
                for (int x = 0; x < dim; x++) {				//so that a region of this color can "move through" these intersections
                	for (int y = 0; y < dim; y++) {
                		if (tmp[x][y] == 2) {
                			tmp[x][y] = 0;
                		}
                	}
                }
	    	}
	    	return false;
		}else if (board[xNow][yNow].equals(color)) {
			lookBoard[xNow][yNow] = 1; 											//Found adjacent stone of this color
			
            if (    (lookAround(sx, sy, xNow, yNow-1, color, lookBoard))     						//Look west, north, east, south
                 || (lookAround(sx, sy, xNow-1, yNow, color, lookBoard))
                 || (lookAround(sx, sy, xNow, yNow+1, color, lookBoard)) 
                 || (lookAround(sx, sy, xNow+1, yNow, color, lookBoard))) {

   				return true;
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

			if (getCurrentPlayer().equals(IS.B)){			//Depending on the player whose turn it was, his latest prisoners are undone
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
    }//areEqualFields
    
    
    //TODO Passing: Display waiting dialog after passing (instead of "Bitte warten Sie bis Sie an der Reihe sind")
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
     * @return true if the draw that is being processed is a double pass
     */
    public boolean isDoublePass(){
    	int pris_tmp;
    	int pris_tmp_b4;
    	
    	if (getCurrentPlayer().equals(IS.B)){
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
     * @param i 
     * @param j
     * @return true if the player is trying to put his stone on an empty intersection
     */
	public boolean isEmptyIntersection(int i, int j){
	    if (board[i][j] == IS.E) {    
	        return true;
	    }else{
	        return false;
	    }
	}//isEmptyIntersection
	
	
	/**
	 * TODO Complete description
	 * <br><br>
	 * Copy the whole board while either processing or undoing a move. Purpose ??? 
	 * @param src
	 * @param dest
	 */
	public void cpyBoard(IS[][] src, IS[][] dest){
        for (int i=0; i < dim; i++){
            for (int j=0; j < dim; j++){
                dest[i][j] = src[i][j];
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
	

}//Model