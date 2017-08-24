package edu.cmu.inmind.multiuser.controller.exceptions;

import com.google.common.base.Throwables;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.log.FileLogger;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.resources.Config;

/**
 * Created by oscarr on 3/24/17.
 */
public class ExceptionHandler {
    private static MessageLog logger;
    private static boolean loggerOn = false;

    public static void setLog(String path){
        loggerOn = path != null;
        if( loggerOn ) {
            logger = new FileLogger();
            logger.setPath(path);
            logger.setId("exception-handler");
        }
    }

    public static void setLog( MessageLog log ){
        loggerOn = log != null;
        if( loggerOn ) {
            logger = log;
        }
    }

    public static void storeLog(){
        try {
            if (loggerOn && logger != null) {
                logger.store();
            }
        }catch(Throwable e){
            e.printStackTrace();
        }
    }

    public static void handle(Throwable e){
        if( loggerOn ){
            logger.add( "ExceptionHandler", Throwables.getStackTraceAsString( e ) );
            storeLog();
        }
        switch ( Config.getExceptionTraceLevel() ){
            case Constants.SHOW_ALL_EXCEPTIONS:
                e.printStackTrace();
                break;
            case Constants.SHOW_MUF_EXCEPTIONS:
                if( e instanceof MultiuserException ){
                    e.printStackTrace();
                }
                break;
            case Constants.SHOW_NO_EXCEPTIONS:
                //do nothing
                break;
            case Constants.SHOW_NON_MUF_EXCEPTIONS:
                if( !(e instanceof MultiuserException ) ){
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    public static void checkAssert(boolean expression) {
        if( !expression ){
            handle(new Exception("Assertion error."));
        }
    }
}
