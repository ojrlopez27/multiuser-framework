package edu.cmu.inmind.multiuser.sara.component;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

/**
 * Created by oscarr on 3/7/17.
 */
@StateType(state = Constants.STATEFULL )
@BlackboardSubscription( messages = {SaraCons.MSG_NLU, SaraCons.MSG_DIALOGUE_RESPONSE})
public class TaskReasonerComponent extends PluggableComponent {

    @Override
    public void startUp(){
        super.startUp();
        // TODO: add code to initialize this component
    }

    @Override
    public void execute() {
        Log4J.info(this, "TaskReasonerComponent: " + hashCode());

        extractAndProcess(getBlackBoard(getSessionId()));
    }

    private SaraOutput extractAndProcess(Blackboard blackboard) {
        SaraOutput saraOutput = new SaraOutput();
        try {
            SaraInput saraInput = (SaraInput) blackboard.get(SaraCons.MSG_ASR);
             saraOutput = (SaraOutput) blackboard.get(SaraCons.MSG_NLU);
            if (saraOutput == null)
                saraOutput = Utils.fromJson((String) blackboard.get(SaraCons.MSG_DIALOGUE_RESPONSE),
                        SaraOutput.class);

            // do some fancy processing
            // ....
            // saraOutput.setSystemIntent("system-intent");
            Log4J.info(this, "Input: " + saraInput + ", Output: " + saraOutput + "\n");

            //update the blackboard
            blackboard.post(this, SaraCons.MSG_TR, saraOutput);
        }catch (Throwable t)
        {
            t.printStackTrace();
        }
        return saraOutput;
    }

    /**
     * If the blackboard model is modified externally, does TR have to do anything? this is useful when running multiple
     * processes in parallel rather than sequentially.
     */
    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent event) {
        //TODO: add code here
        //...
        Log4J.info(this, "TaskReasonerComponent. These objects have been updated at the blackboard: " + event.toString());
        extractAndProcess(blackboard);
    }

    @Override
    public void shutDown() {
        super.shutDown();
        // TODO: add code to release resources
    }
}
