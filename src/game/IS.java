package game;

//TODO This class is used both by Model and View. However, orient values (which were introduced later) are just only by View although they have implications for the Model, too, such as intersections with an orient other than CE are located at the board's border
/**
 * The Go board consists of {@link Constants#BOARD_DIM} * {@link Constants#BOARD_DIM} intersections.
 * An intersection is empty by default. It can also have a white or a black stone on it.
 * A newly created IS also stores its type.
 * 
 * @author Lukas Kern
 */
public class IS {
    /**
     * The intersection's type:
     * <br> center,
     * <br> edge: top, left, right, bottom,
     * <br> corner: top left, top right, bottom left, bottom right
     */
    public enum Type {
        /** Center */
        C,
        /** Bottom left corner */
        CRN_BL, 
        /** Bottom right corner */
        CRN_BR,
        /** Top left corner */
        CRN_TL,
        /** Top right corner */
        CRN_TR,
        /** Bottom edge*/
        E_B,
        /** Left edge*/
        E_L,
        /** Right edge*/
        E_R,
        /** Top edge*/
        E_T
    }

    /** The intersection's state (empty, black, or white) */
    public enum State {
        /** Empty */
        E,
        /** Black */
        B,
        /** White */
        W
    }
    
    private Type type;
    private State state;
    //TODO Obv this belongs in class Stone
    //TODO Which is better: attribute if every intersection (like this) or a "global" variable that holds the intersection (better: the stone) that was put last
    /** Was the last stone that was played put on this intersection? */
    private boolean wasPutLast;
    
    IS(Type type){
        this.type= type;
        state = State.E;
        wasPutLast = false;
    }//IS constructor

    public Type getType() {
        return type;
    }

    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
        if (state.equals(State.B) || state.equals(State.W))
            wasPutLast = true;
    }
    
    /** Sets this intersection to empty */ 
    public void setEmpty(){
    	state = State.E;
    }
    
    /** Was the last stone that was played put on this intersection? */
    public boolean wasPutLast(){
        return wasPutLast;
    }
    
    public void wasNotPutLast(){
        wasPutLast = false;
    }

    public String toString(boolean orientation) {
        String result = (state == State.E) ? " " : state.toString();
        if (orientation){
            result = type + ", " + result; 
        }
        char bracketOpen;
        char bracketClose;
        if (wasPutLast){
            bracketOpen = '(';
            bracketClose = ')';
        }else {
            bracketOpen = '[';
            bracketClose = ']';
        }
        result = bracketOpen + result + bracketClose; 
        return result; 
    }
}
