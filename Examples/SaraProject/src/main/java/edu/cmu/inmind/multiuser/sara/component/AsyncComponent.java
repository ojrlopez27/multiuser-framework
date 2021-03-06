package edu.cmu.inmind.multiuser.sara.component;

import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;
import edu.cmu.inmind.multiuser.controller.sync.ForceSync;

/**
 * Created by oscarr on 3/10/17.
 */
@StateType(state = Constants.STATELESS )
@ForceSync(id = "sync-example")
public class AsyncComponent extends PluggableComponent {
    private String name;

    public AsyncComponent(){}

    public AsyncComponent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void startUp(){
        super.startUp();
        // TODO: add code to initialize this component
    }

    @Override
    public void execute() {
        // let's simulate that this component runs on its own separate thread.
        new Thread(){
            public void run(){
                for( int i = 0; i < 5; i++){
                    Log4J.info(AsyncComponent.this, String.format("AsyncComponent: %d\tRunning: %s  step:  %d",
                            hashCode(), name, i));
                    Utils.sleep(100);
                }

                //use this to synchronize the async component with the next one.
                notifyNext( AsyncComponent.this );
            }
        }.start();
    }


    @Override
    public void shutDown() {
        super.shutDown();
        // TODO: add code to release resources
    }

    @Override
    public void onEvent( Blackboard blackboard, BlackboardEvent blackboardEvent) throws Throwable {
        Log4J.info(this, "AsyncComponent. These objects have been updated at the blackboard: " + blackboardEvent.toString() );

    }
}
