package test;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

/**
 * Created by oscarr on 10/17/17.
 */
@BlackboardSubscription( messages = "MSG_PERFORMANCE_COMPONENT" )
@StateType( state = Constants.STATEFULL )
class MyComponent extends PluggableComponent {
    private int messageCount = 1;

    @Override
    public void onEvent(final Blackboard blackboard, final BlackboardEvent event) throws Throwable{
        Log4J.debug(this, String.format("messageCount for %s is %s and element is %s", 1, messageCount,
                event.getElement()));
        SessionMessage sessionMessage = CommonUtils.fromJson((String) event.getElement(), SessionMessage.class);
        sessionMessage.setPayload("Response: " + messageCount);
        messageCount++;
        blackboard.post(this, "MSG_SEND_RESPONSE", sessionMessage);
    }
}

