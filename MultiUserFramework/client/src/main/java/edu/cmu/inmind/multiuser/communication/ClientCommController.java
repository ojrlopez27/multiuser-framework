
package edu.cmu.inmind.multiuser.communication;

import edu.cmu.inmind.multiuser.controller.common.*;
import edu.cmu.inmind.multiuser.controller.communication.ClientController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.plugin.Const;
import edu.cmu.inmind.multiuser.controller.resources.CommonsResourceLocator;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by oscarr on 3/29/17.
 */

public class ClientCommController implements ClientController, DestroyableCallback {
    //it only communicates with SessionManager for connecting, disconnecting, etc.
    private ClientCommAPI sessionMngrCommAPI;
    //it only communicates with Session for sending domain messages
    private ClientCommAPI sessionCommAPI;
    private String serviceName;
    private String sessionId;
    private String sessionManagerService;
    private String serverAddress;
    private final boolean sendAck;
    private String requestType;
    private String[] subscriptionMessages;
    private SenderThread sendThread;
    private ReceiverThread receiveThread;
    private ResponseTimer timer;
    private CopyOnWriteArrayList<Object> closeableObjects;
    private ZContext ctx;

    private AtomicBoolean isDestroyed = new AtomicBoolean(false);
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    /**
     * We use this socket to communicate with senderSocket, which is running on another
     * thread (SenderThread).
     */
    private ZMQ.Socket clientSocket;
    private String inprocName = "inproc://sender-thread";

    // constants:
    private static final int    difference = 100; //if sentMessages > receivedMessages + difference => stop
    private static final long   timeout = 20000; //we should receive a response from server within 10 seconds
    private static final String TOKEN = "TOKEN";
    private static final String STOP_FLAG = "STOP_FLAG";

    // control
    private AtomicInteger sentMessages; // we need to keep the state in case of failure and reconnection
    private AtomicInteger receivedMessages;
    private AtomicBoolean stop;
    private AtomicInteger sendState;
    private AtomicInteger receiveState;
    private AtomicBoolean isSendThreadAlive;
    private AtomicBoolean isReceiveThreadAlive;
    private AtomicLong lastMessage;
    private AtomicInteger release = new AtomicInteger(Constants.CONNECTION_FINISHED);
    private ConcurrentLinkedQueue<Pair<String, Object>> sendMsgQueue = new ConcurrentLinkedQueue<>();
    private ResponseListener responseListener;
    private boolean shouldProcessReply;
    private boolean isTCPon;
    private List<DestroyableCallback> callbacks;
    private long elapsedTime = 15; //milliseconds to wait between sending messages


    public ClientCommController( Builder builder){
        this.isTCPon = builder.isTCPon;
        this.serviceName = builder.serviceName;
        this.sessionId = builder.sessionId;
        this.serverAddress = builder.serverAddress;
        //this.clientAddress = builder.clientAddress;
        this.requestType = builder.requestType;
        this.subscriptionMessages = builder.subscriptionMessages;
        this.shouldProcessReply = builder.shouldProcessReply;
        this.responseListener = builder.responseListener;
        this.sessionManagerService = builder.sessionManagerService;
        this.sendAck = builder.sendAck;
        this.ctx = CommonsResourceLocator.getContext( this );
        this.callbacks = new ArrayList<>();
        //  Bind to inproc: endpoint, then start upstream thread
        this.inprocName += "-" + sessionId + "-" + this.serviceName;
        this.clientSocket = CommonsResourceLocator.createSocket(ctx, ZMQ.PAIR);
        this.clientSocket.bind(inprocName);
        this.timer = new ResponseTimer();
        this.closeableObjects = new CopyOnWriteArrayList<>();
        Utils.initThreadExecutor();
        execute();
    }

    @Override
    public void setResponseListener(ResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    @Override
    public void disconnect(String sessionId) {
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setRequestType(Constants.REQUEST_DISCONNECT);
        sessionMessage.setSessionId(sessionId);
        setShouldProcessReply(true);
        send(sessionId, sessionMessage);
    }

    public AtomicBoolean getIsConnected() {
        return isConnected;
    }

    public static class Builder{
        private boolean isTCPon = true;
        private String serviceName = String.format("client-%s", Math.random() );
        private String sessionId = "";
        private String serverAddress = "tcp://127.0.0.1:5555";
        private String requestType = Constants.REQUEST_CONNECT;
        private String [] subscriptionMessages;
        private boolean shouldProcessReply = true;
        private ResponseListener responseListener;
        private String sessionManagerService = Constants.SESSION_MANAGER_SERVICE;
        private boolean sendAck = false;

        public ClientCommController build(){
            return new ClientCommController( this);
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

        public Builder setRequestType(String requestType) {
            this.requestType = requestType;
            return this;
        }

        public Builder setSubscriptionMessages(String[] subscriptionMessages) {
            this.subscriptionMessages = subscriptionMessages;
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

        public Builder setSessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder setSendAck(boolean sendAck) {
            this.sendAck = sendAck;
            return this;
        }
    }


    @Override
    public void setShouldProcessReply(boolean shouldProcessReply) {
        this.shouldProcessReply = shouldProcessReply;
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

    private void reset(){
        stop = new AtomicBoolean(false);
        sentMessages = new AtomicInteger(0);
        receivedMessages = new AtomicInteger(0);
        sendState = new AtomicInteger(Constants.CONNECTION_NEW);
        receiveState = new AtomicInteger(Constants.CONNECTION_NEW);
        isSendThreadAlive = new AtomicBoolean(false);
        isReceiveThreadAlive = new AtomicBoolean(false);
        isConnected = new AtomicBoolean(false);
        lastMessage = new AtomicLong( System.currentTimeMillis() );
        release.getAndSet( checkFSM( release, Constants.CONNECTION_NEW) );
    }

    private void connect(){
        try {
            if( isTCPon ) {
                this.sessionMngrCommAPI = new ClientCommAPI(serverAddress);
                closeableObjects.add( sessionMngrCommAPI );
                SessionMessage sessionMessage = new SessionMessage();
                sessionMessage.setSessionId(sessionId);
                sessionMessage.setRequestType(requestType);
                //sessionMessage.setUrl(clientAddress);
                sessionMessage.setPayload(Arrays.toString(subscriptionMessages));
                Utils.setAtom( stop, !sendToBroker( sessionManagerService, Utils.toJson(sessionMessage)) );
                if (!stop.get()) {
                    timer.schedule(new ResponseCheck(), timeout);
                    String replyString = receive(sessionMngrCommAPI);
                    timer.stopTimer();
                    if( replyString.equals(STOP_FLAG) ){
                        stop.getAndSet(true);
                    }else{
                        SessionMessage reply = Utils.fromJson(replyString, SessionMessage.class);
                        if (reply != null) {
                            if (reply.getRequestType().equals(Constants.RESPONSE_ALREADY_CONNECTED)
                                    || reply.getRequestType().equals(Constants.RESPONSE_NOT_VALID_OPERATION)
                                    || reply.getRequestType().equals(Constants.RESPONSE_UNKNOWN_SESSION)) {
                                throw new Exception(reply.getRequestType());
                            } else {
                                if (reply.getRequestType().equals(Constants.SESSION_INITIATED)) {

                                    if(reply.getPayload() != null && Constants.NO_SESSION.equals(reply.getPayload()) ){
                                        this.sessionCommAPI = sessionMngrCommAPI;
                                    }else if( reply.getPayload() != null && Utils.isURLvalid( reply.getPayload() )) {
                                        this.sessionCommAPI = new ClientCommAPI(reply.getPayload());
                                    }else{
                                        this.sessionCommAPI = new ClientCommAPI( sessionMngrCommAPI.getBroker() );
                                    }
                                    closeableObjects.add( sessionCommAPI );
                                }
                                if( responseListener == null ){
                                    ExceptionHandler.handle(new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                                            "responseListener: " + responseListener ));
                                }else {
                                    responseListener.process(replyString);
                                }
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

    @Override
    public void close(DestroyableCallback callback){
        callbacks.add(callback);
        try {
            System.out.println("Closing ClientCommController...");
            stop.getAndSet(true);
            if( timer != null ){
                timer.cancel();
                timer.purge();
            }
            if(sessionMngrCommAPI != null) sessionMngrCommAPI.close(this);
            if(sessionCommAPI != null) sessionCommAPI.close(this);
            if(sendThread != null) sendThread.close(this);
            if(receiveThread != null) receiveThread.close(this);
        }catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
    }


    @Override
    public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable{
        stop.getAndSet(true);
        closeableObjects.remove(destroyedObj);
        if( !isDestroyed.get() ){
            if (closeableObjects.isEmpty()) {
                ctx.destroySocket(clientSocket);
                sessionMngrCommAPI = null;
                sessionCommAPI = null;
                ctx = null;
                System.out.println("Gracefully destroying...");
                isDestroyed.set(true);
                CommonsResourceLocator.setIamDone(this);
                for (DestroyableCallback callback : callbacks) {
                    if (callback != null) callback.destroyInCascade(this);
                }
                reset();
                //release();
            }
        }
    }

    public void release() throws Throwable{
        serviceName = null;
        sessionId = null;
        serverAddress = null;
        requestType = null;
        sendThread.destroyInCascade(null);
        sendThread = null;
        receiveThread.destroyInCascade(null);
        receiveThread = null;
        timer.cancel();
        timer = null;
        inprocName = "inproc://sender-thread";
        sentMessages = null;
        receivedMessages = null;
        responseListener = null;
        callbacks = null;
    }

    /********************************* SEND THREAD **************************************/
    /************************************************************************************/

    @Override
    public void send(String serviceId, Object message){
        if( !isConnected.get() && !isDestroyed.get() ){
            sendMsgQueue.offer( new Pair(serviceId, message) );
        }else {
            try {
                if (!isConnected.get() ){
                    ExceptionHandler.handle(new MultiuserException(ErrorMessages.CLIENT_NOT_CONNECTED));
                }else {
                    if (!isDestroyed.get()) {
                        lastMessage.set(System.currentTimeMillis());
                        sendToInternalSocket(new Pair<>(serviceId, message));
                    } else {
                        reconnect();
                    }
                }
            } catch (Throwable e) {
                ExceptionHandler.handle(e);
            }
        }
    }

    public void sendToInternalSocket(Pair<String, Object> message){
        try {
            checkFrequency();
            System.out.println("4:" + message.snd);
            clientSocket.send(message.fst + TOKEN + (message.snd instanceof String? (String) message.snd
                    : Utils.toJson(message.snd) ));
            String response = clientSocket.recvStr();
            System.out.println("5:" + message.snd);
            if (response.equals( String.valueOf( Constants.CONNECTION_STARTED )) ){
                System.out.println("5.1:" + message.snd);
                sendState.getAndSet( checkFSM(sendState, Constants.CONNECTION_STARTED) );
                response = clientSocket.recvStr();
            }
            if (response.equals(STOP_FLAG)) {
                System.out.println("5.2:" + message.snd);
                sendState.getAndSet( checkFSM(sendState, Constants.CONNECTION_FINISHED) );
                if( stop.get() ) sendState.getAndSet( checkFSM(sendState, Constants.CONNECTION_STOPPED) );
                checkReconnect();
            }
            System.out.println("6:" + message.snd);
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    private void checkFrequency() {
        long delay = elapsedTime - (System.currentTimeMillis() - lastMessage.get() );
        Utils.sleep( delay );
    }

    private boolean sendToBroker(String id, String message) throws Throwable{
        ZMsg request = new ZMsg();
        request.addString( message );
        if( id.equals(sessionManagerService) ) {
            return sessionMngrCommAPI.send(id, request);
        }else{
            return sessionCommAPI.send(id, request);
        }
    }

    private void sendThread(){
        //https://github.com/zeromq/jeromq/wiki/Sharing-ZContext-between-thread
        if( !stop.get() ) {
            sendThread = new SenderThread();
            closeableObjects.add(sendThread);
            Utils.execute( sendThread );
        }
    }

    class SenderThread implements Utils.NamedRunnable, DestroyableCallback {
        /**
         * senderSocket communicates with clientSocket. This communication
         * is interprocess, so clientSocket runs on the muf thread and senderSocket
         * on the SenderThread.
         */
        private ZMQ.Socket senderSocket;
        private ZContext context;
        private DestroyableCallback callback;

        public SenderThread(){
            this.context = CommonsResourceLocator.getContext( this );
        }

        public String getName(){
            return "sender-thread-" + serviceName;
        }

        @Override
        public void run() {
            try{
                connect();
                isSendThreadAlive.getAndSet(true);
                senderSocket = CommonsResourceLocator.createSocket(context, ZMQ.PAIR);
                senderSocket.connect(inprocName);
                senderSocket.send(String.valueOf(Constants.CONNECTION_STARTED), 0);
                while( !stop.get() && !Thread.currentThread().isInterrupted() ) {
                    try{
                        isConnected.set(true);
                        processMsgQueue();
                        String strMsg = senderSocket.recvStr(); //ZMQ.DONTWAIT);
                        System.out.println("7:" + strMsg.split(TOKEN)[1]);
                        if( strMsg != null ) {
                            String[] msg = strMsg.split(TOKEN);
                            System.out.println("8:" + msg[1]);
                            Utils.setAtom( stop, !sendToBroker(msg[0], msg[1])
                                    && (sentMessages.get() > receivedMessages.get() + difference));
                            System.out.println("13:" + msg[1]);
                            if (stop.get()) {
                                continue;
                            }
                            sentMessages.incrementAndGet();
                            //  Signal downstream to client-thread
                            System.out.println("14:" + msg[1]);
                            senderSocket.send("ACK", 0);
                        }else{
                            Utils.sleep(10);
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
            stop.getAndSet(true);
            senderSocket.setLinger(0);
            context.destroySocket(senderSocket);
            Thread.currentThread().interrupt();
            destroyInCascade(this);
        }

        @Override
        public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable {
            CommonsResourceLocator.setIamDone(this);
            if(callback != null) callback.destroyInCascade(this);
        }
    }

    /**
     * Let's process the messages in the queue
     */
    private void processMsgQueue() {
        while (!sendMsgQueue.isEmpty()) {
            new Thread() {
                public void run() {
                    Pair<String, Object> msg = sendMsgQueue.poll();
                    send(msg.fst, msg.snd);
                }
            }.start();
            Utils.sleep(1000); // for some reason, less than 1000ms doesnt' work
        }
    }

    /********************************* RECEIVE THREAD **************************************/
    /************************************************************************************/


    private String receive(ClientCommAPI clientCommAPI){
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
        }catch (AssertionError e) {
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

    class ReceiverThread implements Utils.NamedRunnable, DestroyableCallback {
        private DestroyableCallback callback;

        @Override
        public String getName() {
            return "receiver-thread-" + serviceName;
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
                        System.out.println("33:" + response);
                        receivedMessages.incrementAndGet();
                        if (response == null && (sentMessages.get() > receivedMessages.get() + difference)) {
                            stop.getAndSet(true);
                        } else if( response != null && response.equals(STOP_FLAG) ) {
                            stop.getAndSet(true);
                        } else if (responseListener != null) {
                            if (shouldProcessReply) {
                                System.out.println("34:" + response);
                                responseListener.process(response);
                                if( sendAck ) send(sessionId, new SessionMessage(Constants.ACK));
                                if( response.contains(Constants.SHUTDOW_SERVER) ){
                                    stop.set(true);
                                    ClientCommController.this.close(this);
                                }
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
            stop.getAndSet(true);
            Thread.currentThread().interrupt();
            destroyInCascade(this);
        }

        @Override
        public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable {
            if (isSendThreadAlive.get()) {
                stop.getAndSet(true);
            }
            receiveState.getAndSet( checkFSM(receiveState, Constants.CONNECTION_FINISHED) );
            if( stop.get() ) receiveState.getAndSet( checkFSM(receiveState, Constants.CONNECTION_STOPPED) );
            isReceiveThreadAlive.getAndSet(false);
            checkReconnect();
            if(callback != null){
                callback.destroyInCascade(this);
            }
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

    private void checkReconnect(){
        if ( stop.get() && sendState.get() == Constants.CONNECTION_FINISHED
                && receiveState.get() == Constants.CONNECTION_FINISHED){
            reconnect();
        }
    }

    private void reconnect(){
        release.getAndSet(checkFSM(release, Constants.CONNECTION_STARTED) );
        Utils.execute(new Utils.NamedRunnable() {
            @Override
            public void run() {
                try {
                    release.getAndSet( checkFSM(release, Constants.CONNECTION_FINISHED) );
                    isDestroyed.set(false);
                    execute();
                } catch (Throwable e) {
                    ExceptionHandler.handle(e);
                }
            }
            @Override
            public String getName(){
                return "reconnect-" + serviceName;
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

