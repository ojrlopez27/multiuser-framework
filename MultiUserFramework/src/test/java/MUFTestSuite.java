import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.MultiuserFrameworkFactory;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.plugin.StatelessComponent;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 6/27/17.
 */
public class MUFTestSuite {

    @Test
    public void testMUFCreation() throws Exception{
        MultiuserFramework muf = MultiuserFrameworkFactory.startFramework( getModules()
                , createConfig(), null );
        muf.stop();
    }

    private PluginModule[] getModules(){
        return new PluginModule[]{
                new PluginModule.Builder( TestOrchestrator.class )
                        .addPlugin(TestPluggableComponent.class, "test")
                        .build()
        };
    }

    protected Config createConfig() {
        return new Config.Builder()
                // you can add values directly like this:
                .setSessionManagerPort(5555)
                .setDefaultNumOfPoolInstances(10)
                // or you can refer to values in your config.properties file:
                //.setPathLogs(Utils.getProperty("pathLogs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress("127.0.0.1") //use IP instead of 'localhost'
                .setShouldShowException(true)
                .build();
    }


    static class TestOrchestrator extends ProcessOrchestratorImpl{
        //TODO
    }

    @StatelessComponent
    static class TestPluggableComponent  extends PluggableComponent{

        @Override
        public void onEvent(BlackboardEvent event) {
            //TODO
        }
    }
}
