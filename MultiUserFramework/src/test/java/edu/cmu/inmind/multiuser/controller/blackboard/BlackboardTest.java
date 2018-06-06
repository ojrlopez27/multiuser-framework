package edu.cmu.inmind.multiuser.controller.blackboard;

import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.FileLogger;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.plugin.Pluggable;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;
import edu.cmu.inmind.multiuser.controller.session.Session;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import edu.cmu.inmind.multiuser.controller.session.SessionImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BlackboardTest {

    private Blackboard blackboard;

    // sample messages that would be subscribed to the blackboard
    private static final String MSG_ASR = "MSG_ASR";
    private static final String MSG_NLU = "MSG_NLU";
    private static final String MSG_SEND_RESPONSE = "MSG_SEND_RESPONSE";

    // sample messages to be posted on the blackboard
    private static final String SAY_HELLO = "Say Hello!";
    private static final String TEST_MESSAGE = "Test message";
    private static final String MUF_RESPONSE = "Response from MUF: ";
    private static final String MUF_RESPONSE_BLANK = "Blank Response from MUF";

    // logs
    private static final String LOG_PATH = CommonUtils.getProperty("pathLogs", "/logs");
    private static final String LOG_RECEIVED_EVENT = "Received Event: ";
    private static final String UNIQUE_MSG_ID = "uniqueMsgID: ";

    // dummy session identifier
    private static final String DUMMY_SESSION_ID = "DUMMY_SESSION_ID";

    // external component: server and client address
    private static final String SERVICE_URL = "tcp://127.0.0.1:5555";
    // private static final String CLIENT_ADDRESS = "tcp://127.0.0.1:5556";

    @Before
    public void setUp() throws Throwable {

        CommonUtils.initThreadExecutor();

        // create a blackboard object
        // and set the loggers
        blackboard = new BlackboardImpl(createLogger());
        blackboard.setShouldThrowException(true);
        setExceptionHandlerLogger();

        // assert that the blackboard object is created
        // and that the loggers are not null
        Assert.assertNotNull(blackboard);
        Assert.assertNotNull(blackboard.getLogger());
        Assert.assertNotNull(ExceptionHandler.getLogger());
    }

    @Test
    public void testParameterizedConstructor() throws Throwable {

        // create a blackboard object, set the loggers, and create the components
        blackboard = new BlackboardImpl(createComponents(),
                createDummySession().getId(),
                createLogger());
        setExceptionHandlerLogger();

        // assert that the blackboard object is created
        Assert.assertNotNull(blackboard);

        // assert that the components have been subscribed
        Assert.assertNotNull(blackboard.getSubscribers());
        Assert.assertEquals(blackboard.getSubscribers().length, 1);

        // assert that the loggers are not null
        Assert.assertNotNull(blackboard.getLogger());
        Assert.assertNotNull(ExceptionHandler.getLogger());
    }

    @Test
    public void setComponents() throws Throwable {

        // create a dummy session and set the blackboard components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // assert that the components have been subscribed
        Assert.assertNotNull(blackboard.getSubscribers());
        Assert.assertEquals(blackboard.getSubscribers().length, 1);
    }

    @Test (expected = MultiuserException.class)
    public void testSetComponentsNoneSubscribed() throws Throwable {

        // create a dummy session and set the blackboard components
        Session session = new SessionImpl();
        blackboard.setComponents(null, session.getId());

        // assert that no components have been subscribed
        Assert.assertEquals(blackboard.getSubscribers().length, 0);
        Assert.assertEquals(blackboard.getSubscriptions().size(), 0);
    }

    @Test (expected = MultiuserException.class)
    public void testSetComponentsEmpty() throws Throwable {

        // create a dummy session and set the blackboard components
        Session session = createDummySession();
        Set<Pluggable> components = new CopyOnWriteArraySet<>();
        blackboard.setComponents(components, session.getId());

        // assert that no components have been subscribed
        Assert.assertEquals(blackboard.getSubscribers().length, 0);
        Assert.assertEquals(blackboard.getSubscriptions().size(), 0);
    }

    @Test
    public void setKeepModel() throws Throwable {
        // by default keep model is false
        blackboard.setKeepModel(true);

        // assert that the model is kept
        Assert.assertTrue(blackboard.isModelKept());

        // don't keep the model and assert that it is not kept
        blackboard.setKeepModel(Boolean.FALSE);
        Assert.assertFalse(blackboard.isModelKept());
    }

    @Test
    public void setNotifySubscribers() throws Throwable {

        // assert that the subscribers are notified
        Assert.assertTrue(blackboard.areSubscribersNotified());

        // do not notify subscribers and assert that it is false
        blackboard.setNotifySubscribers(Boolean.FALSE);
        Assert.assertFalse(blackboard.areSubscribersNotified());

        // reset the subscriber notification to true
        blackboard.setNotifySubscribers(Boolean.TRUE);
    }

    @Test
    public void getLogger() throws Throwable {

        // assert that the logger is not null
        Assert.assertNotNull(blackboard.getLogger());
    }

    @Test
    public void setLogger() throws Throwable {

        // create a file logger and assign it to the blackboard
        // blackboard.setLogger(createLogger());

        // assert that the logger is not null and an instance of FileLogger
        Assert.assertNotNull(blackboard.getLogger());
        Assert.assertTrue(blackboard.getLogger() instanceof MessageLog);
    }

    @Test
    public void isLoggerOn() throws Throwable {

        // assert that the logger is on
        Assert.assertTrue(blackboard.isLoggerOn());
    }

    @Test
    public void setLoggerOn() throws Throwable {

        // assert that the state of the logger is modified
        blackboard.setLoggerOn(Boolean.FALSE);
        Assert.assertFalse(blackboard.isLoggerOn());
    }

    @Test
    public void setModel() throws Throwable {

        // set the model and assert that it is not null
        blackboard.setModel(new ConcurrentHashMap<>());
        Assert.assertNotNull(blackboard.getModel());
    }

    @Test (expected = MultiuserException.class)
    public void postWhenSenderNull() throws Throwable {

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // post a message to the blackboard when the sender is null
        blackboard.post(null, MSG_SEND_RESPONSE, MUF_RESPONSE_BLANK);
    }

    @Test (expected = MultiuserException.class)
    public void postWhenKeyNull() throws Throwable {

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // create a messageable component and subscribe it to the blackboard
        PluggableComponent messageableComponent = createMsgASRPluggableComponent();
        blackboard.subscribe(messageableComponent);

        // post a message from the component to the blackboard when the key is null
        blackboard.post(messageableComponent, null, MUF_RESPONSE_BLANK);
    }

    @Test (expected = MultiuserException.class)
    public void postWhenElementNull() throws Throwable {

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // create a messageable component and subscribe it to the blackboard
        PluggableComponent messageableComponent = createMsgASRPluggableComponent();
        blackboard.subscribe(messageableComponent);

        // post message from the component to the blackboard when the element is null
        blackboard.post(messageableComponent, Constants.REMOVE_ALL, null);
        blackboard.post(messageableComponent, MSG_SEND_RESPONSE, null);
    }

    @Test
    public void postWhenKeepModelOn () throws Throwable {

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // create a messageable component and subscribe it to the blackboard with session
        PluggableComponent messageableComponent = createMsgASRPluggableComponent(session);
        blackboard.subscribe(messageableComponent);

        // post message from the component to the blackboard when the model is kept
        blackboard.setKeepModel(Boolean.TRUE);
        blackboard.post(messageableComponent, MSG_ASR, MUF_RESPONSE_BLANK);

        // assert that the message sent to the blackboard was kept in the model
        Assert.assertEquals(blackboard.getModel().size(), 1);
    }

    @Test
    public void postWhenKeepModelOff () throws Throwable {

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // create a messageable component and subscribe it to the blackboard with session
        PluggableComponent messageableComponent = createMsgASRPluggableComponent(session);
        blackboard.subscribe(messageableComponent);

        // post message from the component to the blackboard when the model is not kept
        blackboard.setKeepModel(Boolean.FALSE);
        blackboard.post(messageableComponent, MSG_ASR, MUF_RESPONSE_BLANK);

        // assert that the message sent to the blackboard was not kept in the model
        Assert.assertEquals(blackboard.getModel().size(), 0);
    }

    @Test
    public void getWhenValidKey() throws Throwable {
        blackboard.setKeepModel(true);

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // create a messageable component, subscribe it to the blackboard with session
        // and register the blackboard with the component
        PluggableComponent messageableComponent = createMsgASRPluggableComponent(session);
        messageableComponent.addBlackboard(session.getId(), blackboard);
        blackboard.subscribe(messageableComponent);

        // create another pluggable component, subscribe it to the blackboard with session
        // and register the blackboard with the component
        PluggableComponent sendMsgResponsePC = createSendMsgResponsePC(session);
        sendMsgResponsePC.addBlackboard(session.getId(), blackboard);
        blackboard.subscribe(sendMsgResponsePC);

        // post a message from the messageable component
        blackboard.post(messageableComponent, MSG_ASR,TEST_MESSAGE);

        // assert that the posted message is available with the blackboard
        Assert.assertEquals(blackboard.get(MSG_ASR), TEST_MESSAGE);
    }

    @Test (expected = MultiuserException.class)
    public void getWhenNull() throws Throwable {
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        PluggableComponent messageableComponent = createMsgASRPluggableComponent(session);
        blackboard.subscribe(messageableComponent);
        blackboard.get(null);
    }

    @Test
    public void getModel() throws Throwable {

        // assert that the model has no messages
        Assert.assertEquals(blackboard.getModel().size(), 0);

        // create a dummy session
        Session session = createDummySession();

        // create a messageable component, subscribe it to the blackboard with session
        // and register the blackboard with the component
        PluggableComponent messageableComponent = createMsgASRPluggableComponent(session);
        messageableComponent.addBlackboard(session.getId(), blackboard);
        blackboard.subscribe(messageableComponent);

        // create another pluggable component, subscribe it to the blackboard with session
        // and register the blackboard with the component
        PluggableComponent sendMsgResponsePC = createSendMsgResponsePC(session);
        sendMsgResponsePC.addBlackboard(session.getId(), blackboard);
        blackboard.subscribe(sendMsgResponsePC);
        blackboard.setKeepModel(true);

        // assert that the listener is subscribed
        Assert.assertNotNull(blackboard.getSubscribers());
        Assert.assertEquals(blackboard.getSubscribers().length, 2);

        // assert that the listener is added to the list of subscriptions
        Assert.assertNotNull(blackboard.getSubscriptions());
        Assert.assertEquals(blackboard.getSubscriptions().size(), 2);

        // post a message using the messageable component
        blackboard.post(messageableComponent, MSG_ASR, SAY_HELLO);

        // assert that the model has the SAY_HELLO message
        Assert.assertEquals(blackboard.getModel().size(), 1);
        Assert.assertEquals(blackboard.getModel().get(MSG_ASR), SAY_HELLO);
    }

    @Test
    public void testSubscribeWithoutMessages() throws Throwable {

        // create a pluggable component to listen to the blackboard
        BlackboardListener listener = createPluggableComponent();

        // subscribe an instance of the above pluggable component to the blackboard
        blackboard.subscribe(listener);

        // assert that the listener is subscribed
        Assert.assertNotNull(blackboard.getSubscribers());
        Assert.assertEquals(blackboard.getSubscribers().length, 1);
    }

    @Test
    public void testSubscribeWithMessages() throws Throwable {

        // create a pluggable component to listen to the blackboard
        // and it uses the blackboard subscription annotation
        // with the above message
        @BlackboardSubscription (messages = MSG_ASR)
        @StateType (state = Constants.STATELESS)
        class MessageablePluggableComponentOne extends PluggableComponent {
            @Override
            public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable {
                Log4J.info( this, LOG_RECEIVED_EVENT + event.getElement() );
                String uniqueMsgID = event.getElement().toString().split(" : ")[1];
                try {
                    blackboard.post(this, MSG_SEND_RESPONSE, MUF_RESPONSE + uniqueMsgID);
                }
                catch (Exception e) {
                    ExceptionHandler.handle(e);
                }
            }
        }

        // subscribe an instance of the above pluggable component to the blackboard
        BlackboardListener asrListener = new MessageablePluggableComponentOne();
        blackboard.subscribe(asrListener);

        // assert that the listener is subscribed
        Assert.assertNotNull(blackboard.getSubscribers());
        Assert.assertEquals(blackboard.getSubscribers().length, 1);

        // assert that the listener is added to the list of subscriptions
        Assert.assertNotNull(blackboard.getSubscriptions());
        Assert.assertEquals(blackboard.getSubscriptions().size(), 1);

        // assert that the listener is subscribed to the message on the blackboard
        Assert.assertEquals(blackboard.getSubscription(MSG_ASR).size(), 1);
        Assert.assertEquals(blackboard.getSubscription(MSG_ASR).get(0), asrListener);
        Assert.assertEquals(
                blackboard.getSubscription(MSG_ASR).get(0).getClass(),
                MessageablePluggableComponentOne.class);

        // create another pluggable component that listens
        // to the same message on the blackboard
        // and it uses the blackboard subscription annotation
        @BlackboardSubscription (messages = {MSG_ASR, MSG_NLU})
        @StateType (state = Constants.STATELESS)
        class MessageablePluggableComponentTwo extends PluggableComponent {
            @Override
            public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable {
                Log4J.info( this, LOG_RECEIVED_EVENT + event.getElement() );
                String uniqueMsgID = event.getElement().toString().split(" : ")[1];
                try {
                    blackboard.post(this, MSG_SEND_RESPONSE, MUF_RESPONSE + uniqueMsgID);
                }
                catch (Exception e) {
                    ExceptionHandler.handle(e);
                }
            }
        }

        // subscribe an instance of the above pluggable component to the blackboard
        BlackboardListener nluListener = new MessageablePluggableComponentTwo();
        blackboard.subscribe(nluListener);

        // assert that the listener is subscribed
        Assert.assertNotNull(blackboard.getSubscribers());
        Assert.assertEquals(blackboard.getSubscribers().length, 2);

        // assert that the listener is added to the list of subscriptions
        Assert.assertNotNull(blackboard.getSubscriptions());
        Assert.assertEquals(blackboard.getSubscriptions().size(), 2);

        // assert that the listeners are subscribed to the message on the blackboard
        Assert.assertEquals(blackboard.getSubscription(MSG_ASR).size(), 2);
        Assert.assertEquals(blackboard.getSubscription(MSG_ASR).get(1), nluListener);
        Assert.assertEquals(
                blackboard.getSubscription(MSG_ASR).get(1).getClass(),
                MessageablePluggableComponentTwo.class);
    }

    @Test(expected = MultiuserException.class)
    public void testSubscribeNull() throws Throwable {
        blackboard.subscribe(null);
    }

    @Test
    public void unsubscribe() throws Throwable {

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // create a messageable component and subscribe it to the blackboard
        PluggableComponent messageableComponent = createMsgASRPluggableComponent();
        blackboard.subscribe(messageableComponent);

        // assert that the messageable components has been subscribed
        Assert.assertEquals(blackboard.getSubscribers().length, 2);

        // unsubscribe the messageable component
        blackboard.unsubscribe(messageableComponent);

        // assert that the messageable components has been unsubscribed
        Assert.assertEquals(blackboard.getSubscribers().length, 1);
    }

    @Test
    public void getSubscribers() throws Throwable {

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // create a messageable component and subscribe it to the blackboard
        PluggableComponent messageableComponent = createMsgASRPluggableComponent();
        blackboard.subscribe(messageableComponent);

        // assert that the messageable components has been subscribed
        Assert.assertNotNull(blackboard.getSubscribers());
        Assert.assertEquals(blackboard.getSubscribers().length, 2);
    }

    @Test
    public void reset() throws Throwable {

        blackboard.setKeepModel( true );

        // create a dummy session and set the components
        Session session = createDummySession();
        blackboard.setComponents(createComponents(), session.getId());

        // create a messageable component and subscribe it to the blackboard with session
        PluggableComponent messageableComponent = createMsgASRPluggableComponent(session);
        blackboard.subscribe(messageableComponent);

        // post message from the component to the blackboard
        blackboard.post(messageableComponent, MSG_ASR, MUF_RESPONSE_BLANK);

        // assert that the message sent to the blackboard was kept in the model
        Assert.assertEquals(blackboard.getModel().size(), 1);

        // reset the blackboard
        blackboard.reset();

        // assert that the blackboard has no messages
        Assert.assertEquals(blackboard.getModel().size(), 0);

    }

    @Test
    public void remove() throws Throwable {
        // TODO
    }

    @Test
    public void getSyncEvent() throws Throwable {
        // TODO
    }

    private static Set<Pluggable> createComponents() {
        Set<Pluggable> components = new CopyOnWriteArraySet<>();
        components.add(createPluggableComponent());
        //components.add(createExternalComponent(new String[]{MSG_ASR, MSG_NLU}));
        return components;
    }

    private static Session createDummySession() {
        Session session = new SessionImpl();
        session.setId (DUMMY_SESSION_ID, new ZMsgWrapper(), SERVICE_URL);
        return session;
    }

    private static PluggableComponent createPluggableComponent() {
        return new PluggableComponent() {
            @Override
            public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable {
                Log4J.info( this, LOG_RECEIVED_EVENT + event.getElement() );
                String uniqueMsgID = event.getElement().toString().split(" : ")[1];
                try {
                    blackboard.post(this, MSG_SEND_RESPONSE, MUF_RESPONSE + uniqueMsgID);
                }
                catch (Exception e) {
                    ExceptionHandler.handle(e);
                }
            }
        };
    }

    private static PluggableComponent createMsgASRPluggableComponent (Session session) {
        return addSessionToPC (createMsgASRPluggableComponent(), session);
    }

    private static PluggableComponent createMsgASRPluggableComponent () {

        // create a pluggable component to listen to the blackboard
        // and it uses the blackboard subscription annotation
        @BlackboardSubscription (messages = MSG_ASR)
        @StateType (state = Constants.STATELESS)
        class MessageablePluggableComponent extends PluggableComponent {
            @Override
            public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable {
                Log4J.info( this, LOG_RECEIVED_EVENT + event.getElement() );
                String uniqueMsgID = event.getElement().toString();
                try {
                    blackboard.post(this, MSG_SEND_RESPONSE, MUF_RESPONSE + uniqueMsgID);
                }
                catch (Exception e) {
                    ExceptionHandler.handle(e);
                }
            }
        }

        return new MessageablePluggableComponent();
    }

    private static PluggableComponent createSendMsgResponsePC (Session session) {
        return addSessionToPC(createSendMsgResponsePC(), session);
    }

    private static PluggableComponent createSendMsgResponsePC () {

        // create a pluggable component to listen to the blackboard
        // and it uses the blackboard subscription annotation
        @BlackboardSubscription (messages = MSG_SEND_RESPONSE)
        @StateType (state = Constants.STATELESS)
        class SendMsgResponsePluggableComponent extends PluggableComponent {
            @Override
            public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable {
                Log4J.info( this, LOG_RECEIVED_EVENT + event.getElement() );
                String uniqueMsgID = event.getElement().toString();
                System.out.println(UNIQUE_MSG_ID + uniqueMsgID);
            }
        }

        return new SendMsgResponsePluggableComponent();
    }

    private static PluggableComponent addSessionToPC (PluggableComponent pc, Session session) {
        pc.addSession(session);
        pc.setActiveSession(session.getId());
        return pc;
    }

    private static MessageLog createLogger() {
        MessageLog logger = new FileLogger();
        logger.setId(DUMMY_SESSION_ID);
        logger.setPath(LOG_PATH);
        return logger;
    }

    private static void setExceptionHandlerLogger () {
        ExceptionHandler.checkPath(LOG_PATH);
        ExceptionHandler.setLog(LOG_PATH, FileLogger.class);
    }

    /*
    private static ExternalComponent createExternalComponent(String[] messages) {
        return new ExternalComponent(
                SERVICE_URL,
                CLIENT_ADDRESS,
                DUMMY_SESSION_ID,
                new ZMsgWrapper(),
                messages);
    }
    @Test(expected = MultiuserException.class)
    public void testSubscribeWithExternalComponentNullMessages() throws Throwable {
        ExternalComponent externalComponent = createExternalComponent(null);
        blackboard.subscribe(externalComponent);
    }
    @Test(expected = MultiuserException.class)
    public void testSubscribeWithExternalComponentNoMessages() throws Throwable {
        ExternalComponent externalComponent = createExternalComponent(new String[]{});
        blackboard.subscribe(externalComponent);
    }
    @Test
    public void testSubscribeWithExternalComponent() throws Throwable {
        ExternalComponent externalComponent = createExternalComponent(
                new String[]{MSG_ASR, MSG_NLU});
        blackboard.subscribe(externalComponent);
        // assert that the listener is subscribed
        Assert.assertNotNull(blackboard.getSubscribers());
        Assert.assertEquals(blackboard.getSubscribers().length, 1);
    }
*/

}