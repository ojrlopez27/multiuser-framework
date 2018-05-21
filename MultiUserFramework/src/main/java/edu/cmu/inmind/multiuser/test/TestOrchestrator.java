package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;

/**
 * Created by oscarr on 6/27/17.
 */
@BlackboardSubscription( messages = "MSG_SEND_RESPONSE" )
public class TestOrchestrator extends ProcessOrchestratorImpl {

    @Override
    public void process(String input) throws Throwable{
        System.out.println("Orchestrator receives: " + input);
        SessionMessage sessionMessage = Utils.fromJson( input, SessionMessage.class );
        logger.turnOn( false );
        Log4J.info(this, " input "+input);
        blackboard.post(this, "MSG_COMPONENT_1", sessionMessage.getPayload() );
        sendResponse( input);
    }

    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent event){
        SessionMessage output = new SessionMessage("", event.getElement().toString() );
        sendResponse( output );
    }
}
