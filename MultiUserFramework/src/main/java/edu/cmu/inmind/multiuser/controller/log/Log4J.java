package edu.cmu.inmind.multiuser.controller.log;

import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by oscarr on 3/21/17.
 */
public class Log4J{
    private static boolean turnedOn = true;

    private static String getSessionId(Object caller){
        String sessionId = null;
        try {
            if (caller instanceof ProcessOrchestratorImpl) {
                sessionId = ((ProcessOrchestratorImpl) caller).getSessionId();
            }else if (caller instanceof PluggableComponent) {
                sessionId = ((PluggableComponent) caller).getSessionId();
            }
        }catch (Throwable e){
            e.printStackTrace();
        }
        return sessionId;
    }

    private static String getSessionAndMsg(String sessionId, String message){
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
                getLogger(caller).info(message);
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

    public static void info(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).info( getSessionAndMsg(sessionId, message ));
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

    private static Logger getLogger(Object caller) {
        try {
            Class clazz = Utils.getClass(caller);
            org.apache.logging.log4j.Logger logger = ResourceLocator.getLogger(clazz);
            if (logger == null) {
                logger = LogManager.getLogger(clazz.getSimpleName());
                ResourceLocator.addLogger(clazz, logger);
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
