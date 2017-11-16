package edu.cmu.inmind.multiuser.sara.orchestrator;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.plugin.Pluggable;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.sara.component.AsyncComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oscarr on 3/3/17.
 */
@BlackboardSubscription( messages = {SaraCons.MSG_NLU})
public class SaraOrchestratorEx06 extends ProcessOrchestratorImpl {
    private SaraOutput response = new SaraOutput();

    @Override
    public void initialize(Session session) throws Throwable{
        super.initialize( session );
    }

    @Override
    public void process(String message)throws Throwable {
        super.process(message);

        List<Pluggable> asyncComponents = new ArrayList<>();
        for( int i = 0; i < 5; i++ ){
            // AsyncComponent is async by nature (it runs on its own thread)
            asyncComponents.add( new AsyncComponent("Component " + i) );
        }
        // let's sync these 5 async components:
        forceSync( asyncComponents );
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
