package game;

/**
 * Constants for the game
 */
public class Constants {
    /**
     * The dimension of the board. Standard: 19x19, beginner: 9x9 or 13x13.
     * <br> TODO: Problem arising for 19x19 boards: socket.outputstream.write takes an int but really it's just a byte 
     */
    public static final int BOARD_DIM = 9;
    public static final int SEND_PASS = 253; //large enough to safely fit 19x19 board positions in recv (see Model.receive)
    public static final int SEND_DOUBLEPASS = 254;
    public static final int SEND_QUIT = 255;
}
