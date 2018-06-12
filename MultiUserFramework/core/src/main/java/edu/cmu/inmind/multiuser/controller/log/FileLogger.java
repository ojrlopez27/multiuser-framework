package edu.cmu.inmind.multiuser.controller.log;


import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;

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
        ExceptionHandler.checkPath(path);
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
        CommonUtils.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (turnedOn && !log.toString().isEmpty()) {
                        File file = new File(path + id + "-" + CommonUtils.getDateString() + ".log");
                        PrintWriter printWriter = new PrintWriter(file);
                        printWriter.write(log.toString());
                        printWriter.flush();
                        printWriter.close();
                    }
                }catch (Throwable e){
                    ExceptionHandler.handle( e );
                }
            }
        });
    }

    @Override
    public void turnOn(boolean shouldTurnOn) {
        turnedOn = shouldTurnOn;
    }
}
