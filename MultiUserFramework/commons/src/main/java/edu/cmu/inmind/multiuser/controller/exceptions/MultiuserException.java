package edu.cmu.inmind.multiuser.controller.exceptions;


import java.util.Arrays;

/**
 * Created by oscarr on 3/16/17.
 */
public class MultiuserException extends Exception {
    public MultiuserException(String message) {
        super(message);
    }

    public MultiuserException(String errorCode, Object... params){
        super( String.format( errorCode, params));
    }

    public MultiuserException(StringBuffer errorCode, Object... params){
        /** using Java 1.8 **/
//        super( String.format( Arrays.asList(params).stream().reduce(errorCode, (str, toRem) -> str += " %s,").toString(),
//                params));
        super( convertToString(errorCode, params) );
    }

    /**
     * Using Java 1.7
     * @param errorCode
     * @param params
     * @return
     */
    static String convertToString(StringBuffer errorCode, Object... params){
        for(Object str : params){
            errorCode.append( str );
        }
        return errorCode.toString();
    }
}
