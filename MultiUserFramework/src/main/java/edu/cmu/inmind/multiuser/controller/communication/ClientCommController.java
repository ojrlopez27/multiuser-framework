
package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Pair;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by oscarr on 3/29/17.
 */

public class ClientCommController{
    private static final String TOKEN = "TOKEN";
    private static final String STOP_FLAG = "STOP_FLAG";
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
    private ResponseTimer timer;
    private long timeout = 5000; //we should receive a response from server within 5 seconds

    //we need to keep the state in case of failure and reconnection
    private int sentMessages = 0;
    private int receivedMessages = 0;
//    private final ClientMessage clientMessage;
    private ZContext context;
    private ZMQ.Socket clientSocket;


    //control
    private boolean stop = false;
    private int     difference = 100; //if sentMessages > receivedMessages + difference => stop
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
        //Thread.dumpStack();
        this.shouldProcessReply = builder.shouldProcessReply;
        this.responseListener = builder.responseListener;
        this.sessionManagerService = builder.sessionManagerService;
        this.muf = builder.muf;
        this.context = new ZContext();
        //  Bind to inproc: endpoint, then start upstream thread
        this.clientSocket = context.createSocket(ZMQ.PAIR);
        this.clientSocket.bind("inproc://sender-thread");
        this.timer = new ResponseTimer();
        if( this.muf != null ){
            muf.setClient( this );
        }
        execute();
    }

    public void setResponseListener(ResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    public static class Builder{
        private boolean isTCPon = true;
        private String serviceName = String.format("client-%s", Math.random() );
        private String serverAddress = "tcp://127.0.0.1:5555";
        private String clientAddress = "tcp://127.0.0.1:5555";
        private ZMsgWrapper msgTemplate;
        private String requestType = Constants.REQUEST_CONNECT;
        private String [] subscriptionMessages;
        private MultiuserFramework muf;
        private boolean shouldProcessReply = true;
        private ResponseListener responseListener;
        private String sessionManagerService = Constants.SESSION_MANAGER_SERVICE;

        public ClientCommController build(){
            ClientCommController client = new ClientCommController( this );
            return client;
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
        Flowable.just(this).subscribe(clientCommController -> {
            if( release.equals(Constants.CONNECTION_FINISHED) ) {
                try {
                    reset();
                    connect();
                    sendThread();
                    receiveThread();
                }catch (Throwable e){
                    ExceptionHandler.handle( e );
                }
            }
        });
    }

    private void reset() throws Throwable{
        stop = false;
        sentMessages = 0;
        receivedMessages = 0;
        sendState = checkFSM(sendState, Constants.CONNECTION_NEW);
        receiveState = checkFSM(receiveState, Constants.CONNECTION_NEW);
        release = checkFSM( release, Constants.CONNECTION_NEW);
    }

    private void connect() throws Throwable{
        try {
            if( isTCPon ) {
                this.clientCommAPI = new ClientCommAPI(serverAddress);
                SessionMessage sessionMessage = new SessionMessage();
                sessionMessage.setSessionId(serviceName);
                sessionMessage.setRequestType(requestType);
                sessionMessage.setUrl(clientAddress);
                sessionMessage.setPayload(Arrays.toString(subscriptionMessages));
                timer.schedule(new ResponseCheck(), timeout);
                stop = !sendToBroker( sessionManagerService, Utils.toJson(sessionMessage));
                if (!stop) {
                    SessionMessage reply = Utils.fromJson(receive(), SessionMessage.class);
                    timer.stopTimer();
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
            //ExceptionHandler.handle(e);
        }
    }

    public void close() throws Throwable{
        new Thread(() -> {
            try {
                stop = true;
                timer.cancel();
                timer.purge();
                //let's wait for all messages to be sent
                Thread.sleep( 1000 );
                clientCommAPI.destroy();
                clientCommAPI = null;
                context.destroySocket(clientSocket);
                context.destroy();
            }catch (Throwable e) {
                //ExceptionHandler.handle(e);
            }
        }).start();
    }

    /********************************* SEND THREAD **************************************/
    /************************************************************************************/

    public void send(String serviceId, Object message) throws Throwable{
        send( new Pair<>(serviceId, message) );
    }

    public void send(Pair<String, Object> message) throws Throwable{
        try {
            clientSocket.send(message.fst + TOKEN + Utils.toJson(message.snd));
            String response = clientSocket.recvStr();
            if (response.equals(Constants.CONNECTION_STARTED)) {
                sendState = checkFSM(sendState, Constants.CONNECTION_STARTED);
                response = clientSocket.recvStr();
            }
            if (response.equals(STOP_FLAG)) {
                if (receiveThread.isAlive()) {
                    receiveThread.interrupt();
                }
                sendState = checkFSM(sendState, Constants.CONNECTION_FINISHED);
                checkReconnect();
            }
        }catch (Throwable e){
            //ExceptionHandler.handle(e);
        }
    }

    private boolean sendToBroker(String id, String message) throws Throwable{
        ZMsg request = new ZMsg();
        request.addString( message );
        if( isTCPon ) {
            return clientCommAPI.send(id, request);
        }else{
            muf.getOrchestrator().process( message );
            return true;
        }
    }

    private void sendThread() throws Throwable{
        sendThread = new SenderThread("ClientSendMsgsThread", context);
        sendThread.start();
    }

    class SenderThread extends Thread{
        private ZContext context;
        private ZMQ.Socket senderSocket;
        private boolean stop;

        public SenderThread(String threadName, ZContext context){
            super(threadName);
            this.context = context;
            senderSocket = this.context.createSocket(ZMQ.PAIR);
            senderSocket.connect("inproc://sender-thread");
        }

        public void run() {
            try{
                senderSocket.send(Constants.CONNECTION_STARTED, 0);
                while( !Thread.currentThread().isInterrupted() && !stop ) {
                    try{
                        String[] msg = senderSocket.recvStr().split(TOKEN);
                        stop = (!sendToBroker(msg[0], msg[1])) && (sentMessages > receivedMessages + difference);
                        if (stop) {
                            continue;
                        }
                        sentMessages++;
                        //  Signal downstream to client-thread
                        senderSocket.send("ACK", 0);
                    } catch (Throwable e) {
                        //ExceptionHandler.handle(e);
                    }
                }
                if( !stop ) {
                    senderSocket.send(STOP_FLAG, 0);
                    senderSocket.close();
                }
            }catch (Throwable e){
                //ExceptionHandler.handle( e );
            }
        }
    }

    /********************************* RECEIVE THREAD **************************************/
    /************************************************************************************/


    private String receive() throws Throwable{
        try {
            //TODO: why clientAPI is null?
            if( clientCommAPI != null ) {
                ZMsg reply = clientCommAPI.recv();
                if (reply != null && reply.peekLast() != null) {
                    String response = reply.peekLast().toString();
                    reply.destroy();
                    return response;
                }
            }else{
                return STOP_FLAG;
            }
        }catch (Throwable e){
            if( e instanceof ClosedByInterruptException || e instanceof ClosedChannelException){
                Log4J.debug("ClientCommController.receive", "Exception 1");
            }else{
                Log4J.debug("ClientCommController.receive", "Exception 2. Exception: " + e.getMessage());
                ExceptionHandler.handle( e );
            }
        }
        return STOP_FLAG;
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
                            if (response == null && (sentMessages > receivedMessages + difference)) {
                                stop = true;
                            } else if( response != null && response.equals(STOP_FLAG) ) {
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

    private void checkReconnect() throws Throwable{
        if ( stop && sendState.equals(Constants.CONNECTION_FINISHED)
                && receiveState.equals(Constants.CONNECTION_FINISHED)) {
            reconnect();
        }
    }

    private void reconnect() throws Throwable{
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

    class ResponseCheck extends TimerTask{
        @Override
        public void run() {
            //ExceptionHandler.handle( new MultiuserException(ErrorMessages.NO_RESPONSE_FROM_SERVER, serverAddress, timeout));
        }
    }

    class ResponseTimer extends Timer {
        private TimerTask responseCheck;

        @Override
        public void schedule(TimerTask task, long delay) {
            responseCheck = task;
            super.schedule(task, delay);
        }

        public void stopTimer(){
            if( responseCheck != null ) {
                responseCheck.cancel();
            }
        }
    }
}

