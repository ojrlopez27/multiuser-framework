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
        super( String.format( Arrays.asList(params).stream().reduce(errorCode, (str, toRem) -> str += " %s,").toString(),
                params));
    }
}
