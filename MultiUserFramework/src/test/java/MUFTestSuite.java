import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.MultiuserFrameworkContainer;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertNotSame;

/**
 * Created by oscarr on 6/28/17.
 */
public class MUFTestSuite {

    private long delay = 1000;
    private String serverAddress = "127.0.0.1"; //use IP instead of 'localhost'
    private String clientAddress = "tcp://127.0.0.1:";
    private int[] ports = new int[]{5555, 5556, 5557, 5558};

    @Test
    public void testStartAndStopOneMUF() throws Throwable{
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework( TestUtils.getModules(),
                TestUtils.createConfig( serverAddress, ports[0] ), null );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        MultiuserFrameworkContainer.stopFramework( muf );
        assertNull( MultiuserFrameworkContainer.get( muf.getId() ) );
    }

    @Test
    public void tesStartAndStopTwoMUFs() throws Throwable{
        MultiuserFramework muf1 = MultiuserFrameworkContainer.startFramework( TestUtils.getModules(),
                TestUtils.createConfig( serverAddress, ports[0] ), null );
        assertNotNull(muf1);
        MultiuserFramework muf2 = MultiuserFrameworkContainer.startFramework( TestUtils.getModules(),
                TestUtils.createConfig( serverAddress, ports[1] ), null );
        assertNotNull(muf2);
        assertNotSame( muf1, muf2 );
        Utils.sleep(delay); //give some time to initialize the MUF
        MultiuserFrameworkContainer.stopFramework( muf1 );
        assertNull( MultiuserFrameworkContainer.get( muf1.getId() ) );
        MultiuserFrameworkContainer.stopFramework( muf2 );
        assertNull( MultiuserFrameworkContainer.get( muf2.getId() ) );
    }

    /**
     * This unit test is intended to connect to MUF without performing TCP/IP communication
     * @throws Throwable
     */
    @Test
    public void testMUFwithTCPIPoff() throws Throwable{
        // creates a MUF and set TCP to off
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework( TestUtils.getModules(),
                TestUtils.createConfig( serverAddress, ports[0] ).setTCPon(false), null );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        //let's create a client that sends messages to MUF and TCP is set to off
        ClientCommController client = new ClientCommController( serverAddress, "client-1",
                Constants.REQUEST_CONNECT, false);

        SessionMessage message = new SessionMessage( "test", "test" );
        client.send( Constants.SESSION_MANAGER_SERVICE, message);
        MultiuserFrameworkContainer.stopFramework( muf );
    }

    @Test
    public void testServerClientWithTCP() throws Throwable{
        // creates a MUF and set TCP to off
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework( TestUtils.getModules(),
                TestUtils.createConfig( serverAddress, ports[0] ), null );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        // let's create a client that sends messages to MUF and TCP is set to on
        ClientCommController client = new ClientCommController( serverAddress, "client-1",
                clientAddress + ports[0], Constants.REQUEST_CONNECT);

        SessionMessage message = new SessionMessage( "test", "test" );
        client.send( Constants.SESSION_MANAGER_SERVICE, message);
        MultiuserFrameworkContainer.stopFramework( muf );
    }
}
