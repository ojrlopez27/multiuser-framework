package edu.cmu.inmind.multiuser.sara.log;

import edu.cmu.inmind.multiuser.controller.log.MessageLog;

/**
 * Created by oscarr on 3/16/17.
 */
public class SaraDBLogger implements MessageLog {
    private String id;
    private String path = "";
    private boolean turnedOn = true;

    @Override
    public void setId(String id) {
        //TODO: Use this id as your table primary key
        this.id = id;
    }

    @Override
    public void setPath(String path) {
        this.path = path;
        //TODO: open the database. Use path as a connnection string
    }

    @Override
    public void add(String messageId, String messageContent) {
        //TODO: add messages on a temporarily memory
        //...
        if( turnedOn ){
            // ...
        }
    }

    @Override
    public void store() {
        //TODO: store your logs in the DB and close it.
        if( turnedOn ){
            // ...
        }
    }

    @Override
    public void turnOn(boolean shouldTurnOn) {
        turnedOn = shouldTurnOn;
    }
}
