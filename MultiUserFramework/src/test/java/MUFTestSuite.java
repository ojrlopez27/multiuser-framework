import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.MultiuserFrameworkContainer;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Created by oscarr on 6/28/17.
 */
public class MUFTestSuite {

    private long delay = 3000;
    private String serverAddress = "tcp://127.0.0.1:"; //use IP instead of 'localhost'
    private String clientAddress = "tcp://127.0.0.1:";
    private int[] ports = new int[]{5555, 5556, 5557, 5558};
    private boolean checkAsyncCall = false;


    /**
     * It tests whether MUF starts and stops correctly. No sessions are created.
     * @throws Throwable
     */
    @Test
    public void testStartAndStopOneMUF() throws Throwable{
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules( TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ) );
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
                TestUtils.createConfig( serverAddress, ports[0] ) );
        assertNotNull(muf1);
        MultiuserFramework muf2 = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[1] ) );
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
        //testOneClientCommunication( false );
    }

    /**
     * This method tests the communication between a single client and the MUF using TCP/IP connection.
     * The client sends a message and the server must reply the same plus the sufix: "from MUF"
     * @throws Throwable
     */
    @Test
    public void testOneClientWithTCP() throws Throwable{
        runClient( true, "client-session-1" );
    }

    @Test
    public void testTwoClientsWithTCP() throws Throwable{
        runClient( true, "client-session-1", "client-session-2");
    }

    private void runClient(boolean isTCPon, String... sessionIds) throws Throwable{
        String messageId1 = "MSG_INITIAL_REQUEST", messageId2 = "MSG_COMPONENT_1",
                messageId3 = "MSG_SEND_RESPONSE";
        long uniqueMsgId = System.currentTimeMillis();
        checkAsyncCall = false;
        // let's add some dynamic subcriptions to the orchestrator
        Utils.addOrChangeAnnotation(TestOrchestrator.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId1, messageId3 });
        Utils.addOrChangeAnnotation(TestPluggableComponent.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId2 });
        // creates a MUF and set TCP to on or off
        MultiuserFramework muf = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ).setTCPon( isTCPon ) );
        assertNotNull(muf);
        Utils.sleep(delay); //give some time to initialize the MUF
        // let's create a client that sends messages to MUF
        for(String sessionId : sessionIds ) {
            ClientCommController client = new ClientCommController.Builder()
                    .setServerAddress(serverAddress + ports[0])
                    .setServiceName(sessionId)
                    .setClientAddress(clientAddress + ports[0])
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setTCPon(isTCPon)
                    .setMuf(isTCPon ? null : muf) //when TCP is off, we need to explicitly tell the client who the MUF is
                    .build();
            // this method will be executed asynchronuously, so we need to add a delay before stopping the MUF
            client.setResponseListener(message -> {
                try {
                    SessionMessage sessionMessage = Utils.fromJson(message, SessionMessage.class);
                    assertNotNull(sessionMessage);
                    if (!sessionMessage.getRequestType().equals(Constants.SESSION_CLOSED)) {
                        assertEquals("Response from MUF : " + uniqueMsgId, sessionMessage.getPayload());
                    }
                    client.send(sessionId, new SessionMessage(Constants.REQUEST_DISCONNECT, "" + uniqueMsgId, sessionId));
                    Log4J.info(ResponseListener.class, "1. expected and received messages are the same");
                    MUFTestSuite.this.checkAsyncCall = true;
                } catch (Throwable e) {
                    ExceptionHandler.handle(e);
                }
            });
            SessionMessage message = new SessionMessage( messageId1, "Message from client : " + uniqueMsgId, sessionId );
            client.send( sessionId, message);
        }

        Utils.sleep( delay * 2 );
        MultiuserFrameworkContainer.stopFramework( muf );
        assertTrue( checkAsyncCall );
    }


    //@Test
    public void testOnlyClient(){
        try {
            ClientCommController client = new ClientCommController.Builder()
                    .setServerAddress(serverAddress + ports[0])
                    .setServiceName("session-1")
                    .setClientAddress(clientAddress + ports[0])
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setTCPon(true)
                    .setMuf(null)
                    .build();
            // this method will be executed asynchronuously, so we need to add a delay before stopping the MUF
            client.setResponseListener(message -> {
                SessionMessage sessionMessage = Utils.fromJson(message, SessionMessage.class);
                if (!sessionMessage.getRequestType().equals(Constants.SESSION_CLOSED)) {
                    assertEquals("Response from MUF : " + "uniqueMsgId", sessionMessage.getPayload());
                }
                Log4J.info(ResponseListener.class, "2. expected and received messages are the same");
                MUFTestSuite.this.checkAsyncCall = true;
            });
            SessionMessage message = new SessionMessage("MSG_ASR", "Message from client : " + "uniqueMsgId", "session-1");
            client.send("session-1", message);
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }
}
