package edu.cmu.inmind.multiuser.dialogue.components;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.common.model.UserIntent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

/**
 * Created by oscarr on 3/14/17.
 */
@StateType(state = Constants.STATEFULL)
@BlackboardSubscription( messages = {SaraCons.MSG_ASR} )
public class NLUComponent extends PluggableComponent {
    private ClientCommController commController;
    private String pythonDialogueAddress;
    private String serviceName;
    @Override
    public void postCreate(){
        serviceName = getSessionId();
        pythonDialogueAddress = Utils.getProperty("pythonDialogueAddress");
        commController = new ClientCommController.Builder()
                .setServerAddress( Utils.getProperty("dialogueAddress") )
                .setClientAddress( pythonDialogueAddress )
                .setServiceName( serviceName )
                .setRequestType( Constants.REQUEST_CONNECT)
                .build();
    }

    @Override
    public void execute() {
        System.out.println("do something at execute");
    }

    @Override
    public void onEvent(BlackboardEvent blackboardEvent) {
        // let's forward the ASR message to DialoguePython:
        commController.send( serviceName, blackboardEvent.getElement() );

        // here we receive the response from DialoguePython:
        commController.receive(message -> blackboard().post( NLUComponent.this, SaraCons.MSG_NLU,
                Utils.fromJson( message, SaraOutput.class )));
    }

    @Override
    public void shutDown(){
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setRequestType( Constants.REQUEST_DISCONNECT );
        commController.send( serviceName, sessionMessage );
    }
}
