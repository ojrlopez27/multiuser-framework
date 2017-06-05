package edu.cmu.inmind.multiuser.controller.blackboard;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ConnectRemoteService;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.sync.ForceSync;
import edu.cmu.inmind.multiuser.controller.sync.SynchronizableEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by oscarr on 4/29/16.
 */
public class Blackboard {
    private ConcurrentHashMap<String, Object> model;
    private ConcurrentHashMap<String, List<BlackboardListener>> subscriptions;
    private List<BlackboardListener> subscribers;
    private Lock lock;
    private Condition canAdd;
    private Condition canRetrieve;
    private Condition canRemove;
    private boolean isInserting;
    private boolean isRetrieving;
    private boolean isRemoving;
    private MessageLog logger;
    private boolean loggerOn = true;
    private boolean keepModel = true;
    private boolean notifySubscribers = true;

    public Blackboard(){
        subscribers = new ArrayList<>();
        model = new ConcurrentHashMap<>();
        subscriptions = new ConcurrentHashMap<>();
        lock = new ReentrantLock();
        canAdd = lock.newCondition();
        canRetrieve = lock.newCondition();
        canRemove = lock.newCondition();
    }

    public Blackboard(Set<PluggableComponent> components, String sessionId) {
        this();
        setComponents(components, sessionId);
    }

    public void setComponents(Set<PluggableComponent> components, String sessionId){
        try {
            for (PluggableComponent component : components) {
                subscribe(component);
                if (component instanceof PluggableComponent) {
                    component.addBlackboard(sessionId, this);
                }
            }
        }catch (Exception e){
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
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock( 10, TimeUnit.MILLISECONDS);
            if( isLocked ) {
                while (isRetrieving || isRemoving) {
                    canAdd.await();
                }
                isInserting = true;
                Object clone = shouldClone ? Utils.clone(element) : element;
                if (keepModel) model.put(key, clone);
                isInserting = false;
                canRetrieve.signalAll();
                canRemove.signalAll();
                if (loggerOn) {
                    logger.add(key, clone.toString());
                }
                notifySubscribers(sender, Constants.ELEMENT_ADDED, key, clone);
            }
        }
        catch (NoClassDefFoundError e){
            post( sender, key, element, false);
        }catch(Exception e){
        }finally {
            if( isLocked ) {
                lock.unlock();
            }
        }
    }

    public void remove(BlackboardListener sender, String key){
        remove(sender, key, true);
    }

    private void remove(BlackboardListener sender, String key, boolean shouldClone){
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock( 10, TimeUnit.MILLISECONDS);
            if( isLocked ) {
                while (isRetrieving || isInserting) {
                    canAdd.await();
                }
                isRemoving = true;
                Object clone = Utils.clone(model.get(key));
                if (key.contains(Constants.REMOVE_ALL)) {
                    model.clear();
                } else {
                    if (keepModel) model.remove(key);
                }
                isRemoving = false;
                canAdd.signalAll();
                canRetrieve.signalAll();
                notifySubscribers(sender, Constants.ELEMENT_REMOVED, key, clone);
            }
        }catch( NoClassDefFoundError e){
            remove(sender, key, false);
        }
        catch (Exception e){
            ExceptionHandler.handle( e );
        }finally {
            if( isLocked ) {
                lock.unlock();
            }
        }
    }

    public Object get(String key) {
        return get( key, true );
    }

    private Object get(String key, boolean shouldClone) {
        Object value = null;
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock( 10, TimeUnit.MILLISECONDS);
            if( isLocked ) {
                while (isInserting || isRemoving) {
                    canRetrieve.await();
                }
                isRetrieving = true;
                lock.lock();
                value = shouldClone ? Utils.clone(model.get(key)) : model.get(key);
                isRetrieving = false;
                canAdd.signalAll();
                canRemove.signalAll();
            }
        }catch (NoClassDefFoundError e){
            value = get(key, false);
        }catch (Exception e) {
        }finally {
            if( isLocked ) {
                lock.unlock();
            }
        }
        return value;
    }

    public ConcurrentHashMap<String, Object> getModel() {
        return model;
    }

    public void subscribe(BlackboardListener subscriber){
        subscribers.add( subscriber );
        Class subsClass = Utils.getClass( subscriber );
        if (subsClass.isAnnotationPresent(BlackboardSubscription.class)) {
            String[] messages = ((Class<? extends BlackboardListener>)subsClass)
                    .getAnnotation(BlackboardSubscription.class).messages();
            subscribe(subscriber, messages);
        }
    }

    public void subscribe(BlackboardListener subscriber, String[] messages) {
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
        if(notifySubscribers) {
            try {
                List<BlackboardListener> listeners = subscriptions.get(key);
                if (listeners != null) {
                    for (BlackboardListener subscriber : listeners) {
                        BlackboardEvent event = new BlackboardEvent(status, key, element);
                        if (subscriber instanceof PluggableComponent) {
                            ((PluggableComponent) subscriber).setActiveSession(sender.getSessionId());
                        }
                        new Thread("NotifyBlackboardSubscribersThread"){
                            public void run(){
                                subscriber.onEvent(event);
                            }
                        }.start();
                        if (subscriber instanceof PluggableComponent && subscriber.getClass()
                                .isAnnotationPresent(ConnectRemoteService.class)) {
                            SessionMessage sessionMessage = new SessionMessage();
                            sessionMessage.setSessionId(sender.getSessionId());
                            sessionMessage.setRequestType(status);
                            sessionMessage.setMessageId(key);
                            sessionMessage.setPayload(Utils.toJson( event.getElement() ));
                            ((PluggableComponent) subscriber).send(sessionMessage, false);
                        }
                    }
                }
            } catch (Exception e) {
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
                boolean isLocked = false;
                try {
                    isLocked = lock.tryLock( 10, TimeUnit.MILLISECONDS);
                    if( isLocked ) model.clear();
                }catch (Exception e){
                    ExceptionHandler.handle(e);
                }finally {
                    is( isLocked ) lock.unlock();
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
            lock.lock();
            if (keyElement.getClass().isAnnotationPresent(ForceSync.class)) {
                ForceSync annotation = keyElement.getClass().getAnnotation(ForceSync.class);
                value = shouldClone? Utils.clone(model.get( annotation.id() )) : model.get(annotation.id());
            }
        }catch (NoClassDefFoundError e){
            value = getSyncEvent( keyElement, false );
        }catch(Exception e) {
            ExceptionHandler.handle( e );
        } finally {
            lock.unlock();
            return (SynchronizableEvent) value;
        }
    }
}
