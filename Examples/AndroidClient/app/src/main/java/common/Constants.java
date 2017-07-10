package common;

/**
 * Created by oscarr on 3/3/17.
 */
public class Constants {
    public static final String REQUEST_CONNECT = "REQUEST_CONNECT";
    public static final String REQUEST_DISCONNECT = "REQUEST_DISCONNECT";
    public static final String RESPONSE_ALREADY_CONNECTED = "RESPONSE_ALREADY_CONNECTED";
    public static final String RESPONSE_UNKNOWN_SESSION = "RESPONSE_UNKNOWN_SESSION";
    public static final String RESPONSE_NOT_VALID_OPERATION = "RESPONSE_NOT_VALID_OPERATION";

    //lifecycle
    public static final String REQUEST_PAUSE = "REQUEST_PAUSE";
    public static final String REQUEST_RESUME = "REQUEST_RESUME";
    public static final String SHUTDOWN = "shutdown";

    //communication
    public static final int globalPort = 5555;
    public static final String ADDRESS = "tcp://127.0.0.1"; //tcp://*"; //"tcp://192.168.0.105"


    //session
    public static final String SESSION_INITIATED = "SESSION_INITIATED";
    public static final String SESSION_CLOSED = "SESSION_CLOSED";
    public static final String SESSION_PAUSED = "SESSION_PAUSED";
    public static final String SESSION_RESUMED = "SESSION_RESUMED";

}
