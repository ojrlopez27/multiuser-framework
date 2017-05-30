package edu.cmu.inmind.multiuser.controller.session;

import com.google.common.util.concurrent.ServiceManager;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.communication.*;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
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
    private Map<String, Session> sessions;
    private ServerCommController serverCommController;
    private Config config;
    private Thread thread;
    private ZMsg reply;
    private String serviceId = Constants.SESSION_MANAGER_SERVICE;

    public SessionManager(PluginModule[] modules, Config config, ServiceInfo serviceInfo){
        this.config = config;
        if( modules == null || modules.length == 0 ){
            throw new NullPointerException( "Parameter \"modules\" cannot be null nor empty!" );
        }
        if( serviceInfo != null ){
            createFrameworkAsService(serviceInfo);
        }
        extractConfig();
        ResourceLocator.initializeBroker();
        sessions = new HashMap<>();
        serverCommController = new ServerCommController( Constants.FULL_ADDRESS, serviceId, null);
        DependencyManager.getInstance(modules);
    }

    private void extractConfig() {
        Constants.SESSION_MANAGER_PORT = Config.getSessionManagerPort();
    }

    /**
     * It waits for new clients that want to connect to multiuser. Once a request is received, the system creates an
     * instance of ServerCommController which will start receiving/sending results to the client.
     */
    public void run(){
        Log4J.info(this, "Starting Multiuser framework...");
        try {
            reply = null;
            while (!Thread.currentThread().isInterrupted()) {
                processRequest( );
            }
        }catch (Exception e){
            ExceptionHandler.handle(e);
        }finally{
            boolean done = false;
            while( !done ) {
                done = true;
                for (ServiceManager serviceManager : ResourceLocator.getServiceManagers().keySet()) {
                    if( !ResourceLocator.getServiceManagers().get(serviceManager).equals(Constants.SERVICE_MANAGER_STOPPED) ){
                        done = false;
                        break;
                    }
                }
                Utils.sleep(500);
            }
            System.err.println("Session Manager stopped. Bye bye!");
            System.exit(0);
        }
    }


    /**
     * It processes requests from client related to the Session lifecycle: connect, disconnect, pause and resume;
     * and also from remote services
     */
    private void processRequest( ) throws Exception{
        ZMsgWrapper msgRequest = serverCommController.receive( reply );
        SessionMessage request = getServerRequest(msgRequest);
        Session session = sessions.get(request.getSessionId());
        if( session != null ){
            if( request.getRequestType().equals( Constants.REQUEST_PAUSE) ){
                pause(session, msgRequest);
            }else if( request.getRequestType().equals( Constants.REQUEST_RESUME) ){
                resume(session, msgRequest);
            }else if( request.getRequestType().equals( Constants.REQUEST_DISCONNECT) ){
                disconnect(session, msgRequest);
            }else if( request.getRequestType().equals( Constants.REQUEST_CONNECT) ){
                reconnect(msgRequest, request, session);
            }else{
                serverCommController.send( msgRequest, new SessionMessage(Constants.RESPONSE_NOT_VALID_OPERATION) );
            }
        }else if( request.getRequestType().equals( Constants.REQUEST_CONNECT ) ){
            //if session doesn't exist, SessionManager can only create a new session
            createSession(msgRequest, request);
        }else if( request.getRequestType().equals( Constants.REGISTER_REMOTE_SERVICE) ){
            registerRemoteService(request, msgRequest);
        }else if( request.getRequestType().equals( Constants.UNREGISTER_REMOTE_SERVICE) ){
            unregisterRemoteService(request, msgRequest);
        }else{
            serverCommController.send( msgRequest, new SessionMessage(Constants.RESPONSE_UNKNOWN_SESSION) );
        }
    }

    private void resume(Session session, ZMsgWrapper msgRequest) {
        Log4J.info(this, "Resuming session: " + session.getId());
        session.resume();
        serverCommController.send( msgRequest, new SessionMessage(Constants.SESSION_RESUMED) );
    }

    private void pause(Session session, ZMsgWrapper msgRequest) {
        Log4J.info(this, "Pausing session: " + session.getId());
        session.pause();
        serverCommController.send( msgRequest, new SessionMessage(Constants.SESSION_PAUSED) );
    }

    private void disconnect(Session session, ZMsgWrapper msgRequest) throws Exception{
        Log4J.info(this, "Disconnecting session: " + session.getId());
        session.close( );
        sessions.remove( session.getId() );
        serverCommController.send( msgRequest, new SessionMessage(Constants.SESSION_CLOSED) );
    }

    private void unregisterRemoteService(SessionMessage request, ZMsgWrapper msgRequest) {
        Log4J.info(this, "Unregistering service: " + request.getSessionId());
        ResourceLocator.unregisterService(request);
        serverCommController.send( msgRequest, new SessionMessage(Constants.RESPONSE_REMOTE_UNREGISTERED) );
    }

    private void registerRemoteService(SessionMessage request, ZMsgWrapper msgRequest) {
        Log4J.info(this, "Registering service: " + request.getSessionId());
        ResourceLocator.registerService(request, msgRequest, request.getPayload());
        serverCommController.send( msgRequest, new SessionMessage(Constants.RESPONSE_REMOTE_REGISTERED) );
    }

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
        session.setId(key, msgRequest);
        sessions.put( key, session );
        serverCommController.send( msgRequest, new SessionMessage( Constants.SESSION_INITIATED) );
        Log4J.info(this, "Creating session: " + session.getId());
    }

    private void createFrameworkAsService(ServiceInfo serviceInfo) {
        ClientCommController clientCommController = new ClientCommController(
                serviceInfo.getServerAddress(),
                serviceInfo.getServiceName(),
                serviceInfo.getClientAddress(),
                Constants.REGISTER_REMOTE_SERVICE,
                serviceInfo.getMsgWrapper(),
                serviceInfo.getMsgSubscriptions());
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
                MultiuserFramework.stop();
            }
        });
    }

    private void reconnect(ZMsgWrapper msgRequest, SessionMessage request, Session session){
        Log4J.info(this, "Reconnecting session: " + session.getId() + " as per request " + request.getSessionId());
        if(request.getSessionId().equals(session.getId())){
            serverCommController.send( msgRequest, new SessionMessage(Constants.SESSION_RECONNECTED) );
        } else {
            serverCommController.send( msgRequest, new SessionMessage(Constants.RESPONSE_ALREADY_CONNECTED) );
        }
    }

    /**
     * It disconnects all sessions, closes all sockets and stop the multiuser framework.
     */
    public void stop() throws Exception{
        Log4J.info(this, "Start closing all sessions...");
        for( Session session : sessions.values() ){
            session.close();
        }
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setRequestType( Constants.REQUEST_SHUTDOWN_SYSTEM );
        sessionMessage.setMessageId( Constants.SESSION_MANAGER_SERVICE );
        for( ServiceComponent serviceComponent : ResourceLocator.getServiceRegistry().values() ){
            serverCommController.send( serviceComponent.getMsgTemplate(), sessionMessage );
        }
        serverCommController.close();
        ResourceLocator.getBroker().close();
    }

    public void start() {
        thread = new Thread( this, "SessionManagerThread" );
        thread.start();
    }
}
