package performance;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

/**
 * Created by oscarr on 11/1/17.
 */
@BlackboardSubscription( messages = "MSG_SEND_TO_STATELESS" )
@StateType( state = Constants.STATELESS )
public class PerformanceTestPCStateless extends PluggableComponent {
    private int messageCount = 1;
    private String agentId;
    private boolean verbose = false;

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    @Override
    public void onEvent(BlackboardEvent event) {
        if( messageCount != Integer.valueOf( event.getElement().toString()) ){
            Log4J.error(this, String.format("messageCount for %s is %s and element is %s", agentId, messageCount,
                    event.getElement()));
        }
        messageCount++;
        blackboard().post(this, "MSG_SEND_RESPONSE", event.getElement());
    }
}
