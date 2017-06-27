package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.Formatter;

/**
 * Created by oscarr on 3/29/17.
 */

public class ClientCommAPI {
    private String broker;
    private ZContext ctx;
    private ZMQ.Socket client;
    private long timeout = 10000; // ten seconds
    private int highWaterMark = 10 * 1000; //amount of enqueued messages
    private ZMQ.Poller items; // Poll socket for a reply, with timeout
    private Formatter log = new Formatter(System.out);

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public ClientCommAPI(String broker) throws Throwable{
        this.broker = broker;
        ctx = new ZContext();
        reconnectToBroker();
        initialize();
    }

    /**
     * Connect or reconnect to broker
     */
    void reconnectToBroker() {
        if (client != null) {
            ctx.destroySocket(client);
        }
        client = ctx.createSocket(ZMQ.DEALER);
        client.setSendTimeOut(0); //  Send messages immediately or return EAGAIN  ojrl
        client.setHWM(highWaterMark); //  Set a high-water mark that allows for reasonable activity
        client.setRcvHWM(highWaterMark);
        client.connect(broker);
    }

    public void initialize() throws Throwable{
        // Poll socket for a reply, with timeout
        items = new ZMQ.Poller(1);
        items.register(client, ZMQ.Poller.POLLIN);
    }

    /**
     * Returns the reply message or NULL if there was no reply. Does not attempt
     * to recover from a broker failure, this is not possible without storing
     * all unanswered requests and resending them all…
     */
    public ZMsg recv() throws Throwable{
        ZMsg reply = null;
        if (items.poll(timeout * 1000) == -1)
            return null; // Interrupted

        if (items.pollin(0)) {
            Log4J.debug(this, "attempting to receive ... ");
            ZMsg msg = ZMsg.recvMsg(client, ZMQ.DONTWAIT);
            Log4J.debug(this, "received message " + msg.toString());

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
    }

    /**
     * Send request to broker and get reply by hook or crook Takes ownership of
     * request message and destroys it when sent.
     */
    public boolean send(String service, ZMsg request) throws Throwable{
        assert (request != null);

        // Prefix request with protocol frames
        // Frame 0: empty (REQ emulation)
        // Frame 1: "MDPCxy" (six bytes, MDP/Client x.y)
        // Frame 2: Service name (printable string)
        request.addFirst(service);
        request.addFirst(MDP.C_CLIENT.newFrame());
        request.addFirst("");
        //Log4J.debug(this, "about to send " + request.toString());
        if( !request.send(client) ){
            //Log4J.debug(this, "failed to send " + request.toString());
            return false;
        }
        //Log4J.debug(this, "successfully sent " + request.toString());
        return true;
    }

    public void destroy() throws Throwable{
        ctx.destroy();
    }
}
