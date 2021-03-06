package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

/**
 * Created by oscarr on 10/17/17.
 */
@BlackboardSubscription( messages = "MSG_PERFORMANCE_COMPONENT" )
@StateType( state = Constants.STATEFULL )
public class PerformanceTestPC extends PluggableComponent {
    private int messageCount = 1;
    private String agentId;
    private boolean verbose = false;

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    @Override
    public void onEvent(final Blackboard blackboard, final BlackboardEvent event) throws Throwable{
        if( verbose ) {
            Log4J.debug(this, String.format("messageCount for %s is %s and element is %s", agentId, messageCount,
                    event.getElement()));
        }
        messageCount++;
        blackboard.post(this, "MSG_SEND_TO_STATELESS", event.getElement());
    }
}
