package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.session.Session;

/**
 * Created by oscarr on 10/17/17.
 */
@BlackboardSubscription( messages = "MSG_SEND_RESPONSE" )
public class PerformanceTestOrchestrator extends ProcessOrchestratorImpl {
    private String agentId;
    private boolean verbose = false;

    @Override
    public void initialize(Session session) throws Throwable{
        super.initialize( session );
        this.agentId = session.getId();
        ((PerformanceTestPC) getComponents().get(0)).setAgentId(agentId);
        if(verbose)
            Log4J.debug(this, "Initialize Orchestrator for agent: " + agentId);
    }

    @Override
    public void process(String input){
        //we use plain strings instead of SessionMessage to avoid json parsing
        blackboard.post(this, ConstantsTests.MSG_PERFORMANCE_COMPONENT, input);
    }

    @Override
    public void onEvent(BlackboardEvent event){
        //we use plain strings instead of SessionMessage to avoid json parsing
        sendResponse( event.getElement() );
        if(verbose)
            Log4J.debug(this, "onEvent sendResponse: " + event.getElement());
    }
}
