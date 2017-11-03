package edu.cmu.inmind.multiuser.controller.blackboard;


/**
 * Created by oscarr on 4/29/16.
 */
public interface BlackboardListener {
    void onEvent(final Blackboard blackboard, final BlackboardEvent event) throws Throwable;
    String getSessionId() throws Throwable;
    boolean isClosing();
}
