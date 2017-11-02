package edu.cmu.inmind.multiuser.controller.session;

import edu.cmu.inmind.multiuser.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ServerCommController;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.OrchestratorListener;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;

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
public class Session extends Utils.MyRunnable implements Runnable, OrchestratorListener, DestroyableCallback {
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
    /** we use this controller to communicate back with the client who made a request **/
    private ServerCommController sessionCommController;
    /** we use this controller to communicate back with the client when TCP is off **/
    private ClientCommController client;

    public Session(String name){
        this.name = name;
    }

    public Session() {
        if(useSessionTimeout) this.timer = new InactivityTimer();
        this.observers = new ArrayList<>();
        this.closeableObjects = new CopyOnWriteArrayList();
    }

    public String getId() {
        return id;
    }

    public Config getConfig() {
        return config;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public ProcessOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public void setClient(ClientCommController client) {
        this.client = client;
    }

    /**
     * each session must have a unique id. A new thread is created for each new session
     * @param id
     * @param msg
     */
    public void setId(String id, ZMsgWrapper msg, String fullAddress) {
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
            this.setName("session-" + id);
            Utils.execute(this);
        }
        this.id = id;
    }

    public List<PluggableComponent> getComponents(){
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
        Log4J.warn(this, "=== 12");
        if(this.callback == null && this.callback != this)
            this.callback = callback;
        if( !isClosed.getAndSet(true) ) {
            notifyObservers();
            Log4J.info(this, String.format("Closing session: %s", id));
            if( sessionCommController != null ) { // it is null when TCP is off
                //notify the client
                sessionCommController.disconnect();
            }
            Log4J.warn(this, "=== 13");
            orchestrator.close( this );
            Log4J.warn(this, "=== 14");
            sessionCommController.close(this);
            Log4J.warn(this, "=== 22");
        }
    }

    @Override
    public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable{
        closeableObjects.remove( destroyedObj );
        Log4J.warn(this, "=== 19");
        if( closeableObjects.isEmpty() ) {
            Log4J.warn(this, "=== 20");
            orchestrator = null;
            if (sessionCommController != null) { // it is null when TCP is off
                sessionCommController = null;
                if( useSessionTimeout ) {
                    timer.cancel();
                    timer.purge();
                }
                status = Constants.SESSION_CLOSED;
                DependencyManager.setIamDone( this );
                Log4J.info(this, "Gracefully destroying...");
                Log4J.info(this, String.format("Session: %s has been disconnected!", id));
                Log4J.warn(this, "=== 21");
                callback.destroyInCascade(this);
            }
        }
    }

    /**
     * this method creates a new orchestrator and injects a set of pre-defined components
     * @throws Throwable
     */
    private void initialize() throws Throwable{
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
                if( sessionCommController != null ) { //sessionCommController is null when not using TCP
                    ZMsgWrapper request = sessionCommController.receive(replyMsg.getMsg());
                    if (request == null)
                        break; //Interrupted
                    replyMsg = request; //  Echo is complex :-)
                    String message = replyMsg.getMsg().peekLast().toString();
                    stopTimer();
                    if (message.contains(Constants.REQUEST_DISCONNECT)) {
                        status = Constants.SESSION_CLOSED;
                    } else {
                        if( orchestrator != null ) {
                            orchestrator.process(message);
                        }else{
                            Log4J.warn(this, "+++ Orchestrator is null");
                        }
                    }
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
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
        void notifyCloseSession(Session session);
    }

    /**
     * we need a timer to determine whether a session is inactive (it has reached a timeout)
     */
    class InactivityCheck extends TimerTask{
        @Override
        public void run() {
            try {
                close(Session.this);
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
