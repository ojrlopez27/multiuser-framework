package edu.cmu.inmind.multiuser.controller.orchestrator;

/**
 * Created by oscarr on 3/10/17.
 */
public interface OrchestratorListener {
    void processOutput(Object output) throws Throwable;
}
