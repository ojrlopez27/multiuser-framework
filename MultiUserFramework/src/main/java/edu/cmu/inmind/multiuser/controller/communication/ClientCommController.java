
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
    private boolean stop;
    private boolean checkNumSent = true;
    private int     difference = 10; //if sentMessages > receivedMessages + difference => stop
    private String release = Constants.CONNECTION_FINISHED;
    private String  sendState = Constants.CONNECTION_NEW;
    private String  receiveState = Constants.CONNECTION_NEW;
    private ResponseListener responseListener;
    private boolean shouldProcessReply;
    private boolean isTCPon;
    private MultiuserFramework muf;


    public ClientCommController(String serverAddress, String serviceName, String requestType, boolean isTCPon) {
        this(serverAddress, serviceName, null, requestType, new ZMsgWrapper(), new String[]{}, isTCPon );
    }

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
        this(serverAddress, serviceName, clientAddress, requestType, msgWrapper, new String[]{}, true );
    }

    public ClientCommController(String serverAddress, String serviceName, String clientAddress, String requestType,
                                String[] msgSubscriptions){
        this(serverAddress, serviceName, clientAddress, requestType, new ZMsgWrapper(), msgSubscriptions, true);
    }

    public ClientCommController(String serverAddress, String serviceName, String clientAddress, String requestType,
                                ZMsgWrapper msgWrapper, String[] msgSubscriptions, boolean isTCPon){
        this.isTCPon = isTCPon;
        this.serviceName = serviceName;
        this.sessionManagerService = Constants.SESSION_MANAGER_SERVICE;
        this.serverAddress = serverAddress;
        this.clientAddress = clientAddress;
        this.msgTemplate = msgWrapper.duplicate();
        this.requestType = requestType;
        this.subscriptionMessages = msgSubscriptions;
        this.clientMessage = new ClientMessage();
        execute();
    }

    public void setShouldProcessReply(boolean shouldProcessReply) {
        this.shouldProcessReply = shouldProcessReply;
    }

    public void setMUF(MultiuserFramework muf) {
        this.muf = muf;
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
                            if( message.snd.toString().contains("test") ){
                                System.out.println("here");
                            }
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

