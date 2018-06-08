package edu.cmu.inmind.multiuser.log;

import edu.cmu.inmind.multiuser.controller.log.ILog4J;

/**
 * Created by oscarr on 6/6/18.
 */
public class LogC implements ILog4J {
    private static ILog4J instance = new LogC();
    protected boolean turnedOn = true;

    public static void setLog(ILog4J log){
        instance = log;
    }

    public static String getSessionId(Object caller) {
        return instance.getSessionIdI(caller);
    }

    public static String getSessionAndMsg(String sessionId, String message) {
        return instance.getSessionAndMsgI(sessionId, message);
    }

    public static void info(Object caller, String message) {
        instance.infoI(caller, message);
    }

    public static void trace(Object caller, String message) {
        instance.traceI(caller, message);
    }

    public static void debug(Object caller, String message) {
        instance.debugI(caller, message);
    }

    public static void error(Object caller, String message) {
        instance.errorI(caller, message);
    }

    public static void warn(Object caller, String message) {
        instance.warnI(caller, message);
    }

    public static void track(Object caller, String message) {
        instance.trackI(caller, message);
    }

    public static void info(Object caller, String sessionId, String message) {
        instance.infoI(caller, sessionId, message);
    }

    public static void trace(Object caller, String sessionId, String message) {
        instance.traceI(caller, sessionId, message);
    }

    public static void debug(Object caller, String sessionId, String message) {
        instance.debugI(caller, sessionId, message);
    }

    public static void error(Object caller, String sessionId, String message) {
        instance.errorI(caller, sessionId, message);
    }

    public static void warn(Object caller, String sessionId, String message) {
        instance.warnI(caller, sessionId, message);
    }

    public static void track(Object caller, String sessionId, String message) {
        instance.trackI(caller, sessionId, message);
    }

    public static void turnOn(boolean shouldTurnOn) {
        instance.turnOnI(shouldTurnOn);
    }




    @Override
    public String getSessionIdI(Object caller) {
        return "session-id";
    }

    @Override
    public String getSessionAndMsgI(String sessionId, String message) {
        return String.format( "%s\t%s", sessionId, message );
    }

    private void print(String level, Object caller, String message){
        System.out.println( String.format("[$s] %s - %s", level, caller, message) );
    }

    @Override
    public void infoI(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                info( caller, sessionId, message);
            }else{
                print("info", caller, message);
            }
        }
    }

    @Override
    public void traceI(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                trace( caller, sessionId, message);
            }else{
                print("trace", caller, message);
            }
        }
    }


    @Override
    public void debugI(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                debug( caller, sessionId, message);
            }else{
                print("debug", caller, message);
            }
        }
    }

    @Override
    public void errorI(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                error( caller, sessionId, message);
            }else{
                print("error", caller, message);
            }
        }
    }

    @Override
    public void warnI(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                warn( caller, sessionId, message);
            }else{
                print("warn", caller, message);
            }
        }
    }

    @Override
    public void trackI(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                track( caller, sessionId, message);
            }else{
                print("track", caller, message);
            }
        }
    }

    @Override
    public void infoI(Object caller, String sessionId, String message){
        if( turnedOn ) print("info", caller, getSessionAndMsg(sessionId, message ));
    }

    @Override
    public void traceI(Object caller, String sessionId, String message){
        if( turnedOn ) print("trace", caller, getSessionAndMsg(sessionId, message ));
    }

    @Override
    public void debugI(Object caller, String sessionId, String message){
        if( turnedOn ) print("debug", caller, getSessionAndMsg(sessionId, message ));
    }

    @Override
    public void errorI(Object caller, String sessionId, String message){
        if( turnedOn ) print("error", caller, getSessionAndMsg(sessionId, message ));
    }

    @Override
    public void warnI(Object caller, String sessionId, String message){
        if( turnedOn ) print("warn", caller, getSessionAndMsg(sessionId, message ));
    }


    @Override
    public void trackI(Object caller, String sessionId, String message){
        if( turnedOn ) print("track", caller, getSessionAndMsg(sessionId, message ));
    }

    @Override
    public void turnOnI(boolean shouldTurnOn){
        turnedOn = shouldTurnOn;
    }
}
