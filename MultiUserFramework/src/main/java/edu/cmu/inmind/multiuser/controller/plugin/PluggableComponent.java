package edu.cmu.inmind.multiuser.controller.plugin;

import com.google.common.util.concurrent.AbstractIdleService;
import edu.cmu.inmind.multiuser.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardListener;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.controller.sync.SynchronizableEvent;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 3/16/17.
 */
@StateType( state = Constants.STATEFULL )
public abstract class PluggableComponent extends AbstractIdleService
                                         implements BlackboardListener, Pluggable, DestroyableCallback {
    private ConcurrentHashMap<String, Blackboard> blackboards;
    protected ConcurrentHashMap<String, MessageLog> messageLoggers;
    protected ConcurrentHashMap<String, Session> sessions;
    private Session activeSession;
    private AtomicBoolean isShutDown = new AtomicBoolean(false);
    private AtomicBoolean isClosed = new AtomicBoolean(false);
    private ClientCommController clientCommController;
    private CopyOnWriteArrayList<DestroyableCallback> callbacks;
    private String type;
    private String defaultSessionId;

    public PluggableComponent(){
        blackboards = new ConcurrentHashMap<>();
        messageLoggers = new ConcurrentHashMap<>();
        sessions = new ConcurrentHashMap<>();//a component may be shared by several sessions (Stateless)
        callbacks = new CopyOnWriteArrayList();
        isShutDown.getAndSet(false);
        try {
            type = Utils.getAnnotation(getClass(), StateType.class).state();
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    public String getType() {
        return type;
    }

    public void addBlackboard(String sessionId, Blackboard blackboard) {
        if( blackboards == null || sessionId == null || blackboard == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "blackboards: "+blackboards,
                    "sessionId: " + sessionId, "blackboard: " + blackboard));
        }
        blackboards.put(sessionId, blackboard);
    }

    public Blackboard getBlackBoard(String sessionId){
        Blackboard bb = null;
        if( !isClosing() ) {
            if (blackboards == null || sessionId == null) {
                ExceptionHandler.handle(new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "blackboards: " + blackboards,
                        "sessionId: " + sessionId));
            } else {
                bb = blackboards.get(sessionId);
                if (bb == null) {
                    ExceptionHandler.handle(new MultiuserException(ErrorMessages.NO_BLACKBOARD, sessionId));
                }
            }
        }
        return bb;
    }

    public Session getSession() throws Throwable{
        checkActiveSession();
        return activeSession;
    }

    public void postCreate(){
        //TODO: this method may be implemented by subclasses
    }


    /** ================================================ START OVERRIDE ============================================ **/

    /**
     * Super: Pluggable interface
     */
    @Override
    public void execute(){
        //TODO: this method has to be implemented by subclasses
    }

    /**
     * Super: AbstractExecutionThreadService class (GUAVA)
     */
    @Override
    protected void startUp() {
        Log4J.info(this, "Starting up component: " + this.getClass().getSimpleName() +
                " on session: " + checkActiveSession().getId() );
    }

    /**
     * Super: AbstractExecutionThreadService class (GUAVA)
     */
    @Override
    public void shutDown() {
        isShutDown.getAndSet(true);
        Log4J.info(this, "Shutting down component: " + this.getClass().getSimpleName() +
                " instantiation " + this.hashCode());
        if(blackboards != null) blackboards.clear();
        blackboards = null;
        if(messageLoggers != null) messageLoggers.clear();
        messageLoggers = null;
    }

    /**
     * Super: BlackboardListener interface
     */
    @Override
    public String getSessionId(){
        if( !isClosed.get() ) {
            checkActiveSession();
            return activeSession.getId();
        }else{
            return defaultSessionId;
        }
    }

    /**
     * Super: BlackboardListener interface
     */
    @Override
    public boolean isClosing(){
        return !isRunning();
    }

    /** ================================================ END OVERRIDE ============================================ **/


    @Deprecated
    private Blackboard blackboard(){
        Blackboard bb = null;
        if( !isClosed.get() ) {
            if (activeSession != null && activeSession.getId() != null && blackboards != null) {
                bb = blackboards.get(activeSession.getId());
            } else {
                ExceptionHandler.handle(new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                        "blackboards: " + blackboards, "activeSession: " + activeSession));
            }
        }else{
            bb = blackboards.get(defaultSessionId);
        }
        //TODO: why blackboard is null?
        if( bb == null ){
            bb = new Blackboard( getMessageLogger() );
        }
        return bb;
    }

    public void setClientCommController(ClientCommController clientCommController) {
        this.clientCommController = clientCommController;
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
            clientCommController.setResponseListener(responseListener);
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void setActiveSession(Session activeSession){
        this.activeSession = activeSession;
        if( this.activeSession != null && this.activeSession.getId() != null ) defaultSessionId = this.activeSession.getId();
    }

    public void setActiveSession(String sessionId){
        this.activeSession = sessions.get( sessionId );
        if( activeSession != null && activeSession.getId() != null ) defaultSessionId = activeSession.getId();
    }

    public MessageLog getMessageLogger(){
        try {
            if( !isClosed.get() ) {
                checkActiveSession();
                return messageLoggers.get(activeSession.getId());
            }else {
                return messageLoggers.get( defaultSessionId );
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
        return null;
    }

    private Session checkActiveSession(){
        if (activeSession == null){
            if( sessions != null && sessions.size() > 0 ){
                activeSession = new ArrayList<>( sessions.values() ).get( sessions.size() - 1 );
                if( activeSession != null && activeSession.getId() != null )
                    defaultSessionId = activeSession.getId();
            }else {
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "activeSession: "
                        + activeSession, "sessions: " + sessions) );
            }
        }
        return activeSession;
    }

    public void addMessageLogger(String sessionId, MessageLog messageLogger) {
        if( messageLoggers == null || sessionId == null || sessionId.isEmpty() || messageLogger == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                    "messageLoggers: " + messageLoggers, "sessionId: " + sessionId, "messagaLogger: " + messageLogger) );
        }
        messageLoggers.put(sessionId, messageLogger);
    }

    public void addSession(Session session){
        if( session == null || sessions == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                    "session: " + session, "sessions: " + sessions) );
        }
        sessions.put(session.getId(), session);
    }

    public void close(String sessionId, DestroyableCallback callback) throws Throwable{
        close(callback);
        sessions.remove( sessionId );
        if( clientCommController != null ) {
            clientCommController.disconnect(sessionId);
        }
        destroyInCascade(null);
    }

    @Override
    public void close(DestroyableCallback callback) throws Throwable{
        isClosed.getAndSet(true);
        callbacks.add(callback);
    }

    @Override
    public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable{
        if( clientCommController != null ){
            clientCommController.close(null);
        }
        ResourceLocator.setIamDone( this );
        Log4J.info(this, "Gracefully destroying...");
        for(DestroyableCallback callback : callbacks){
            callback.destroyInCascade( this );
        }
    }

    public void notifyNext(PluggableComponent component){
        try {
            if( blackboards == null || component == null || component.getSessionId() == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                        "blackboards: " + blackboards, "component: " + component, "sessionId: " +
                        component != null? component.getSessionId() : null) );
            }
            SynchronizableEvent next = blackboards.get(component.getSessionId()).getSyncEvent(component);
            if (next != null) {
                next.notifyNext();
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }
}
