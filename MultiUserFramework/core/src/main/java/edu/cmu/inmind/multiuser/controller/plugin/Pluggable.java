package edu.cmu.inmind.multiuser.controller.plugin;

import com.google.common.util.concurrent.Service;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardListener;
import edu.cmu.inmind.multiuser.controller.communication.DestroyableCallback;
import edu.cmu.inmind.multiuser.controller.communication.ClientController;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.session.Session;

/**
 * Created by oscarr on 5/16/17.
 */
public interface Pluggable extends Service, BlackboardListener {
    void execute();
    String getType();
    String getSessionId();
    void close(String sessionId, DestroyableCallback callback) throws Throwable;
    void setActiveSession(Session activeSession);
    void addMessageLogger(String sessionId, MessageLog messageLogger);
    void addSession(Session session);
    void postCreate();
    MessageLog getMessageLogger();
    void setClientCommController(ClientController clientController);
}
