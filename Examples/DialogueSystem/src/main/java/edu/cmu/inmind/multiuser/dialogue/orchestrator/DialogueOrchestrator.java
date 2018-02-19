package edu.cmu.inmind.multiuser.dialogue.orchestrator;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;

/**
 * Created by oscarr on 3/31/17.
 */
@BlackboardSubscription( messages = {SaraCons.MSG_NLU} )
public class DialogueOrchestrator extends ProcessOrchestratorImpl {

    @Override
    public void process(String message) throws Throwable{
        super.process(message);
        SessionMessage inputMessage = Utils.fromJson(message, SessionMessage.class);
        // let's assume that you already know that the message payload wraps an object of class SaraInput, however,
        // you may want to process the message id (inputMessage.getId()) to determine how to translate the message
        // payload.
        SaraInput input = Utils.fromJson(inputMessage.getPayload(), SaraInput.class);
        Log4J.info(this, "This is the ASR input forwarded by Sara: " + input.getASRinput());

        //NLUComponent will be called after posting this:
        blackboard.post(this, SaraCons.MSG_ASR, input);
    }

    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable{
        // obtain the response from blackboard.
        SaraOutput output = (SaraOutput) blackboard.get(SaraCons.MSG_NLU);
        // once you have a response, notify the orchestrator's listeners (session, etc.) which in turns sends a response
        // to the client (i.e., SaraProject)
        sendResponse( new SessionMessage(SaraCons.MSG_DIALOGUE_RESPONSE, Utils.toJson(output) ) );
        Log4J.error(this, "sending response to SaraMUF: " + Utils.toJson(output));
    }
}
