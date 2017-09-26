package edu.cmu.inmind.multiuser.controller.exceptions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

}
