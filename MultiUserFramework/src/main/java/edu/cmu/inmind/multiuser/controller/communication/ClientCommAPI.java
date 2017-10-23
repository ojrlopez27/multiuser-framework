package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 3/29/17.
 */

public class ClientCommAPI implements DestroyableCallback {
    private String broker;
    private ZContext ctx;
    private ZMQ.Socket clientSocket;
    private long timeout = 2500; //10000; // ten seconds
    private int highWaterMark = 10 * 1000; //amount of enqueued messages
    private ZMQ.Poller items; // Poll socket for a reply, with timeout
    private AtomicBoolean isAlreadyDestroyed = new AtomicBoolean(false);
    private DestroyableCallback callback;
    private AtomicBoolean canUseSocket = new AtomicBoolean( true );
    private String whoPrevious;

    public ClientCommAPI(String broker) throws Throwable{
        this.broker = broker;
        ctx = new ZContext();
        reconnectToBroker();
    }

    /**
     * Connect or reconnect to broker
     */
    void reconnectToBroker() throws Throwable{
        checkAndSleep("reconnectToBroker");
        if (clientSocket != null) {
            ctx.destroySocket(clientSocket);
        }
        clientSocket = ctx.createSocket(ZMQ.DEALER);
        clientSocket.setSendTimeOut(0); //  Send messages immediately or return EAGAIN  ojrl
        clientSocket.setHWM(highWaterMark); //  Set a high-water mark that allows for reasonable activity
        clientSocket.setRcvHWM(highWaterMark);
        clientSocket.connect(broker);
        initialize();
        canUseSocket.getAndSet(true);
    }

    public void initialize() throws Throwable{
        // Poll socket for a reply, with timeout
        items = ctx.createPoller(1); //new ZMQ.Poller(1);
        items.register(clientSocket, ZMQ.Poller.POLLIN);
    }

    /**
     * Returns the reply message or NULL if there was no reply. Does not attempt
     * to recover from a broker failure, this is not possible without storing
     * all unanswered requests and resending them all…
     */
    public ZMsg recv() throws Throwable{
        ZMsg reply = null;
        try {
            if (items.poll(timeout * 1000) == -1)
                return null; // Interrupted

            if (items.pollin(0)) {
                checkAndSleep("recv");
                ZMsg msg = ZMsg.recvMsg(clientSocket, ZMQ.DONTWAIT);
                canUseSocket.getAndSet(true);

                // Don't try to handle errors, just assert noisily
                assert (msg.size() >= 4);

                ZFrame empty = msg.pop();
                assert (empty.getData().length == 0);
                empty.destroy();

                ZFrame header = msg.pop();
                assert (MDP.C_CLIENT.toString().equals(header.toString())) : header.toString() + " vs. " + MDP.C_CLIENT;
                header.destroy();

                ZFrame replyService = msg.pop();
                replyService.destroy();

                reply = msg;
            }
            return reply;
        }catch (Exception e){
            destroyInCascade( this );
            return null;
        }
    }

    /**
     * Send request to broker and get reply by hook or crook Takes ownership of
     * request message and destroys it when sent.
     */
    public boolean send(String service, ZMsg request) throws Throwable{
        try {
            assert (request != null);
            // Prefix request with protocol frames
            // Frame 0: empty (REQ emulation)
            // Frame 1: "MDPCxy" (six bytes, MDP/Client x.y)
            // Frame 2: Service name (printable string)
            request.addFirst(service);
            request.addFirst(MDP.C_CLIENT.newFrame());
            request.addFirst("");
            checkAndSleep("send");
            boolean wentWell = request.send(clientSocket);
            canUseSocket.getAndSet(true);
            if (!wentWell) {
                return false;
            }
            return true;
        }catch (Throwable e){
            e.printStackTrace();
            destroyInCascade(this);
            return false;
        }
    }

    public void close(DestroyableCallback callback) throws Throwable{
        this.callback = callback;
        destroyInCascade(this);
    }

    @Override
    public void destroyInCascade(Object destroyedObj) throws Throwable{
        if( !isAlreadyDestroyed.getAndSet(true) ) {
            checkAndSleep("destroyInCascade");
            if (clientSocket != null) {
                ctx.destroySocket(clientSocket);
            }
            ctx.destroy();
            Log4J.info(this, "Gracefully destroying...");
            if(callback != null) callback.destroyInCascade( this );
        }
        canUseSocket.getAndSet(true);
    }

    private void checkAndSleep(String who){
        try {
            int times = 0;
            while (!canUseSocket.get() && times++ < 200) { // 200 * 5 = 1000 milliseconds
                Log4J.error(this, "waiting for atomic boolean: " + who + "\tbefore: " + whoPrevious);
                Thread.sleep(5);
                if(times == 200){
                    System.currentTimeMillis();
                }
            }
            canUseSocket.getAndSet(false);
            whoPrevious = who;
            //Log4J.warn(this, "setting to false: " + who);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
