package edu.cmu.inmind.multiuser.sara.orchestrator;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.sara.log.SaraDBLogger;

/**
 * Created by oscarr on 3/3/17.
 */
@BlackboardSubscription( messages = {SaraCons.MSG_NLU})
public class SaraOrchestratorEx02 extends ProcessOrchestratorImpl {
    private SaraOutput response = new SaraOutput();

    @Override
    public void initialize(Session session) throws Throwable{
        // TODO: If you want to use your own implementation of a messageLogger, you have to declare it here in the
        // TODO: orchestrator's constructor. For instance, do something like this:
        logger = new SaraDBLogger();
        logger.setPath( "your-db-connection-string-goes-here");

        super.initialize( session );
    }

    /**
     * Execute the components in whichever order you want (i.e., sequentially, in parallel, mixed), and then return the
     * output.
     * @param message
     */
    @Override
    public void process(String message) throws Throwable{
        super.process(message);

        // how to extract and translate the message that comes from clients:
        SessionMessage inputMessage = Utils.fromJson(message, SessionMessage.class);

        // post input/output objects through the Blackboard in order to make them available to other components.
        // Let's assume that you already know that the message payload wraps an object of class SaraInput, however,
        // you may want to process the message id (inputMessage.getId()) to determine how to translate the
        // message payload.
        blackboard.post( this, inputMessage.getMessageId(), Utils.fromJson(inputMessage.getPayload(), SaraInput.class) );

        // now, blackboard will deliver the message to NLU, which in turn will post a message MSG_NLU (for task reasoner).
        // If you need to intercept this message at Process Orchestrator, just subscribe it as the annotation above:
        // @BlackboardSubscription( messages = {SaraCons.MSG_NLU})
    }

    @Override
    /**
     * This method will be called when the system has a response to send out, that is (in our example), when message
     * MSG_NLU is posted in the blackboard by NLUComponent. Note that SaraOrchestratorEx02 is subscribed to MSG_NLU
     */
    public void onEvent(Blackboard blackboard,BlackboardEvent event) throws Throwable{
        // obtain the response from blackboard.
        response = (SaraOutput) blackboard.get(SaraCons.MSG_NLU);
        //once you have a response, notify the orchestrator's listeners (the corresponding session and any other listener)
        //Also, the response will be sent out to the client.
        sendResponse( new SessionMessage(SaraCons.MSG_NLG, Utils.toJson(response) ) );
    }


    @Override
    public void start() {
        super.start();
        //TODO: add some logic when session is started (e.g., startUp resources)
    }

    @Override
    public void pause() {
        super.pause();
        //TODO: add some logic when session is paused (e.g., stop temporarily execute execution)
    }

    @Override
    public void resume() {
        super.resume();
        //TODO: add some logic when session is resumed (e.g., resume execute execution)
    }

    @Override
    public void close() throws Throwable{
        super.close();
        //TODO: add some logic when session is closed (e.g., release resources)
    }
}
