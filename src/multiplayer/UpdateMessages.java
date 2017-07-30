package multiplayer;

/**
 * Constants that indicate a certain change in the {@link java.util.Observable}, i.e. in the Model
 */
public enum UpdateMessages {
    CLIENT_CONNECTED, RECVD_MOVE, RECVD_PASS, RECVD_DOUBLEPASS, RECVD_QUIT
}
