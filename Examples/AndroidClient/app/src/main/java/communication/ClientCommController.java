package communication;

import org.zeromq.ZMsg;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import common.Constants;
import common.Utils;
import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.model.SaraInput;
import test.cmu.com.jeromqandroid.MainActivity;

/**
 * Created by oscarr on 3/29/17.
 */

public class ClientCommController extends Thread{
    private ClientCommAPI clientCommAPI;
    private String sessionService;
    private String sessionManagerService;
    private MainActivity activity;
    private String address;
    private Thread sendThread;
    private Thread receiveThread;

    //constants
    private static final String NEW = "NEW";
    private static final String STARTED = "STARTED";
    private static final String FINISHED = "FINISHED";
    private static final String STOPPED = "STOPPED";
    private static final int    QUEUE_CAPACITY = 100;

    //we need to keep the state in case of failure and reconnection
    private int sentMessages = 0;
    private int receivedMessages = 0;
    private SharedObject sharedObject;
    private long timeToWait = 15;

    //control
    private boolean stop;
    private boolean checkNumSent = true;
    private int     difference = 10; //if sentMessages > receivedMessages + difference => stop
    private boolean stopClient = false;
    private String release = FINISHED;
    private String  sendState = NEW;
    private String  receiveState = NEW;


    public ClientCommController(String address, String sufix, MainActivity activity) {
        this.address = address;
        this.sessionService = Utils.getDeviceId( activity.getApplicationContext() ) + sufix;
        this.activity = activity;
        this.sessionManagerService = "session-manager";
        this.sharedObject = new SharedObject();
    }

    /********************************* MAIN THREAD **************************************/
    /************************************************************************************/

    public void run(){
        while( !Thread.currentThread().isInterrupted() && !stopClient) {
            while( !release.equals( FINISHED ) ){
                Utils.sleep( timeToWait );
            }
            if( stopClient ) break;
            execute();
            release = checkFSM(release, NEW);
        }
        System.out.println("Bye bye...");
    }

    private void execute() {
        reset();
        connect();
        sendThread();
        receiveThread();
    }

    private void reset() {
        stop = false;
        sentMessages = 0;
        receivedMessages = 0;
        sendState = checkFSM(sendState, NEW);
        receiveState = checkFSM(receiveState, NEW);
    }

    private void connect() {
        try {
            this.clientCommAPI = new ClientCommAPI(address, MainActivity.VERBOSE);
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setSessionId(sessionService);
            sessionMessage.setRequestType(Constants.REQUEST_CONNECT);
            stop = !sendToServer( sessionManagerService, Utils.toJson(sessionMessage));
            if( !stop ) {
                SessionMessage reply = Utils.fromJson(rcvString(), SessionMessage.class);
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
            e.printStackTrace();
        }
    }

    private String rcvString() throws Exception{
        ZMsg reply = clientCommAPI.recv();
        if( reply != null && reply.peekLast() != null){
            String response = reply.peekLast().toString();
            reply.destroy();
            return response;
        }
        return "";
    }

    private void print(ZMsg reply) throws Exception{
        final String response = reply.peek() != null? reply.peek().toString() : "";
        receivedMessages++;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    activity.processOutput(response, sessionService);
                }catch (Exception e){}
            }
        } );
    }

    private void close(){
        new Thread(){
            public void run(){
                try {
                    sendToServer(sessionManagerService, Utils.toJson(
                            new SessionMessage(Constants.REQUEST_DISCONNECT, sessionService, "")));
                    stop = true;
                    sendThread.interrupt();
                    receiveThread.interrupt();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void destroyCtx() throws Exception{
        clientCommAPI.destroy();
        clientCommAPI = null;
    }

    public void stopClient() {
        close();
        stopClient = true;
    }


    /********************************* SEND THREAD **************************************/
    /************************************************************************************/

    public void send(String msg){
        sharedObject.put( msg );
    }

    private void sendThread() {
        sendThread = new Thread(){
            public void run(){
                sendState = checkFSM(sendState, STARTED);
                while( !Thread.currentThread().isInterrupted() && !stop){
                    try {
                        String message = sharedObject.get();
                        stop = (!sendToServer(sessionService, createMockMessage(message))
                                || checkNumSent) && (sentMessages > receivedMessages + difference);
                        if (stop) {
                            continue;
                        }
                        sentMessages++;
                    }catch (Exception e){
                        stop = true;
                        continue;
                    }
                }
                if( receiveThread.isAlive() ){
                    receiveThread.interrupt();
                }
                sendState = checkFSM(sendState, FINISHED);
                checkReconnect();
            }
        };
        sendThread.start();
    }

    private boolean sendToServer(String serviceId, String message) throws Exception{
        ZMsg request = new ZMsg();
        request.addString( message );
        return clientCommAPI.send(serviceId, request);
    }


    /********************************* RECEIVE THREAD **************************************/
    /************************************************************************************/

    private void receiveThread() {
        receiveThread = new Thread(){
            public void run(){
                receiveState = checkFSM(receiveState, STARTED);
                while( !Thread.currentThread().isInterrupted() && !stop){
                    try {
                        if ((!receive() || checkNumSent) && (sentMessages > receivedMessages + difference)) {
                            stop = true;
                        }
                    }catch (Exception e){
                        stop = true;
                        continue;
                    }
                }
                if( sendThread.isAlive() ){
                    sendThread.interrupt();
                }
                receiveState = checkFSM(receiveState, FINISHED);
                checkReconnect();
            }
        };
        receiveThread.start();
    }

    private boolean receive() throws Exception{
        ZMsg reply = clientCommAPI.recv();
        if (reply != null) {
            print(reply);
            reply.destroy();
            return true;
        }
        return false;
    }

    /********************************* RECONNECT THREAD **************************************/
    /*****************************************************************************************/

    private void checkReconnect(){
        if ( stop && sendState.equals(FINISHED) && receiveState.equals(FINISHED)) {
            reconnect();
        }
    }

    private void reconnect() {
        release = checkFSM(release, STARTED);
        new Thread(){
            public void run(){
                try {
                    sendThread.join();
                    receiveThread.join();
                    release = checkFSM(release, FINISHED);
                    if( stopClient ){
                        destroyCtx();
                    }
                }catch (Exception e){
                }
            }
        }.start();
    }


    /********************************* UTILS ********************************************/
    /************************************************************************************/

    private String createMockMessage(String asrInput ) {
        SaraInput saraInput = new SaraInput();
        saraInput.setASRinput( asrInput  );
        saraInput.setIsGazeAtPartner( true );
        saraInput.setIsSmiling( true ) ;
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setSessionId(sessionService);
        sessionMessage.setMessageId( SaraCons.MSG_ASR );
        sessionMessage.setPayload( Utils.toJson( saraInput ) );
        return  Utils.toJson( sessionMessage );
    }


    class SharedObject{
        private final LinkedList<String> messageQueue = new LinkedList<>();
        // lock and condition variables
        private Lock aLock = new ReentrantLock();
        private Condition bufferNotFull = aLock.newCondition();
        private Condition bufferNotEmpty = aLock.newCondition();

        public void put(String message){
            aLock.lock();
            try {
                while (messageQueue.size() >= QUEUE_CAPACITY) {
                    bufferNotEmpty.await( timeToWait * 10, TimeUnit.MILLISECONDS);
                    //we start to lose messages :(
                    messageQueue.clear();
                }

                boolean isAdded = messageQueue.offer( message );
                if (isAdded) {
                    bufferNotFull.signalAll();
                }
            }catch (Exception e) {
            }finally {
                aLock.unlock();
            }
        }

        public String get(){
            aLock.lock();
            String value = null;
            try {
                while (messageQueue.size() == 0) {
                    bufferNotFull.await();
                }

                value = messageQueue.poll();
                if (value != null) {
                    bufferNotEmpty.signalAll();
                }
            } catch(Exception e){
            } finally{
                aLock.unlock();
            }
            return value;
        }

        public void reset() {
            aLock = new ReentrantLock();
            bufferNotFull = aLock.newCondition();
            bufferNotEmpty = aLock.newCondition();
            messageQueue.clear();
        }

        public int size() {
            aLock.lock();
            try{
                return messageQueue.size();
            }finally{
                aLock.unlock();
            }
        }
    }

    private String checkFSM(String currentState, String newState){
        if (    (currentState.equals( newState) )
                || (currentState.equals(NEW) && newState.equals(STARTED))
                || (currentState.equals(STARTED) && (newState.equals(STOPPED)
                    || newState.equals(FINISHED)))
                || (currentState.equals(STOPPED) && newState.equals(FINISHED))
                || (currentState.equals(FINISHED) && newState.equals(NEW))) {
            return newState;
        }
        return currentState;
    }
}

