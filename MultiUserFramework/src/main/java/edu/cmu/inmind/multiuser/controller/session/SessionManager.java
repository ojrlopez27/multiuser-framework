package edu.cmu.inmind.multiuser.controller.session;

import com.google.common.util.concurrent.ServiceManager;
import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.communication.DestroyableCallback;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.exceptions.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.communication.*;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by oscarr on 3/3/17.
 * This class will control the sessions lifecycle (connect, disconnect, pause, resume)
 */
public class SessionManager implements CommonUtils.NamedRunnable, SessionImpl.SessionObserver, DestroyableCallback {
    /** sessions handled by the session manager */
    private ConcurrentHashMap<String, SessionImpl> sessions;
    private CopyOnWriteArrayList<Object> closeableObjects;
    /** communication controller that process
     * lifecycle request messages (connect a client, disconnect, etc.)*/
    private ServerCommController serverCommController;
    private Config config;
    /** message that is used to reply to clients */
    private ZMsg reply;
    /** this is the id of the session manager. we use it to filter messages that must be
     * processed by the session manager
     */
    private String serviceId = Constants.SESSION_MANAGER_SERVICE;
    private Broker[] brokers;
    private Broker managerBroker;
    private AtomicLong portIncrease = new AtomicLong(0);

    private int numOfPorts;
    private int sessionMngPort;
    private String address;
    private String fullAddress;
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private AtomicBoolean isDestroyed = new AtomicBoolean(false);
    private List<Object> postCreationList;


    public SessionManager(PluginModule[] modules, Config config, ServiceInfo serviceInfo) throws MultiuserException{
        closeableObjects = new CopyOnWriteArrayList<>();
        if( modules == null || modules.length <= 0 || config == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "modules: " + modules,
                    "config: " + config));
        }
        DependencyManager.getInstance(modules);
        this.config = config;
        if( config == null ){
            throw new MultiuserException(ErrorMessages.OBJECT_NULL, "config");
        }
        if( modules == null || modules.length == 0 ){
            throw new MultiuserException(ErrorMessages.OBJECT_NULL, "modules");
        }

        if( serviceInfo != null ){
            // if your MUF is a slave MUF
            createFrameworkAsService(serviceInfo);
        }
        sessions = new ConcurrentHashMap<>();
        extractConfig();
        if( config.isTCPon() ) {
            initializeBrokers();
            serverCommController = new ServerCommController(fullAddress, serviceId, null);
            closeableObjects.add(serverCommController);
        }
    }


    @Override
    public String getName(){
        return Constants.SESSION_MANAGER_SERVICE;
    }

    /**
     * settings information that belongs to the session manager
     */
    private void extractConfig(){
        sessionMngPort = config.getSessionManagerPort();
        address = config.getServerAddress(); //"tcp://*";
        fullAddress = (address.startsWith("tcp:") ? address : "tcp://" + address)
                    + (address.lastIndexOf(":") == address.length() - 1 ? sessionMngPort : ":" + sessionMngPort);
        // ...
    }

    /**
     * ZMQ docs: It can be extended to run multiple threads, each managing one socket and one set of clients and
     * workers. This could be interesting for segmenting large architectures. The C code is already organized around
     * a broker class to make this trivial.
     */
    public void initializeBrokers(){
        numOfPorts = config.getNumOfSockets();
        //if numOfPorts is <= 1, use always managerBroker
        if( numOfPorts > 1 ) {
            brokers = new Broker[numOfPorts];
            for (int i = 0; i < numOfPorts; i++) {
                // Can be called multiple times with different endpoints
                brokers[i] = new Broker(sessionMngPort + (i + 1));
                CommonUtils.execute(brokers[i]);
                closeableObjects.add(brokers[i]);
            }
        }
        managerBroker = new Broker(sessionMngPort);
        CommonUtils.execute( managerBroker );
        closeableObjects.add(managerBroker);
    }

    /**
     * It waits for new clients that want to connect to MUF. Once a request is received, the system creates an
     * instance of ServerCommController which will start receiving/sending results to the client.
     */
    public void run(){
        Log4J.info(this, "Starting Multiuser framework...");
        try {
            reply = null;
            loadRemoteServices();
            while (!isDestroyed.get() && !stopped.get() ) {
                processRequest( );
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }finally{
            boolean done = false;
            try {
                while (!done) {
                    done = true;
                    for (ServiceManager serviceManager : ResourceLocator.getServiceManagers().keySet()) {
                        // if the sever manager has stopped, we are done!
                        if (!ResourceLocator.getServiceManagers().get(serviceManager)
                                .equals(Constants.SERVICE_MANAGER_STOPPED)) {
                            done = false;
                            break;
                        }
                    }
                }
                ExceptionHandler.storeLog();
                if (config.executeExit()) {
                    System.exit(0);
                }
            }catch (Throwable e){
                ExceptionHandler.handle( e );
            }
        }
    }


    /**
     * It processes requests from clients related to the session lifecycle: connect, disconnect, pause and resume;
     * and also requests from remote services.
     */
    private void processRequest( ) throws Throwable{
        if( !stopped.get() ) {
            ZMsgWrapper msgRequest = null;
            SessionMessage request;
            if( config.isTCPon() ){
                msgRequest = serverCommController.receive(reply);
                request = getServerRequest(msgRequest);
            }else{
                request = new SessionMessage();
            }
            SessionImpl session = sessions.get(request.getSessionId());
            if (session != null) {
                if (request.getRequestType().equals(Constants.REQUEST_PAUSE)) {
                    pause(session, msgRequest);
                } else if (request.getRequestType().equals(Constants.REQUEST_RESUME)) {
                    resume(session, msgRequest);
                } else if (request.getRequestType().equals(Constants.REQUEST_DISCONNECT)) {
                    disconnect(session, msgRequest);
                } else if (request.getRequestType().equals(Constants.REQUEST_CONNECT)) {
                    reconnect(msgRequest, request, session);
                } else {
                    send(msgRequest, new SessionMessage(Constants.RESPONSE_NOT_VALID_OPERATION));
                }
            } else if (request.getRequestType().equals(Constants.REQUEST_CONNECT)) {
                //if session doesn't exist, SessionManager can only create a new session
                createSession(msgRequest, request);
            } else if (request.getRequestType().equals(Constants.REGISTER_REMOTE_SERVICE)) {
                registerRemoteService(request, msgRequest);
            } else if (request.getRequestType().equals(Constants.UNREGISTER_REMOTE_SERVICE)) {
                unregisterRemoteService(request, msgRequest);
            } else {
                if(request.getRequestType() != null && !request.getRequestType().isEmpty() ) {
                    send(msgRequest, new SessionMessage(Constants.RESPONSE_UNKNOWN_SESSION));
                }
            }
        }
    }

    private void loadRemoteServices() throws Throwable{
        try {
            ServiceInfoContainer container = CommonUtils.fromJsonFile(config.getServiceConfigPath(), ServiceInfoContainer.class);
            if( config.getServiceConfigPath() != null && (container.getServices() == null
                    || container.getServices().isEmpty()) ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.SERVICES_FILE_EMPTY, config.getServiceConfigPath()) );
            }
            if (container != null) {
                for (ServiceInfo serviceInfo : container.getServices()) {
                    registerRemoteService(serviceInfo);
                }
            }
        }catch (Exception e){
            ExceptionHandler.handle( new MultiuserException( ErrorMessages.FILE_NOT_EXISTS,
                    "Json Service Configuration (e.g., 'services.json')") );
            e.printStackTrace();
        }
    }

    /**
     * This method should be used only when working with TCP off
     * @param sessionId
     * @param message
     */
    public void send(String sessionId, Object message) throws Throwable{
        if( config.isTCPon() ){
            throw new MultiuserException( ErrorMessages.USE_TCP_INSTEAD, "send" );
        }
    }

    private void resume(SessionImpl session, ZMsgWrapper msgRequest) throws Throwable{
        Log4J.info(this, "Resuming session: " + session.getId());
        session.resume();
        send( msgRequest, new SessionMessage(Constants.SESSION_RESUMED) );
    }

    private void pause(SessionImpl session, ZMsgWrapper msgRequest) throws Throwable{
        Log4J.info(this, "Pausing session: " + session.getId());
        session.pause();
        send( msgRequest, new SessionMessage(Constants.SESSION_PAUSED) );
    }

    private void disconnect(SessionImpl session, ZMsgWrapper msgRequest) throws Throwable{
        Log4J.info(this, "Disconnecting session: " + session.getId());
        send( msgRequest, new SessionMessage(Constants.SESSION_CLOSED) );
        session.close( this );
        sessions.remove( session.getId() );
    }

    /**
     * Remote services can register and unregister with the session manager through the Resource Locator.
     * @param request
     * @param msgRequest
     */
    private void registerRemoteService(SessionMessage request, ZMsgWrapper msgRequest)  throws Throwable{
        Log4J.info(this, "Registering service: " + request.getSessionId());
        ResourceLocator.registerService(request, msgRequest, request.getPayload());
        if(msgRequest != null) send( msgRequest, new SessionMessage(Constants.RESPONSE_REMOTE_REGISTERED) );
    }


    private void registerRemoteService(ServiceInfo serviceInfo) throws Throwable{
        Log4J.info(this, "Registering service: " + serviceInfo.getServiceName());
        ResourceLocator.registerService(serviceInfo);
    }

    private void unregisterRemoteService(SessionMessage request, ZMsgWrapper msgRequest) throws Throwable {
        Log4J.info(this, "Unregistering service: " + request.getSessionId());
        ResourceLocator.unregisterService(request);
        send( msgRequest, new SessionMessage(Constants.RESPONSE_REMOTE_UNREGISTERED) );
    }

    /**
     * extracts the message (which comes as byte array format) and parses it to an instance of SessionMessage
     * @param msgRequest
     * @return
     */
    private SessionMessage getServerRequest(ZMsgWrapper msgRequest) throws Throwable{
        if( msgRequest != null && msgRequest.getMsg().peekLast() != null ) {
            return CommonUtils.fromJson(msgRequest.getMsg().peekLast().toString(), SessionMessage.class);
        }
        return new SessionMessage();
    }

    /**
     * Once a new session instance is created (for a specific client), this method returns to that client the port
     * number on which it should be sending (pushing information to) and listening (pulling information from).
     */
    private void createSession(ZMsgWrapper msgRequest, SessionMessage request) throws Throwable{
        //if numOfPorts is <= 1, then use sessionMngPort and only one broker (managerBroker)
        final int port = sessionMngPort + (int)(portIncrease.getAndIncrement() % numOfPorts) + (numOfPorts > 1? 1 : 0);
        final String address = fullAddress.replace("" + sessionMngPort, "" + port);
        String key = request.getSessionId();
        Log4J.info(this, "Creating session: " + key);
        SessionImpl session = DependencyManager.getInstance().getComponent(SessionImpl.class);
        session.setPostCreationList(postCreationList);
        session.onClose(this);
        session.setConfig( config );
        session.setId(key, msgRequest, address);
        ResourceLocator.addSession(session);
        sessions.put( key, session );
        closeableObjects.add(session);
        SessionMessage sm = new SessionMessage( Constants.SESSION_INITIATED);
        //sm.setPayload(address);
        sm.setPayload( String.format("tcp://%s:%s", CommonUtils.getPublicIP(), port) );
        send( msgRequest, sm );
    }

    /**
     * If the MUF behaves a slave MUF, it must be created as a service that registers itself with the
     * master MUF.
     * @param serviceInfo
     */
    private void createFrameworkAsService(final ServiceInfo serviceInfo) {
        try {
            final ClientCommController clientCommController = new ClientCommController.Builder(Log4J.getInstance())
                    .setServerAddress(serviceInfo.getMasterMUFAddress())
                    .setServiceName(serviceInfo.getServiceName())
                    .setSubscriptionMessages(serviceInfo.getMsgSubscriptions())
                    .setRequestType(Constants.REGISTER_REMOTE_SERVICE)
                    .build();
            Log4J.info(this, "Creating new service as a framework: " + serviceInfo.getServiceName());

            // let's process the response
            clientCommController.setResponseListener(new ResponseListener() {
                @Override
                public void process(String message) {
                    try {
                        SessionMessage sessionMessage = CommonUtils.fromJson(message, SessionMessage.class);
                        String messageId = sessionMessage.getMessageId();
                        sessionMessage.setMessageId("");
                        serviceInfo.getResponseListener().process(CommonUtils.toJson(sessionMessage));
                        if (sessionMessage.getRequestType().equals(Constants.REQUEST_SHUTDOWN_SYSTEM)
                                && messageId.equals(Constants.SESSION_MANAGER_SERVICE)) {
                            clientCommController.close(SessionManager.this);
                            //MultiuserFramework.stop();
                        }
                    }catch (Throwable e){
                        ExceptionHandler.handle(e);
                    }
                }
            });
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    private void reconnect(ZMsgWrapper msgRequest, SessionMessage request, SessionImpl session) throws Throwable{
        Log4J.info(this, "Reconnecting session: " + session.getId() + " as per request "
                + request.getSessionId());
        if(request.getSessionId().equals(session.getId())){
            send( msgRequest, new SessionMessage(Constants.SESSION_RECONNECTED) );
        } else {
            send( msgRequest, new SessionMessage(Constants.RESPONSE_ALREADY_CONNECTED) );
        }
    }

    private void send(ZMsgWrapper msgRequest, SessionMessage request) throws Throwable{
        if( serverCommController != null ){
            serverCommController.send( msgRequest, request );
        }
    }


    /**
     * MUF runs on its own separate thread
     */
    public void start() throws Throwable{
        CommonUtils.execute(this);
    }

    @Override
    public void notifyCloseSession(SessionImpl session) {
        if( session == null || sessions == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "session: " + session,
                    "sessions: " + sessions));
        }
        this.sessions.remove(session.getId());
    }

    /**
     * It disconnects all sessions, closes all sockets and stop the multiuser framework.
     */
    public void close(DestroyableCallback callback) throws Throwable{
        stopped.getAndSet(true);
        Log4J.info(this, "Start closing all external services (slave MUF's)...");
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setRequestType( Constants.REQUEST_SHUTDOWN_SYSTEM );
        sessionMessage.setMessageId( Constants.SESSION_MANAGER_SERVICE );
        for( ServiceComponent serviceComponent : ResourceLocator.getServiceRegistry().values() ){
            send( serviceComponent.getMsgTemplate(), sessionMessage );
        }
        Log4J.info(this, "Start closing all sessions...");
        for( SessionImpl session : sessions.values() ){
            session.close(this);
        }
        if( config.isTCPon() ) {
            serverCommController.close(this);
            if( numOfPorts > 0 && brokers != null ) {
                for (Broker broker : brokers) {
                    broker.close(this);
                }
            }
            managerBroker.close(this);
        }
    }

    @Override
    public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable {
        closeableObjects.remove(destroyedObj);
        if (closeableObjects.isEmpty()) {
            ResourceLocator.stopStatlessComp();
            CommonUtils.shutdownThreadExecutor();
            ResourceLocator.setIamDone(this);
            DependencyManager.getInstance().release();
            ResourceLocator.closeContexts();
            isDestroyed.getAndSet(true);
            Log4J.info(this, "Gracefully destroying...");
            Log4J.info(this, "Session Manager stopped. Bye bye!");
        }
    }

    public void addPostCreate(Object postCreationObj) {
        if( postCreationList == null ){
            postCreationList = new ArrayList<>();
        }
        postCreationList.add(postCreationObj);
    }
}
