package edu.cmu.inmind.multiuser.controller.log;


import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;

import java.io.File;
import java.io.PrintWriter;

/**
 * Created by oscarr on 3/16/17.
 */
public class FileLogger implements MessageLog {
    private String id;
    private StringBuffer log;
    private String path = "";
    private boolean turnedOn = true;

    @Override
    public void setId(String id) {
        this.id = id;
        log = new StringBuffer();
    }

    @Override
    public void setPath(String path) {
        if( path == null || path.isEmpty() || !(new File(path).exists()) ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.FILE_NOT_EXISTS, path) );
        }
        this.path = path;
    }

    @Override
    public void add(String messageId, String messageContent) {
        if( turnedOn ) {
            log.append(String.format("%-20s %-20s %s", System.currentTimeMillis(), messageId, messageContent) + "\n");
        }
    }

    @Override
    public void store() throws Throwable{
        Flowable.just(log).subscribe(stringBuffer -> {
            try {
                if (turnedOn && !stringBuffer.toString().isEmpty()) {
                    File file = new File(path + id + "-" + Utils.getDateString() + ".log");
                    PrintWriter printWriter = new PrintWriter(file);
                    printWriter.write(stringBuffer.toString());
                    printWriter.flush();
                    printWriter.close();
                }
            }catch (Throwable e){
                ExceptionHandler.handle( e );
            }
        });
    }

    @Override
    public void turnOn(boolean shouldTurnOn) {
        turnedOn = shouldTurnOn;
    }
}
