package edu.cmu.inmind.multiuser.controller.plugin;

import com.google.common.util.concurrent.AbstractIdleService;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
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
@StateType( state = Constants.STATEFULL )
public abstract class PluggableComponent extends AbstractIdleService implements BlackboardListener, Pluggable {
    private Map<String, Blackboard> blackboards;
    protected Map<String, MessageLog> messageLoggers;
    protected Map<String, Session> sessions;
    private Session activeSession;
    private boolean isShutDown;
    private ClientCommController clientCommController;

    public PluggableComponent(){
        blackboards = new HashMap<>();
        messageLoggers = new HashMap<>();
        sessions = new HashMap<>();//a component may be shared by several sessions (Stateless)
        isShutDown = false;
    }


    public void addBlackboard(String sessionId, Blackboard blackboard) {
        blackboards.put(sessionId, blackboard);
    }

    public Session getSession() throws Throwable{
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
                throw new MultiuserException( ErrorMessages.NO_REMOTE_ANNOTATION, getClass().getSimpleName() );
            }
            clientCommController.setShouldProcessReply( shouldProcessReply );
            clientCommController.send( getSessionId(), sessionMessage);
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void receive(ResponseListener responseListener){
        try {
            if (clientCommController == null) {
                throw new MultiuserException(ErrorMessages.NO_REMOTE_ANNOTATION, getClass().getSimpleName());
            }
            clientCommController.receive(responseListener);
        }catch (Throwable e){
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
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
        return null;
    }

    private void checkActiveSession(){
        if (activeSession == null) {
            if( sessions.size() == 1 ){
                activeSession = new ArrayList<>( sessions.values() ).get(0);
            }else {
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ATTRIBUTE_NULL, "activeSession",
                        "setActiveSession") );
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

    public void close(String sessionId) throws Throwable{
        sessions.remove( sessionId );

        // if this is a stateless component, we can only close it if all sessions have stopped
        String state = this.getClass().getAnnotation( StateType.class ).state();
        if( (state.equals(Constants.STATEFULL)) || ( (state.equals( Constants.STATELESS )
                || state.equals( Constants.POOL)) && sessions.isEmpty() ) ) {
            if (clientCommController != null) {
                for (Session session : sessions.values()) {
                    clientCommController.send(session.getId(), new SessionMessage(Constants.SESSION_CLOSED));
                }
                clientCommController.close();
            }
            if (!isShutDown) {
                shutDown();
            }
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
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    @Override
    public String getSessionId(){
        checkActiveSession();
        return activeSession.getId();
    }
}
