package edu.cmu.inmind.multiuser.controller.orchestrator;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardListener;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.FileLogger;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.plugin.ExternalComponent;
import edu.cmu.inmind.multiuser.controller.plugin.Interceptable;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import edu.cmu.inmind.multiuser.controller.session.ServiceComponent;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.controller.sync.ForceSync;
import edu.cmu.inmind.multiuser.controller.sync.SynchronizableEvent;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 3/10/17.
 * 
 */
public abstract class ProcessOrchestratorImpl implements ProcessOrchestrator, BlackboardListener {

    @Inject Set<PluggableComponent> componentsSet;
    protected Set<PluggableComponent> components;
    protected Blackboard blackboard;
    protected String status;
    protected CopyOnWriteArrayList<OrchestratorListener> orchestratorListeners;
    protected Session session;
    protected MessageLog logger;
    protected ServiceManager serviceManager;
    private String sessionId;
    private boolean isClosed;
    private Config config;
    private String fullAddress;
    private boolean initialized = false;

    public ProcessOrchestratorImpl(){
        blackboard = new Blackboard( );
        components = new CopyOnWriteArraySet();
    }

    public Session getSession() {
        return session;
    }

    public MessageLog getLogger() {
        return logger;
    }

    @Override
    public List<PluggableComponent> getComponents(){
        return new ArrayList<>(components);
    }

    public <T extends PluggableComponent> T get(Class<T> clazz){
        String className = clazz.getName();
        for( PluggableComponent component : components ){
            if( component.getClass().getName().contains( className ) ){
                return (T)component;
            }
        }
        return null;
    }

    public String getPrefix(String message) {
        return message.substring( 0, message.indexOf(" ") );
    }

    public String getBody(String message) {
        return message.substring( message.indexOf(" ") + 1);
    }

    public Blackboard getBlackboard() {
        return blackboard;
    }

    @Override
    public void process(String input){
        while( status != null && (status.equals( Constants.ORCHESTRATOR_STOPPED)
                || status.equals( Constants.ORCHESTRATOR_PAUSED))) {
            Utils.sleep( 500 );
        }
    }

    protected void sendResponse(SessionMessage output){
        orchestratorListeners.forEach( listener ->  listener.processOutput(output) );
    }

    @Override
    public void initialize( Session session ) throws Throwable{
        this.session = session;
        this.config = session.getConfig();
        this.fullAddress = session.getFullAddress();
        sessionId = session.getId();
        Log4J.info(this, String.format("Creating Process Orchestrator for session: %s", sessionId));
        components.addAll(componentsSet);
        components.addAll( createExternalComponents() );
        //by default we use a file messageLogger
        if( logger == null ){
            logger = new FileLogger();
            logger.setPath( config.getPathLogs() );
        }
        logger.setId( sessionId );
        ResourceLocator.addServiceToComponent(components, sessionId, fullAddress );
        for( PluggableComponent component : components ){
            if( component instanceof PluggableComponent){
                component.addMessageLogger(sessionId, logger);
                component.addSession(session);
                component.postCreate();
            }
        }
        blackboard.setLogger( logger );
        initServiceManager();
        initialized = true;
    }

    private void initServiceManager() {
        Log4J.info(this, String.format("Initializing ServiceManager for session: %s", sessionId));
        Set<PluggableComponent> newComponents = ResourceLocator.addComponentsToRegistry(components, sessionId);
        serviceManager = new ServiceManager( newComponents );
        serviceManager.addListener(
                new ServiceManager.Listener() {
                    public void stopped() {
                        Log4J.info(ProcessOrchestratorImpl.this, String.format("All components have been shut down. " +
                                "Closing ServiceManager for Session: %s", sessionId));
                        status = Constants.ORCHESTRATOR_STOPPED;
                        ResourceLocator.addServiceManager( serviceManager, Constants.SERVICE_MANAGER_STOPPED );
                    }

                    public void healthy() {
                        // Services have been initialized and are healthy, start accepting requests...
                        Log4J.info(ProcessOrchestratorImpl.this, String.format("ServiceManager has initialized all the " +
                                        "services for session: %s", sessionId));
                        status = Constants.ORCHESTRATOR_STARTED;
                    }

                    public void failure(Service service) {
                        // Something failed, at this point we could log it, notify a load balancer, or take
                        // some other action.  For now we will just exit.
                        Log4J.error(ProcessOrchestratorImpl.this, String.format("There was a failure with service: %s " +
                                        "in session: %s", service.getClass().getName(), sessionId));
                    }
                },
                MoreExecutors.directExecutor());
        serviceManager.startAsync();
        ResourceLocator.addServiceManager( serviceManager, Constants.SERVICE_MANAGER_STARTED );
    }

    @Override
    public void start() {
        Log4J.info(this, String.format("Starting Process Orchestrator for session: %s", sessionId));
        blackboard.setComponents( components, sessionId );
        blackboard.subscribe( this );
        orchestratorListeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void pause() {
        Log4J.info(this, String.format("Pausing Process Orchestrator for session: %s", sessionId));
        status = Constants.ORCHESTRATOR_PAUSED;
    }

    @Override
    public void resume() {
        Log4J.info(this, String.format("Resuming Process Orchestrator for session: %s", sessionId));
        status = Constants.ORCHESTRATOR_RESUMED;
    }

    @Override
    public void close() throws Throwable{
        if( !isClosed && initialized ) {
            Log4J.info(this, String.format("Closing Process Orchestrator for session: %s", sessionId));
            isClosed = true;
            logger.store();
            status = Constants.ORCHESTRATOR_STOPPED;
            serviceManager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
            serviceManager.awaitStopped();
            for(PluggableComponent component : components ){
                component.close( sessionId );
            }
            orchestratorListeners.forEach(this::unsubscribe);
            blackboard.remove(this, Constants.REMOVE_ALL);
            if( blackboard != null ) {
                blackboard.reset();
            }
            blackboard = null;
            serviceManager = null;
            components = null;
            componentsSet = null;
            session = null;
        }
    }

    @Override
    public void subscribe(OrchestratorListener listener) {
        orchestratorListeners.add( listener );
    }

    @Override
    public void unsubscribe(OrchestratorListener listener) {
        orchestratorListeners.remove( listener );
    }

    @Override
    public PluggableComponent processMsg(SessionMessage message){
        try {
            if ( message == null || message.getMessageId() == null) {
                throw new MultiuserException(ErrorMessages.OBJECT_NULL, "message");
            }
            Class<? extends PluggableComponent> clazz = ResourceLocator.getMsgMapping(message.getMessageId());
            if (clazz == null) {
                for( PluggableComponent component : components ){
                    if( component.getClass().isAnnotationPresent(BlackboardSubscription.class) ){
                        for(String mssg : component.getClass().getAnnotation(BlackboardSubscription.class).messages() ){
                            if(mssg.equals(message.getMessageId())){
                                clazz = component.getClass();
                                break;
                            }
                        }
                    }
                }
                if( clazz == null ) {
                    throw new MultiuserException( ErrorMessages.PREFIX_NOT_MAPPED, message );
                }
            }
            for (PluggableComponent component : components) {
                if (component.getClass() == clazz) {
                    return component;
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
        return null;
    }


    public void executeSync(List<PluggableComponent> components) {
        try {
            for (PluggableComponent component : components) {
                component.setActiveSession( session );
                execute(component);
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void executeAsync(List<PluggableComponent> components) {
        try{
            for (PluggableComponent component : components) {
                component.setActiveSession( session );
                new Thread("ExecuteComponentAsyncThread"){
                    public void run(){
                        try{
                            execute(component);
                        }catch (Throwable e){
                            ExceptionHandler.handle( e );
                        }
                    }
                }.start();
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void forceSync(List<PluggableComponent> asynComponents){
        checkComponents( asynComponents );
        forceSync(asynComponents, blackboard);
    }

    public void forceSync(List<PluggableComponent> asynComponents, List<SynchronizableEvent> events){
        checkComponents( asynComponents );
        forceSync(asynComponents, events, blackboard);
    }

    private void forceSync(List<PluggableComponent> asynComponents, Blackboard blackboard) {
        forceSync( asynComponents, null, blackboard);
    }

    private void forceSync(List<PluggableComponent> asynComponents, List<SynchronizableEvent> events,
                           Blackboard blackboard) {
        try {
            if (asynComponents != null && !asynComponents.isEmpty() && blackboard != null) {
                List<CompSyncEvent> compSyncEvents = new ArrayList<>();
                for (int i = 0; i < asynComponents.size(); i++) {
                    asynComponents.get(i).setActiveSession( session );
                    compSyncEvents.add(new CompSyncEvent(asynComponents.get(i),
                            events == null || i >= events.size() ? null : events.get(i)));
                }
                PluggableComponent component = compSyncEvents.get(0).component;
                if (component.getClass().isAnnotationPresent(ForceSync.class)) {
                    String id = component.getClass().getAnnotation(ForceSync.class).id();
                    Queue<CompSyncEvent> queue = new LinkedList<>(compSyncEvents);
                    ResourceLocator.putSyncMap(id, queue);
                    addAsyncEvents(id, blackboard);
                    queue.poll().component.execute();
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    private void addAsyncEvents( String id, Blackboard blackboard ){
        Queue<CompSyncEvent> queue = ResourceLocator.getSyncMap( id );
        if( !queue.isEmpty() ){
            if( queue.peek().event != null ){
                blackboard.post(this,id, (SynchronizableEvent) () -> {
                    addAsyncEvents(id, blackboard);
                    if (!queue.isEmpty()) {
                        CompSyncEvent compSyncEvent = queue.poll();
                        compSyncEvent.component.execute();
                        compSyncEvent.event.notifyNext();
                    }
                });
            }else {
                blackboard.post(this,id, (SynchronizableEvent) () -> {
                    addAsyncEvents(id, blackboard);
                    if (!queue.isEmpty()) {
                        queue.poll().component.execute();
                    }
                });
            }
        }
    }

    static class CompSyncEvent{
        PluggableComponent component;
        SynchronizableEvent event;

        public CompSyncEvent(PluggableComponent component, SynchronizableEvent event) {
            this.component = component;
            this.event = event;
        }
    }

    private void checkComponents( List<PluggableComponent> otherComponents ){
        for( PluggableComponent component : otherComponents ){
            if( !components.contains(component) ){
                if( component instanceof PluggableComponent){
                    component.addMessageLogger(sessionId, logger);
                    component.addBlackboard(sessionId, blackboard);
                }
                components.add( component );
            }
        }
    }

    /**
     * Use this method to notify remote subscribers when some specific messages (@subMessages) are triggered.
     * We use this method only if the remote service has provided a list of subscription messages, otherwise
     * it will be responsibility of developer to decide when to send messages to remote service.
     */
    private List<ExternalComponent> createExternalComponents(){
        Log4J.debug(this, "createExternalComponents");
        List<ExternalComponent> externalComponents = new ArrayList<>();
        for(ServiceComponent service : ResourceLocator.getServiceRegistry().values() ) {
            if( service.getSubMessages() != null && service.getSubMessages().length > 0 ) {
                externalComponents.add(new ExternalComponent(service.getServiceURL(), fullAddress, sessionId,
                        service.getMsgTemplate(), service.getSubMessages()));
            }
        }
        return externalComponents;
    }

    @Override
    @Interceptable
    public void execute(PluggableComponent component){
        component.execute();
    }

    @Override
    public void onEvent(BlackboardEvent event){
        //do nothing
    }

    @Override
    public String getSessionId() throws Throwable{
        return sessionId;
    }

}
