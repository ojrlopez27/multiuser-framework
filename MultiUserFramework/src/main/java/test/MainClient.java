package test;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.log.LogC;

import java.util.Random;

/**
 * Created by oscarr on 10/16/18.
 */
class MainClient {
    private ClientCommController commController;
    private String serverAddress = "tcp://127.0.0.1:5555";
    private String sessionId = "my-session-id";
    private ResponseListener responseListener;

    public MainClient() {
        this.responseListener = new MyResponseListener();
        commController = new ClientCommController.Builder(new LogC())
                .setServerAddress(this.serverAddress)
                .setSessionId(this.sessionId)
                .setRequestType(Constants.REQUEST_CONNECT)
                .setResponseListener(this.responseListener)
                .build();
    }

    public void send(Object message){
        try {
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setPayload(message.toString());
            sessionMessage.setSessionId(sessionId);
            commController.send(sessionId, CommonUtils.toJson(sessionMessage));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void disconnect() {
        commController.disconnect(sessionId);
    }

    class MyResponseListener implements ResponseListener {
        private int step = 1;

        @Override
        public void process(String message) {
            LogC.info(this, "Response from server: " + message);
            for(int i = 1; i < 5; i++){
                send(String.format("message from process: %s.%s", step, i));
            }
            step++;
        }
    }



    public static void main(String args[]){
        MainClient mc = new MainClient();

        //let's simulate a human user also sending messages in parallel
        CommonUtils.execute(new Runnable() {
            @Override
            public void run() {
                int userMessage = 1;
                while( !Thread.currentThread().isInterrupted() ){
                    Random random = new Random();
                    mc.send("message from user: " + userMessage);
                    userMessage++;
                    CommonUtils.sleep(random.nextInt(20));
                }
            }
        });
    }
}
