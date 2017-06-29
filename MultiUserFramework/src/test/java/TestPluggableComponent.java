import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
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
        System.out.println("event: " + event.getElement() );
    }
}
