package edu.cmu.inmind.multiuser.controller.orchestrator;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.blackboard.*;
import edu.cmu.inmind.multiuser.controller.common.*;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.FileLogger;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.muf.AfterCreationHook;
import edu.cmu.inmind.multiuser.controller.plugin.*;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import edu.cmu.inmind.multiuser.controller.communication.ServiceComponent;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.controller.sync.ForceSync;
import edu.cmu.inmind.multiuser.controller.sync.SynchronizableEvent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 3/10/17.
 * 
 */
public abstract class ProcessOrchestratorImpl implements ProcessOrchestrator, DestroyableCallback, AfterCreationHook {

    @Inject Set<PluggableComponent> componentsSet;
    protected Set<Pluggable> components;
    protected Blackboard blackboard;
    protected String status;
    protected CopyOnWriteArrayList<OrchestratorListener> orchestratorListeners;
    protected Session session;
    protected MessageLog logger;
    protected ServiceManager statefullServManager;
    private CopyOnWriteArrayList closeableObjects;
    private String sessionId;
    private AtomicBoolean isClosed = new AtomicBoolean(false);
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private Config config;
    private String fullAddress;
    private DestroyableCallback callback;
    private List<Object> postCreationList;

    public ProcessOrchestratorImpl(){
        if( logger == null ){
            logger = new FileLogger();
        }
        blackboard = new BlackboardImpl( logger );
        components = new CopyOnWriteArraySet();
        closeableObjects = new CopyOnWriteArrayList();
        orchestratorListeners = new CopyOnWriteArrayList<>();
    }

    public Session getSession() {
        return session;
    }

    @Override
    public MessageLog getLogger() {
        return logger;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public void setStatefullServManager(ServiceManager statefullServManager) {
        this.statefullServManager = statefullServManager;
    }

    @Override
    public List<Pluggable> getComponents(){
        return new ArrayList<>(components);
    }

    @Override
    public <T extends Pluggable> T get(Class<T> clazz) throws Exception{
        try {
            if( components == null || components.isEmpty() ){
                throw new MultiuserException(ErrorMessages.COMPONENTS_NULL, sessionId );
            }
            String className = clazz.getName();
            for (Pluggable component : components) {
                if (component.getClass().getName().contains(className)) {
                    return (T) component;
                }
            }
        }catch (Throwable e ){
            ExceptionHandler.handle(e);
        }
        return null;
    }


    @Override
    public void addBlackboard(String sessionId, Blackboard blackboard){
        this.blackboard = blackboard;
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
    public void process(String input) throws Throwable{
        try {
            while (status != null && (status.equals(Constants.ORCHESTRATOR_STOPPED)
                    || status.equals(Constants.ORCHESTRATOR_PAUSED))) {
                //we need to wait until the orchestrator is ready to process messages
                Utils.sleep(100);
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    /**
     * This method sends the response from MUF to the client
     * @param output ideally this attribute should be an instance of SessionMessage class, however, you can define
     *               whichever format you want to exchange messages with the client (e.g., you can just send a String)
     *               but make sure that client correctly parses this message.
     */
    protected void sendResponse(Object output){
        try {
            orchestratorListeners.forEach(listener -> {
                try {
                    Log4J.track("ProcessOrchestratorImpl", "25:" + output);
                    // we need a delay in order to avoid consecutive messages to block the sender socket
                    Utils.sleep( Utils.getProperty("orchestrator.send.response.delay", 5L));
                    listener.processOutput(output);
                } catch (Throwable throwable) {
                    ExceptionHandler.handle(throwable);
                }
            });
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    @Override
    public void initialize( Session session ) throws Throwable{
        this.session = session;
        this.config = session.getConfig();
        this.fullAddress = session.getFullAddress();
        this.postCreationList = session.getPostCreationList();
        sessionId = session.getId();
        Log4J.info(this, String.format("Creating Process Orchestrator for session: %s", sessionId));
        components.addAll(componentsSet);
        components.addAll( createExternalComponents() );
        addOnlyStatefullToCloseable();
        //by default we use a file messageLogger
        if( logger != null ){
            logger.setPath( config.getPathLogs() );
        }else{
            ExceptionHandler.handle(new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "logger: " + logger));
        }
        logger.setId( sessionId );
        List<Pair<Pluggable, ServiceComponent>> srvcs = ResourceLocator.addServiceToComponent(components, sessionId, fullAddress );
        for(Pair pair : srvcs ){
            if( pair != null ){
                ((Pluggable) pair.fst).setClientCommController( new ClientCommController.Builder()
                        .setServerAddress( ((ServiceComponent)pair.snd).getServiceURL())
                        .setServiceName(sessionId)
                        .setMsgTemplate( ((ServiceComponent)pair.snd).getMsgTemplate() )
                        .setRequestType( Constants.REQUEST_CONNECT )
                        .build() );
            }
        }
        blackboard.setComponents( components, sessionId );
        blackboard.subscribe( this );
        for( Pluggable component : components ){
            if( component instanceof Pluggable){
                component.addMessageLogger(sessionId, logger);
                component.addSession(session);
                component.postCreate();
            }
        }
        blackboard.setLogger( logger );
        initServiceManager();
        initialized.getAndSet(true);
        postCreate();
    }

    private void postCreate() {
        for(Object process : postCreationList ){
            processHook( process );
        }
    }

    private void addOnlyStatefullToCloseable() throws Throwable{
        for(Pluggable component : components) {
            if( Utils.getAnnotation(component.getClass(), StateType.class).state().equals(Constants.STATEFULL) ){
                closeableObjects.add( component );
            }
        }
    }

    private void initServiceManager() throws Throwable{
        Log4J.info(this, String.format("Initializing ServiceManager for session: %s", sessionId));
        ResourceLocator.addComponentsToRegistry(this, components, sessionId);
    }

    @Override
    public void start() {
        Log4J.info(this, String.format("Starting Process Orchestrator for session: %s", sessionId));
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
    public void close(DestroyableCallback callback) throws Throwable{
        this.callback = callback;
        close();
    }

    @Override
    public void close() throws Throwable{
        if( !isClosed.getAndSet(true) && initialized.get() ) {
            Log4J.info(this, String.format("Closing Process Orchestrator for session: %s", sessionId));
            logger.store();
            status = Constants.ORCHESTRATOR_STOPPED;
            for(Pluggable component : components ){
                component.close( sessionId, this );
            }
        }
    }


    @Override
    public void destroyInCascade(DestroyableCallback destroyedObject) throws Throwable{
        closeableObjects.remove( destroyedObject );
        if( closeableObjects.isEmpty() ) {
            if( statefullServManager != null ) {
                statefullServManager.stopAsync().awaitStopped(20, TimeUnit.SECONDS);
            }
            orchestratorListeners.forEach(this::unsubscribe);
            if (blackboard != null) {
                blackboard.remove(this, Constants.REMOVE_ALL);
                blackboard.reset();
            }
            blackboard = null;
            statefullServManager = null;
            components = null;
            componentsSet = null;
            session = null;
            ResourceLocator.setIamDone( this );
            Log4J.info(this, "Gracefully destroying...");
            Log4J.info(this, String.format("Process Orchestrator for session %s is destroyed!", sessionId));
            if (callback != null) callback.destroyInCascade(this);
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
    public Pluggable processMsg(SessionMessage message){
        try {
            if ( message == null || message.getMessageId() == null) {
                throw new MultiuserException(ErrorMessages.OBJECT_NULL, "message");
            }
            Class<? extends Pluggable> clazz = ResourceLocator.getMsgMapping(message.getMessageId());
            if (clazz == null) {
                for( Pluggable component : components ){
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
            for (Pluggable component : components) {
                if (component.getClass() == clazz) {
                    return component;
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
        return null;
    }


    public void executeSync(List<Pluggable> components) {
        try {
            for (Pluggable component : components) {
                component.setActiveSession( session );
                execute(component);
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void executeAsync(List<Pluggable> components) {
        try{
            for (Pluggable component : components) {
                component.setActiveSession( session );
                Utils.execute(() -> {
                    try{
                        execute(component);
                    }catch (Throwable e){
                        ExceptionHandler.handle( e );
                    }
                });
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void forceSync(List<Pluggable> asynComponents){
        checkComponents( asynComponents );
        forceSync(asynComponents, blackboard);
    }

    public void forceSync(List<Pluggable> asynComponents, List<SynchronizableEvent> events){
        checkComponents( asynComponents );
        forceSync(asynComponents, events, blackboard);
    }

    private void forceSync(List<Pluggable> asynComponents, Blackboard blackboard) {
        forceSync( asynComponents, null, blackboard);
    }

    private void forceSync(List<Pluggable> asynComponents, List<SynchronizableEvent> events,
                           Blackboard blackboard) {
        try {
            if (asynComponents != null && !asynComponents.isEmpty() && blackboard != null) {
                List<CompSyncEvent> compSyncEvents = new ArrayList<>();
                for (int i = 0; i < asynComponents.size(); i++) {
                    asynComponents.get(i).setActiveSession( session );
                    compSyncEvents.add(new CompSyncEvent(asynComponents.get(i),
                            events == null || i >= events.size() ? null : events.get(i)));
                }
                Pluggable component = compSyncEvents.get(0).component;
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

    private void addAsyncEvents( String id, Blackboard blackboard ) throws Throwable{
        Queue<CompSyncEvent> queue = ResourceLocator.getSyncMap( id );
        if( !queue.isEmpty() ){
            if( queue.peek().event != null ){
                blackboard.post(this,id, (SynchronizableEvent) () -> {
                    try {
                        addAsyncEvents(id, blackboard);
                        if (!queue.isEmpty()) {
                            CompSyncEvent compSyncEvent = queue.poll();
                            compSyncEvent.component.execute();
                            compSyncEvent.event.notifyNext();
                        }
                    }catch (Throwable e){
                        ExceptionHandler.handle(e);
                    }
                });
            }else {
                blackboard.post(this,id, (SynchronizableEvent) () -> {
                    try {
                        addAsyncEvents(id, blackboard);
                        if (!queue.isEmpty()) {
                            queue.poll().component.execute();
                        }
                    }catch (Throwable e){
                        ExceptionHandler.handle(e);
                    }
                });
            }
        }
    }

    static class CompSyncEvent{
        Pluggable component;
        SynchronizableEvent event;

        public CompSyncEvent(Pluggable component, SynchronizableEvent event) {
            this.component = component;
            this.event = event;
        }
    }

    private void checkComponents( List<Pluggable> otherComponents ){
        if( components == null || components.isEmpty() ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.COMPONENTS_NULL) );
        }
        try {
            for (Pluggable component : otherComponents) {
                if (!components.contains(component)) {
                    if (component instanceof Pluggable) {
                        component.addMessageLogger(sessionId, logger);
                        if( component instanceof BlackboardListener)
                            ((BlackboardListener) component).addBlackboard(sessionId, blackboard);
                    }
                    components.add(component);
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    /**
     * Use this method to notify remote subscribers when some specific messages (@subMessages) are triggered.
     * We use this method only if the remote service has provided a list of subscription messages, otherwise
     * it will be responsibility of developer to decide when to send messages to remote service.
     */
    private List<ExternalComponent> createExternalComponents() throws Throwable{
        List<ExternalComponent> externalComponents = new ArrayList<>();
        for(ServiceComponent service : ResourceLocator.getServiceRegistry().values() ) {
            if( service.getSubMessages() != null && service.getSubMessages().length > 0 ) {
                externalComponents.add(new ExternalComponent(service.getServiceInfo(), fullAddress, sessionId,
                        service.getMsgTemplate(), service.getSubMessages()));
            }else{
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.NO_SUBSCRIPTION_MESSSAGES,
                        service.getServiceURL()));
            }
        }
        return externalComponents;
    }

    @Override
    @Interceptable
    public void execute(Pluggable component){
        component.execute();
    }

    @Override
    public void onEvent(Blackboard blackboard, BlackboardEvent event) throws Throwable{
        //do nothing
    }

    /** ============================== BlackboardListener ============================ **/

    @Override
    public boolean isClosing(){
        return isClosed.get();
    }

    @Override
    public String getSessionId() throws Throwable{
        return sessionId;
    }



    /** ============================== AfterCreationHook ============================ **/
    public void processHook(Object hook){
        //do some processing here in the extended class
    }

}
