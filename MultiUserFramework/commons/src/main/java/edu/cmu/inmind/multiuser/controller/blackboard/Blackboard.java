package edu.cmu.inmind.multiuser.controller.blackboard;

import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.plugin.Pluggable;
import edu.cmu.inmind.multiuser.controller.sync.SynchronizableEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oscarr on 11/6/17.
 */
public interface Blackboard {
    SynchronizableEvent getSyncEvent(Pluggable component);
    void post(BlackboardListener sender, String key, Object element);
    void setShouldThrowException(boolean shouldThrowException);
    void setComponents(Set<Pluggable> components, String sessionId) throws Throwable;
    void setKeepModel(boolean keepModel);
    void setNotifySubscribers(boolean notifySubscribers);
    MessageLog getLogger();
    void setLogger(MessageLog logger);
    boolean isLoggerOn();
    void setLoggerOn(boolean loggerOn);
    void setModel(ConcurrentHashMap<String, Object> model);
    void remove(BlackboardListener sender, String key) throws Throwable;
    Object get(String key) throws Throwable;
    ConcurrentHashMap<String, Object> getModel();
    void subscribe(BlackboardListener subscriber) throws Throwable;
    boolean unsubscribe(BlackboardListener subscriber);
    BlackboardListener[] getSubscribers();
    void reset() throws Throwable;
    boolean isModelKept();
    boolean areSubscribersNotified();
    ConcurrentHashMap<String, List<BlackboardListener>> getSubscriptions();
    List<BlackboardListener> getSubscription (String key) throws Throwable;
}
