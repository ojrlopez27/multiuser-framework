package edu.cmu.inmind.multiuser.controller.exceptions;

import com.google.common.base.Throwables;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.log.FileLogger;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.resources.Config;

import java.io.File;
import java.nio.channels.ClosedByInterruptException;

/**
 * Created by oscarr on 3/24/17.
 */
public class ExceptionHandler {
    private static MessageLog logger;
    private static boolean loggerOn = false;
    private static String messageId = "ExceptionHandler";

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
        if( !(e instanceof org.zeromq.ZMQException && e instanceof ClosedByInterruptException )) {
            if (loggerOn) {
                logger.add(messageId, Throwables.getStackTraceAsString(e));
                if (e instanceof OutOfMemoryError)
                    addOutOfMemoryLog();
                storeLog();
            }
            switch (Config.getExceptionTraceLevel()) {
                case Constants.SHOW_ALL_EXCEPTIONS:
                    e.printStackTrace();
                    break;
                case Constants.SHOW_MUF_EXCEPTIONS:
                    if (e instanceof MultiuserException) {
                        e.printStackTrace();
                    }
                    break;
                case Constants.SHOW_NO_EXCEPTIONS:
                    //do nothing
                    break;
                case Constants.SHOW_NON_MUF_EXCEPTIONS:
                    if (!(e instanceof MultiuserException)) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public static void checkAssert(boolean expression) {
        if( !expression ){
            handle(new Exception("Assertion error."));
        }
    }


    private static void addOutOfMemoryLog(){
        logger.add( messageId, "============ Tracking OutOfMemoryError ============");
        /** Total number of processors or cores available to the JVM **/
        logger.add( messageId, "Available processors (cores): " +
                Runtime.getRuntime().availableProcessors());

        /** Total amount of free memory available to the JVM **/
        logger.add( messageId,"Free memory (bytes): " +
                Runtime.getRuntime().freeMemory());

        /** This will return Long.MAX_VALUE if there is no preset limit **/
        long maxMemory = Runtime.getRuntime().maxMemory();
        /** Maximum amount of memory the JVM will attempt to use **/
        logger.add( messageId,"Maximum memory (bytes): " +
                (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /** Total memory currently in use by the JVM **/
        logger.add( messageId,"Total memory (bytes): " +
                Runtime.getRuntime().totalMemory());

        /** Get a list of all filesystem roots on this system **/
        File[] roots = File.listRoots();

        /** For each filesystem root, print some info */
        for (File root : roots) {
            logger.add( messageId,"File system root: " + root.getAbsolutePath());
            logger.add( messageId, "Total space (bytes): " + root.getTotalSpace());
            logger.add( messageId,"Free space (bytes): " + root.getFreeSpace());
            logger.add( messageId,"Usable space (bytes): " + root.getUsableSpace());
        }

        // https://stackoverflow.com/questions/12807797/java-get-available-memory
        long allocatedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;

        logger.add( messageId, "Presumable free memory: " + presumableFreeMemory );
    }
}
