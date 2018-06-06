package edu.cmu.inmind.multiuser.controller.log;

import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
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
public class Log4J implements ILog4J{
    protected boolean turnedOn = true;
    /**
     * We use this custom level to log messages related to tracking/monitoring messages
     * that are sent from ClientCommController to a Session and back to it. The intValue
     * of 700 means that it is lower than TRACE level.
     */
    protected final Level TRACK = Level.forName("TRACK", 700);
    private static Log4J instance = new Log4J();

    protected static String getSessionId(Object caller){
        return instance.getSessionIdI(caller);
    }

    @Override
    public String getSessionIdI(Object caller){
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
        return instance.getSessionAndMsgI(sessionId, message);
    }

    @Override
    public String getSessionAndMsgI(String sessionId, String message){
        return String.format( "%s\t%s", sessionId, message );
    }

    public static void info(Object caller, String message){
        instance.infoI(caller, message);
    }

    @Override
    public void infoI(Object caller, String message){
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
        instance.traceI(caller, message);
    }

    @Override
    public void traceI(Object caller, String message){
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
        instance.debugI(caller, message);
    }

    @Override
    public void debugI(Object caller, String message){
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
        instance.errorI(caller, message);
    }

    @Override
    public void errorI(Object caller, String message){
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
        instance.warnI(caller, message);
    }

    @Override
    public void warnI(Object caller, String message){
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
        instance.trackI(caller, message);
    }

    @Override
    public void trackI(Object caller, String message){
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
        instance.infoI(caller, sessionId, message);
    }

    @Override
    public void infoI(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).info( getSessionAndMsg(sessionId, message ));
    }

    public static void trace(Object caller, String sessionId, String message){
        instance.traceI(caller, sessionId, message);
    }

    @Override
    public void traceI(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).trace( getSessionAndMsg(sessionId, message ));
    }

    public static void debug(Object caller, String sessionId, String message){
        instance.debugI(caller, sessionId, message);
    }

    @Override
    public void debugI(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).debug( getSessionAndMsg(sessionId, message ));
    }

    public static void error(Object caller, String sessionId, String message){
        instance.errorI(caller, sessionId, message);
    }

    @Override
    public void errorI(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).error( getSessionAndMsg(sessionId, message ));
    }

    public static void warn(Object caller, String sessionId, String message){
        instance.warnI(caller, sessionId, message);
    }

    @Override
    public void warnI(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).warn( getSessionAndMsg(sessionId, message ));
    }

    public static void track(Object caller, String sessionId, String message){
        instance.traceI(caller, sessionId, message);
    }

    @Override
    public void trackI(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).log( TRACK, getSessionAndMsg(sessionId, message ));
    }

    protected static Logger getLogger(Object caller) {
        return instance.getLoggerI(caller);
    }

    public Logger getLoggerI(Object caller) {
        try {
            String logName = "";
            Logger logger = null;
            Class clazz = null;
            if( caller instanceof String ){
                logName = (String)caller;
            }else {
                clazz = caller instanceof Class ? (Class) caller : CommonUtils.getClass(caller);
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
        instance.turnOn(shouldTurnOn);
    }

    @Override
    public void turnOnI(boolean shouldTurnOn){
        turnedOn = shouldTurnOn;
    }

    public static ILog4J getInstance() {
        return instance;
    }
}
