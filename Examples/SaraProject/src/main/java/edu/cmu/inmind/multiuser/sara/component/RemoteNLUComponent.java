package edu.cmu.inmind.multiuser.sara.component;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.ConnectRemoteService;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StatelessComponent;


/**
 * Created by oscarr on 3/13/17.
 *
 * To communicate with remote services, you just have to add a ConnectRemoteService annotation
 * and indicate the remote service id as it was registered (in this case, the remote NLU was
 * registered as NLU_SERVICE by the DialogueService project).
 *
 * The BlackboardSubscription annotation will tell the blackboard which messages should be
 * delivered to this component when the onEvent method is called.
 */
@StatelessComponent
@BlackboardSubscription( messages = {SaraCons.MSG_ASR})
@ConnectRemoteService( remoteService = SaraCons.NLU_SERVICE )
public class RemoteNLUComponent extends PluggableComponent {

    @Override
    public void startUp(){
        super.startUp();
        // TODO: add code to initialize this component
    }

    @Override
    public void execute() {
        Log4J.info(this, "RemoteNLUComponent: " + hashCode());

        // You can explicitly send messages to and receive messages from the remote service. If you
        // prefer to send messages implicitly and automatically, use only the onEvent method instead.
        // Let's intercept the message coming from the client (MSG_ASR), then modify it and forward
        // it to the remote service:
        SaraInput saraInput = (SaraInput) blackboard().get(SaraCons.MSG_ASR);
        saraInput.setASRinput( saraInput.getASRinput() + " - this is my contribution on execute");
        // sending message to remote service. You ALWAYS have to add the session id to message:
        send(new SessionMessage(SaraCons.MSG_ASR, Utils.toJson(saraInput)));

        // Receiving response from remote service. You need to process this response asynchronously,
        // so create a ResponseListener for this purpose:
        receive(response -> {
            Log4J.info(this, "This is the response from remote: " + response);

            SessionMessage sessionMessage = Utils.fromJson( response, SessionMessage.class );
            SaraOutput saraOutput = Utils.fromJson( sessionMessage.getPayload(), SaraOutput.class );
            String userIntent = saraOutput.getUserIntent().getUserIntent();
            saraOutput.getUserIntent().setUserIntent( userIntent + " - contribution from receive" );

            // notify those components that are subscribed to messages from NLUComponent
            blackboard().setNotifySubscribers(true);
            blackboard().post( this, SaraCons.MSG_ASR,  saraOutput);
        });
    }

    @Override
    public void onEvent(BlackboardEvent event) {
        // TODO: add code here
        //...
        Log4J.info(this, "RemoteNLUComponent. These objects have been updated at the blackboard: "
                + event.toString());

        // You can implicitly communicate with the remote service, that is, you won't have to use
        // send and receive methods as illustrated in execute() method, the blackboard will do it
        // for you. When you are communicating to a remote service, all the changes on the
        // blackboard are automatically notified to the remote service (i.e., you don't have to do
        // anything for this to happen). However, if you want to pre-process the BlackboardEvent
        // before it is sent to the remote service, you just have to modify the event like this:
        if( event.getId().equals(SaraCons.MSG_ASR) ){
            SaraOutput saraOutput = (SaraOutput) event.getElement();
            String userIntent = saraOutput.getUserIntent().getUserIntent();
            SaraInput saraInput = new SaraInput();
            // this is incorrect, but works for illustration purposes
            saraInput.setASRinput( userIntent + " - this is my contribution on onEvent" );

            event.setElement( saraInput );
        }
        // when the method exits, it automatically sends the "event" out to the remote service...
    }

    @Override
    public void shutDown() {
        super.shutDown();
        // TODO: add code to release resources
    }
}
