package edu.cmu.inmind.multiuser.dialogue.components;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.common.model.UserIntent;
import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;
import edu.cmu.inmind.multiuser.dialogue.MainClass;

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
        if( !MainClass.isMasterMUFCallingMe ) {
            serviceName = getSessionId();
            //pythonDialogueAddress = Utils.getProperty("pythonDialogueAddress");
            commController = new ClientCommController.Builder()
                    .setServerAddress(Utils.getProperty("dialogueAddress"))
                    .setServiceName(serviceName)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .build();
        }
    }

    @Override
    public void execute() {
        System.out.println("do something at execute");
    }

    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent blackboardEvent) {
        if( MainClass.usePythonDialogue ){
            // let's forward the ASR message to DialoguePython:
            commController.send( serviceName, blackboardEvent.getElement() );

            // here we receive the response from DialoguePython:
            commController.setResponseListener(message -> blackboard.post( NLUComponent.this, SaraCons.MSG_NLU,
                    Utils.fromJson( message, SaraOutput.class )));
        }else{
            SaraOutput output = new SaraOutput();
            output.setUserIntent( new UserIntent("greeting", null) );
            blackboard.post( NLUComponent.this, SaraCons.MSG_NLU, output);
        }
    }

    @Override
    public void shutDown(){
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setRequestType( Constants.REQUEST_DISCONNECT );
        if( MainClass.usePythonDialogue ) {
            commController.send(serviceName, sessionMessage);
        }
    }
}
