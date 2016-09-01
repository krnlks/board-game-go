package game;

//TODO This class is used both by Model and View. However, orient values (which were introduced later) are just only by View although they have implications for the Model, too, such as intersections with an orient other than CE are located at the board's border
/**
 * An intersection that is empty by default. It can also have a white or a black stone on it.
 * A newly created IS also stores its type, i.e. whether it's in the center, on the edge,
 * or in one of the corners of the board.
 * 
 * @author Lukas Kern
 */
public class IS {
    /**
     * The intersection's orientation
     * (center, top, left, right, bottom, top left, top right, bottom left, bottom right).
     * Through this one also knows whether it's a center, edge or corner intersection
     */
    public enum Orient {
        C, T, L, R, B, TL, TR, BL, BR
    }
    /**
     * The intersection's state (empty, black, or white)
     */
    public enum State {E, B, W};

    private Orient orient;
    private State state;
    
    IS(Orient orient){
        this.orient= orient;
        state = State.E;
    }//IS constructor

    public Orient getOrient() {
        return orient;
    }

    public State getState() {
        return state;
    }
    
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "[" + orient + ", " + state + "]";
    }
    
    
}//enum IS
