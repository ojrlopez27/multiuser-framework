import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StatelessComponent;

/**
 * Created by oscarr on 6/27/17.
 */
@StatelessComponent
@BlackboardSubscription( messages = "" )
public class TestPluggableComponent  extends PluggableComponent {

    @Override
    public void onEvent(BlackboardEvent event) {
        Log4J.info( this, "Received event: " + event.getElement() );
        String uniqueMsgID = event.getElement().toString().split(" : ")[1];
        blackboard().post(this, "MSG_SEND_RESPONSE", "Response from MUF : " + uniqueMsgID );
    }
}
