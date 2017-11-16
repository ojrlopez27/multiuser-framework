package edu.cmu.inmind.multiuser.sara.component;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.common.model.UserIntent;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by oscarr on 3/7/17.
 */
@StateType(state = Constants.STATELESS)
@BlackboardSubscription( messages = {SaraCons.MSG_ASR})
public class NLUComponent extends PluggableComponent {

    @Override
    public void startUp(){
        super.startUp();
        // TODO: add code to initialize this component
    }

    @Override
    public void execute() {
        Log4J.info(this, "NLUComponent: " + hashCode());
        final Blackboard blackboard = getBlackBoard(getSessionId());
        try {
            SaraOutput saraOutput = extractAndProcess(blackboard);
            //update the blackboard
            blackboard.post(this, SaraCons.MSG_NLU, saraOutput );
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }


    private SaraOutput extractAndProcess(Blackboard blackboard ) {

        SaraOutput saraOutput = new SaraOutput();
        SaraInput saraInput = null;
        try {
            saraInput = (SaraInput) blackboard.get(SaraCons.MSG_ASR);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        // do some fancy processing
        // ....
        saraOutput.setUserIntent( new UserIntent( "user-intent", new ArrayList<>() ) );
        Log4J.info(this, "Input: " + saraInput + ", Output: " + saraOutput + "\n");
        return saraOutput;
    }

    /**
     * If the blackboard model is modified externally, does NLUComponent have to do anything? this is useful when running multiple
     * processes in parallel rather than sequentially.
     */
    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable {
        //TODO: add code here
        //...
        Log4J.info(this, "NLUComponent. These objects have been updated at the blackboard: " + event.toString());
        SaraOutput saraOutput = extractAndProcess(blackboard);

        //TODO: uncomment this code to run Ex13_UserModel and Ex15_WholePipeline
        saraOutput.getUserIntent().setUserIntent("user-interests");
        List<String> entities = Arrays.asList(new String[]{"robotics", "IA", "cooking"});
        saraOutput.getUserIntent().setEntitities( entities );
        //update the blackboard

        blackboard.post(this, SaraCons.MSG_NLU, saraOutput);

    }

    @Override
    public void shutDown() {
        super.shutDown();
        // TODO: add code to release resources
    }
}
