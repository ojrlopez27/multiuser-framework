package edu.cmu.inmind.multiuser.sara.component;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.common.model.VerbalOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StatelessComponent;

/**
 * Created by oscarr on 3/7/17.
 */
@StatelessComponent
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
        SaraInput saraInput = (SaraInput) blackboard().get(SaraCons.MSG_ASR);
        SaraOutput saraOutput = (SaraOutput) blackboard().get(SaraCons.MSG_SR);

        // do some fancy processing
        // ....
        saraOutput.setVerbal(new VerbalOutput("system realization", "VSN"));
        Log4J.info(this, "Input: " + saraInput + ", Output: " + saraOutput + "\n");

        //update the blackboard
        blackboard().post( this, SaraCons.MSG_NLG, saraOutput );

        return saraOutput;
    }

    /**
     * If the blackboard model is modified externally, does NLGComponent have to do anything? this is useful when running multiple
     * processes in parallel rather than sequentially.
     */
    @Override
    public void onEvent(BlackboardEvent event) {
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
