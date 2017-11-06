package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardImpl;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oscarr on 11/1/17.
 */
@BlackboardSubscription( messages = "MSG_SEND_TO_STATELESS" )
@StateType( state = Constants.STATELESS )
public class PerformanceTestPCStateless extends PluggableComponent {
    private AtomicInteger messageCount = new AtomicInteger(0);
    private boolean verbose = false;

    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable{
        String[] msgs = event.getElement().toString().split(":");
        String msg = msgs[0];
        String agentId = msgs[1];
        if( verbose ) {
            Log4J.error(this, String.format("messageCount for %s is %s and element is %s", agentId,
                    messageCount.incrementAndGet(), event.getElement()));
        }
        Log4J.track("PerformanceTestPCStateless", "23:" + event.getElement());
        if( !agentId.equals(event.getSessionId()) || !agentId.equals(event.getElement().toString().split(":")[1]) ){
            Log4J.track("PerformanceTestPCStateless", "23.1: They are not equal");
        }
        blackboard.post(this, "MSG_SEND_RESPONSE", event.getElement());
    }
}
