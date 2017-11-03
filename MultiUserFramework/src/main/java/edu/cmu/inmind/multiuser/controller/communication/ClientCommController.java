
package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.common.*;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oscarr on 3/29/17.
 */

public class ClientCommController implements DestroyableCallback {
    //it only communicates with SessionManager for connecting, disconnecting, etc.
    private ClientCommAPI sessionMngrCommAPI;
    //it only communicates with Session for sending domain messages
    private ClientCommAPI sessionCommAPI;
    private String serviceName;
    private String sessionManagerService;
    private String serverAddress;
    private String clientAddress;
    private String requestType;
    private String[] subscriptionMessages;
    private SenderThread sendThread;
    private ReceiverThread receiveThread;
    private ResponseTimer timer;
    private CopyOnWriteArrayList<Object> closeableObjects;

    private ZContext ctx;
    private AtomicBoolean isDestroyed = new AtomicBoolean(false);
    /**
     * We use this socket to communicate with senderSocket, which is running on another
     * thread (SenderThread).
     */
    private ZMQ.Socket clientSocket;
    private String inprocName = "inproc://sender-thread";

    // constants:
    private static final int    difference = 100; //if sentMessages > receivedMessages + difference => stop
    private static final long   timeout = 10000; //we should receive a response from server within 10 seconds
    private static final String TOKEN = "TOKEN";
    private static final String STOP_FLAG = "STOP_FLAG";

    // control
    private AtomicInteger sentMessages = new AtomicInteger(0); // we need to keep the state in case of failure and reconnection
    private AtomicInteger receivedMessages = new AtomicInteger(0);
    private AtomicBoolean stop = new AtomicBoolean(false);
    private AtomicInteger release = new AtomicInteger(Constants.CONNECTION_FINISHED);
    private AtomicInteger sendState = new AtomicInteger(Constants.CONNECTION_NEW);
    private AtomicInteger receiveState = new AtomicInteger(Constants.CONNECTION_NEW);
    private AtomicBoolean isSendThreadAlive = new AtomicBoolean(false);
    private AtomicBoolean isReceiveThreadAlive = new AtomicBoolean(false);
    private ResponseListener responseListener;
    private boolean shouldProcessReply;
    private boolean isTCPon;
    private MultiuserFramework muf;
    private List<DestroyableCallback> callbacks;


    public ClientCommController( Builder builder ){
        this.isTCPon = builder.isTCPon;
        this.serviceName = builder.serviceName;
        this.serverAddress = builder.serverAddress;
        this.clientAddress = builder.clientAddress;
        this.requestType = builder.requestType;
        this.subscriptionMessages = builder.subscriptionMessages;
        this.shouldProcessReply = builder.shouldProcessReply;
        this.responseListener = builder.responseListener;
        this.sessionManagerService = builder.sessionManagerService;
        this.muf = builder.muf;
        this.ctx = ResourceLocator.getContext( this );
        this.callbacks = new ArrayList<>();
        //  Bind to inproc: endpoint, then start upstream thread
        this.inprocName += "-" + this.serviceName;
        this.clientSocket = ResourceLocator.createSocket(ctx, ZMQ.PAIR);
        this.clientSocket.bind(inprocName);
        this.timer = new ResponseTimer();
        this.closeableObjects = new CopyOnWriteArrayList<>();
        if( this.muf != null ){
            muf.setClient( this );
        }
        Utils.initThreadExecutor();
        execute();
    }

    public void setResponseListener(ResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    public void disconnect(String sessionId) {
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setRequestType(Constants.REQUEST_DISCONNECT);
        sessionMessage.setSessionId(sessionId);
        setShouldProcessReply(true);
        send(sessionId, sessionMessage);
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
            ExceptionHandler.checkIpAddress(serverAddress);
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder setClientAddress(String clientAddress) {
            ExceptionHandler.checkIpAddress(clientAddress);
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
        if( release.get() == Constants.CONNECTION_FINISHED){
            try {
                reset();
                sendThread();
                receiveThread();
            }catch (Throwable e){
                ExceptionHandler.handle( e );
            }
        }
    }

    private void reset() throws Throwable{
        stop.getAndSet(false);
        sentMessages.getAndSet( 0 );
        receivedMessages.getAndAdd(0);
        sendState.getAndSet( checkFSM(sendState, Constants.CONNECTION_NEW) );
        receiveState.getAndSet( checkFSM(receiveState, Constants.CONNECTION_NEW) );
        release.getAndSet( checkFSM( release, Constants.CONNECTION_NEW) );
        isSendThreadAlive.getAndSet(false);
        isReceiveThreadAlive.getAndSet(false);
    }

    private void connect() throws Throwable{
        try {
            if( isTCPon ) {
                this.sessionMngrCommAPI = new ClientCommAPI(serverAddress);
                closeableObjects.add( sessionMngrCommAPI );
                SessionMessage sessionMessage = new SessionMessage();
                sessionMessage.setSessionId(serviceName);
                sessionMessage.setRequestType(requestType);
                sessionMessage.setUrl(clientAddress);
                sessionMessage.setPayload(Arrays.toString(subscriptionMessages));
                timer.schedule(new ResponseCheck(), timeout);
                Utils.setAtom( stop, !sendToBroker( sessionManagerService, Utils.toJson(sessionMessage)) );
                if (!stop.get()) {
                    String replyString = receive(sessionMngrCommAPI);
                    if( replyString.equals(STOP_FLAG) ){
                        stop.getAndSet(true);
                    }else{
                        SessionMessage reply = Utils.fromJson(replyString, SessionMessage.class);
                        if (reply != null) {
                            timer.stopTimer();
                            if (reply.getRequestType().equals(Constants.RESPONSE_ALREADY_CONNECTED)
                                    || reply.getRequestType().equals(Constants.RESPONSE_NOT_VALID_OPERATION)
                                    || reply.getRequestType().equals(Constants.RESPONSE_UNKNOWN_SESSION)) {
                                throw new Exception(reply.getRequestType());
                            } else {
                                if (reply.getRequestType().equals(Constants.SESSION_INITIATED)) {
                                    this.sessionCommAPI = new ClientCommAPI(reply.getPayload());
                                    closeableObjects.add( sessionCommAPI );
                                }
                                if( responseListener == null ){
                                    ExceptionHandler.handle(new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                                            "responseListener: " + responseListener ));
                                }
                                responseListener.process(replyString);
                            }
                        } else {
                            stop.getAndSet(true);
                        }
                    }
                }
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    public void close(DestroyableCallback callback){
        callbacks.add(callback);
        try {
            //Log4J.warn(this, "=== 3.1");
            Log4J.info(ClientCommController.this, "Closing ClientCommController...");
            stop.getAndSet(true);
            timer.cancel();
            timer.purge();
            //Log4J.warn(this, "=== 3.2");
            sessionMngrCommAPI.close(this);
            //Log4J.warn(this, "=== 3.6");
            sessionCommAPI.close(this);
            //Log4J.warn(this, "=== 3.7");
            sendThread.close(this);
            //Log4J.warn(this, "=== 3.12");
            receiveThread.close(this);
            //Log4J.warn(this, "=== 3.22");
        }catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
    }


    @Override
    public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable{
        //Log4J.warn(this, "=== 3.19");
        stop.getAndSet(true);
        closeableObjects.remove(destroyedObj);
        if( closeableObjects.isEmpty() && !isDestroyed.getAndSet(true) ) {
            //Log4J.warn(this, "=== 3.20");
            sessionMngrCommAPI = null;
            sessionCommAPI = null;
            ctx = null;
            ResourceLocator.setIamDone( this );
            Log4J.info(this, "Gracefully destroying...");
            //Log4J.warn(this, "=== 3.21");
            for (DestroyableCallback callback : callbacks) {
                if(callback != null) callback.destroyInCascade(this);
            }
        }
    }

    /********************************* SEND THREAD **************************************/
    /************************************************************************************/

    public void send(String serviceId, Object message){
        try {
            if( !isDestroyed.get() ) {
                sendToInternalSocket(new Pair<>(serviceId, message));
            }else{
                reconnect();
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e   );
        }
    }

    public void sendToInternalSocket(Pair<String, Object> message) throws Throwable{
        try {
            Log4J.error(this, "4:" + message.snd);
            clientSocket.send(message.fst + TOKEN + (message.snd instanceof String? (String) message.snd
                    : Utils.toJson(message.snd) ));
            String response = clientSocket.recvStr();
            Log4J.error(this, "5:" + message.snd);
            if (response.equals(Constants.CONNECTION_STARTED)) {
                Log4J.error(this, "5.1:" + message.snd);
                sendState.getAndSet( checkFSM(sendState, Constants.CONNECTION_STARTED) );
                response = clientSocket.recvStr();
            }
            if (response.equals(STOP_FLAG)) {
                Log4J.error(this, "5.2:" + message.snd);
                sendState.getAndSet( checkFSM(sendState, Constants.CONNECTION_FINISHED) );
                if( stop.get() ) sendState.getAndSet( checkFSM(sendState, Constants.CONNECTION_STOPPED) );
                checkReconnect();
            }
            Log4J.error(this, "6:" + message.snd);
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    private boolean sendToBroker(String id, String message) throws Throwable{
        ZMsg request = new ZMsg();
        request.addString( message );
        if(message.startsWith("@@@")) Log4J.error(this, "9:" + message);
        if( isTCPon ) {
            if( id.equals(sessionManagerService) ) {
                if(message.startsWith("@@@"))  Log4J.error(this, "9.1:@@@:" + serviceName + ":" + message);
                return sessionMngrCommAPI.send(id, request);
            }else{
                if(message.startsWith("@@@")) Log4J.error(this, "10:" + message);
                return sessionCommAPI.send(id, request);
            }
        }else{
            muf.getOrchestrator().process( message );
            return true;
        }
    }

    private void sendThread() throws Throwable{
        //https://github.com/zeromq/jeromq/wiki/Sharing-ZContext-between-thread
        if( !stop.get() ) {
            sendThread = new SenderThread();
            closeableObjects.add(sendThread);
            Utils.execute( sendThread );
        }
    }

    class SenderThread extends Utils.MyRunnable implements DestroyableCallback{
        /**
         * senderSocket communicates with clientSocket. This communication
         * is interprocess, so clientSocket runs on the main thread and senderSocket
         * on the SenderThread.
         */
        private ZMQ.Socket senderSocket;
        private ZContext context;
        private DestroyableCallback callback;

        public SenderThread(){
            this.context = ResourceLocator.getContext( this );
            this.setName("sender-thread-" + serviceName);
        }

        @Override
        public void run() {
            try{
                connect();
                isSendThreadAlive.getAndSet(true);
                senderSocket = ResourceLocator.createSocket(context, ZMQ.PAIR);
                senderSocket.connect(inprocName);
                senderSocket.send(String.valueOf(Constants.CONNECTION_STARTED), 0);
                while( !stop.get() && !Thread.currentThread().isInterrupted() ) {
                    try{
                        String strMsg = senderSocket.recvStr(); //ZMQ.DONTWAIT);
                        Log4J.error(this, "7:" + strMsg.split(TOKEN)[1]);
                        if( strMsg != null ) {
                            String[] msg = strMsg.split(TOKEN);
                            Log4J.error(this, "8:" + msg[1]);
                            Utils.setAtom( stop, !sendToBroker(msg[0], msg[1])
                                    && (sentMessages.get() > receivedMessages.get() + difference));
                            Log4J.error(this, "13:" + msg[1]);
                            if (stop.get()) {
                                continue;
                            }
                            sentMessages.incrementAndGet();
                            //  Signal downstream to client-thread
                            Log4J.error(this, "14:" + msg[1]);
                            senderSocket.send("ACK", 0);
                        }else{
                            Utils.sleep(1);
                        }
                    } catch (Throwable e) {
                        if( !Utils.isZMQException(e) ) {
                            ExceptionHandler.handle(e);
                        }
                    }
                }
                destroyInCascade(this);
            }catch (Throwable e){
                try {
                    if( Utils.isZMQException(e) ) {
                        destroyInCascade(this); // interrupted
                    }else{
                        ExceptionHandler.handle(e);
                    }
                }catch (Throwable t){
                }
            }
            isSendThreadAlive.getAndSet(false);
        }

        @Override
        public void close(DestroyableCallback callback) throws Throwable {
            this.callback = callback;
            //Log4J.warn(this, "++++ closing senderThread");
            //Log4J.warn(this, "=== 3.8");
            stop.getAndSet(true);
            senderSocket.setLinger(0);
            Thread.currentThread().interrupt();
            //Log4J.warn(this, "=== 3.9");
            destroyInCascade(this);
        }

        @Override
        public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable {
            //nothing
            //Log4J.warn(this, "=== 3.10");
            //Log4J.warn(this, "++++ destroying senderThread");
            ResourceLocator.setIamDone(this);
            //Log4J.warn(this, "=== 3.11");
            if(callback != null) callback.destroyInCascade(this);
        }
    }

    /********************************* RECEIVE THREAD **************************************/
    /************************************************************************************/


    private String receive(ClientCommAPI clientCommAPI) throws Throwable{
        try {
            //TODO: why clientAPI is null?
            if( clientCommAPI != null ) {
                ZMsg reply = clientCommAPI.recv();
                if (reply != null && reply.peekLast() != null) {
                    String response = reply.peekLast().toString();
                    reply.destroy();
                    return response;
                }else{
                    return "";
                }
            }else{
                return STOP_FLAG;
            }
        }catch (java.lang.AssertionError e) {
            //Log4J.error("ClientCommController.receive", "Exception 2. Exception: " + e.getMessage());
            reconnect();
        }catch (Throwable e){
            try {
                if( Utils.isZMQException(e) ) {
                    destroyInCascade(this); // interrupted
                }else{
                    ExceptionHandler.handle(e);
                }
            }catch (Throwable t){
            }
        }
        return STOP_FLAG;
    }

    class ReceiverThread extends Utils.MyRunnable implements DestroyableCallback {
        private DestroyableCallback callback;

        public ReceiverThread() {
            this.setName("receiver-thread-" + serviceName);
        }

        @Override
        public void run() {
            try{
                // let's wait for senderThread to connect()
                while( !isSendThreadAlive.get() ){
                    Utils.sleep(1);
                }
                isReceiveThreadAlive.getAndSet(true);
                receiveState.getAndSet( checkFSM(receiveState, Constants.CONNECTION_STARTED) );
                while ( !stop.get() && !Thread.currentThread().isInterrupted() ){
                    try {
                        String response = receive(sessionCommAPI);
                        if( "".equals(response) )
                            continue;
                        Log4J.error(this, "33:" + response);
                        receivedMessages.incrementAndGet();
                        if (response == null && (sentMessages.get() > receivedMessages.get() + difference)) {
                            stop.getAndSet(true);
                        } else if( response != null && response.equals(STOP_FLAG) ) {
                            stop.getAndSet(true);
                        } else if (responseListener != null) {
                            if (shouldProcessReply) {
                                Log4J.error(this, "34:" + response);
                                responseListener.process(response);
                            } else {
                                shouldProcessReply = true;
                            }
                        }
                    } catch (Throwable e) {
                        if( Utils.isZMQException(e) ) {
                            destroyInCascade(this); // interrupted
                        }else{
                            ExceptionHandler.handle(e);
                        }
                    }
                }
                destroyInCascade(this);
            }catch (Throwable e){
                try {
                    if( Utils.isZMQException(e) ) {
                        destroyInCascade(this); // interrupted
                    }else{
                        ExceptionHandler.handle(e);
                    }
                }catch (Throwable t){
                }
            }
        }

        @Override
        public void close(DestroyableCallback callback) throws Throwable {
            this.callback = callback;
            //Log4J.warn(this, "++++ closing receiverThread");
            //Log4J.warn(this, "=== 3.13");
            stop.getAndSet(true);
            Thread.currentThread().interrupt();
            //Log4J.warn(this, "=== 3.14");
            destroyInCascade(this);
        }

        @Override
        public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable {
            //Log4J.warn(this, "++++ destroying receiverThread");
            //Log4J.warn(this, "=== 3.15");
            if (isSendThreadAlive.get()) {
                stop.getAndSet(true);
            }
            //Log4J.warn(this, "=== 3.16");
            receiveState.getAndSet( checkFSM(receiveState, Constants.CONNECTION_FINISHED) );
            if( stop.get() ) receiveState.getAndSet( checkFSM(receiveState, Constants.CONNECTION_STOPPED) );
            //Log4J.warn(this, "=== 3.17");
            isReceiveThreadAlive.getAndSet(false);
            checkReconnect();
            //Log4J.warn(this, "=== 3.18");
            if(callback != null) callback.destroyInCascade(this);
        }
    }

    private void receiveThread() {
        if( !stop.get() ) {
            receiveThread = new ReceiverThread();
            closeableObjects.add(receiveThread);
            Utils.execute( receiveThread );
        }
    }

    /********************************* RECONNECT THREAD **************************************/
    /*****************************************************************************************/

    private void checkReconnect() throws Throwable{
        //Log4J.warn(this, "++++ checking reconnect");
        if ( stop.get() && sendState.get() == Constants.CONNECTION_FINISHED
                && receiveState.get() == Constants.CONNECTION_FINISHED){
            reconnect();
        }
    }

    private void reconnect() throws Throwable{
        //Log4J.warn(this, "++++ checking reconnect 1");
        release.getAndSet(checkFSM(release, Constants.CONNECTION_STARTED) );
        Utils.execute(new Utils.MyRunnable("reconnect") {
            @Override
            public void run() {
                try {
                    //Log4J.warn(this, "++++ checking reconnect 2");
                    release.getAndSet( checkFSM(release, Constants.CONNECTION_FINISHED) );
                    execute();
                } catch (Throwable e) {
                    ExceptionHandler.handle(e);
                }
            }
        });
    }

    /********************************* UTILS ********************************************/
    /************************************************************************************/

    private int checkFSM(AtomicInteger currentState, int newState){
        if (    (newState == Constants.CONNECTION_STOPPED)
                || (currentState.get() == newState)
                || (currentState.get() == Constants.CONNECTION_NEW && newState == Constants.CONNECTION_STARTED)
                || (currentState.get() == Constants.CONNECTION_STARTED && newState == Constants.CONNECTION_FINISHED)
                || (currentState.get() == Constants.CONNECTION_STOPPED && newState == Constants.CONNECTION_FINISHED)
                || (currentState.get() == Constants.CONNECTION_FINISHED && newState == Constants.CONNECTION_NEW)) {
            if(newState == Constants.CONNECTION_STOPPED){
                //Log4J.warn(this, "++++ stopping");
            }
            return newState;
        }
        return currentState.get();
    }


    class ResponseCheck extends TimerTask{
        @Override
        public void run() {
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.NO_RESPONSE_FROM_SERVER, serverAddress, timeout));
        }
    }

    class ResponseTimer extends Timer {
        private TimerTask responseCheck;
        private boolean isCanceled = false;


        @Override
        public void schedule(TimerTask task, long delay) {
            if( !isCanceled ) {
                responseCheck = task;
                super.schedule(task, delay);
            }
        }

        @Override
        public void cancel() {
            isCanceled = true;
            super.cancel();
        }

        public void stopTimer(){
            if( responseCheck != null ) {
                responseCheck.cancel();
            }
        }
    }
}

