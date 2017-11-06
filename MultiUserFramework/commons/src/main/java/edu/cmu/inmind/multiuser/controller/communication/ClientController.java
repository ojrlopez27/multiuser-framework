package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.controller.common.DestroyableCallback;

/**
 * Created by oscarr on 11/6/17.
 */
public interface ClientController {
    void setShouldProcessReply(boolean shouldProcessReply);
    void send(String serviceId, Object message);
    void setResponseListener(ResponseListener responseListener);
    void disconnect(String sessionId);
    void close(DestroyableCallback callback);
    ResponseListener getResponseListener();
}
