import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;

/**
 * Created by oscarr on 6/27/17.
 */
@BlackboardSubscription( messages = "" )
public class TestOrchestrator extends ProcessOrchestratorImpl {

    @Override
    public void process(String input){
        System.out.println("orchestrator.process");
        SessionMessage sessionMessage = Utils.fromJson( input, SessionMessage.class );
        blackboard.post(this, sessionMessage.getMessageId(), sessionMessage.getPayload() );
    }
}
