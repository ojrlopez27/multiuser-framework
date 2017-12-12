package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardImpl;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;

/**
 * Created by oscarr on 6/27/17.
 */
@BlackboardSubscription( messages = "" )
@StateType( state = Constants.STATEFULL )
public class TestPluggableComponent  extends PluggableComponent {

    @Override
    public void onEvent(Blackboard bb, BlackboardEvent event) throws Throwable{
        String uniqueMsgID = event.getElement().toString().split(" : ")[1];
        bb.post(this, "MSG_SEND_RESPONSE",
                "Response from MUF : " + uniqueMsgID );
    }
}
