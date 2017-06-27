package edu.cmu.inmind.multiuser.controller.resources;


import com.google.common.util.concurrent.ServiceManager;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.communication.*;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.session.ServiceComponent;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by oscarr on 3/21/17.
 */
public class ResourceLocator {
    private static Map<String, ServiceComponent> serviceRegistry = new HashMap<>();
    private static Map<Object, List<String>> componentsRegistry = new HashMap<>();
    private static Map<String, Class<? extends PluggableComponent>> messageMapping = new HashMap<>();
    private static Map<String, Queue> syncMap = new HashMap<>();
    private static Map<ServiceManager, String> serviceManagers = new HashMap<>();
    private static Map<Class, Logger> loggers = new HashMap<>();

    /**
     * This method registers remote or external services that can be looked up by other components
     *
     * @param request
     */
    public static void registerService(SessionMessage request, ZMsgWrapper msg, String payload) {
        ServiceComponent serviceComponent = serviceRegistry.get(request.getSessionId());
        serviceComponent = serviceComponent != null ? serviceComponent.setServiceURL(request.getUrl())
                : new ServiceComponent(null, request.getUrl(), msg.duplicate());
        if( payload != null && !payload.equals("null") && payload.length() > 2 ){
            serviceComponent.setSubMessages(payload.substring(1, payload.length() - 1).split(", "));
        }
        serviceRegistry.put(request.getSessionId(), serviceComponent);
    }

    public static void unregisterService(SessionMessage request) {
        serviceRegistry.remove(request.getSessionId());
    }

    public static void addComponentToService(String serviceId, Class<? extends PluggableComponent> component,
                                             ZMsgWrapper msg) {
        ServiceComponent serviceComponent = serviceRegistry.get( serviceId );
        serviceComponent = serviceComponent != null ? serviceComponent.setComponent(component)
                : new ServiceComponent(component, null, msg.duplicate());
        serviceRegistry.put(serviceId, serviceComponent);
    }

    public static Map<String, ServiceComponent> getServiceRegistry() {
        return serviceRegistry;
    }

    public static void addServiceToComponent(Set<PluggableComponent> components, String sessionId, String fullAddress)
            throws Throwable{
        for (PluggableComponent component : components) {
            addServiceToComponent(component, sessionId, fullAddress);
        }
    }

    public static void addServiceToComponent(PluggableComponent component, String sessionId, String fullAddress)
            throws Throwable{
        if (component.getClass().isAnnotationPresent(ConnectRemoteService.class)) {
            String servideId = component.getClass().getAnnotation(ConnectRemoteService.class).remoteService();
            ServiceComponent serviceComponent = serviceRegistry.get(servideId);
            if (serviceComponent != null && serviceComponent.getServiceURL() != null ) {
                if (serviceComponent.getComponent() == null) {
                    serviceComponent.setComponent( component.getClass() );
                }
                component.setClientCommController(new ClientCommController(serviceComponent.getServiceURL(),
                        sessionId, fullAddress, Constants.REQUEST_CONNECT,
                        serviceComponent.getMsgTemplate()));
            }else {
                throw new MultiuserException(ErrorMessages.SERVICE_NOT_REGISTERED, servideId );
            }
        }
    }

    public static Set<PluggableComponent> addComponentsToRegistry(Set<PluggableComponent> components, String sessionId) {
        Set<PluggableComponent> newComponents = new HashSet<>();
        for( PluggableComponent component : components ) {
            List<String> sessionIds = componentsRegistry.get( component );
            if( sessionIds == null ){
                sessionIds = new ArrayList<>();
                sessionIds.add( sessionId );
                newComponents.add( component );
            }else{
                if( !sessionIds.contains( sessionId ) ){
                    sessionIds.add( sessionId );
                }
            }
            componentsRegistry.put( component, sessionIds );
        }
        return newComponents;
    }

    public static ServiceComponent getService(String serviceId) {
        return serviceRegistry.get(serviceId);
    }

    public static void addMsgMapping(String message, Class<? extends PluggableComponent> component){
        messageMapping.put(message, component);
    }

    public static Class<? extends PluggableComponent> getMsgMapping(String message){
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

    public static Map<ServiceManager, String> getServiceManagers() {
        return serviceManagers;
    }

    public static Logger getLogger(Class clazz){
        return loggers.get(clazz);
    }

    public static void addLogger(Class clazz, Logger logger){
        loggers.put(clazz, logger);
    }

}
