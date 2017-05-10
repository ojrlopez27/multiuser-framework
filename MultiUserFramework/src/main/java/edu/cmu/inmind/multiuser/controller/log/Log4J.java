package edu.cmu.inmind.multiuser.controller.log;

import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by oscarr on 3/21/17.
 */
public class Log4J{
    private static boolean turnedOn = true;

    public static void info(Object caller, String message){
        if( turnedOn ) getLogger(caller).info( message );
    }

    public static void debug(Object caller, String message){
        if( turnedOn ) getLogger(caller).debug(message);
    }

    public static void error(Object caller, String message){
        if( turnedOn ) getLogger(caller).error(message);
    }

    public static void warn(Object caller, String message){
        if( turnedOn ) getLogger(caller).warn( message );
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
