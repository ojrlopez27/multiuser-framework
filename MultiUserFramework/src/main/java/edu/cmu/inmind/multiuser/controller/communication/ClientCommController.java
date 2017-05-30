package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Pair;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
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
    private final SharedObject sharedObject;
    private long timeToWait = 15;

    //control
    private boolean stop;
    private boolean checkNumSent = true;
    private int     difference = 10; //if sentMessages > receivedMessages + difference => stop
    private String release = Constants.CONNECTION_FINISHED;
    private String  sendState = Constants.CONNECTION_NEW;
    private String  receiveState = Constants.CONNECTION_NEW;
    private ResponseListener responseListener;
    private boolean shouldProcessReply;


    public ClientCommController(String serverAddress, String serviceName, String clientAddress, String requestType) {
        this(serverAddress, serviceName, clientAddress, requestType, new ZMsgWrapper() );
    }

    public ClientCommController(String serverAddress, String serviceName, String clientAddress, String requestType,
                                boolean shouldProcessReply) {
        this(serverAddress, serviceName, clientAddress, requestType, new ZMsgWrapper() );
        this.shouldProcessReply = shouldProcessReply;
    }



    public ClientCommController(String serverAddress, String serviceName, String clientAddress, String requestType,
                                ZMsgWrapper msgWrapper){
        this(serverAddress, serviceName, clientAddress, requestType, msgWrapper, new String[]{} );
    }

    public ClientCommController(String serverAddress, String serviceName, String clientAddress, String requestType,
                                String[] msgSubscriptions){
        this(serverAddress, serviceName, clientAddress, requestType, new ZMsgWrapper(), msgSubscriptions);
    }

    public ClientCommController(String serverAddress, String serviceName, String clientAddress, String requestType,
                                ZMsgWrapper msgWrapper, String[] msgSubscriptions){
        this.serviceName = serviceName;
        this.sessionManagerService = Constants.SESSION_MANAGER_SERVICE;
        this.serverAddress = serverAddress;
        this.clientAddress = clientAddress;
        this.msgTemplate = msgWrapper.duplicate();
        this.requestType = requestType;
        this.subscriptionMessages = msgSubscriptions;
        this.sharedObject = new SharedObject();
        execute();
    }

    public void setShouldProcessReply(boolean shouldProcessReply) {
        this.shouldProcessReply = shouldProcessReply;
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

    public void connect() {
        try {
            this.clientCommAPI = new ClientCommAPI(serverAddress);
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setSessionId(serviceName);
            sessionMessage.setRequestType(requestType);
            sessionMessage.setUrl(clientAddress);
            sessionMessage.setPayload( Arrays.toString(subscriptionMessages));
            stop = !sendToBroker( new Pair<>(sessionManagerService, sessionMessage) );
            if( !stop ) {
                SessionMessage reply = Utils.fromJson(receive(), SessionMessage.class);
                if( reply != null ) {
                    if (reply.getRequestType().equals(Constants.RESPONSE_ALREADY_CONNECTED)
                            || reply.getRequestType().equals(Constants.RESPONSE_NOT_VALID_OPERATION)
                            || reply.getRequestType().equals(Constants.RESPONSE_UNKNOWN_SESSION)) {
                        throw new Exception(reply.getRequestType());
                    }
                }else {
                    stop = true;
                }
            }
        }catch (Exception e){
            ExceptionHandler.handle(e);
        }
    }

    public void close(){
        try {
            stop = true;
            clientCommAPI.destroy();
            clientCommAPI = null;
        }catch (Exception e) {
            ExceptionHandler.handle(e);
        }
    }

    /********************************* SEND THREAD **************************************/
    /************************************************************************************/

    public void send(String serviceId, Object message){
        send( new Pair<>(serviceId, message) );
    }

    public void send(Pair<String, Object> message) {
        sharedObject.put( message );
    }

    private boolean sendToBroker(Pair<String, Object> message) throws Exception{
        ZMsg request = new ZMsg();
        request.addString( Utils.toJson( message.snd ) );
        return clientCommAPI.send( message.fst, request);
    }

    private void sendThread() {
        sendThread = new Thread("ClientSendMsgsThread"){
            public void run(){
                sendState = checkFSM(sendState, Constants.CONNECTION_STARTED);
                while( !Thread.currentThread().isInterrupted() && !stop){
                    try {
                        Pair<String, Object> message = sharedObject.get();
                        stop = (!sendToBroker(message) || checkNumSent) && (sentMessages > receivedMessages + difference);
                        if (stop) {
                            continue;
                        }
                        sentMessages++;
                    }catch (Exception e){
                        ExceptionHandler.handle(e);
                    }
                }
            }
            if( receiveThread.isAlive() ){
                receiveThread.interrupt();
            }
            sendState = checkFSM(sendState, Constants.CONNECTION_FINISHED);
            checkReconnect();
        }, "clientcommcontroler send thread");
        sendThread.start();
    }

    /********************************* RECEIVE THREAD **************************************/
    /************************************************************************************/


    private String receive() throws Exception{
        ZMsg reply = clientCommAPI.recv();
        if( reply != null && reply.peekLast() != null){
            String response = reply.peekLast().toString();
            reply.destroy();
            return response;
        }
        return null;
    }

    public void receive(ResponseListener responseListener) {
        this.responseListener = responseListener;
    }

    private void receiveThread() {
        receiveThread = new Thread("ClientReceiveMsgsThread"){
            public void run() {
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
                    } catch (Exception e) {
                        ExceptionHandler.handle(e);
                    }
                }
                if (sendThread.isAlive()) {
                    sendThread.interrupt();
                }
                receiveState = checkFSM(receiveState, Constants.CONNECTION_FINISHED);
                checkReconnect();
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
                } catch (Exception e) {
                    ExceptionHandler.handle(e);
                }
            }
        }.start();
    }

    /********************************* UTILS ********************************************/
    /************************************************************************************/

    class SharedObject{
        private final LinkedList<Pair<String, Object>> messageQueue = new LinkedList<>();
        // lock and condition variables
        private Lock aLock = new ReentrantLock();
        private Condition bufferNotFull = aLock.newCondition();
        private Condition bufferNotEmpty = aLock.newCondition();

        void put(Pair<String, Object> message){
            //Log4J.debug(this, "putting: " + message.toString());
            boolean isLocked = false;
            try {
                isLocked = aLock.tryLock( timeToWait * 10, TimeUnit.MILLISECONDS);
                if( isLocked ) {
                    while (messageQueue.size() >= Constants.QUEUE_CAPACITY) {
                        bufferNotEmpty.await(timeToWait * 10, TimeUnit.MILLISECONDS);
                        //we start to lose messages :(
                        messageQueue.clear();
                    }

                    boolean isAdded = messageQueue.offer(message);
                    if (isAdded) {
                        bufferNotFull.signalAll();
                    }
                }
            }catch (Exception e) {
                ExceptionHandler.handle(e);
            }finally {
                if( isLocked ) aLock.unlock();
            }
            //Log4J.debug(this, "done putting ...");
        }

        Pair<String, Object> get(){
            //Log4J.debug(this, "attempting to get ...");
            boolean isLocked = false;
            Pair<String, Object> value = null;
            try {
                isLocked = aLock.tryLock(timeToWait * 10, TimeUnit.MILLISECONDS );
                if( isLocked ) {
                    while (messageQueue.size() == 0) {
                        bufferNotFull.await();
                    }
                    value = messageQueue.poll();
                    if (value != null) {
                        bufferNotEmpty.signalAll();
                    }
                }
            } catch(Exception e){
                ExceptionHandler.handle( e );
            } finally{
                if( isLocked ) aLock.unlock();
            }
            //Log4J.debug(this, "got: " + value.toString());
            return value;
        }

        //TODO: who needs to call thi?
        public void reset() {
            aLock = new ReentrantLock();
            bufferNotFull = aLock.newCondition();
            bufferNotEmpty = aLock.newCondition();
            messageQueue.clear();
        }
    }

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

