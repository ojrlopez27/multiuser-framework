import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
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
    public void process(String input){
        Log4J.info(this, "Inside orchestrator.process");
        SessionMessage sessionMessage = Utils.fromJson( input, SessionMessage.class );
        logger.turnOn( false );
        blackboard.post(this, "MSG_COMPONENT_1", sessionMessage.getPayload() );
    }

    @Override
    public void onEvent(BlackboardEvent event){
        SessionMessage output = new SessionMessage("", event.getElement().toString() );
        sendResponse( output );
    }
}
