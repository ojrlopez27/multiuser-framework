package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
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

    // Return address, if any
    private ZFrame replyTo;
    private AtomicBoolean isAlreadyDestroyed = new AtomicBoolean(false);
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
            ctx = new ZContext();
            items = ctx.createPoller(1); //new ZMQ.Poller(1);
            reconnectToBroker();
            items.register(workerSocket, ZMQ.Poller.POLLIN);
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }

    /**
     * Send message to broker If no msg is provided, creates one internally
     *
     * @param command
     * @param option
     * @param msg
     */
    void sendToBroker(MDP command, String option, ZMsg msg) throws Throwable{
        msg = msg != null ? msg.duplicate() : new ZMsg();
        // Stack protocol envelope to start of message
        if (option != null)
            msg.addFirst(new ZFrame(option));

        msg.addFirst(command.newFrame());
        msg.addFirst(MDP.S_ORCHESTRATOR.newFrame());
        msg.addFirst(new ZFrame(new byte[0]));
        msg.send(workerSocket);
    }

    /**
     * Connect or reconnect to broker
     */
    void reconnectToBroker() throws Throwable{
        if (workerSocket != null) {
            ctx.destroySocket(workerSocket);
        }
        workerSocket = ctx.createSocket(ZMQ.DEALER);
        workerSocket.connect(serverAddress);

        // Register service with broker
        sendToBroker(MDP.S_READY, service, null);

        // If liveness hits zero, queue is considered disconnected
        liveness = HEARTBEAT_LIVENESS;
        heartbeatAt = System.currentTimeMillis() + heartbeat;
    }

    /**
     * Send reply, if any, to broker and wait for next request.
     */
    public ZMsgWrapper receive(ZMsg reply) throws Throwable{
        try {
            // Format and send the reply if we were provided one
            //assert ( reply != null || !expectReply);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Poll socket for a reply, with timeout
                    if (items.poll(timeout) == -1)
                        break; // Interrupted

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
                            command.destroy();
                            return new ZMsgWrapper(msg, replyTo); // We have a request to process
                        } else if (MDP.S_HEARTBEAT.frameEquals(command)) {
                            // Do nothing for heartbeats
                        } else if (MDP.S_DISCONNECT.frameEquals(command)) {
                            reconnectToBroker();
                        } else {
                            Log4J.error(this, "invalid input message: " + command.toString());
                        }
                        command.destroy();
                        msg.destroy();
                    } else if (--liveness == 0) {
                        try {
                            Thread.sleep(reconnect);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Restore the
                            // interrupted status
                            Thread.currentThread().join();
                            break;
                        }
                        reconnectToBroker();
                    }
                    // Send HEARTBEAT if it's time
                    if (System.currentTimeMillis() > heartbeatAt) {
                        sendToBroker(MDP.S_HEARTBEAT, null, null);
                        heartbeatAt = System.currentTimeMillis() + heartbeat;
                    }
                } catch (Throwable error) {
                    destroyInCascade(this);
                    ExceptionHandler.handle(error);
                    break;
                }
            }
            return null;
        }catch (Throwable e){
            destroyInCascade(this);
            return null;
        }
    }


    public void send(ZMsgWrapper reply, Object message) throws Throwable{
        try {
            if (reply != null && message != null) {
                if (replyTo == null || replyTo.toString().isEmpty() ) {
                    if (reply.getReplyTo() != null && !reply.getReplyTo().toString().isEmpty() ) {
                        replyTo = reply.getReplyTo();
                    } else {
                        ExceptionHandler.checkAssert(replyTo != null);
                    }
                }
                reply.getMsg().wrap(replyTo);
                if (reply.getMsg().peekLast() != null) {
                    reply.getMsg().peekLast().reset( message instanceof String? (String) message : Utils.toJson(message));
                } else {
                    reply.getMsg().addLast(Utils.toJson(message));
                }
                sendToBroker(MDP.S_REPLY, null, reply.getMsg());
                reply.destroy();
            }else{
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                        "reply: " + reply, "message: " + message));
            }
        }catch (Throwable e){
            destroyInCascade(this);
        }
    }

    public void send(Object message) throws Throwable{
        if( msgTemplate == null ){
            ExceptionHandler.handle( new MultiuserException( ErrorMessages.OBJECT_NULL, "msgTemplate" ) );
        }
        send(msgTemplate.duplicate(), message);
    }


    public void close(DestroyableCallback callback) throws Throwable{
        this.callback = callback;
        Log4J.info(this, "Closing ServerCommController...");
        destroyInCascade(this);
    }

    @Override
    public void destroyInCascade(Object destroyedObj) throws Throwable{
        try {
            if( !isAlreadyDestroyed.getAndSet(true) ) {
                if (msgTemplate != null) msgTemplate.destroy();
                if (replyTo != null) replyTo.destroy();
                if (ctx != null){
                    ctx.destroy();
                }
                Log4J.info(this, "Gracefully destroying...");
                if(callback != null) callback.destroyInCascade(this);
            }
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
}
