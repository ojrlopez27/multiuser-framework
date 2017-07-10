package edu.cmu.inmind.multiuser.sara.orchestrator;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.session.Session;

/**
 * Created by oscarr on 3/3/17.
 */
@BlackboardSubscription( messages = {SaraCons.MSG_NLU})
public class SaraOrchestratorEx05 extends ProcessOrchestratorImpl {
    private SaraOutput response = new SaraOutput();

    @Override
    public void initialize(Session session) throws Throwable{
        super.initialize( session );
    }

    @Override
    public void process(String message) {
        super.process(message);

        // we need this so event-oriented approach is disabled
        blackboard.setNotifySubscribers( false );
        SessionMessage inputMessage = Utils.fromJson(message, SessionMessage.class);
        blackboard.post( this, inputMessage.getMessageId(), Utils.fromJson(inputMessage.getPayload(), SaraInput.class));
        // let's add an empty object for output, otherwise some components such as NLG may crash since there is no
        // guarantee that NLU (who creates SaraOutput) executes first than NLG.
        blackboard.post( this, SaraCons.MSG_NLU, new SaraOutput() );
        Utils.sleep(2000);


        // this method will execute all plugable components in different threads (in parallel). This method does NOT
        // guarantee that messages are synchronized. Unless you synchronize the output, you may get some errors.
        // Synchronization is illustrated in the next example.
        executeAsync(getComponents());

        // you will be responsible to send out the response (not onEvent method):
        response = (SaraOutput) blackboard.get(SaraCons.MSG_NLG);
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
