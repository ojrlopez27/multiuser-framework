package edu.cmu.inmind.multiuser.controller.blackboard;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ConnectRemoteService;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.plugin.ExternalComponent;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import edu.cmu.inmind.multiuser.controller.sync.ForceSync;
import edu.cmu.inmind.multiuser.controller.sync.SynchronizableEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oscarr on 4/29/16.
 */
public class Blackboard {
    private ConcurrentHashMap<String, Object> model;
    private ConcurrentHashMap<String, List<BlackboardListener>> subscriptions;
    private List<BlackboardListener> subscribers;
    private MessageLog logger;
    private boolean loggerOn = true;
    private boolean keepModel = true;
    private boolean notifySubscribers = true;

    public Blackboard(){
        subscribers = new ArrayList<>();
        model = new ConcurrentHashMap<>();
        subscriptions = new ConcurrentHashMap<>();
    }

    public Blackboard(Set<PluggableComponent> components, String sessionId) {
        this();
        setComponents(components, sessionId);
    }

    public void setComponents(Set<PluggableComponent> components, String sessionId){
        try {
            if( components == null || components.isEmpty() ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.COMPONENTS_NULL, "") );
            }
            for (PluggableComponent component : components) {
                subscribe(component);
                if (component instanceof PluggableComponent) {
                    component.addBlackboard(sessionId, this);
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void setKeepModel(boolean keepModel) {
        this.keepModel = keepModel;
    }

    public void setNotifySubscribers(boolean notifySubscribers) {
        this.notifySubscribers = notifySubscribers;
    }

    public MessageLog getLogger() {
        return logger;
    }

    public void setLogger(MessageLog logger) {
        this.logger = logger;
    }

    public boolean isLoggerOn() {
        return loggerOn;
    }

    public void setLoggerOn(boolean loggerOn) {
        this.loggerOn = loggerOn;
    }

    public void setModel(ConcurrentHashMap<String, Object> model) {
        this.model = model;
    }

    public void post(BlackboardListener sender, String key, Object element){
        post( sender, key, element, true );
    }

    private void post(BlackboardListener sender, String key, Object element, boolean shouldClone){
        try {
            if( key == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.BLACKBOARD_KEY_NULL, "") );
            }
            if( sender == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.BLACKBOARD_SENDER_NULL, "") );
            }
            if( !key.equals(Constants.REMOVE_ALL) && element == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.BLACKBOARD_ELEMENT_NULL, "") );
            }
            Object clone = shouldClone ? Utils.clone(element) : element;
            if (keepModel && clone != null && key != null)
                model.put(key, clone);
            if (loggerOn){
                logger.add(key, clone == null? "element is null" : clone.toString());
            }
            notifySubscribers(sender, Constants.ELEMENT_ADDED, key, clone);
        }
        catch (NoClassDefFoundError e){
            post( sender, key, element, false);
        }catch(Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public void remove(BlackboardListener sender, String key){
        remove(sender, key, true);
    }

    private void remove(BlackboardListener sender, String key, boolean shouldClone){
        try {
            if( key == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.BLACKBOARD_KEY_NULL) );
            }
            Object clone = Utils.clone(model.get(key));
            if (key.contains(Constants.REMOVE_ALL)) {
                model.clear();
            } else {
                if (keepModel) model.remove(key);
            }
            notifySubscribers(sender, Constants.ELEMENT_REMOVED, key, clone);
        }catch( NoClassDefFoundError e){
            remove(sender, key, false);
        }
        catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    public Object get(String key) {
        return get( key, true );
    }

    private Object get(String key, boolean shouldClone) {
        Object value = null;
        if( key == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.BLACKBOARD_KEY_NULL) );
        }
        try {
            value = shouldClone ? Utils.clone(model.get(key)) : model.get(key);
        }catch (NoClassDefFoundError e){
            value = get(key, false);
        }catch (Throwable e) {
            ExceptionHandler.handle( e );
        }
        return value;
    }

    public ConcurrentHashMap<String, Object> getModel() {
        return model;
    }

    public void subscribe(BlackboardListener subscriber){
        if( subscriber == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.BLACKBOARD_SUBSCRIBER_NULL) );
        }
        subscribers.add( subscriber );
        Class subsClass = Utils.getClass( subscriber );
        if( subscriber instanceof ExternalComponent ){
            //If the component is a ExternalComponent, we cannot modify its annotations because the subscription message
            // list will be overriden.
            subscribe( subscriber, ResourceLocator.getComponentsSubscriptions( subscriber.hashCode() ) );
        }else if (subsClass.isAnnotationPresent(BlackboardSubscription.class)) {
            String[] messages = ((Class<? extends BlackboardListener>)subsClass)
                    .getAnnotation(BlackboardSubscription.class).messages();
            subscribe(subscriber, messages);
        }
    }

    private void subscribe(BlackboardListener subscriber, String[] messages) {
        if( subscriber == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.BLACKBOARD_SUBSCRIBER_NULL));
        }
        if( messages == null || messages.length == 0){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.BLACKBOARD_MESSAGES_NULL));
        }
        for (String message : messages) {
            List<BlackboardListener> listeners = subscriptions.get( message );
            if( listeners == null ){
                listeners = new ArrayList<>();
                subscriptions.put( message, listeners );
            }
            if( !listeners.contains(subscriber) ){
                listeners.add( subscriber );
            }
        }
    }

    public boolean unsubscribe(BlackboardListener subscriber){
        return subscribers.remove( subscriber );
    }

    private void notifySubscribers(BlackboardListener sender, String status, String key, Object element){
        if(notifySubscribers && key != null) {
            try {
                List<BlackboardListener> listeners = subscriptions.get(key);
                if( !key.equals(Constants.REMOVE_ALL) && (listeners == null || listeners.isEmpty() )){
                    ExceptionHandler.handle( new MultiuserException(ErrorMessages.NOBODY_IS_SUBSCRIBED, key) );
                }
                if (listeners != null) {
                    BlackboardEvent event = new BlackboardEvent(status, key, element);
                    final String sessionId = sender.getSessionId();

                    for(BlackboardListener subscriber : listeners ){
                        Utils.execObsParallel(blackboardListener -> {
                            if (subscriber instanceof PluggableComponent) {
                                ((PluggableComponent) subscriber).setActiveSession(sessionId);
                            }
                            try {
//                                Log4J.debug( Blackboard.this, sessionId,
//                                        String.format("Component [%s] sends the following message to component [%s]: %s",
//                                                sender.getClass().getSimpleName(), subscriber.getClass().getSimpleName(),
//                                                element == null? "" : element.toString()) );
                                subscriber.onEvent(event);
                                if (subscriber instanceof PluggableComponent && subscriber.getClass()
                                        .isAnnotationPresent(ConnectRemoteService.class)) {
                                    SessionMessage sessionMessage = new SessionMessage();
                                    sessionMessage.setSessionId(sender.getSessionId());
                                    sessionMessage.setRequestType(status);
                                    sessionMessage.setMessageId(key);
                                    sessionMessage.setPayload(Utils.toJson( event.getElement() ));
                                    ((PluggableComponent) subscriber).send(sessionMessage);
                                    //((PluggableComponent) subscriber).send(sessionMessage, true);
                                }
                            }catch (Throwable e){
                                ExceptionHandler.handle( e );
                            }
                        });
                    }
                }
            } catch (Throwable e) {
                ExceptionHandler.handle(e);
            }
        }
    }

    public BlackboardListener[] getSubscribers() {
        return subscribers.toArray( new BlackboardListener[ subscribers.size()] );
    }

    public void reset(){
        new Thread("BlackboardResetThread"){
            public void run(){
                try {
                    model.clear();
                }catch (Throwable e){
                    ExceptionHandler.handle(e);
                }
            }
        }.start();
    }


    public SynchronizableEvent getSyncEvent(PluggableComponent keyElement) {
        return getSyncEvent( keyElement, true );
    }

    private SynchronizableEvent getSyncEvent(PluggableComponent keyElement, boolean shouldClone) {
        Object value = null;
        try {
            if (keyElement.getClass().isAnnotationPresent(ForceSync.class)) {
                ForceSync annotation = keyElement.getClass().getAnnotation(ForceSync.class);
                value = shouldClone? Utils.clone(model.get( annotation.id() )) : model.get(annotation.id());
            }
        }catch (NoClassDefFoundError e){
            value = getSyncEvent( keyElement, false );
        }catch(Throwable e) {
            ExceptionHandler.handle( e );
        } finally {
            return (SynchronizableEvent) value;
        }
    }
}
