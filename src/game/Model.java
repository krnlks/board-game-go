package game;

import java.io.IOException;
import java.util.Observable;

import multiplayer.Client;
import multiplayer.LAN_Conn;
import multiplayer.Server;
import multiplayer.UpdateMessages;

/* Verbesserungsideen:
 * - isNoSuicide (schleifen)
 * - Globale variablen
 * - markRegion() gibt wahr/falsch zurück -> tut nicht das, was man erwarten würde
 * - Der rückgabewert von markRegion wird nie verwendet -> legal, diesen für rekursion zu "missbrauchen"?
 * - Rekursionen können bestimmt noch zusammengefasst werden
 */


public class Model extends Observable{
    
    private LAN_Conn lan;       //Server or client
    private Player player;         //Am I black or white?
    private int ter_B;                    //Territory occupied by Black
    private int ter_W;                    //Territory occupied by White
    private int pris_B = 0;                    //Conquered opponent stones of Black
    private int pris_W = 0;                    //Conquered opponent stones of White
    private int pris_B_b4 = pris_B;
    private int pris_W_b4 = pris_W;
    
	private int gamecnt;
	private IS[][] fields;					//The state of the field
	private IS[][] fields_b4;			 	//The state preceding state of the field 
	private IS[][] tmp_4_ko;				//Saves the state of fields_b4 while testing on illegal move in "ko"-situation
	int[][] fieldCpy;
	boolean blackRegion;
	boolean whiteRegion;
	int currentRegion = 0;							  //How many different regions exist

	
	public Model(){
		gamecnt = 1;
		fields = new IS[9][9];		//Create game table with empty intersections
		fields_b4 = new IS[9][9];	//Copy of the game table for undoing moves
		tmp_4_ko = new IS[9][9];	//New KO-test field
		
		for (int y=0; y<=8; y++){
			for (int x=0; x<=8; x++){
				fields[y][x] = IS.E;
				fields_b4[y][x] = IS.E;
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
                int y = recv / 9;
                int x = recv - 9*y;
                this.processMove(y, x);
            }
            setChanged();
            notifyObservers(UpdateMessages.OPPONENT);
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
	
    public IS getIntersection(int i, int j) {
        return fields[i][j];
    }// getIntersection
    
    public IS getCpyIntersection(int i, int j){
        return fields_b4[i][j];
    }//getCpyIntersection
    
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

    public IS[][] getFields() {
        return fields;
    }//getFields

    public IS[][] getFields_b4() {
        return fields_b4;
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
	 * Get both players' territory on the game table
     */
	public void getTerritory(){
		fieldCpy = new int[9][9];                 //Create a copy of the go table to mark empty intersections
		ter_B = 0;
	    ter_W = 0;
	    currentRegion = 0;						  //Number of current region (1-81)
	    for (int i=0; i < fields.length; i++){     
	        for (int j=0; j < fields.length; j++){ //Make sure that every intersection on the table has been tested
	            
	            blackRegion = false;               //Reset region marker
	            whiteRegion = false;               //Reset region marker
	            currentRegion++;				   //Mark intersections of an empty region in isEmptyRegion() as a specific region
	            markRegion(i, j);
	            
	            int cnt = 0;
	            for (int[] ia : fieldCpy){
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
	 * @param i
	 * @param j
	 * @return true if ???
	 */
	public boolean markRegion(int i, int j){        
		if (i < 0 || i >= fields.length || j < 0 || j >= fields.length){
	        return false;	                        //Reached border of game table
		}else if ( fieldCpy[i][j] > 0 ){
	    	return false;        					//Found already marked field ( >0 = a region, -1 = black stone, -2 = white stone)
		}else if ( fieldCpy[i][j] == -1 ){
			blackRegion = true;
			return false;
		}else if ( fieldCpy[i][j] == -2 ){
			whiteRegion = true;
			return false;
	    }else if ( fields[i][j].equals(IS.B) ){    
	        blackRegion = true;
	        fieldCpy[i][j] = -1;
	        return false;                          //Found black field -> border of empty region!
	    }else if ( fields[i][j].equals(IS.W) ){
            whiteRegion = true;
            fieldCpy[i][j] = -2;
            return false;                          //Found white field -> border of empty region!
	    }else{
	        
	        fieldCpy[i][j] = currentRegion;        //Mark the empty intersection as part of the current region
	        
	        if (   ( markRegion(i, j-1) )           //Look west
	             ||( markRegion(i-1, j) )           //north
	             ||( markRegion(i, j+1) )           //east
	             ||( markRegion(i+1, j) ) ) {       //south
	            
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
    public boolean isNoSuicide(int i, int j) {
        int [][] fieldCpy = new int[9][9];										//Make new field copy
        if (hasRegionFreedom(i, j, i, j, getCurrentPlayer(), fieldCpy)) {				//If the stone's region has a freedom, it isn't suicide
        	return true;
        } else {																//Is it really suicide?
        																		//New matrix for marking fields from the view of the
        	int [][] tmp = new int[9][9];										//Current player's opponent 
        																		//Mark the position of the current field with the color of
        	tmp[i][j] = 1;														//The stone that shall be put 
        	for (int x=0; x < fields.length; x++){
        		for (int y=0; y < fields.length; y++){
        			if (fieldCpy[x][y] == 1){									//If it's an adjacent opponent stone
        				if (!hasRegionFreedom(x, y, x, y, getOpponent(getCurrentPlayer()), tmp)){	//If a surrounding opponent region has no freedom,
        					return true;													//it's no suicide
        				}
        			}
                	for (int k=0; k < fields.length; k++){
                		for (int l=0; l < fields.length; l++){
                			if (tmp[k][l] == 2){	//If the field has already been marked:
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
     * @param sx
     * @param sy
     * @param xNow
     * @param yNow
     * @param color
     * @param fieldCpy
     * @return
     */
	public boolean hasRegionFreedom(int sx, int sy, int xNow, int yNow, IS color, int[][] fieldCpy){
	    if (xNow < 0 || xNow>= fields.length || yNow < 0 || yNow >= fields.length){
	        return false;                                //reached border of game table
	    }else if (fieldCpy[xNow][yNow] == 1 || fieldCpy[xNow][yNow] == 2) {
	    	return false;                                // already been here
	    }else if (fields[xNow][yNow].equals(getOpponent(color))){
	    	fieldCpy[xNow][yNow] = 1;					 //found adjacent stone of opponent color
	    	return false;
	    }else if (fields[xNow][yNow].equals(IS.E) && (sx != xNow || sy != yNow)){
	        return true;                                 //found a freedom which is not the starting intersection
        }else{

            fieldCpy[xNow][yNow] = 2;					 //found adjacent stone of this color
            if (    (hasRegionFreedom(sx, sy, xNow, yNow-1, color, fieldCpy))     // look west, north, east, south
                 || (hasRegionFreedom(sx, sy, xNow-1, yNow, color, fieldCpy))
                 || (hasRegionFreedom(sx, sy, xNow, yNow+1, color, fieldCpy)) 
                 || (hasRegionFreedom(sx, sy, xNow+1, yNow, color, fieldCpy))) {

				return true;
            }// if
			return false;           
        }//if
    }//hasRegionFreedom
	
	
	/**
	 * TODO Complete description
	 * <br><br>
	 * Executes the move and tests for KO.
	 * Returns the field in its state after the preceding move 
	 * @param y
	 * @param x
	 */
	public void processMove(int y, int x) {
		//Save state of prisoners for eventual undoing
		this.pris_W_b4 = this.pris_W;
		this.pris_B_b4 = this.pris_B;
        
		if (!areEqualFields(fields_b4, fields)){
			cpyField(fields_b4, tmp_4_ko);								//Save of copy of fields_b4 for testing on illegal move in "ko"-situation
		}
		
        cpyField(fields, fields_b4);									//Save state of the fields for eventual undoing
        fields[y][x] = getCurrentPlayer();										//Put player's stone on empty intersection
        int [][] lookField = new int [9][9];
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
     * @param lookField
     * @return true if ???
     */
    public boolean lookAround(int sx, int sy, int xNow, int yNow, IS color, int[][] lookField){
	    if (xNow < 0 || xNow>= fields.length || yNow < 0 || yNow >= fields.length){
	        return false;
	    }else if (lookField[xNow][yNow] == 1 || lookField[xNow][yNow] == 2) {
	    	return false;                                //Already been here
	    }else if (fields[xNow][yNow].equals(IS.E)){
	    	return false;
	    }else if (fields[xNow][yNow].equals(getOpponent(color))){				//Found adjacent stone of opponent color
	     	int [][] tmp = new int[9][9];										//New field copy to process the move
	     	tmp[sx][sy] = 1;
	    	if (!hasRegionFreedom(xNow, yNow, xNow, yNow, getOpponent(color), tmp)){	//The opponent region has no freedom and is removed
                for (int x = 0; x < fields.length; x++) {
                    for (int y = 0; y < fields.length; y++) {
                        if (tmp[x][y] == 2) {
                            fields[x][y] = IS.E;
                            tmp[x][y] = 0;               
							if (gamecnt % 2 == 0) { 							//White's move
								pris_W++; 										//White captures the removed black stone
							} else {											//Black's move
								pris_B++; 										//Black captures the removed white stone
							}// if                               
                        }//if
                    }//for
                }//for
	    	}else{														//the opponent region has a freedom and is unmarked														
                for (int x = 0; x < fields.length; x++) {				//so that a region of this color can "move through" these intersections
                	for (int y = 0; y < fields.length; y++) {
                		if (tmp[x][y] == 2) {
                			tmp[x][y] = 0;
                		}
                	}
                }
	    	}
	    	return false;
		}else if (fields[xNow][yNow].equals(color)) {
			lookField[xNow][yNow] = 1; 											//Found adjacent stone of this color
			
            if (    (lookAround(sx, sy, xNow, yNow-1, color, lookField))     						//Look west, north, east, south
                 || (lookAround(sx, sy, xNow-1, yNow, color, lookField))
                 || (lookAround(sx, sy, xNow, yNow+1, color, lookField)) 
                 || (lookAround(sx, sy, xNow+1, yNow, color, lookField))) {

   				return true;
            }// if
            return false;
		}//if
	    return false;
    }//lookAround

	
    /**
     * TODO Complete description
     * <br><br>
     */
    public void undoMove(){
		if (gamecnt > 1 ){							//If it's the first move, there's nothing to be undone
		    
			cpyField(fields_b4, fields);			//Copy array fields before -> fields				

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
     * Returns true if two {@code IS[][]} fields have identical values.
	 * 
	 * <p> Used to assure that the undo button is hit only once in a row.
	 * If {@code fields} and {@code fields_b4} are already equal, it's not allowed to undo your move.
	 * Used to find out whether it is a KO-situation.
	 * If {@code fields} and {@code tmp_4_ko} are equal, the move reproduces the preceding state of the field and therefore isn't allowed.
     * @param one one {@code IS[][]} field
     * @param other the other
     * @return true if two {@code IS[][]} fields have identical values. 
     */
    public boolean areEqualFields(IS[][] one, IS[][] other){
        for (int i=0; i < one.length; i++){
            for (int j=0; j < one.length; j++){
                if ( !one[i][j].equals(other[i][j]) ){
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
    	cpyField(fields, fields_b4);
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
	    if (fields[i][j] == IS.E) {    
	        return true;
	    }else{
	        return false;
	    }
	}//isEmptyIntersection
	
	
	/**
	 * TODO Complete description
	 * <br><br>
	 * Copy the whole field while either processing or undoing a move. Purpose ??? 
	 * @param src
	 * @param dest
	 */
	public void cpyField(IS[][] src, IS[][] dest){
        for (int i=0; i < src.length; i++){
            for (int j=0; j < src.length; j++){
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