import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.MultiuserFrameworkContainer;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Created by oscarr on 6/28/17.
 */
public class MUFTestSuite {

    private long delay = 10000;
    private String serverAddress = "tcp://127.0.0.1:"; //use IP instead of 'localhost'
    private String clientAddress = "tcp://127.0.0.1:";
    private int[] ports = new int[]{5555, 5556, 5557, 5558};


    /**
     * It tests whether MUF starts and stops correctly. No sessions are created.
     * @throws Throwable
     */
    @Test
    public void testStartAndStopOneMUF() throws Throwable{
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules( TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ), null );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        MultiuserFrameworkContainer.stopFramework( muf );
        assertNull( MultiuserFrameworkContainer.get( muf.getId() ) );
    }

    /**
     * It tests the creation of two different tests (it could be even more), starts them (listening to
     * different ports) and finally stopping both of them.
     * @throws Throwable
     */
    @Test
    public void tesStartAndStopTwoMUFs() throws Throwable{
        MultiuserFramework muf1 = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ), null );
        assertNotNull(muf1);
        MultiuserFramework muf2 = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
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
     * This unit test is intended to connect to MUF without performing TCP/IP communication.
     * The only possible scenario for NOT using TCP/IP is when we need to test different pipelines
     * controlled by the orchestrator, so this test only instantiates an orchestrator (no session
     * manager nor sessions) and it only allows the creation of one pipeline at a time (one user).
     * If you want to test multiple users, you MUST use TCP/IP of course.
     * @throws Throwable
     */
    @Test
    public void testMUFwithTCPIPoff() throws Throwable{

        // let's add some dynamic subcriptions to the orchestrator
        Utils.addOrChangeAnnotation(TestOrchestrator.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{"MSG_RESPONSE"});
        Utils.addOrChangeAnnotation(TestPluggableComponent.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{"MSG_RESPONSE"});
        // creates a MUF and set TCP to off
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ).setTCPon(false), null );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        //let's create a client that sends messages to MUF and TCP is set to off
        ClientCommController client = new ClientCommController( serverAddress, "client-1",
                Constants.REQUEST_CONNECT, false);
        //since communication is not through TCP, we need to explicitly tell the client who the MUF is
        client.setMUF( muf );

        client.receive(message -> assertSame( "This is a test", message));
        SessionMessage message = new SessionMessage( "test", "This is a test" );
        client.send( Constants.SESSION_MANAGER_SERVICE, message);

        Utils.sleep( delay ); // we need time to process the orchestrator
        MultiuserFrameworkContainer.stopFramework( muf );
    }

    /**
     * This method tests the communication between a single client and the MUF using TCP/IP connection.
     * The client sends a message and the server must reply the same plus the sufix: "from MUF"
     * @throws Throwable
     */
    @Test
    public void testServerClientWithTCP() throws Throwable{
        String sessionId = "session-1", messageId1 = "MSG_INITIAL_REQUEST", messageId2 = "MSG_COMPONENT_1",
                messageId3 = "MSG_SEND_RESPONSE";
        // let's add some dynamic subcriptions to the orchestrator
        Utils.addOrChangeAnnotation(TestOrchestrator.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId1, messageId3 });
        Utils.addOrChangeAnnotation(TestPluggableComponent.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId2 });
        // creates a MUF and set TCP to off
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ), null );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        // let's create a client that sends messages to MUF and TCP is set to on
        ClientCommController client = new ClientCommController( serverAddress + ports[0], sessionId
                ,clientAddress + ports[0], Constants.REQUEST_CONNECT);
        client.receive(message -> {
            SessionMessage sessionMessage = Utils.fromJson( message, SessionMessage.class );
            assertEquals("This is a test from MUF", sessionMessage.getPayload() );
            Log4J.info(this, "expected and received messages are the same.");
        });
        SessionMessage message = new SessionMessage( messageId1, "This is a test", sessionId );
        client.send( sessionId, message);
        Utils.sleep( delay * 5 );
        MultiuserFrameworkContainer.stopFramework( muf );
    }
}
