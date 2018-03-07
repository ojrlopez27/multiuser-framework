package edu.cmu.inmind.multiuser.controller.log;

import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.Pluggable;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by oscarr on 3/21/17.
 */
public class Log4J{
    protected static boolean turnedOn = true;
    /**
     * We use this custom level to log messages related to tracking/monitoring messages
     * that are sent from ClientCommController to a Session and back to it. The intValue
     * of 700 means that it is lower than TRACE level.
     */
    protected final static Level TRACK = Level.forName("TRACK", 700);


    protected static String getSessionId(Object caller){
        String sessionId = null;
        try {
            if (caller instanceof ProcessOrchestrator) {
                sessionId = ((ProcessOrchestrator) caller).getSessionId();
            }else if (caller instanceof Pluggable) {
                sessionId = ((Pluggable) caller).getSessionId();
            }
        }catch (Throwable e){
            e.printStackTrace();
        }
        return sessionId;
    }

    protected static String getSessionAndMsg(String sessionId, String message){
        return String.format( "%s\t%s", sessionId, message );
    }

    public static void info(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                info( caller, sessionId, message);
            }else{
                getLogger(caller).info(message);
            }
        }
    }

    public static void trace(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                trace( caller, sessionId, message);
            }else{
                getLogger(caller).trace(message);
            }
        }
    }

    public static void debug(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                debug( caller, sessionId, message);
            }else{
                getLogger(caller).debug(message);
            }
        }
    }

    public static void error(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                error( caller, sessionId, message);
            }else{
                getLogger(caller).error(message);
            }
        }
    }

    public static void warn(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                warn( caller, sessionId, message);
            }else{
                getLogger(caller).warn(message);
            }
        }
    }

    public static void track(Object caller, String message){
        String sessionId = getSessionId(caller);
        if (turnedOn){
            if( sessionId != null ){
                track( caller, sessionId, message);
            }else{
                getLogger(caller).log(TRACK, message);
            }
        }
    }

    public static void info(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).info( getSessionAndMsg(sessionId, message ));
    }

    public static void trace(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).trace( getSessionAndMsg(sessionId, message ));
    }

    public static void debug(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).debug( getSessionAndMsg(sessionId, message ));
    }

    public static void error(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).error( getSessionAndMsg(sessionId, message ));
    }

    public static void warn(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).warn( getSessionAndMsg(sessionId, message ));
    }

    public static void track(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).log( TRACK, getSessionAndMsg(sessionId, message ));
    }

    protected static Logger getLogger(Object caller) {
        try {
            String logName = "";
            org.apache.logging.log4j.Logger logger = null;
            Class clazz = null;
            if( caller instanceof String ){
                logName = (String)caller;
            }else {
                clazz = caller instanceof Class ? (Class) caller : Utils.getClass(caller);
                logger = ResourceLocator.getLogger(clazz);
                logName = clazz.getSimpleName();
            }
            if (logger == null) {
                logger = LogManager.getLogger(logName);
                if(clazz != null) ResourceLocator.addLogger(clazz, logger);
            }
            return logger;
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
        return null;
    }

    public static void turnOn(boolean shouldTurnOn){
        turnedOn = shouldTurnOn;
    }
}
