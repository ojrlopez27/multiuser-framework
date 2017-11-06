package edu.cmu.inmind.multiuser.controller.orchestrator;

import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;

/**
 * Created by oscarr on 3/10/17.
 */
public interface OrchestratorListener {
    void processOutput(Object output) throws Throwable;
}
