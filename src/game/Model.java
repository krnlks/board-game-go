package game;

import java.io.IOException;
import java.util.Observable;

import multiplayer.Client;
import multiplayer.LAN_Conn;
import multiplayer.Server;
import multiplayer.UpdateMessages;

/* Verbesserungsideen:
 * - isNoSuicide (schleifen)
 * - globale variablen
 * - markRegion() gibt wahr/falsch zurück -> tut nicht das, was man erwarten würde
 * - der rückgabewert von markregion wird nie verwendet -> legal, diesen für rekursion zu "missbrauchen"?
 * - rekursionen können bestimmt noch zusammengefasst werden
 */


public class Model extends Observable{
    
    private LAN_Conn lan;       //Server or client
    private Player player;         //am I black or white?
    private int ter_B;                    //territory occupied by Black
    private int ter_W;                    //territory occupied by White
    private int pris_B = 0;                    //conquered opponent stones of Black
    private int pris_W = 0;                    //conquered opponent stones of White
    private int pris_B_b4 = pris_B;
    private int pris_W_b4 = pris_W;
    
	private int gamecnt;
	private IS[][] fields;					//the state of the field
	private IS[][] fields_b4;			 	//the state preceding state of the field 
	private IS[][] tmp_4_ko;				//saves the state of fields_b4 while testing on illegal move in "ko"-situation
	int[][] fieldCpy;
	boolean blackRegion;
	boolean whiteRegion;
	int currentRegion = 0;							  //how many different regions exist

	
	public Model(){
		gamecnt = 1;
		fields = new IS[9][9];		//create game table with empty intersections
		fields_b4 = new IS[9][9];	//copy of the game table for undoing moves
		tmp_4_ko = new IS[9][9];	//new "ko"-test field
		
		for (int y=0; y<=8; y++){
			for (int x=0; x<=8; x++){
				fields[y][x] = IS.E;
				fields_b4[y][x] = IS.E;
			}
		}
	}
	
	public void send(int b){
	    try {
	        System.out.println("Model: Sende Zug...");
            lan.send(b); 
            System.out.println("Model: Zug gesendet");
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	}
	
	public void receive(){
	    try {
	        System.out.println("Model: call to receive");
            int recv = lan.receive();
            System.out.println("Model: return from receive");
            if (recv == -1){
                //TODO gegn. Pass lokal ausführen
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
	}
	
    /**
     * Starts the {@link LAN_Conn} and thereby establishes the LAN connection 
     * @return 0 in case of success; -1 if an IOException has occurred
     * and re-initialization is necessary
     */
    public int estbl_LanComm() {
        try{
            this.lan.init();
            this.lan.start();
            return 0;
        }catch (Exception e) {
            return -1;
        }
    }   
	
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
	}
	
	public boolean isMyTurn(){
	    if ( player.equals(Player.BLACK) && getCurrentPlayer().equals(IS.B)
          || player.equals(Player.WHITE) && getCurrentPlayer().equals(IS.W)){
	        return true;
	    }else{
	        return false;
	    }
	}
	
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
    
    private IS getCurrentPlayer(){       //getting the color of the player whose turn it is
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

    
    //method to get both players' territory on the game table
	public void getTerritory(){
		fieldCpy = new int[9][9];                 //create a copy of the go table to mark empty intersections
		ter_B = 0;
	    ter_W = 0;
	    currentRegion = 0;						  //number of current region (1-81)
	    for (int i=0; i < fields.length; i++){     
	        for (int j=0; j < fields.length; j++){ //make sure that every intersection on the table has been tested
	            
	            blackRegion = false;               //reset region marker
	            whiteRegion = false;               //reset region marker
	            currentRegion++;				   //mark intersections of an empty region in isEmptyRegion() as a specific region
	            markRegion(i, j);
	            
	            int cnt = 0;
	            for (int[] ia : fieldCpy){
	                for (int val : ia){
	                    if (val == currentRegion){
	                        cnt++;
	                    }//if
	                }//for
	            }//for
	            if (blackRegion && !whiteRegion){      	 //test whether the region marked before is Black's territory
	                ter_B += cnt;
	            }else if (whiteRegion && !blackRegion){      //test whether the region marked before is White's territory
	                ter_W += cnt;            	
	            }//if
	        }//for
	    }//for
	}//getTerritory

	//method to mark empty regions and find out whether they are a region of Black or White
	public boolean markRegion(int i, int j){        
		
		if (i < 0 || i >= fields.length || j < 0 || j >= fields.length){
	        return false;	                        //reached border of game table
		}else if ( fieldCpy[i][j] > 0 ){
	    	return false;        					//found already marked field ( >0 = a region, -1 = black stone, -2 = white stone)
		}else if ( fieldCpy[i][j] == -1 ){
			blackRegion = true;
			return false;
		}else if ( fieldCpy[i][j] == -2 ){
			whiteRegion = true;
			return false;
	    }else if ( fields[i][j].equals(IS.B) ){    
	        blackRegion = true;
	        fieldCpy[i][j] = -1;
	        return false;                          //found black field -> border of empty region!
	    }else if ( fields[i][j].equals(IS.W) ){
            whiteRegion = true;
            fieldCpy[i][j] = -2;
            return false;                          //found white field -> border of empty region!
	    }else{
	        
	        fieldCpy[i][j] = currentRegion;        //mark the empty intersection as part of the current region
	        
	        if (   ( markRegion(i, j-1) )           //look west
	             ||( markRegion(i-1, j) )           //north
	             ||( markRegion(i, j+1) )           //east
	             ||( markRegion(i+1, j) ) ) {       //south
	            
	            return true;   
	        }//if     
	        return false;
	    }//if
	}//isEmptyRegion

	
	
	//is the move no suicide?
    public boolean isNoSuicide(int i, int j) {
        int [][] fieldCpy = new int[9][9];										//make new field copy
        if (hasRegionFreedom(i, j, i, j, getCurrentPlayer(), fieldCpy)) {				//if the stone's region has a freedom, it isn't suicide
        	return true;
        } else {																//is it really suicide?
        																		//new matrix for marking fields from the view of the
        	int [][] tmp = new int[9][9];										//current player's opponent 
        																		//mark the position of the current field with the color of
        	tmp[i][j] = 1;														//the stone that shall be put 
        	for (int x=0; x < fields.length; x++){
        		for (int y=0; y < fields.length; y++){
        			if (fieldCpy[x][y] == 1){									//if it's an adjacent opponent stone
        				if (!hasRegionFreedom(x, y, x, y, getOpponent(getCurrentPlayer()), tmp)){	//if a surrounding opponent region has no freedom
        					return true;													//it's no suicide
        				}
        			}
                	for (int k=0; k < fields.length; k++){
                		for (int l=0; l < fields.length; l++){
                			if (tmp[k][l] == 2){	//if the field has already been marked:
                				tmp[k][l] = 0;		//clear it for hasRegionFreedom
                			}
                		}
                	}
        		}
        	}
        	return false;
        }// if
    }// isNoSuicide
    
    
	
	//basically: "has the region of the stone [sx][sy] of the player [color] a freedom?"
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
	
	//the method for test on "ko"
	//and for actually executing the move
	//returns the field in its state after the preceding move
    public void processMove(int y, int x) {
    																	//save state of prisoners for eventual undoing
    
		this.pris_W_b4 = this.pris_W;
		this.pris_B_b4 = this.pris_B;
        
		if (!areEqualFields(fields_b4, fields)){
			cpyField(fields_b4, tmp_4_ko);								//save of copy of fields_b4 for testing on illegal move in "ko"-situation
		}
		
        cpyField(fields, fields_b4);									//save state of the fields for eventual undoing
        fields[y][x] = getCurrentPlayer();										//put player's stone on empty intersection
        int [][] lookField = new int [9][9];
        lookAround(y, x, y, x, getCurrentPlayer(), lookField);					//search for opponent regions to be removed							
        gamecnt++;														//game counter is set to next player's turn
    }// processMove
    
    
    
    //the current player's stone "looks around"
    //if he finds an opponent stone, the opponent stone marks its region and finds out whether it has a freedom
    //if its region has no freedom, the region is removed
    public boolean lookAround(int sx, int sy, int xNow, int yNow, IS color, int[][] lookField){
	    if (xNow < 0 || xNow>= fields.length || yNow < 0 || yNow >= fields.length){
	        return false;
	    }else if (lookField[xNow][yNow] == 1 || lookField[xNow][yNow] == 2) {
	    	return false;                                // already been here
	    }else if (fields[xNow][yNow].equals(IS.E)){
	    	return false;
	    }else if (fields[xNow][yNow].equals(getOpponent(color))){				//found adjacent stone of opponent color
	     	int [][] tmp = new int[9][9];										//new field copy to process the move
	     	tmp[sx][sy] = 1;
	    	if (!hasRegionFreedom(xNow, yNow, xNow, yNow, getOpponent(color), tmp)){	//the opponent region has no freedom and is removed
                for (int x = 0; x < fields.length; x++) {
                    for (int y = 0; y < fields.length; y++) {
                        if (tmp[x][y] == 2) {
                            fields[x][y] = IS.E;
                            tmp[x][y] = 0;               
							if (gamecnt % 2 == 0) { 							// White's move
								pris_W++; 										// White captures the removed black stone
							} else {											// Black's move
								pris_B++; 										// Black captures the removed white stone
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
			lookField[xNow][yNow] = 1; 											// found adjacent stone of this color
			
            if (    (lookAround(sx, sy, xNow, yNow-1, color, lookField))     						// look west, north, east, south
                 || (lookAround(sx, sy, xNow-1, yNow, color, lookField))
                 || (lookAround(sx, sy, xNow, yNow+1, color, lookField)) 
                 || (lookAround(sx, sy, xNow+1, yNow, color, lookField))) {

   				return true;
            }// if
            return false;
		}//if
	    return false;
    }//lookAround

	
	
	public void undoMove(){
		if (gamecnt > 1 ){							//if it's the first move, there's nothing to be undone
		    
			cpyField(fields_b4, fields);			//copy array fields before -> fields				

			if (getCurrentPlayer().equals(IS.B)){			//depending on the player whose turn it was, his latest prisoners are undone
				this.pris_W = this.pris_W_b4;							
			}else{
				this.pris_B = this.pris_B_b4;				
			}
			gamecnt--;								//set game count to last turn
		}
	}//undoMove
	

    //method to find out whether to IS[][] fields have identical values
	//used to assure that the undo button is hit only once in a row
		//if fields and fields_b4 are already equal, it's not allowed to undo your move
	//used to find out whether it is a "ko"-situation
		//if fields and tmp_4_ko are equal, the move reproduces the preceding state of the field and therefore isn't allowed
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
    
    
	
    //method to pass a move
    public void pass() {
    	cpyField(fields, fields_b4);
        if (this.gamecnt % 2 == 0) { // white passes
            this.pris_B_b4 = this.pris_B;
            this.pris_B++; // and black conquers a white stone

        } else { // black passes
            this.pris_W_b4 = this.pris_W;
            this.pris_W++; // and white conquers a black stone
        }// if
        this.gamecnt++;
    }// pass
    
    
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
    
    
    
	//method to find out whether the player is trying to put his stone on an empty intersection
	public boolean isEmptyIntersection(int i, int j){
	    if (fields[i][j] == IS.E) {    
	        return true;
	    }else{
	        return false;
	    }
	}//isEmptyIntersection
	
	
    //method for copying the whole field while either processing or undoing a move 
    public void cpyField(IS[][] src, IS[][] dest){
        for (int i=0; i < src.length; i++){
            for (int j=0; j < src.length; j++){
                dest[i][j] = src[i][j];
            }            
        }
    }//cpyField

    public void setChanged1() {
        setChanged();
    }
    
    public void notifyObservers2(Object updateMessage){
        notifyObservers(updateMessage);
    }
	

}//Model
