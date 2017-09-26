package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import zmq.ZError;

import java.nio.channels.ClosedByInterruptException;

/**
 * Created by oscarr on 3/29/17.
 */

public class ClientCommAPI {
    private String broker;
    private ZContext ctx;
    private ZMQ.Socket clientSocket;
    private long timeout = 10000; // ten seconds
    private int highWaterMark = 10 * 1000; //amount of enqueued messages
    private ZMQ.Poller items; // Poll socket for a reply, with timeout

    public ClientCommAPI(String broker) throws Throwable{
        this.broker = broker;
        ctx = new ZContext();
        reconnectToBroker();
        initialize();
    }

    /**
     * Connect or reconnect to broker
     */
    void reconnectToBroker() throws Throwable{
        if (clientSocket != null) {
            ctx.destroySocket(clientSocket);
        }
        clientSocket = ctx.createSocket(ZMQ.DEALER);
        clientSocket.setSendTimeOut(0); //  Send messages immediately or return EAGAIN  ojrl
        clientSocket.setHWM(highWaterMark); //  Set a high-water mark that allows for reasonable activity
        clientSocket.setRcvHWM(highWaterMark);
        clientSocket.connect(broker);
    }

    public void initialize() throws Throwable{
        // Poll socket for a reply, with timeout
        items = new ZMQ.Poller(1);
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
                //Log4J.debug(this, "attempting to receive ... ");
                ZMsg msg = ZMsg.recvMsg(clientSocket, ZMQ.DONTWAIT);
                //            Log4J.debug(this, "received message " + msg.toString());

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
            if( e instanceof ZError.IOException){
                return null;
            }else{
                throw e;
            }
        }
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
        if( !request.send(clientSocket) ){
            return false;
        }
        return true;
    }

    public void destroy() throws Throwable{
        ctx.destroy();
    }
}
