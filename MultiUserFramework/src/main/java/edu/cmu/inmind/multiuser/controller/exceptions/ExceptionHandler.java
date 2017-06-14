package edu.cmu.inmind.multiuser.controller.exceptions;

import edu.cmu.inmind.multiuser.controller.resources.Config;
import org.zeromq.ZMQException;
import zmq.ZError;

import java.nio.channels.CancelledKeyException;
import java.util.MissingFormatArgumentException;

/**
 * Created by oscarr on 3/24/17.
 */
public class ExceptionHandler {
    public static void handle(Exception e){
        /*if( Config.isShouldShowException() ) {
            // We only execute errors different to a shutdown error
            if (!(e instanceof ZMQException && ((ZMQException) e).getErrorCode() == 156384765) &&
                    !(e instanceof ZError.IOException) && !(e instanceof MissingFormatArgumentException) &&
                    !(e instanceof CancelledKeyException) &&
                    !(e instanceof NullPointerException) &&
                    !(e instanceof IllegalStateException)) {
                e.printStackTrace();
            }
        }else{*/
            e.printStackTrace();
        //}
    }

    public static void checkAssert(boolean expression) {
        if( !expression ){
            handle(new Exception("Assertion error."));
        }
    }
}
