package edu.cmu.inmind.multiuser.sara.orchestrator;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.sara.component.RemoteNLUComponent;

/**
 * Created by oscarr on 3/3/17.
 */
@BlackboardSubscription( messages = {SaraCons.MSG_NLU})
public class SaraOrchestratorEx08 extends ProcessOrchestratorImpl {

    @Override
    public void initialize(Session session) throws Throwable{
        super.initialize( session );
    }

    @Override
    public void process(String message) throws Throwable {
        super.process(message);

        // we do this to not trigger onEvent method (just for now)
        //blackboard.setNotifySubscribers( false );
        SessionMessage inputMessage = Utils.fromJson(message, SessionMessage.class);
        blackboard.post( this, inputMessage.getMessageId(), Utils.fromJson(inputMessage.getPayload(),
                SaraInput.class));

        // work with the remote service
        RemoteNLUComponent remoteNLUComponentComponent = get(RemoteNLUComponent.class);
        execute( remoteNLUComponentComponent );
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
