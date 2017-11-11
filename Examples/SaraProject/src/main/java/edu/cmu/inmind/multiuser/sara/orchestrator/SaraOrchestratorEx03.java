package edu.cmu.inmind.multiuser.sara.orchestrator;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.plugin.Pluggable;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.session.Session;

/**
 * Created by oscarr on 3/3/17.
 */
@BlackboardSubscription( messages = {SaraCons.MSG_NLU})
public class SaraOrchestratorEx03 extends ProcessOrchestratorImpl {
    private SaraOutput response = new SaraOutput();

    @Override
    public void initialize(Session session) throws Throwable{
        super.initialize( session );
    }

    @Override
    public void process(String message) throws Throwable{
        super.process(message);
        SessionMessage inputMessage = Utils.fromJson(message, SessionMessage.class);

        // since this example doesn't use the event-oriented approach but the direct-invocation approach, you may
        // want to tell the blackboard not to notify subscribers (you will notify them):
        //blackboard.setNotifySubscribers( false );
        blackboard.post( this, inputMessage.getMessageId(), Utils.fromJson(inputMessage.getPayload(), SaraInput.class));

        // it selects only one component and then execute it:
        Pluggable selectedComponent = processMsg( inputMessage );
        if( selectedComponent != null ){
            // remember that you have to execute component by using orchestrator's execute method, like this:
            execute(selectedComponent);
        }

        // you will be responsible to send out the response (not onEvent method):
        response = (SaraOutput) blackboard.get(SaraCons.MSG_NLU);
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
