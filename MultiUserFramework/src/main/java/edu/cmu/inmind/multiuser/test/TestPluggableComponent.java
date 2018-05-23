package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardImpl;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

/**
 * Created by oscarr on 6/27/17.
 */
@BlackboardSubscription( messages = {"MSG_COMPONENT_1", "MSG_START_SESSION", "MSG_ASR" })
@StateType( state = Constants.STATEFULL )
public class TestPluggableComponent  extends PluggableComponent {

    @Override
    public void onEvent(Blackboard bb, BlackboardEvent event) throws Throwable{
        String uniqueMsgID = event.getElement().toString();
        bb.post(this, "MSG_SEND_RESPONSE",
                "Response from MUF : " + uniqueMsgID );
        Log4J.info(this, "MSG_SEND_RESPONSE"+uniqueMsgID);
        Thread.sleep(100);
        bb.post(this, "MSG_SEND_RESPONSE",
                "Response from MUF : " + uniqueMsgID );
        Log4J.info(this, "MSG_SEND_RESPONSE"+uniqueMsgID);
        Thread.sleep(100);

        bb.post(this, "MSG_SEND_RESPONSE",
                "Response from MUF : " + uniqueMsgID );
        Log4J.info(this, "MSG_SEND_RESPONSE"+uniqueMsgID);
    }
}
