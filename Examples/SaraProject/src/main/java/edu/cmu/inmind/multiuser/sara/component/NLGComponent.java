package edu.cmu.inmind.multiuser.sara.component;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.common.model.VerbalOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

/**
 * Created by oscarr on 3/7/17.
 */
@StateType(state = Constants.STATELESS )
@BlackboardSubscription( messages = {SaraCons.MSG_SR})
public class NLGComponent extends PluggableComponent {

    @Override
    public void startUp(){
        super.startUp();
        // TODO: add code to initialize this component
    }

    @Override
    public void execute() {
        Log4J.info(this, "NLGComponent: " + hashCode());

        extractAndProcess();
    }

    private SaraOutput extractAndProcess() {
        SaraInput saraInput = new SaraInput();
        SaraOutput saraOutput = new SaraOutput();
        try {
             saraInput = (SaraInput) getBlackBoard(getSessionId()).get(SaraCons.MSG_ASR);
             saraOutput = (SaraOutput) getBlackBoard(getSessionId()).get(SaraCons.MSG_SR);
        }catch(Throwable e)
        {
            e.printStackTrace();
        }
        // do some fancy processing
        // ....
        saraOutput.setVerbal(new VerbalOutput("system realization", "VSN"));
        Log4J.info(this, "Input: " + saraInput + ", Output: " + saraOutput + "\n");

        //update the blackboard
        try {
            getBlackBoard(getSessionId()).post(this, SaraCons.MSG_NLG, saraOutput);
        }catch (Throwable t)
        {
            t.printStackTrace();
        }

        return saraOutput;
    }

    /**
     * If the blackboard model is modified externally, does NLGComponent have to do anything? this is useful when running multiple
     * processes in parallel rather than sequentially.
     */
    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable {
        //TODO: add code here
        //...
        Log4J.info(this, "NLGComponent. These objects have been updated at the blackboard: " + event.toString());
        extractAndProcess();
    }

    @Override
    public void shutDown() {
        super.shutDown();
        // TODO: add code to release resources
    }
}
