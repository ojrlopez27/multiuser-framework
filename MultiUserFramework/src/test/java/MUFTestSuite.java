import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.MultiuserFrameworkContainer;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by oscarr on 6/27/17.
 */
public class MUFTestSuite {
    private long delay = 3000;
    private String serverAddress = "127.0.0.1"; //use IP instead of 'localhost'

    @Test
    public void testStartStopOneMUF() throws Throwable{
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework( getModules(),
                createConfig( 5555 ), null );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        MultiuserFrameworkContainer.stopFramework( muf );
        assertNull( MultiuserFrameworkContainer.get( muf.getId() ) );
    }

    @Test
    public void tesStartStopTwoMUFs() throws Throwable{
        MultiuserFramework muf1 = MultiuserFrameworkContainer.startFramework( getModules(),
                createConfig( 5555 ), null );
        assertNotNull(muf1);
        MultiuserFramework muf2 = MultiuserFrameworkContainer.startFramework( getModules(),
                createConfig( 5556 ), null );
        assertNotNull(muf2);
        assertNotEquals( muf1, muf2 );
        Utils.sleep(delay); //give some time to initialize the MUF
        MultiuserFrameworkContainer.stopFramework( muf1 );
        assertNull( MultiuserFrameworkContainer.get( muf1.getId() ) );
        MultiuserFrameworkContainer.stopFramework( muf2 );
        assertNull( MultiuserFrameworkContainer.get( muf2.getId() ) );
    }


    @Test
    public void testMUFwithTCPoff() throws Throwable{
        // creates a MUF and set TCP to off
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework( getModules(),
                createConfig( 5555 ).setTCPon(false), null );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        //let's create a client that sends messages to MUF and TCP is set to off
        ClientCommController client = new ClientCommController( serverAddress, "client-1",
                Constants.REQUEST_CONNECT, false);
        SessionMessage message = new SessionMessage( "test", "test" );
        client.send( Constants.SESSION_MANAGER_SERVICE, message);
        MultiuserFrameworkContainer.stopFramework( muf );
    }




    private PluginModule[] getModules(){
        return new PluginModule[]{
                new PluginModule.Builder( TestOrchestrator.class )
                        .addPlugin(TestPluggableComponent.class, "test")
                        .build()
        };
    }

    protected Config createConfig(int port) {
        return new Config.Builder()
                // you can add values directly like this:
                .setSessionManagerPort(port)
                .setDefaultNumOfPoolInstances(10)
                // or you can refer to values in your config.properties file:
                //.setPathLogs(Utils.getProperty("pathLogs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress(serverAddress)
                .setExceptionTraceLevel(Constants.SHOW_MUF_EXCEPTIONS)
                .build();
    }
}
