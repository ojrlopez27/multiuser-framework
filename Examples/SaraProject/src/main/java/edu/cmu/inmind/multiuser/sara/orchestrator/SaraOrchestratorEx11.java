package edu.cmu.inmind.multiuser.sara.orchestrator;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.sara.component.GuavaServiceComponent;

/**
 * Created by oscarr on 3/3/17.
 */
@BlackboardSubscription( messages = {SaraCons.MSG_NLU})
public class SaraOrchestratorEx11 extends ProcessOrchestratorImpl {
    private final static String ASYNC = "ASYNC";
    private final static String SYNC = "SYNC";

    @Override
    public void initialize(Session session) throws Throwable{
        super.initialize( session );
    }

    @Override
    public void process(String message) throws Throwable{
        super.process(message);

        //TODO: change this to test both scenarios
        String scenario = ASYNC; //String scenario = SYNC;

        // let's work with a component created ad hoc for testing the "Guava Service" behavior:
        GuavaServiceComponent guavaComponent = get(GuavaServiceComponent.class);

        if( scenario.equals(ASYNC) ) {
            // ============================ ASYNCHRONOUS ==========================================

            // To deal with asynchronous transitions you have to add a listener that will be invoked on every state
            // transition of the service:
            guavaComponent.addListener(new Service.Listener() {
                public void starting() {
                    Log4J.info(guavaComponent, String.format( "%s: Component is in STARTING state...", ASYNC));
                }

                public void running() {
                    Log4J.info(guavaComponent, String.format( "%s: Component is in RUNNING state...", ASYNC));
                }

                public void stopping(Service.State from) {
                    Log4J.info(guavaComponent, String.format( "%s: Component is in STOPPING state...", ASYNC));
                }

                public void terminated(Service.State from) {
                    Log4J.info(guavaComponent, "ASYNC: Component is in TERMINATED state...");
                }

                public void failed(Service.State from, Throwable failure) {
                    Log4J.error(guavaComponent, String.format( "%s: Component is in FAILED state...", ASYNC));
                }
            }, MoreExecutors.directExecutor());

            // let's execute the async component and check the listener states (the flag is optional and it is used to
            // tell the component when to start):
            guavaComponent.setFlagStart(true);
            guavaComponent.setKindOfService( ASYNC );
            execute(guavaComponent);
            Log4J.info(guavaComponent, String.format( "%s: This is printed out even when the async component hasn't" +
                    " been TERMINATED", ASYNC));

        }else if( scenario.equals(SYNC) ) {

            // ============================ SYNCHRONOUS ==========================================
            // now let's see the difference for a sync component, let's execute the sync component:
            guavaComponent.setFlagStart(true);
            guavaComponent.setKindOfService( SYNC );
            execute(guavaComponent);

            // Now, this is uninterruptible, throws no checked exceptions, and returns once the service has reached
            // a terminal state.
            guavaComponent.awaitTerminated();
            Log4J.info(guavaComponent, String.format("%s: This is printed out only after component has finished", SYNC));
        }
    }


    @Override
    public void start() {
        super.start();
        //TODO: add some logic when session is started (e.g., startUp resources)
    }

    @Override
    public void pause() {
        super.pause();
        //TODO: add some logic when session is paused (e.g., stop temporarily execute execution)
    }

    @Override
    public void resume() {
        super.resume();
        //TODO: add some logic when session is resumed (e.g., resume execute execution)
    }

    @Override
    public void close() throws Throwable{
        super.close();
        //TODO: add some logic when session is closed (e.g., release resources)
    }
}
