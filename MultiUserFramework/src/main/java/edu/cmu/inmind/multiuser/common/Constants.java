package edu.cmu.inmind.multiuser.common;

import edu.cmu.inmind.multiuser.controller.resources.Config;

/**
 * Created by oscarr on 3/3/17.
 */
public class Constants {

    public static final String RESPONSE_ALREADY_CONNECTED = "RESPONSE_ALREADY_CONNECTED";
    public static final String RESPONSE_UNKNOWN_SESSION = "RESPONSE_UNKNOWN_SESSION";
    public static final String RESPONSE_NOT_VALID_OPERATION = "RESPONSE_NOT_VALID_OPERATION";
    public static final String REGISTER_REMOTE_SERVICE = "REGISTER_REMOTE_SERVICE";
    public static final String RESPONSE_REMOTE_REGISTERED = "RESPONSE_REMOTE_REGISTERED";
    public static final String UNREGISTER_REMOTE_SERVICE = "UNREGISTER_REMOTE_SERVICE";
    public static final String RESPONSE_REMOTE_UNREGISTERED = "RESPONSE_REMOTE_UNREGISTERED";
    public static final String RESPONSE_PUBLISHED = "RESPONSE_PUBLISHED";

    //lifecycle
    public static final String REQUEST_PAUSE = "REQUEST_PAUSE";
    public static final String REQUEST_RESUME = "REQUEST_RESUME";
    public static final String REQUEST_PUBLISH = "REQUEST_PUBLISH";
    public static final String REQUEST_CONNECT = "REQUEST_CONNECT";
    public static final String REQUEST_DISCONNECT = "REQUEST_DISCONNECT";
    public static final String REQUEST_SHUTDOWN_SYSTEM = "REQUEST_SHUTDOWN_SYSTEM";
    public static final String RESPONSE_SUBSCRIBE = "RESPONSE_SUBSCRIBE";
    public static final boolean VERBOSE = true;

    //communication
    public static final String SESSION_MANAGER_SERVICE = "session-manager";
    public static final String CONNECTION_NEW = "CONNECTION_NEW";
    public static final String CONNECTION_STARTED = "CONNECTION_STARTED";
    public static final String CONNECTION_FINISHED = "CONNECTION_FINISHED";
    public static final String CONNECTION_STOPPED = "CONNECTION_STOPPED";
    public static final int    QUEUE_CAPACITY = 100;


    //session
    public static final String SESSION_INITIATED = "SESSION_INITIATED";
    public static final String SESSION_CLOSED = "SESSION_CLOSED";
    public static final String SESSION_PAUSED = "SESSION_PAUSED";
    public static final String SESSION_RESUMED = "SESSION_RESUMED";
    public static final String SESSION_RECONNECTED = "SESSION_RECONNECTED";

    //blackboard
    public static final String REMOVE_ALL = "REMOVE_ALL";
    public static final String ELEMENT_ADDED = "ELEMENT_ADDED";
    public static final String ELEMENT_REMOVED = "ELEMENT_REMOVED";

    //orchestrator
    public static final String ORCHESTRATOR_STARTED = "ORCHESTRATOR_STARTED";
    public static final String ORCHESTRATOR_PAUSED = "ORCHESTRATOR_PAUSED";
    public static final String ORCHESTRATOR_RESUMED = "ORCHESTRATOR_RESUMED";
    public static final String ORCHESTRATOR_STOPPED = "ORCHESTRATOR_STOPPED";

    //service manager
    public static final String SERVICE_MANAGER_STARTED = "SERVICE_MANAGER_STARTED";
    public static final String SERVICE_MANAGER_STOPPED = "SERVICE_MANAGER_STOPPED";

}
