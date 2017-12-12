package edu.cmu.inmind.multiuser.controller.resources;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import edu.cmu.inmind.multiuser.controller.common.*;
import edu.cmu.inmind.multiuser.controller.communication.*;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.Pluggable;
import edu.cmu.inmind.multiuser.controller.plugin.StateType;
import edu.cmu.inmind.multiuser.controller.session.Session;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 3/21/17.
 */
public class ResourceLocator {
    private static ConcurrentHashMap<String, ServiceComponent> serviceRegistry = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Class<? extends Pluggable>> messageMapping = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Queue> syncMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<ServiceManager, String> serviceManagers = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Class, Logger> loggers = new ConcurrentHashMap<>();
    private static Cache<String, Object> cache;
    private static ConcurrentHashMap<Integer, String[]> componentsSubscriptions = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Pluggable, List<String>> statelessCompRegistry = new ConcurrentHashMap<>();
    /** This ServiceManager is only used for Stateless and Pool components */
    private static ServiceManager statelessServManager;
    private static ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * This method registers remote or external services that can be looked up by other components
     *
     * @param request
     */
    public static void registerService(SessionMessage request, ZMsgWrapper msg, String payload) {
        if( request == null || msg == null || payload == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "request: "
                    + request, "msg: " + msg, "payload: " + payload) );
        }
        ServiceInfo serviceInfo = new ServiceInfo.Builder()
                .setServiceName(request.getMessageId())
                .setSlaveMUFAddress( request.getUrl() )
                .setRequestType( request.getRequestType() )
                .build();
        ServiceComponent serviceComponent = serviceRegistry.get(request.getSessionId());
        serviceComponent = serviceComponent != null ? serviceComponent.setServiceURL(request.getUrl())
                : new ServiceComponent(null, serviceInfo, msg.duplicate());
        if( payload != null && !payload.equals("null") && payload.length() > 2 ){
            serviceComponent.setSubMessages(payload.substring(1, payload.length() - 1).split(", "));
        }
        serviceRegistry.put(request.getSessionId(), serviceComponent);
    }


    public static void registerService(ServiceInfo serviceInfo) {
        if( serviceInfo == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "serviceInfo: "
                    + serviceInfo) );
        }
        if( serviceInfo.getMasterMUFAddress() == null || serviceInfo.getSlaveMUFAddress() == null
                || serviceInfo.getMasterMUFAddress().isEmpty() || serviceInfo.getSlaveMUFAddress().isEmpty() ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.MASTER_SLAVE_NOT_NULL, "Master MUF address: "
                    + serviceInfo.getMasterMUFAddress(), "Slave MUF address: " + serviceInfo.getSlaveMUFAddress() ) );
        }
        if( !Utils.isURLvalid( serviceInfo.getMasterMUFAddress() ) ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.INCORRECT_IP_ADDRESS,
                    serviceInfo.getMasterMUFAddress()));
        }
        if( !Utils.isURLvalid( serviceInfo.getSlaveMUFAddress() ) ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.INCORRECT_IP_ADDRESS,
                    serviceInfo.getSlaveMUFAddress()));
        }
        ServiceComponent serviceComponent = serviceRegistry.get( serviceInfo.getServiceName() );
        serviceComponent = serviceComponent != null ? serviceComponent.setServiceInfo( serviceInfo )
                : new ServiceComponent(null, serviceInfo, null);
        if( serviceInfo.getMsgSubscriptions() != null ){
            serviceComponent.setSubMessages( serviceInfo.getMsgSubscriptions() );
        }
        serviceRegistry.put(serviceInfo.getServiceName() , serviceComponent);
    }

    public static void unregisterService(SessionMessage request) {
        serviceRegistry.remove(request.getSessionId());
    }

    public static void addComponentToService(String serviceId, Class<? extends Pluggable> component,
                                             ZMsgWrapper msg) {
        if( component == null || msg == null){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "component: "
                    + component, "msg: " + msg) );
        }
        ServiceComponent serviceComponent = serviceRegistry.get( serviceId );
        serviceComponent = serviceComponent != null ? serviceComponent.setComponent(component)
                : new ServiceComponent(component, null, msg.duplicate());
        serviceRegistry.put(serviceId, serviceComponent);
    }

    public static ConcurrentHashMap<String, ServiceComponent> getServiceRegistry() {
        return serviceRegistry;
    }

    public static List<Pair<Pluggable, ServiceComponent>> addServiceToComponent(Set<Pluggable> components, String sessionId, String fullAddress)
            throws Throwable{
        List<Pair<Pluggable, ServiceComponent>> services = new ArrayList<>();
        for (Pluggable component : components) {
            services.add( addServiceToComponent(component, sessionId, fullAddress) );
        }
        return services;
    }

    public static Pair<Pluggable, ServiceComponent> addServiceToComponent(Pluggable component, String sessionId, String fullAddress)
            throws Throwable{
        if( component == null || sessionId == null || sessionId.isEmpty() || fullAddress == null || fullAddress.isEmpty()){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "sessionId: "
                    + sessionId, "component: " + component, "fulAddress: " + fullAddress) );
        }
        if (component.getClass().isAnnotationPresent(ConnectRemoteService.class)) {
            String serviceId = component.getClass().getAnnotation(ConnectRemoteService.class).remoteService();
            if( serviceRegistry.get( serviceId ) != null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.DUPLICATED_SERVICE_DEFINITION, serviceId) );
            }
            ServiceComponent serviceComponent = serviceRegistry.get(serviceId);
            if (serviceComponent != null && serviceComponent.getServiceURL() != null ) {
                if (serviceComponent.getComponent() == null) {
                    serviceComponent.setComponent( component.getClass() );
                }
                return new Pair<>( component, serviceComponent);
            }else {
                throw new MultiuserException(ErrorMessages.SERVICE_NOT_REGISTERED, serviceId );
            }
        }
        return null;
    }

    public static void addComponentsToRegistry(final ProcessOrchestrator orchestrator, final Set<Pluggable> components,
                                                         final String sessionId) throws Throwable{
        try{
            if( components == null || components.isEmpty() || sessionId == null || sessionId.isEmpty() ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "sessionId: "
                        + sessionId, "components: " + components, "sessionId: " + sessionId) );
            }
            ConcurrentHashMap<Pluggable, List<String>> statefullCompRegistry = new ConcurrentHashMap<>();
            List<String> statefullSessionIds = null, statelessSessionIds = null;
            for( Pluggable component : components ) {
                if( component.getClass().isAnnotationPresent(StateType.class) &&
                        component.getClass().getAnnotation(StateType.class).state().equals(Constants.STATEFULL) ) {
                    statefullSessionIds = getSessionIds(statefullCompRegistry, component, sessionId);
                    statefullCompRegistry.put( component, statefullSessionIds );
                }else{
                    statelessSessionIds = getSessionIds(statelessCompRegistry, component, sessionId);
                    statelessCompRegistry.put( component, statelessSessionIds );
                }
            }
            statelessServManager = initServices( statelessServManager, orchestrator, (Set) statelessCompRegistry.keySet() );
            orchestrator.setStatefullServManager( initServices( null, orchestrator, (Set) statefullCompRegistry.keySet() ) );
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }


    private static List<String> getSessionIds(ConcurrentHashMap<Pluggable, List<String>> componentsRegistry,
                                       Pluggable component, String sessionId){
        List<String> sessionIds = componentsRegistry.get( component );
        if( sessionIds == null ){
            sessionIds = new ArrayList<>();
            sessionIds.add( sessionId );
        }else{
            if( !sessionIds.contains( sessionId ) ){
                sessionIds.add( sessionId );
            }
        }
        return sessionIds;
    }



    public static ServiceComponent getService(String serviceId) {
        return serviceRegistry.get(serviceId);
    }

    public static void addMsgMapping(String message, Class<? extends Pluggable> component){
        if( component == null || message == null || message.isEmpty() ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "component: "
                    + component, "message: " + message) );
        }
        messageMapping.put(message, component);
    }

    public static Class<? extends Pluggable> getMsgMapping(String message){
        return messageMapping.get(message);
    }

    public static Queue getSyncMap( Object id ){
         return syncMap.get( id );
    }

    public static void putSyncMap(String id, Queue queue) {
        syncMap.put(id, queue);
    }

    public static void addServiceManager(ServiceManager serviceManager, String status) {
        serviceManagers.put(serviceManager, status);
    }

    public static ConcurrentHashMap<ServiceManager, String> getServiceManagers() {
        return serviceManagers;
    }

    public static Logger getLogger(Class clazz){
        return loggers.get(clazz);
    }

    public static void addLogger(Class clazz, Logger logger){
        loggers.put(clazz, logger);
    }


    /**
     * Cache Memmory
     */
    static{
        cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                //.expireAfterWrite(10, TimeUnit.MINUTES)
                //.removalListener(MY_LISTENER)
                .build();
    }

    public static void toCache(String key, Object value){
        cache.put( key, value );
    }

    public static Object fromCache(String key){
        return cache.asMap().get(key);
    }

    public static void addComponentSubscriptions(int hashcode, String[] messages) {
        if( hashcode <=0 || messages == null || messages.length <= 0 ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "hashcode: "
                    + hashcode, "messages: " + messages) );
        }
        componentsSubscriptions.put( hashcode, messages );
    }

    public static String[] getComponentsSubscriptions(int hashcode) {
        return componentsSubscriptions.get(hashcode);
    }


    public static ServiceManager initServices(ServiceManager serviceManager, final ProcessOrchestrator orchestrator,
                                              Set<Service> services ) throws Throwable{

        if( services != null && !services.isEmpty() && serviceManager == null ) {
            Service service = services.toArray(new Pluggable[services.size()])[0];
            final boolean isStatefull = ((Pluggable)service).getType().equals(Constants.STATEFULL);
            serviceManager = new ServiceManager(services);
            final ServiceManager sm = serviceManager;
            serviceManager.addListener(
                new ServiceManager.Listener() {
                    public void stopped() {
                        try {
                            Log4J.info(orchestrator, String.format("All %s components have been shut down. " +
                                            "Closing ServiceManager for Session: %s", isStatefull? "statefull" : "stateless",
                                    orchestrator.getSessionId()));
                            if (isStatefull) orchestrator.setStatus(Constants.ORCHESTRATOR_STOPPED);
                            ResourceLocator.addServiceManager(sm, Constants.SERVICE_MANAGER_STOPPED);
                        } catch (Throwable e) {
                            ExceptionHandler.handle(e);
                        }
                    }

                    public void healthy() {
                        try {
                            // Services have been initialized and are healthy, start accepting requests...
                            Log4J.info(orchestrator, String.format("ServiceManager has initialized all the %s " +
                                    "components for session: %s", isStatefull? "statefull" : "stateless",
                                    orchestrator.getSessionId()));
                            if (isStatefull) orchestrator.setStatus(Constants.ORCHESTRATOR_STARTED);
                        } catch (Throwable e) {
                            ExceptionHandler.handle(e);
                        }
                    }

                    public void failure(Service service) {
                        try {
                            // Something failed, at this point we could log it, notify a load balancer, or take
                            // some other action.  For now we will just exit.
                            Log4J.error(orchestrator, String.format("There was a failure with service/component: %s " +
                                    "in session: %s", service.getClass().getName(), orchestrator.getSessionId()));
                        } catch (Throwable e) {
                            ExceptionHandler.handle(e);
                        }
                    }
                },
                Utils.getExecutor() );
            serviceManager.startAsync();
            addServiceManager(serviceManager, Constants.SERVICE_MANAGER_STARTED);
        }
        return serviceManager;
    }

    public static void stopStatlessComp() throws Throwable{
        if( statelessServManager != null ){
            statelessServManager.stopAsync().awaitStopped(20, TimeUnit.SECONDS);
        }
    }



    /** ======================== ZeroMQ Contexts =================================== **/

    /**
     * ZMQ docs: You should create and use exactly one context in your process. Technically, the context is the
     * container for all sockets in a single process, and acts as the transport for inproc sockets, which are the
     * fastest way to connect threads in one process. If at runtime a process has two contexts, these are like separate
     * ZeroMQ instances
     */
    private static ZContext context;
    private static ConcurrentHashMap<DestroyableCallback, Boolean> destroyables = new ConcurrentHashMap<>();
    private static CopyOnWriteArrayList<ZMQ.Socket> sockets = new CopyOnWriteArrayList<>();
    private static CopyOnWriteArrayList<ZContext> contexts = new CopyOnWriteArrayList<>();


    public static ZContext getContext(DestroyableCallback contextOwner) {
        if( context == null ){
            context = new ZContext();
        }
        // contexts keeps a record about which owner has released its context.
        // at initialization, nobody has released it.
        destroyables.put(contextOwner, false);
        ZContext ctx = ZContext.shadow(context);
        contexts.add(ctx);
        return ctx;
    }

    public static void setIamDone(DestroyableCallback contextOwner){
        if( destroyables.get(contextOwner) != null )
            destroyables.put(contextOwner, true);
    }

    public static ZMQ.Socket createSocket(ZContext ctx, int type){
        ZMQ.Socket socket = ctx.createSocket(type);
        sockets.add(socket);
        return socket;
    }

    public static void closeContexts(){
        try {
            if (context != null && destroyables != null) {
                boolean allTerminated;
                do {
                    allTerminated = true;
                    for (DestroyableCallback key : destroyables.keySet()) {
                        if (!destroyables.get(key)) {
                            key.close(null);
                            allTerminated = false;
                            Utils.sleep(50);
                            break;
                        }
                    }
                } while (!allTerminated);

                try {
                    for (ZContext ctx : contexts) {
                        ctx.destroy();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //context.destroy();
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    public static void addSession(Session session){
        sessions.put( session.getId(), session );
    }

    public static Pluggable getComponent(String sessionId, Class<? extends Pluggable> componentClass) throws Exception{
        Session session = sessions.get(sessionId);
        if( session == null ){
            throw new MultiuserException(ErrorMessages.SESSION_NOT_EXIST, sessionId );
        }
        Pluggable component = session.getOrchestrator().get(componentClass);
        if( component != null ){
            return component;
        }else{
            throw new MultiuserException(ErrorMessages.COMPONENT_NOT_IN_SESSION, componentClass, sessionId );
        }
    }
}
