package edu.cmu.inmind.multiuser.controller.exceptions;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.resources.Config;

/**
 * Created by oscarr on 3/24/17.
 */
public class ExceptionHandler {
    public static void handle(Throwable e){

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

//        if( !Config.isShowAllExceptions() ) {
//            // We only execute errors different to a shutdown error
//            if (!(e instanceof ZMQException && ((ZMQException) e).getErrorCode() == 156384765) &&
//                    !(e instanceof ZError.IOException) && !(e instanceof MissingFormatArgumentException) &&
//                    !(e instanceof CancelledKeyException) &&
//                    !(e instanceof NullPointerException) &&
//                    !(e instanceof IllegalStateException)) {
//                e.printStackTrace();
//            }
//        }else{
//            e.printStackTrace();
//        }
    }

    public static void checkAssert(boolean expression) {
        if( !expression ){
            handle(new Exception("Assertion error."));
        }
    }
}
