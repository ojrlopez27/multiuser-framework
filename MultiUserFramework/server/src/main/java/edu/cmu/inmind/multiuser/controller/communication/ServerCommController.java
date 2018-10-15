package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.exceptions.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.resources.CommonsResourceLocator;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 3/28/17.
 *
 * This is the implementation of a Worker according to the Majordomo Pattern
 */
public class ServerCommController implements DestroyableCallback {
    private static final int HEARTBEAT_LIVENESS = 3; // 3-5 is reasonable

    private String serverAddress;
    private ZContext ctx;
    private String service;

    private ZMQ.Socket workerSocket; // Socket to broker
    private long heartbeatAt;// When to send HEARTBEAT
    private int liveness;// How many attempts left
    private int heartbeat = 2500;// Heartbeat delay, msecs
    private int reconnect = 2500; // Reconnect delay, msecs
    private ZMsgWrapper msgTemplate;
    private ZMQ.Poller items;

    private long timeout = 2500;
    private long timeoutForDebug = 1000 * 60 * 60; // so we can test 1 hour without annoying disconnections messages
    private boolean DEBUG_MODE = false;

    // Return address, if any
    private ZFrame replyTo;
    private ZFrame replyToBackup;
    private AtomicBoolean isDestroyed = new AtomicBoolean(false);
    private AtomicBoolean stop = new AtomicBoolean(false);
    private DestroyableCallback callback;

    public ServerCommController(String serverAddress, String serviceId, ZMsgWrapper msgTemplate) {
        try {
            ExceptionHandler.checkAssert(serverAddress != null);
            ExceptionHandler.checkAssert(serviceId != null);
            this.serverAddress = serverAddress;
            this.service = serviceId;
            if (msgTemplate != null) {
                this.msgTemplate = msgTemplate.duplicate();
            }
            ctx = ResourceLocator.getContext( this );
            items = ctx.createPoller(1); //new ZMQ.Poller(1);
            reconnectToBroker();
            items.register(workerSocket, ZMQ.Poller.POLLIN);
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    /**
     * Connect or reconnect to broker
     */
    void reconnectToBroker() throws Throwable{
        if (workerSocket != null) {
            ctx.destroySocket(workerSocket);
        }
        workerSocket = ResourceLocator.createSocket(ctx, ZMQ.DEALER);
        workerSocket.connect(serverAddress);

        // Register service with broker
        sendToBroker(MDP.S_READY, service, null);

        // If liveness hits zero, queue is considered disconnected
        liveness = HEARTBEAT_LIVENESS;
        heartbeatAt = System.currentTimeMillis() + heartbeat;
    }

    /*******************************************************************************************/
    /********************************** RECEIVE ************************************************/
    /*******************************************************************************************/

    /**
     * Send reply, if any, to broker and wait for next request.
     */
    public ZMsgWrapper receive(ZMsg reply){
        try {
            // Format and send the reply if we were provided one
            //assert ( reply != null || !expectReply);
            while ( !isDestroyed.get() ) {
                try {
                    // Poll socket for a reply, with timeout
                    if (items.poll( DEBUG_MODE? timeoutForDebug : timeout) == -1) {
                        break; // Interrupted
                    }

                    if (items.pollin(0)) {
                        ZMsg msg = ZMsg.recvMsg(workerSocket);
                        if (msg == null)
                            break; // Interrupted
                        liveness = HEARTBEAT_LIVENESS;
                        // Don't try to handle errors, just assert noisily
                        ExceptionHandler.checkAssert(msg != null && msg.size() >= 3);

                        ZFrame empty = msg.pop();
                        ExceptionHandler.checkAssert(empty.getData().length == 0);
                        empty.destroy();

                        ZFrame header = msg.pop();
                        ExceptionHandler.checkAssert(MDP.S_ORCHESTRATOR.frameEquals(header));
                        header.destroy();

                        ZFrame command = msg.pop();
                        if (MDP.S_REQUEST.frameEquals(command)) {
                            // We should pop and save as many addresses as there are
                            // up to a null part, but for now, just save one
                            replyTo = msg.unwrap();
                            if( replyTo != null && !replyTo.toString().isEmpty() ) replyToBackup = replyTo.duplicate();
                            command.destroy();
                            return new ZMsgWrapper(msg, replyTo); // We have a request to process
                        } else if (MDP.S_HEARTBEAT.frameEquals(command)) {
                            // Do nothing for heartbeats
                        } else if (MDP.S_DISCONNECT.frameEquals(command)) {
                            Log4J.info(this, "**** reconnectToBroker");
                            reconnectToBroker();
                        } else {
                            Log4J.error(this, "Invalid input message: " + command.toString());
                        }
                        command.destroy();
                        msg.destroy();
                    } else if (--liveness == 0) {
                        boolean result = CommonUtils.sleep(reconnect);
                        if( !result )
                            break;
                        reconnectToBroker();
                    }
                    // Send HEARTBEAT if it's time
                    if (System.currentTimeMillis() > heartbeatAt) {
                        sendToBroker(MDP.S_HEARTBEAT, null, null);
                        heartbeatAt = System.currentTimeMillis() + heartbeat;
                    }
                } catch (Throwable error) {
                    if( stop.get() || CommonUtils.isZMQException(error) ) {
                        ResourceLocator.setIamDone(this);
                        destroyInCascade(this);
                        break;
                    }else{
                        ExceptionHandler.handle(error);
                    }
                }
            }
            return null;
        }catch (Throwable e){
            try {
                if( CommonUtils.isZMQException(e) ) {
                    ResourceLocator.setIamDone(this);
                    destroyInCascade(this); // interrupted
                }else{
                    ExceptionHandler.handle(e);
                }
            }catch (Throwable t){
                //t.printStackTrace();
            }finally {
                return null;
            }
        }
    }

    /*******************************************************************************************/
    /**********************************   SEND  ************************************************/
    /*******************************************************************************************/

    public void disconnect() throws Throwable{
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setRequestType(Constants.REQUEST_DISCONNECT);
        sessionMessage.setSessionId(service);
        ZMsg msg = new ZMsg();
        msg.addFirst(new ZFrame( CommonUtils.toJson(sessionMessage) ));
        msg.wrap(msgTemplate.getReplyTo());
        sendToBroker(MDP.S_DISCONNECT, null, msg);
        msg.destroy();
    }


    public void send(ZMsgWrapper reply, Object message) throws Throwable{
        send(MDP.S_REPLY, reply, message);
    }

    private void send(MDP command, ZMsgWrapper reply, Object message) throws Throwable{
        try {
            if (reply != null && message != null) {
                replyTo = replyToBackup.duplicate();
                //FIXME: for some reason, msgTemplate does not keep the correct replyTo, so we replaced it with replyToBackup
//                if (replyTo == null || replyTo.toString().isEmpty() ) {
//                    if (reply.getReplyTo() != null && !reply.getReplyTo().toString().isEmpty() ) {
//                        replyTo = reply.getReplyTo();
//                    } else {
//                        if( replyToBackup != null && replyToBackup.hasData() ){
//                            replyTo = replyToBackup.duplicate();
//                            System.out.println("*** replyTo 3: " + replyTo);
//                        }else {
//                            ExceptionHandler.checkAssert(replyTo != null);
//                        }
//                    }
//                }
                if( message.equals("") ){
                    ExceptionHandler.handle( new MultiuserException(ErrorMessages.SESSION_MESSAGE_IS_EMPTY ));
                }
                reply.getMsg().wrap(replyTo);
                if (reply.getMsg().peekLast() != null) {
                    reply.getMsg().peekLast().reset( message instanceof String? (String) message : CommonUtils.toJson(message));
                } else {
                    reply.getMsg().addLast(CommonUtils.toJson(message));
                }
                sendToBroker(command, null, reply.getMsg());
                reply.destroy();
            }else{
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                        "reply: " + reply, "message: " + message));
            }
        }catch (Throwable e){
            if( CommonUtils.isZMQException(e) ) {
                destroyInCascade(this); // interrupted
            }else{
                ExceptionHandler.handle(e);
            }
        }
    }


    public void send(Object message) throws Throwable{
        send(MDP.S_REPLY, message);
    }

    private void send(MDP command, Object message) throws Throwable{
        if( msgTemplate == null ){
            if( service.equals(Constants.SESSION_MANAGER_SERVICE) )
                return;
            ExceptionHandler.handle(new MultiuserException(ErrorMessages.OBJECT_NULL, "msgTemplate"));
        }
        send(command, msgTemplate.duplicate(), message);
    }

    /**
     * Send message to broker If no msg is provided, creates one internally
     *
     * @param command
     * @param option
     * @param msg
     */
    void sendToBroker(MDP command, String option, ZMsg msg) throws Throwable{
        if( !Thread.currentThread().isInterrupted() && Thread.currentThread().isAlive() ) {
            try {
                msg = msg != null ? msg.duplicate() : new ZMsg();
                // Stack protocol envelope to start of message
                if (option != null)
                    msg.addFirst(new ZFrame(option));
                msg.addFirst(command.newFrame());
                msg.addFirst(MDP.S_ORCHESTRATOR.newFrame());
                msg.addFirst(new ZFrame(new byte[0]));
                if(MDP.S_SHUTDOWN.equals(command)){
                    msg.addLast(new ZFrame(Constants.SERVICE_NAME + service));
                }
                msg.send(workerSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*******************************************************************************************/
    /********************************** RELEASE ************************************************/
    /*******************************************************************************************/

    public void close(DestroyableCallback callback) throws Throwable{
        sendShutdown();
        this.callback = callback;
        stop.getAndSet(true);
        items.close();
        Log4J.info(this, "Closing ServerCommController... Callback: " + callback);
        destroyInCascade(this);
    }

    private void sendShutdown() throws Throwable{
        send(MDP.S_SHUTDOWN, new SessionMessage(Constants.SHUTDOW_SERVER));
    }

    @Override
    public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable{
        try {
            if( !isDestroyed.getAndSet(true) ) {
                if( items.isLocked() )
                    items.close();
                if (msgTemplate != null) msgTemplate.destroy();
                if (replyTo != null) replyTo.destroy();
                ctx = null;
                Log4J.info(this, "Gracefully destroying...");
            }
            CommonsResourceLocator.setIamDone(this);
            if(callback != null) callback.destroyInCascade(this);
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }


    // ==============   getters and setters =================
    public int getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
    }

    public int getReconnect() {
        return reconnect;
    }

    public void setReconnect(int reconnect) {
        this.reconnect = reconnect;
    }

    public void setDebugMode(boolean isDebugMode){
        DEBUG_MODE = isDebugMode;
    }
}
