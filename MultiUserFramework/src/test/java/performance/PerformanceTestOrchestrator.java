package performance;

import common.ConstantsTests;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.session.Session;

/**
 * Created by oscarr on 10/17/17.
 */
@BlackboardSubscription( messages = "MSG_SEND_RESPONSE" )
public class PerformanceTestOrchestrator extends ProcessOrchestratorImpl {
    String agentId;
    private boolean verbose = false;

    @Override
    public void initialize(Session session) throws Throwable{
        super.initialize( session );
        getLogger().turnOn(false);
        this.agentId = session.getId();
        ((PerformanceTestPC) getComponents().get(0)).setAgentId(agentId);
        if(verbose)
            Log4J.debug(this, "Initialize Orchestrator for agent: " + agentId);
    }

    @Override
    public void process(String input) throws Throwable{
        //we use plain strings instead of SessionMessage to avoid json parsing
        Log4J.track("PerformanceTestOrchestrator", "21:" + input);
        if( !input.startsWith("@@@") ){
            Log4J.error(this, "Wrong message?");
        }
        blackboard.post(this, ConstantsTests.MSG_PERFORMANCE_COMPONENT, input);
    }

    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent event){
        //we use plain strings instead of SessionMessage to avoid json parsing
        Log4J.track("PerformanceTestOrchestrator", "24:" + event.getElement());
        sendResponse( event.getElement());
        if(verbose)
            Log4J.debug(this, "onEvent sendResponse: " + event.getElement());
    }
}
