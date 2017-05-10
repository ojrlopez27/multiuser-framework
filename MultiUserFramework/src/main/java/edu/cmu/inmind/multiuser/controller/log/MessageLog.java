package edu.cmu.inmind.multiuser.controller.log;

/**
 * Created by oscarr on 3/16/17.
 */
public interface MessageLog {
    void setId(String id);
    void setPath(String path);
    void add(String messageId, String messageContent);
    void store() throws Exception;
    void turnOn(boolean shouldTurnOn);
}
