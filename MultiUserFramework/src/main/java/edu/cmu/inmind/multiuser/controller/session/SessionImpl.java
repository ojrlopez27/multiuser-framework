package edu.cmu.inmind.multiuser.controller.session;

import edu.cmu.inmind.multiuser.controller.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ClientController;
import edu.cmu.inmind.multiuser.controller.communication.ServerCommController;
import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.OrchestratorListener;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.Pluggable;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 3/3/17.
 * This class controls all the interaction between a client and a set of specific components.
 */
public class SessionImpl implements Session, Utils.NamedRunnable, OrchestratorListener, DestroyableCallback {
    private String id;
    private String status;
    private ProcessOrchestrator orchestrator;
    private AtomicBoolean isClosed = new AtomicBoolean(false);
    private InactivityTimer timer;
    private ZMsgWrapper replyMsg = new ZMsgWrapper();
    private Config config;
    private String fullAddress;
    private List<SessionObserver> observers;
    private DestroyableCallback callback;
    private CopyOnWriteArrayList closeableObjects;
    private boolean useSessionTimeout = false;
    private boolean useAutomaticAck = Utils.getProperty("session.receive.automatic.ack", false);
    /** we use this controller to communicate back with the client who made a request **/
    private ServerCommController sessionCommController;
    /** we use this controller to communicate back with the client when TCP is off **/
    private ClientController client;
    private List<Object> postCreationList;

    public SessionImpl() {
        if(useSessionTimeout) this.timer = new InactivityTimer();
        this.observers = new ArrayList<>();
        this.closeableObjects = new CopyOnWriteArrayList();
    }

    @Override
    public String getName() {
        return "session-" + id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public String getFullAddress() {
        return fullAddress;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public ProcessOrchestrator getOrchestrator() {
        return orchestrator;
    }

    @Override
    public List<Object> getPostCreationList() {
        return postCreationList;
    }

    public void setPostCreationList(List<Object> postCreationList) {
        this.postCreationList = postCreationList;
    }

    public void setClient(ClientController client) {
        this.client = client;
    }

    /**
     * each session must have a unique id. A new thread is created for each new session
     * @param id
     * @param msg
     */
    public void setId(String id, ZMsgWrapper msg, String fullAddress, boolean shouldExecute) {
        if(  id == null || id.isEmpty() || msg == null || fullAddress == null || fullAddress.isEmpty()){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "id: " + id,
                    "msg: " + msg, "fullAddress: " + fullAddress) );
        }
        if( this.id == null ){
            Log4J.info(this, "A new session has been created with id: " + id);
            this.fullAddress = fullAddress;
            if(msg != null) {
                this.sessionCommController = new ServerCommController(fullAddress, id, msg);
                closeableObjects.add( sessionCommController );
            }
        }
        this.id = id;
        if(shouldExecute) Utils.execute(this);
    }

    @Override
    public void setId(String id, ZMsgWrapper msg, String fullAddress) {
        setId(id, msg, fullAddress, true);
    }

    public List<Pluggable> getComponents(){
        return orchestrator.getComponents();
    }

    public void pause(){
        status = Constants.SESSION_PAUSED;
        orchestrator.pause();
        Log4J.info(this, String.format("Session with id: %s has been paused.", id));
        //TODO: add some logic when session is paused
    }

    public void resume(){
        status = Constants.SESSION_RESUMED;
        orchestrator.resume();
        Log4J.info(this, String.format("Session with id: %s has been resumed.", id));
        //TODO: add some logic when session is resumed
    }

    /**
     * it clises the session and all its subcomponents: orchestrator, pluggable components and
     * communication controllers
     * @throws Throwable
     */
    public void close(DestroyableCallback callback) throws Throwable{
        if(this.callback == null && this.callback != this)
            this.callback = callback;
        if( !isClosed.getAndSet(true) ) {
            notifyObservers();
            Log4J.info(this, String.format("Closing session: %s", id));
            if( sessionCommController != null ) { // it is null when TCP is off
                //notify the client
                sessionCommController.disconnect();
            }
            if(orchestrator != null) orchestrator.close( this );
            sessionCommController.close(this);
        }
    }

    @Override
    public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable{
        closeableObjects.remove( destroyedObj );
        if( closeableObjects.isEmpty() ) {
            orchestrator = null;
            if (sessionCommController != null) { // it is null when TCP is off
                sessionCommController = null;
                if( useSessionTimeout ) {
                    timer.cancel();
                    timer.purge();
                }
                status = Constants.SESSION_CLOSED;
                ResourceLocator.setIamDone( this );
                Log4J.info(this, "Gracefully destroying...");
                Log4J.info(this, String.format("Session: %s has been disconnected!", id));
                if(callback != null) callback.destroyInCascade(this);
            }
        }
    }

    /**
     * this method creates a new orchestrator and injects a set of pre-defined components
     * @throws Throwable
     */
    public void initialize() throws Throwable{
        Log4J.info(this, String.format("Initializing session: %s.", id));
        orchestrator = DependencyManager.getInstance().getOrchestrator();
        orchestrator.initialize(this);
        orchestrator.start();
        orchestrator.subscribe(this);
        closeableObjects.add(orchestrator);
        status = Constants.SESSION_INITIATED;
    }

    /**
     * While session is active, this method will be processing incoming information from client
     */
    @Override
    public void run(){
        try {
            initialize();
            while (!status.equals(Constants.SESSION_CLOSED) && !Thread.currentThread().isInterrupted()) {
                if (sessionCommController != null) { //sessionCommController is null when not using TCP
                    ZMsgWrapper request = sessionCommController.receive(replyMsg.getMsg());
                    if (request == null) {
                        break; //Interrupted
                    }
                    replyMsg = request; //  Echo is complex :-)
                    String message = replyMsg.getMsg().peekLast().toString();
                    stopTimer();
                    if (message.contains(Constants.REQUEST_DISCONNECT)) {
                        status = Constants.SESSION_CLOSED;
                    } else {
                        if (orchestrator != null) {
                            if (useAutomaticAck || !message.contains(Constants.ACK)) {
                                process(message);
                            }
                        }else{
                            Log4J.error(this, "Orchestrator is null");
                        }
                    }
                }
                if (status.equals(Constants.SESSION_CLOSED)) {
                    close(null);
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    /**
     * This method has been factorized so we can call it from outside SessionImpl,
     * specially when we are simulating the interaction with sessions and components.
     * @param message
     */
    public void process(String message){
        try {
            orchestrator.process(message);
            Log4J.info(this, message.toString());
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    /**
     * it sends a response back to the client
     * @param output
     */
    @Override
    public void processOutput(Object output) throws Throwable{
        if( output == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "output: " + output));
        }
        if( config.isTCPon() ) {
            Log4J.track("ProcessOrchestratorImpl", "26:" + output);
            sessionCommController.send(output);
        }else{
            client.getResponseListener().process( Utils.toJson(output) );
        }
        if(useSessionTimeout) timer.schedule(new InactivityCheck(), config.getSessionTimeout());
    }

    private void stopTimer(){
        if(useSessionTimeout) timer.stopTimer();
    }

    public void onClose(SessionObserver observer) {
        if( observer == null || observers == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "observer: " + observer,
                    "observers: " + observers));
        }
        this.observers.add(observer);
    }

    private void notifyObservers(){
        for(SessionObserver observer : observers){
            observer.notifyCloseSession(this);
        }
    }

    interface SessionObserver{
        void notifyCloseSession(SessionImpl session);
    }

    /**
     * we need a timer to determine whether a session is inactive (it has reached a timeout)
     */
    class InactivityCheck extends TimerTask{
        @Override
        public void run() {
            try {
                close(SessionImpl.this);
            }catch (Throwable e){
                ExceptionHandler.handle( e );
            }
        }
    }

    class InactivityTimer extends Timer{
        private TimerTask inactivityCheck;

        @Override
        public void schedule(TimerTask task, long delay) {
            inactivityCheck = task;
            super.schedule(task, delay);
        }

        public void stopTimer(){
            if( inactivityCheck != null ) {
                inactivityCheck.cancel();
            }
        }
    }
}
