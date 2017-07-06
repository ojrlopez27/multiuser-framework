
package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Pair;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import org.zeromq.ZMsg;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by oscarr on 3/29/17.
 */

public class ClientCommController {
    private ClientCommAPI clientCommAPI;
    private String serviceName;
    private String sessionManagerService;
    private String serverAddress;
    private String clientAddress;
    private String requestType;
    private String[] subscriptionMessages;
    private ZMsgWrapper msgTemplate;
    private Thread sendThread;
    private Thread receiveThread;

    //we need to keep the state in case of failure and reconnection
    private int sentMessages = 0;
    private int receivedMessages = 0;
    private final ClientMessage clientMessage;

    //control
    private boolean stop = false;
    private boolean checkNumSent = true;
    private int     difference = 10; //if sentMessages > receivedMessages + difference => stop
    private String release = Constants.CONNECTION_FINISHED;
    private String  sendState = Constants.CONNECTION_NEW;
    private String  receiveState = Constants.CONNECTION_NEW;
    private ResponseListener responseListener;
    private boolean shouldProcessReply;
    private boolean isTCPon;
    private MultiuserFramework muf;

    public ClientCommController( Builder builder ){
        this.isTCPon = builder.isTCPon;
        this.serviceName = builder.serviceName;
        this.serverAddress = builder.serverAddress;
        this.clientAddress = builder.clientAddress;
        this.msgTemplate = builder.msgTemplate != null? builder.msgTemplate.duplicate() : null;
        this.requestType = builder.requestType;
        this.subscriptionMessages = builder.subscriptionMessages;
        this.shouldProcessReply = builder.shouldProcessReply;
        this.responseListener = builder.responseListener;
        this.sessionManagerService = builder.sessionManagerService;
        this.clientMessage = new ClientMessage();
        this.muf = builder.muf;
        if( this.muf != null ){
            muf.setClient( this );
        }
        execute();
    }

    public static class Builder{
        private boolean isTCPon = true;
        private String serviceName;
        private String serverAddress = "tcp://127.0.0.1:5555";
        private String clientAddress = "tcp://127.0.0.1:5555";
        private ZMsgWrapper msgTemplate;
        private String requestType;
        private String [] subscriptionMessages;
        private MultiuserFramework muf;
        private boolean shouldProcessReply = true;
        private ResponseListener responseListener;
        private String sessionManagerService = Constants.SESSION_MANAGER_SERVICE;
        private ClientMessage clientMessage;

        public ClientCommController build(){
            return new ClientCommController( this );
        }

        public Builder setTCPon(boolean TCPon) {
            isTCPon = TCPon;
            return this;
        }

        public Builder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder setClientAddress(String clientAddress) {
            this.clientAddress = clientAddress;
            return this;
        }

        public Builder setMsgTemplate(ZMsgWrapper msgTemplate) {
            this.msgTemplate = msgTemplate;
            return this;
        }

        public Builder setRequestType(String requestType) {
            this.requestType = requestType;
            return this;
        }

        public Builder setSubscriptionMessages(String[] subscriptionMessages) {
            this.subscriptionMessages = subscriptionMessages;
            return this;
        }

        /**
         * We use this method ONLY when TCP/IP is off, so we only test the orchestrator and pluggable components
         * and therefore we only use one session and one client.
         * @param muf
         * @return
         */
        public Builder setMuf(MultiuserFramework muf) {
            this.muf = muf;
            return this;
        }

        public Builder setShouldProcessReply(boolean shouldProcessReply) {
            this.shouldProcessReply = shouldProcessReply;
            return this;
        }

        public Builder setResponseListener(ResponseListener responseListener) {
            this.responseListener = responseListener;
            return this;
        }

        public Builder setSessionManagerService(String sessionManagerService) {
            this.sessionManagerService = sessionManagerService;
            return this;
        }

        public Builder setClientMessage(ClientMessage clientMessage) {
            this.clientMessage = clientMessage;
            return this;
        }
    }


    public void setShouldProcessReply(boolean shouldProcessReply) {
        this.shouldProcessReply = shouldProcessReply;
    }

    public void setMUF(MultiuserFramework muf) {
        this.muf = muf;
    }

    public ResponseListener getResponseListener() {
        return responseListener;
    }

    /********************************* MAIN THREAD **************************************/
    /************************************************************************************/

    private void execute() {
        if( release.equals(Constants.CONNECTION_FINISHED) ) {
            reset();
            connect();
            sendThread();
            receiveThread();
        }
    }

    private void reset() {
        stop = false;
        sentMessages = 0;
        receivedMessages = 0;
        sendState = checkFSM(sendState, Constants.CONNECTION_NEW);
        receiveState = checkFSM(receiveState, Constants.CONNECTION_NEW);
        release = checkFSM( release, Constants.CONNECTION_NEW);
    }

    private void connect() {
        try {
            if( isTCPon ) {
                this.clientCommAPI = new ClientCommAPI(serverAddress);
                SessionMessage sessionMessage = new SessionMessage();
                sessionMessage.setSessionId(serviceName);
                sessionMessage.setRequestType(requestType);
                sessionMessage.setUrl(clientAddress);
                sessionMessage.setPayload(Arrays.toString(subscriptionMessages));
                stop = !sendToBroker(new Pair<>(sessionManagerService, sessionMessage));
                if (!stop) {
                    SessionMessage reply = Utils.fromJson(receive(), SessionMessage.class);
                    if (reply != null) {
                        if (reply.getRequestType().equals(Constants.RESPONSE_ALREADY_CONNECTED)
                                || reply.getRequestType().equals(Constants.RESPONSE_NOT_VALID_OPERATION)
                                || reply.getRequestType().equals(Constants.RESPONSE_UNKNOWN_SESSION)) {
                            throw new Exception(reply.getRequestType());
                        }
                    } else {
                        stop = true;
                    }
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    public void close(){
        try {
            stop = true;
            clientCommAPI.destroy();
            clientCommAPI = null;
        }catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
    }

    /********************************* SEND THREAD **************************************/
    /************************************************************************************/

    public void send(String serviceId, Object message){
        send( new Pair<>(serviceId, message) );
    }

    public void send(Pair<String, Object> message) {
        clientMessage.put( message );
    }

    private boolean sendToBroker(Pair<String, Object> message) throws Throwable{
        ZMsg request = new ZMsg();
        String msgString = Utils.toJson( message.snd );
        request.addString( msgString );
        if( isTCPon ) {
            return clientCommAPI.send(message.fst, request);
        }else{
            muf.getOrchestrator().process( msgString );
            return true;
        }
    }

    private void sendThread() {
        sendThread = new Thread("ClientSendMsgsThread"){
            public void run() {
                try {
                    sendState = checkFSM(sendState, Constants.CONNECTION_STARTED);
                    while (!Thread.currentThread().isInterrupted() && !stop) {
                        try {
                            Pair<String, Object> message = clientMessage.get();
                            stop = (!sendToBroker(message) || checkNumSent) && (sentMessages > receivedMessages + difference);
                            if (stop) {
                                continue;
                            }
                            sentMessages++;
                        } catch (Throwable e) {
                            ExceptionHandler.handle(e);
                        }
                    }
                    if (receiveThread.isAlive()) {
                        receiveThread.interrupt();
                    }
                    sendState = checkFSM(sendState, Constants.CONNECTION_FINISHED);
                    checkReconnect();
                }catch (Throwable e){
                    ExceptionHandler.handle( e );
                }
            }
        };
        sendThread.start();
    }

    /********************************* RECEIVE THREAD **************************************/
    /************************************************************************************/


    private String receive() throws Throwable{
        if( isTCPon ) {
            ZMsg reply = clientCommAPI.recv();
            if (reply != null && reply.peekLast() != null) {
                String response = reply.peekLast().toString();
                reply.destroy();
                return response;
            }
        }else{
            Pair<String, Object> message = clientMessage.get();
        }
        return null;
    }

    public void receive(ResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    private void receiveThread() {
        receiveThread = new Thread("ClientReceiveMsgsThread"){
            public void run() {
                try{
                    receiveState = checkFSM(receiveState, Constants.CONNECTION_STARTED);
                    while (!Thread.currentThread().isInterrupted() && !stop) {
                        try {
                            String response = receive();
                            receivedMessages++;
                            if ((response == null || checkNumSent) && (sentMessages > receivedMessages + difference)) {
                                stop = true;
                            } else if (responseListener != null) {
                                if (shouldProcessReply) {
                                    responseListener.process(response);
                                } else {
                                    shouldProcessReply = true;
                                }
                            }
                        } catch (Throwable e) {
                            ExceptionHandler.handle(e);
                        }
                    }
                    if (sendThread.isAlive()) {
                        sendThread.interrupt();
                    }
                    receiveState = checkFSM(receiveState, Constants.CONNECTION_FINISHED);
                    checkReconnect();
                }catch (Throwable e){
                    ExceptionHandler.handle( e );
                }
            }
        };
        receiveThread.start();
    }

    /********************************* RECONNECT THREAD **************************************/
    /*****************************************************************************************/

    private void checkReconnect(){
        if ( stop && sendState.equals(Constants.CONNECTION_FINISHED)
                && receiveState.equals(Constants.CONNECTION_FINISHED)) {
            reconnect();
        }
    }

    private void reconnect() {
        release = checkFSM(release, Constants.CONNECTION_STARTED);
        new Thread("ReconnectClientThread") {
            public void run() {
                try {
                    sendThread.join();
                    receiveThread.join();
                    release = checkFSM(release, Constants.CONNECTION_FINISHED);
                    execute();
                } catch (Throwable e) {
                    ExceptionHandler.handle(e);
                }
            }
        }.start();
    }

    /********************************* UTILS ********************************************/
    /************************************************************************************/

    private String checkFSM(String currentState, String newState){
        if (    (currentState.equals( newState) )
                || (currentState.equals(Constants.CONNECTION_NEW) && newState.equals(Constants.CONNECTION_STARTED))
                || (currentState.equals(Constants.CONNECTION_STARTED) && (newState.equals(Constants.CONNECTION_STOPPED)
                || newState.equals(Constants.CONNECTION_FINISHED)))
                || (currentState.equals(Constants.CONNECTION_STOPPED) && newState.equals(Constants.CONNECTION_FINISHED))
                || (currentState.equals(Constants.CONNECTION_FINISHED) && newState.equals(Constants.CONNECTION_NEW))) {
            return newState;
        }
        return currentState;
    }
}

