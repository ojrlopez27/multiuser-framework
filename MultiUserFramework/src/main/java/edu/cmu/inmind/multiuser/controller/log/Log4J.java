package edu.cmu.inmind.multiuser.controller.log;

import edu.cmu.inmind.multiuser.common.Utils;
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
        }catch (Exception e){
            e.printStackTrace();
        }
        return sessionId;
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
        if( turnedOn ) getLogger(caller).info( sessionId + "\t" + message );
    }

    public static void debug(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).debug(sessionId + "\t" +message);
    }

    public static void error(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).error( sessionId + "\t" + message);
    }

    public static void warn(Object caller, String sessionId, String message){
        if( turnedOn ) getLogger(caller).warn( sessionId + "\t" + message );
    }

    private static Logger getLogger(Object caller){
        Class clazz = Utils.getClass(caller);
        org.apache.logging.log4j.Logger logger = ResourceLocator.getLogger(clazz);
        if( logger == null ){
            logger = LogManager.getLogger( clazz.getSimpleName() );
            ResourceLocator.addLogger(clazz, logger);
        }
        return logger;
    }

    public static void turnOn(boolean shouldTurnOn){
        turnedOn = shouldTurnOn;
    }
}
