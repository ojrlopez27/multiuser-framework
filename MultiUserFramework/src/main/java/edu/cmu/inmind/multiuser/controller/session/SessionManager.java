package edu.cmu.inmind.multiuser.controller.session;

import com.google.common.util.concurrent.ServiceManager;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Pair;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.*;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import org.zeromq.ZMsg;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by oscarr on 3/3/17.
 * This class will control the sessions lifecycle (connect, disconnect, pause, resume)
 */
public class SessionManager implements Runnable{
    /** sessions handled by the session manager */
    private Map<String, Session> sessions;
    /** communication controller that process
     * lifecycle request messages (connect a client, disconnect, etc.)*/
    private ServerCommController serverCommController;
    private Config config;
    /** the session manager runs on its own thread */
    private Thread thread;
    /** message that is used to reply to clients */
    private ZMsg reply;
    /** this is the id of the session manager. we use it to filter messages that must be
     * processed by the session manager
     */
    private String serviceId = Constants.SESSION_MANAGER_SERVICE;

    private Broker broker;

    private int port;
    private String address;
    private String fullAddress;
    private boolean stopped;
    private ClientMessage clientMessage;


    public SessionManager(PluginModule[] modules, Config config, ServiceInfo serviceInfo) throws Throwable{
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
        sessions = new HashMap<>();
        extractConfig();
        if( config.isTCPon() ) {
            initializeBroker();
            serverCommController = new ServerCommController(fullAddress, serviceId, null);
        }else{
            clientMessage = new ClientMessage();
        }
        DependencyManager.getInstance(modules);
    }

    /**
     * settings information that belongs to the session manager
     */
    private void extractConfig(){
        port = config.getSessionManagerPort();
        address = config.getServerAddress(); //"tcp://*";
        fullAddress = address + port;
        // ...
    }

    public void initializeBroker(){
        broker = new Broker(port);
        // Can be called multiple times with different endpoints
        broker.start();
    }

    /**
     * It waits for new clients that want to connect to MUF. Once a request is received, the system creates an
     * instance of ServerCommController which will start receiving/sending results to the client.
     */
    public void run(){
        Log4J.info(this, "Starting Multiuser framework...");
        try {
            reply = null;
            while (!Thread.currentThread().isInterrupted() && !stopped ) {
                processRequest( );
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }finally{
            boolean done = false;
            while( !done ) {
                done = true;
                for (ServiceManager serviceManager : ResourceLocator.getServiceManagers().keySet()) {
                    // if the sever manager has stopped, we are done!
                    if( !ResourceLocator.getServiceManagers().get(serviceManager).equals(Constants.SERVICE_MANAGER_STOPPED) ){
                        done = false;
                        break;
                    }
                }
                Utils.sleep(500);
            }
            Log4J.info(this, "Session Manager stopped. Bye bye!");
            if( config.executeExit() ) {
                System.exit(0);
            }
        }
    }


    /**
     * It processes requests from clients related to the session lifecycle: connect, disconnect, pause and resume;
     * and also requests from remote services.
     */
    private void processRequest( ) throws Throwable{
        if( !stopped ) {
            ZMsgWrapper msgRequest = null;
            SessionMessage request = null;
            if( config.isTCPon() ){
                msgRequest = serverCommController.receive(reply);
                request = getServerRequest(msgRequest);
            }else{
                request = getClientMessage();
            }
            Session session = sessions.get(request.getSessionId());
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
                send(msgRequest, new SessionMessage(Constants.RESPONSE_UNKNOWN_SESSION));
            }
        }
    }

    private SessionMessage getClientMessage() {
        Pair<String, Object> message = clientMessage.get();
        return (SessionMessage) message.snd;
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
        clientMessage.put( new Pair<>(sessionId, message) );
    }

    private void resume(Session session, ZMsgWrapper msgRequest) {
        Log4J.info(this, "Resuming session: " + session.getId());
        session.resume();
        send( msgRequest, new SessionMessage(Constants.SESSION_RESUMED) );
    }

    private void pause(Session session, ZMsgWrapper msgRequest) {
        Log4J.info(this, "Pausing session: " + session.getId());
        session.pause();
        send( msgRequest, new SessionMessage(Constants.SESSION_PAUSED) );
    }

    private void disconnect(Session session, ZMsgWrapper msgRequest) throws Throwable{
        Log4J.info(this, "Disconnecting session: " + session.getId());
        session.close( );
        sessions.remove( session.getId() );
        send( msgRequest, new SessionMessage(Constants.SESSION_CLOSED) );
    }

    /**
     * Remote services can register and unregister with the session manager through the Resource Locator.
     * @param request
     * @param msgRequest
     */
    private void registerRemoteService(SessionMessage request, ZMsgWrapper msgRequest) {
        Log4J.info(this, "Registering service: " + request.getSessionId());
        ResourceLocator.registerService(request, msgRequest, request.getPayload());
        send( msgRequest, new SessionMessage(Constants.RESPONSE_REMOTE_REGISTERED) );
    }

    private void unregisterRemoteService(SessionMessage request, ZMsgWrapper msgRequest) {
        Log4J.info(this, "Unregistering service: " + request.getSessionId());
        ResourceLocator.unregisterService(request);
        send( msgRequest, new SessionMessage(Constants.RESPONSE_REMOTE_UNREGISTERED) );
    }

    /**
     * extracts the message (which comes as byte array format) and parses it to an instance of SessionMessage
     * @param msgRequest
     * @return
     */
    private SessionMessage getServerRequest(ZMsgWrapper msgRequest) {
        if( msgRequest != null && msgRequest.getMsg().peekLast() != null ) {
            return Utils.fromJson(msgRequest.getMsg().peekLast().toString(), SessionMessage.class);
        }
        return new SessionMessage();
    }

    /**
     * Once a new session instance is created (for a specific client), this method returns to that client the port
     * number on which it should be sending (pushing information to) and listening (pulling information from).
     */
    private void createSession(ZMsgWrapper msgRequest, SessionMessage request) {
        String key = request.getSessionId();
        Session session = DependencyManager.getInstance().getComponent(Session.class);
        session.setConfig( config );
        session.setId(key, msgRequest, fullAddress);
        sessions.put( key, session );
        send( msgRequest, new SessionMessage( Constants.SESSION_INITIATED) );
        Log4J.info(this, "Creating session: " + session.getId());
    }

    /**
     * If the MUF behaves a slave MUF, it must be created as a service that registers itself with the
     * master MUF.
     * @param serviceInfo
     */
    private void createFrameworkAsService(ServiceInfo serviceInfo) {
        ClientCommController clientCommController = new ClientCommController.Builder()
                .setServerAddress(serviceInfo.getServerAddress())
                .setServiceName(serviceInfo.getServiceName())
                .setClientAddress( serviceInfo.getClientAddress() )
                .setMsgTemplate( serviceInfo.getMsgWrapper() )
                .setSubscriptionMessages( serviceInfo.getMsgSubscriptions() )
                .setRequestType( Constants.REGISTER_REMOTE_SERVICE )
                .build();
        Log4J.info(this, "Creating new service as a framework: " + serviceInfo.getServiceName());

        // let's process the response
        clientCommController.receive(message -> {
            SessionMessage sessionMessage = Utils.fromJson( message, SessionMessage.class );
            String messageId = sessionMessage.getMessageId();
            sessionMessage.setMessageId("");
            serviceInfo.getResponseListener().process( Utils.toJson(sessionMessage) );
            if( sessionMessage.getRequestType().equals( Constants.REQUEST_SHUTDOWN_SYSTEM )
                    && messageId.equals( Constants.SESSION_MANAGER_SERVICE) ){
                clientCommController.close();
                //MultiuserFramework.stop();
            }
        });
    }

    private void reconnect(ZMsgWrapper msgRequest, SessionMessage request, Session session){
        Log4J.info(this, "Reconnecting session: " + session.getId() + " as per request "
                + request.getSessionId());
        if(request.getSessionId().equals(session.getId())){
            send( msgRequest, new SessionMessage(Constants.SESSION_RECONNECTED) );
        } else {
            send( msgRequest, new SessionMessage(Constants.RESPONSE_ALREADY_CONNECTED) );
        }
    }

    private void send(ZMsgWrapper msgRequest, SessionMessage request){
        if( serverCommController != null ){
            serverCommController.send( msgRequest, request );
        }
    }

    /**
     * It disconnects all sessions, closes all sockets and stop the multiuser framework.
     */
    public void stop() throws Throwable{
        stopped = true;
        Log4J.info(this, "Start closing all sessions...");
        for( Session session : sessions.values() ){
            session.close();
        }
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setRequestType( Constants.REQUEST_SHUTDOWN_SYSTEM );
        sessionMessage.setMessageId( Constants.SESSION_MANAGER_SERVICE );
        for( ServiceComponent serviceComponent : ResourceLocator.getServiceRegistry().values() ){
            send( serviceComponent.getMsgTemplate(), sessionMessage );
        }
        if( config.isTCPon() ) {
            serverCommController.close();
            broker.close();
        }
        thread.interrupt();
    }

    /**
     * MUF runs on its own separate thread
     */
    public void start() {
        thread = new Thread( this, "SessionManagerThread" );
        thread.start();
    }
}
