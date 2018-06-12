package common;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.muf.MUFLifetimeManager;
import edu.cmu.inmind.multiuser.controller.muf.MultiuserController;
import edu.cmu.inmind.multiuser.test.TestOrchestrator;
import edu.cmu.inmind.multiuser.test.TestPluggableComponent;
import edu.cmu.inmind.multiuser.test.TestUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;


/**
 * Created by oscarr on 6/28/17.
 */
public class MUFTestSuite {

    private long delay = 3000;
    private String serverAddress = "tcp://127.0.0.1:"; //use IP instead of 'localhost'
    private String clientAddress = "tcp://127.0.0.1:";
    private int[] ports = new int[]{5555, 5556, 5557, 5558, 5559, 5560};
    protected static boolean checkAsyncCall = false;
    HashMap<String, ClientCommController> clientCommControllerHashMap = new HashMap<>();
    HashMap<String, ProcessResponse> responseListenerHashMap = new HashMap<>();
    ClientCommController clientCommController ;
    ProcessResponse processResponse = null;
    int j= 0;

    /**
     * It tests whether MUF starts and stops correctly. No sessions are created.
     * @throws Throwable
     */
    @Test
    public void testStartAndStopOneMUF() throws Throwable{
        MultiuserController muf = MUFLifetimeManager.startFramework(
                TestUtils.getModules( TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ) );
        assertNotNull(muf);
        CommonUtils.sleep(delay); //give some time to initialize the MUF
        MUFLifetimeManager.stopFramework( muf );
        assertNull( MUFLifetimeManager.get( muf.getId() ) );
    }

    /**
     * It tests the creation of two different tests (it could be even more), starts them (listening to
     * different ports) and finally stopping both of them.
     * @throws Throwable
     */
    @Test
    public void testStartAndStopTwoMUFs() throws Throwable{
        MultiuserController muf1 = MUFLifetimeManager.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[1] ) );
        assertNotNull(muf1);
        MultiuserController muf2 = MUFLifetimeManager.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[2] ) );
        assertNotNull(muf2);
        assertNotSame( muf1, muf2 );
        CommonUtils.sleep(delay); //give some time to initialize the MUF
        MUFLifetimeManager.stopFramework( muf1 );
        assertNull( MUFLifetimeManager.get( muf1.getId() ) );
        MUFLifetimeManager.stopFramework( muf2 );
        assertNull( MUFLifetimeManager.get( muf2.getId() ) );
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
        runClient( true, 3,"client-session-1" );
    }

    @Test
    public void testTwoClientsWithTCP() throws Throwable{
        runClient( true, 4, "client-session-1", "client-session-2");
    }

    private void runClient(boolean isTCPon, int i, String... sessionIds) throws Throwable{
        AtomicInteger countConnected = new AtomicInteger(0);
        AtomicBoolean allConnected = new AtomicBoolean(false);
        long timeout = 1000 * 10; // ten seconds
        String messageId1 = "MSG_INITIAL_REQUEST", messageId2 = "MSG_COMPONENT_1",
                messageId3 = "MSG_SEND_RESPONSE";
        long uniqueMsgId = System.currentTimeMillis();
        checkAsyncCall = false;
        // let's add some dynamic subcriptions to the orchestrator
        CommonUtils.addOrChangeAnnotation(TestOrchestrator.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId1, messageId3 });
        CommonUtils.addOrChangeAnnotation(TestPluggableComponent.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId2 });
        // creates a MUF and set TCP to on or off
        MultiuserController muf = MUFLifetimeManager.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[i] ).setTCPon( isTCPon ) );
        assertNotNull(muf);
        CommonUtils.sleep(delay); //give some time to initialize the MUF
        // let's create a client that sends messages to MUF
        for(String sessionId : sessionIds ) {
            processResponse = new ProcessResponse(countConnected, allConnected, sessionIds.length, sessionId);
             clientCommController = new ClientCommController.Builder(Log4J.getInstance())
                    .setServerAddress(serverAddress + ports[i])
                    .setSessionId(sessionId)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setTCPon(isTCPon)
                    .setResponseListener( processResponse )
                     .setShouldProcessReply(true)
                    .build();
             clientCommControllerHashMap.put(sessionId, clientCommController);
             responseListenerHashMap.put(sessionId, processResponse);
        }


        await().atMost(timeout, TimeUnit.MILLISECONDS).until( () -> ((ProcessResponse)responseListenerHashMap.values().toArray()[0]).allConnected.get() );
        await().atMost(timeout, TimeUnit.MILLISECONDS).until( () -> ((ProcessResponse)responseListenerHashMap.values().toArray()[1]).allConnected.get() );

        for(String sessionId : sessionIds){
            SessionMessage message = new SessionMessage( messageId1, "Message from client : " + uniqueMsgId, sessionId );
            clientCommControllerHashMap.get(sessionId).send(sessionId, message);
        }
        CommonUtils.sleep( delay * 2 );
        await().untilTrue( new AtomicBoolean( checkAsyncCall));

        for(String sessionId : sessionIds){
            SessionMessage message = new SessionMessage( Constants.REQUEST_DISCONNECT, "Message from client : " + uniqueMsgId, sessionId );
            clientCommControllerHashMap.get(sessionId).disconnect(sessionId);
        }
        MUFLifetimeManager.stopFramework( muf );
    }


    @Test
    public void testTwoClientsWithTCP_RemoteService() throws Throwable{
        runClientWithRemoteServiceComponent( true, 3, "client-session-1", "client-session-2");
    }

    @Test
    public void testOneClientsWithTCP_RemoteService() throws Throwable{
        runClientWithRemoteServiceComponent( true, 2, "client-session-123");
    }

    private void runClientWithRemoteServiceComponent(boolean isTCPon, int i, String... sessionIds) throws Throwable{
        AtomicInteger countConnected = new AtomicInteger(0);
        AtomicBoolean allConnected = new AtomicBoolean(false);
        long timeout = 1000 * 10; // ten seconds
        String messageId1 = "MSG_INITIAL_REQUEST", messageId2 = "MSG_COMPONENT_1",
                messageId3 = "MSG_SEND_RESPONSE";
        long uniqueMsgId = System.currentTimeMillis();
        checkAsyncCall = false;
        // let's add some dynamic subcriptions to the orchestrator
        CommonUtils.addOrChangeAnnotation(TestOrchestrator.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId1, messageId3 });
        CommonUtils.addOrChangeAnnotation(TestPluggableComponent.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId2 });
        // creates a MUF and set TCP to on or off
        MultiuserController muf = MUFLifetimeManager.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfigWithServices( serverAddress, ports[i] ).setTCPon( isTCPon ) );
        assertNotNull(muf);
        CommonUtils.sleep(delay); //give some time to initialize the MUF
        // let's create a client that sends messages to MUF
        for(String sessionId : sessionIds ) {
            processResponse = new ProcessResponse(countConnected, allConnected, sessionIds.length, sessionId);
            clientCommController = new ClientCommController.Builder(Log4J.getInstance())
                    .setServerAddress(serverAddress + ports[i])
                    .setSessionId(sessionId)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setTCPon(isTCPon)
                    .setResponseListener( processResponse )
                    .setShouldProcessReply(true)
                    .build();
            clientCommControllerHashMap.put(sessionId, clientCommController);
            responseListenerHashMap.put(sessionId, processResponse);
            CommonUtils.sleep(1000);
        }

        int size = clientCommControllerHashMap.size();

        for(j=0;j<size;j++) {
            await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> ((ProcessResponse) responseListenerHashMap.values().toArray()[j]).allConnected.get());
        }

        for(String sessionId : sessionIds){
            SessionMessage message = new SessionMessage( messageId1, "Message from client : " + uniqueMsgId, sessionId );
            clientCommControllerHashMap.get(sessionId).send(sessionId, message);
        }
        CommonUtils.sleep( delay * 2 );
        for(String sessionId : sessionIds){
            SessionMessage message = new SessionMessage( messageId1, "Message from client : " + uniqueMsgId, sessionId );
            clientCommControllerHashMap.get(sessionId).disconnect(sessionId);
        }

        CommonUtils.sleep( delay * 2 );

        await().untilTrue( new AtomicBoolean( checkAsyncCall));

        MUFLifetimeManager.stopFramework( muf );
    }

    @Test
    public void testOneClientsWithTCP_RemoteServiceReconection() throws Throwable{
        runClientWithRemoteServiceComponentReconnect( true, 2, "client-session-1");
    }

    private void runClientWithRemoteServiceComponentReconnect(boolean isTCPon, int i, String... sessionIds) throws Throwable{
        AtomicInteger countConnected = new AtomicInteger(0);
        AtomicBoolean allConnected = new AtomicBoolean(false);
        long timeout = 1000 * 10; // ten seconds
        String messageId1 = "MSG_INITIAL_REQUEST", messageId2 = "MSG_COMPONENT_1",
                messageId3 = "MSG_SEND_RESPONSE";
        long uniqueMsgId = System.currentTimeMillis();
        checkAsyncCall = false;
        // let's add some dynamic subcriptions to the orchestrator
        CommonUtils.addOrChangeAnnotation(TestOrchestrator.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId1, messageId3 });
        CommonUtils.addOrChangeAnnotation(TestPluggableComponent.class.getAnnotation(BlackboardSubscription.class), "messages",
                new String[]{ messageId2 });
        // creates a MUF and set TCP to on or off
        MultiuserController muf = MUFLifetimeManager.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfigWithServices( serverAddress, ports[i] ).setTCPon( isTCPon ) );
        assertNotNull(muf);
        CommonUtils.sleep(delay); //give some time to initialize the MUF
        // let's create a client that sends messages to MUF
        for(String sessionId : sessionIds ) {
            processResponse = new ProcessResponse(countConnected, allConnected, sessionIds.length, sessionId);
            clientCommController = new ClientCommController.Builder(Log4J.getInstance())
                    .setServerAddress(serverAddress + ports[i])
                    .setSessionId(sessionId)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setTCPon(isTCPon)
                    .setResponseListener( processResponse )
                    .setShouldProcessReply(true)
                    .build();
            clientCommControllerHashMap.put(sessionId, clientCommController);
            responseListenerHashMap.put(sessionId, processResponse);
        }

        int size = clientCommControllerHashMap.size();

        for(j=0;j<size;j++) {
            await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> ((ProcessResponse) responseListenerHashMap.values().toArray()[j]).allConnected.get());
        }

        for(String sessionId : sessionIds){
            SessionMessage message = new SessionMessage( messageId1, "Message from client : " + uniqueMsgId, sessionId );
            clientCommControllerHashMap.get(sessionId).send(sessionId, message);
        }
        CommonUtils.sleep( delay * 2 );
        for(String sessionId : sessionIds){
            SessionMessage message = new SessionMessage( messageId1, "Message from client : " + uniqueMsgId, sessionId );
            clientCommControllerHashMap.get(sessionId).disconnect(sessionId);
        }
        CommonUtils.sleep( delay * 10 );

        clientCommControllerHashMap.clear();
        responseListenerHashMap.clear();
        for(String sessionId : sessionIds ) {
            processResponse = new ProcessResponse(countConnected, allConnected, sessionIds.length, sessionId);
            clientCommController = new ClientCommController.Builder(Log4J.getInstance())
                    .setServerAddress(serverAddress + ports[i])
                    .setSessionId(sessionId)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setTCPon(isTCPon)
                    .setResponseListener( processResponse )
                    .setShouldProcessReply(true)
                    .build();
            clientCommControllerHashMap.put(sessionId, clientCommController);
            responseListenerHashMap.put(sessionId, processResponse);
        }
        size = clientCommControllerHashMap.size();

        for(j=0;j<size;j++) {
            await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> ((ProcessResponse) responseListenerHashMap.values().toArray()[j]).allConnected.get());
        }
        CommonUtils.sleep( delay * 2 );

        for(String sessionId : sessionIds){
            SessionMessage message = new SessionMessage( messageId1, "Message from client : " + uniqueMsgId, sessionId );
            clientCommControllerHashMap.get(sessionId).send(sessionId, message);
        }
        CommonUtils.sleep( delay * 2 );

        await().untilTrue( new AtomicBoolean( checkAsyncCall));

        MUFLifetimeManager.stopFramework( muf );
    }


    //@Test
    public void testOnlyClient(){
        try {
            ClientCommController client = new ClientCommController.Builder(Log4J.getInstance())
                    .setServerAddress(serverAddress + ports[5])
                    .setServiceName("session-1")
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setTCPon(true)
                    .build();
            // this method will be executed asynchronuously, so we need to add a delay before stopping the MUF
            client.setResponseListener(message -> {
                SessionMessage sessionMessage = CommonUtils.fromJson(message, SessionMessage.class);
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

    class ProcessResponse implements ResponseListener{
        private AtomicInteger countConnected;
        public AtomicBoolean allConnected;
        private int totalSessions;
        private String sessionId;

        public ProcessResponse(AtomicInteger countConnected, AtomicBoolean allConnected, int totalSessions, String sessionId) {
            this.countConnected = countConnected;
            this.allConnected = allConnected;
            this.totalSessions = totalSessions;
            this.sessionId = sessionId;
        }

        @Override
        public void process(String message) {
            try {
                if( message.contains(Constants.SESSION_INITIATED) ){
                    countConnected.incrementAndGet();
                    if( countConnected.get() == totalSessions ){
                        allConnected.set(true);
                    }
                }
                SessionMessage sessionMessage = CommonUtils.fromJson(message, SessionMessage.class);
                assertNotNull(sessionMessage);
                if (!sessionMessage.getRequestType().equals(Constants.SESSION_CLOSED)) {
                    //assertEquals("Response from MUF : " + uniqueMsgId, sessionMessage.getPayload());
                }
                //client.send(sessionId, new SessionMessage(Constants.REQUEST_DISCONNECT, "" + uniqueMsgId, sessionId));

                Log4J.info(ResponseListener.class, "1. expected and received messages are the same");
                MUFTestSuite.checkAsyncCall = true;
            } catch (Throwable e) {
                ExceptionHandler.handle(e);
            }
        }
    }
}
