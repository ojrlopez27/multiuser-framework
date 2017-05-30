package edu.cmu.inmind.multiuser.controller.plugin;

import com.google.common.util.concurrent.AbstractIdleService;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardListener;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.controller.sync.SynchronizableEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oscarr on 3/16/17.
 */
public abstract class PluggableComponent extends AbstractIdleService implements BlackboardListener, Pluggable {
    private Map<String, Blackboard> blackboards = new HashMap<>();
    protected Map<String, MessageLog> messageLoggers = new HashMap<>();
    protected Map<String, Session> sessions = new HashMap<>();//a component may be shared by several sessions (Stateless)
    private Session activeSession;
    private boolean isShutDown = false;
    private ClientCommController clientCommController;


    public void addBlackboard(String sessionId, Blackboard blackboard) {
        blackboards.put(sessionId, blackboard);
    }

    public Session getSession() throws Exception{
        checkActiveSession();
        return activeSession;
    }

    public void postCreate(){
        // do something after creation of this component
    }

    public Blackboard blackboard(){
        return blackboards.get( activeSession.getId() );
    }

    public void setClientCommController(ClientCommController clientCommController) {
        this.clientCommController = clientCommController;
    }

    public ClientCommController getClientCommController() {
        return clientCommController;
    }


    public void send( SessionMessage sessionMessage ){
        send( sessionMessage, true );
    }

    public void send( SessionMessage sessionMessage , boolean shouldProcessReply ){
        try {
            if (clientCommController == null) {
                throw new MultiuserException("PluggableComponent " + getClass().getSimpleName() + " has not defined a " +
                        "ConnectRemoteService annotation, thus no messages can be sent to remote services");
            }
            clientCommController.setShouldProcessReply( shouldProcessReply );
            clientCommController.send( getSessionId(), sessionMessage);
        }catch (Exception e){
            ExceptionHandler.handle( e );
        }
    }

    public void receive(ResponseListener responseListener){
        try {
            if (clientCommController == null) {
                throw new MultiuserException("PluggableComponent " + getClass().getSimpleName() + " has not defined a " +
                        "ConnectRemoteService annotation, thus no messages can be sent to remote services");
            }
            clientCommController.receive(responseListener);
        }catch (Exception e){
            ExceptionHandler.handle( e );
        }
    }

    public void setActiveSession(Session activeSession){
        this.activeSession = activeSession;
    }

    public void setActiveSession(String sessionId){
        this.activeSession = sessions.get( sessionId );
    }

    public MessageLog getMessageLogger(){
        try {
            checkActiveSession();
            return messageLoggers.get( activeSession.getId() );
        }catch (Exception e){
            ExceptionHandler.handle(e);
        }
        return null;
    }

    private void checkActiveSession(){
        if (activeSession == null) {
            if( sessions.size() == 1 ){
                activeSession = new ArrayList<>( sessions.values() ).get(0);
            }else {
                ExceptionHandler.handle( new MultiuserException("Attribute 'activeSession' is null. It must be defined "
                        + "somewhere by using setActiveSession") );
            }
        }
    }

    public void addMessageLogger(String sessionId, MessageLog messageLogger) {
        messageLoggers.put(sessionId, messageLogger);
    }

    public void addSession(Session session){
        sessions.put(session.getId(), session);
    }

    public void execute(){
        //do nothing
    }

    public void close() throws Exception{
        if( clientCommController != null ){
            for(Session session : sessions.values() ) {
                clientCommController.send(session.getId(), new SessionMessage(Constants.SESSION_CLOSED));
            }
            clientCommController.close();
        }
        if( !isShutDown ){
            shutDown();
        }
    }

    /** METHODS OF BlackboardListener INTERFACE **/
    @Override
    public abstract void onEvent(BlackboardEvent event);


    /** METHODS OF AbstractExecutionThreadService class (GUAVA) **/
    @Override
    protected void startUp() {
        Log4J.info(this, "Starting up component: " + this.getClass().getSimpleName());
    }

    @Override
    public void shutDown() {
        Log4J.info(this, "Shutting down component: " + this.getClass().getSimpleName() + " instantiation " + this.hashCode());
        isShutDown = true;
        blackboards.clear();
        blackboards = null;
        messageLoggers.clear();
        messageLoggers = null;
    }

    public void notifyNext(PluggableComponent component){
        try {
            SynchronizableEvent next = blackboards.get(component.getSessionId()).getSyncEvent(component);
            if (next != null) {
                next.notifyNext();
            }
        }catch (Exception e){
            ExceptionHandler.handle( e );
        }
    }

    @Override
    public String getSessionId(){
        checkActiveSession();
        return activeSession.getId();
    }
}
