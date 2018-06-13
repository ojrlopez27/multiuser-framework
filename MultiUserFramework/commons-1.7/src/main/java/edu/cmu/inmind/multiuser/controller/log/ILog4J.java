package edu.cmu.inmind.multiuser.controller.log;


/**
 * Created by oscarr on 6/6/18.
 */
public interface ILog4J {
    String getSessionIdI(Object caller);
    String getSessionAndMsgI(String sessionId, String message);
    void infoI(Object caller, String message);
    void traceI(Object caller, String message);
    void debugI(Object caller, String message);
    void errorI(Object caller, String message);
    void warnI(Object caller, String message);
    void trackI(Object caller, String message);
    void infoI(Object caller, String sessionId, String message);
    void traceI(Object caller, String sessionId, String message);
    void debugI(Object caller, String sessionId, String message);
    void errorI(Object caller, String sessionId, String message);
    void warnI(Object caller, String sessionId, String message);
    void trackI(Object caller, String sessionId, String message);
    void turnOnI(boolean shouldTurnOn);
    boolean isTurnedOn();
}
