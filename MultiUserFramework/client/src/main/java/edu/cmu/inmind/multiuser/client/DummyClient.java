package edu.cmu.inmind.multiuser.client;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 5/8/18.
 */
public class DummyClient {
    private ClientCommController commController;
    private String serverAddress = "tcp://127.0.0.1:5555";
    private String sessionId = "my-session-id";
    private ResponseListener responseListener;

    public DummyClient(String serverAddress, String sessionId, ResponseListener responseListener) {
        if( serverAddress != null ) this.serverAddress = serverAddress;
        if( sessionId != null ) this.sessionId = sessionId;
        if( responseListener != null ){
            this.responseListener = responseListener;
        }else{
            this.responseListener = new MyResponseListener();
        }
        commController = new ClientCommController.Builder()
                .setServerAddress(this.serverAddress)
                .setSessionId(this.sessionId)
                .setRequestType(Constants.REQUEST_CONNECT)
                .setResponseListener(this.responseListener)
                .build();
    }

    public DummyClient(){
        this(null, null, null);
    }

    public void test() {
        send("test");
        Utils.sleep(3000);
        System.out.println("Done!");
    }

    public void send(Object message){
        try {
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setPayload(message.toString());
            sessionMessage.setSessionId(sessionId);
            commController.send(sessionId, Utils.toJson(sessionMessage));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void disconnect() {
        commController.disconnect(sessionId);
    }

    public Boolean getIsConnected() {
        return commController.getIsConnected().get();
    }

    class MyResponseListener implements ResponseListener {
        @Override
        public void process(String message) {
            Log4J.debug(this, "Response from server: " + message);
        }
    }
}
