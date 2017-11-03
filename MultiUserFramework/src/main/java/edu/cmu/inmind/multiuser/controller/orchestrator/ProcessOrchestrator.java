package edu.cmu.inmind.multiuser.controller.orchestrator;

import edu.cmu.inmind.multiuser.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.session.Session;

import java.util.List;

/**
 * Created by oscarr on 3/7/17.
 * Extensions of this abstract class have to deal with a mechanism to coordinate internal execution (e.g., sequential,
 * in parallel, etc.) given an input message, and return a single output as result.
 */
public interface ProcessOrchestrator {
    void process(String message) throws Throwable;
    void start();
    void pause();
    void resume();
    void close(DestroyableCallback callback) throws Throwable;
    void close() throws Throwable;
    void subscribe(OrchestratorListener listener);
    void unsubscribe(OrchestratorListener listener);
    PluggableComponent processMsg(SessionMessage message);
    List<PluggableComponent> getComponents();
    void initialize( Session session ) throws Throwable;
    void execute(PluggableComponent component);
}
