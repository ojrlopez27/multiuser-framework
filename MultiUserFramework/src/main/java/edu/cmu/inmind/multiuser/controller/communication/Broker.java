package edu.cmu.inmind.multiuser.controller.communication;

/**
 * Created by oscarr on 3/28/17.
 */

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;

/**
 *  Majordomo Protocol broker
 *  A minimal implementation of http://rfc.zeromq.org/spec:7 and spec:8
 */
public class Broker extends Thread {

    // We'd normally pull these from config data
    private static final String INTERNAL_SERVICE_PREFIX = "mmi.";
    private static final int HEARTBEAT_LIVENESS = 5; // 3-5 is reasonable
    private static final int HEARTBEAT_INTERVAL = 2500; // msecs
    private static final int HEARTBEAT_EXPIRY = HEARTBEAT_INTERVAL * HEARTBEAT_LIVENESS;
    private boolean isTerminated = false;
    private int port;

    // ---------------------------------------------------------------------

    /**
     * This defines a single service.
     */
    private static class Service {
        public final String name; // Service name
        Deque<ZMsg> requests; // List of client requests
        Deque<Worker> waiting; // List of waiting workers

        public Service(String name) {
            this.name = name;
            this.requests = new ArrayDeque<>();
            this.waiting = new ArrayDeque<>();
        }

        @Override
        public String toString() {
            return "service " + name;
        }
    }

    /**
     * This defines one worker, idle or active (orchestrator).
     */
    private static class Worker {
        String identity;// Identity of worker
        ZFrame address;// Address frame to route to
        Service service; // Owning service, if known
        long expiry;// Expires at unless heartbeat

        public Worker(String identity, ZFrame address) {
            this.address = address;
            this.identity = identity;
            this.expiry = System.currentTimeMillis() + HEARTBEAT_INTERVAL * HEARTBEAT_LIVENESS;
        }
        @Override public String toString() {
            return "worker " + identity + " for service " + (service == null? service : service.toString())
                    + " with address " + address + " and expiration " + expiry;
        }
    }

    // ---------------------------------------------------------------------

    private ZContext ctx;// Our context
    private ZMQ.Socket socket; // Socket for clients & workers

    private long heartbeatAt;// When to send HEARTBEAT
    private Map<String, Service> services;// known services
    private Map<String, Worker> workers;// known workers
    private Deque<Worker> waiting;// idle workers

    // ---------------------------------------------------------------------

    /**
     * Initialize broker state.
     */
    public Broker(int port) {
        super("broker thread");
        this.services = new HashMap<>();
        this.workers = new HashMap<>();
        this.waiting = new ArrayDeque<>();
        this.heartbeatAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL;
        this.ctx = new ZContext();
        this.socket = ctx.createSocket(ZMQ.ROUTER);
        this.port = port;
    }

    // ---------------------------------------------------------------------
    @Override
    public void run(){
        try {
            bind("tcp://*:" + port);
            mediate();
        }catch (Exception e){
            ExceptionHandler.handle( e );
        }
    }

    /**
     * Main broker work happens here
     */
    public void mediate() throws Exception{
        while (!Thread.currentThread().isInterrupted()) {
            //Log4J.debug(this, "polling ZMQ");
            ZMQ.Poller items = new ZMQ.Poller(1);
            items.register(socket, ZMQ.Poller.POLLIN);
            if (items.poll(HEARTBEAT_INTERVAL) == -1)
                break; // Interrupted
            if (items.pollin(0)) {
                ZMsg msg = ZMsg.recvMsg(socket);
                if (msg == null) {
                    Log4J.debug(this, "interrupted while receiving mesage.");
                    break; // Interrupted
                }

                ZFrame sender = msg.pop();
                ZFrame empty = msg.pop();
                ZFrame header = msg.pop();

                if (MDP.C_CLIENT.frameEquals(header)) {
                    Log4J.debug(this, "received message " + msg.toString() + " for client and coming from " + sender.toString());
                    processClient(sender, msg);
                } else if (MDP.S_ORCHESTRATOR.frameEquals(header)) {
                    Log4J.debug(this, "received message " + msg.toString() + " is for orchestrator");
                    processWorker(sender, msg);
                }else {
                    Log4J.debug(this, "received message " + msg.toString() + " remains unhandled.");
                    msg.destroy();
                }

                sender.destroy();
                empty.destroy();
                header.destroy();

            }
            purgeWorkers();
            sendHeartbeats();
        }
        Log4J.debug(this, "mediate() is about to terminate.");
        close(); // interrupted
    }

    /**
     * Disconnect all workers, destroy context.
     */
    public void close() throws Exception{
        if( !isTerminated ){
            isTerminated = true;
            ArrayList<Worker> wrkrs = new ArrayList( workers.values() );
            wrkrs.forEach(worker -> deleteWorker(worker, true));
            ctx.destroy();
            Log4J.debug(this, "Broker is now closing down.");
        }
    }

    /**
     * Process a request coming from a client.
     */
    private void processClient(ZFrame sender, ZMsg msg) {
        ExceptionHandler.checkAssert(msg.size() >= 2); // Service name + body
        ZFrame serviceFrame = msg.pop();
        // Set reply return address to client sender
        msg.wrap(sender.duplicate());
        if (serviceFrame.toString().startsWith(INTERNAL_SERVICE_PREFIX))
            serviceInternal(serviceFrame, msg);
        else
            dispatch(requireService(serviceFrame), msg);
        serviceFrame.destroy();
    }

    /**
     * Process message sent to us by a worker.
     */
    private void processWorker(ZFrame sender, ZMsg msg) {
        ExceptionHandler.checkAssert( (msg.size() >= 1) ); // At least, command

        ZFrame command = msg.pop();

        boolean workerReady = workers.containsKey(sender.strhex());

        Worker worker = requireWorker(sender);

        if (MDP.S_READY.frameEquals(command)) {
            // Not first command in session || Reserved service name
            if (workerReady
                    || sender.toString().startsWith(INTERNAL_SERVICE_PREFIX))
                deleteWorker(worker, true);
            else {
                // Attach worker to service and mark as idle
                ZFrame serviceFrame = msg.pop();
                worker.service = requireService(serviceFrame);
                workerWaiting(worker);
                serviceFrame.destroy();
            }
        } else if (MDP.S_REPLY.frameEquals(command)) {
            if (workerReady) {
                // Remove & save client return envelope and insert the
                // protocol header and service name, then rewrap envelope.
                ZFrame client = msg.unwrap();
                msg.addFirst(worker.service.name);
                msg.addFirst(MDP.C_CLIENT.newFrame());
                msg.wrap(client);
                msg.send(socket);
                workerWaiting(worker);
            } else {
                deleteWorker(worker, true);
            }
        } else if (MDP.S_HEARTBEAT.frameEquals(command)) {
            if (workerReady) {
                worker.expiry = System.currentTimeMillis() + HEARTBEAT_EXPIRY;
            } else {
                deleteWorker(worker, true);
            }
        } else if (MDP.S_DISCONNECT.frameEquals(command))
            deleteWorker(worker, false);
        else {
            Log4J.error(this, "invalid message: " + command.toString());
        }
        msg.destroy();
    }

    /**
     * Deletes worker from all data structures, and destroys worker.
     */
    private void deleteWorker(Worker worker, boolean disconnect) {
        ExceptionHandler.checkAssert( (worker != null) );
        if (disconnect) {
            sendToWorker(worker, MDP.S_DISCONNECT, null, null);
        }
        if (worker.service != null)
            worker.service.waiting.remove(worker);
        workers.remove(worker.identity);
        worker.address.destroy();
    }

    /**
     * Finds the worker (creates if necessary).
     */
    private Worker requireWorker(ZFrame address) {
        ExceptionHandler.checkAssert( (address != null) );
        String identity = address.strhex();
        Worker worker = workers.get(identity);
        if (worker == null) {
            worker = new Worker(identity, address.duplicate());
            workers.put(identity, worker);
        }
        return worker;
    }

    /**
     * Locates the service (creates if necessary).
     */
    private Service requireService(ZFrame serviceFrame) {
        ExceptionHandler.checkAssert( (serviceFrame != null) );
        String name = serviceFrame.toString();
        Service service = services.get(name);
        if (service == null) {
            service = new Service(name);
            services.put(name, service);
        }
        return service;
    }

    /**
     * Bind broker to endpoint, can call this multiple times. We use a single
     * socket for both clients and workers.
     */
    public void bind(String endpoint) {
        socket.bind(endpoint);
    }

    /**
     * Handle internal service according to 8/MMI specification
     */
    private void serviceInternal(ZFrame serviceFrame, ZMsg msg) {
        String returnCode = "501";
        if ("mmi.service".equals(serviceFrame.toString())) {
            String name = msg.peekLast().toString();
            returnCode = services.containsKey(name) ? "200" : "400";
        }
        msg.peekLast().reset(returnCode.getBytes());
        // Remove & save client return envelope and insert the
        // protocol header and service name, then rewrap envelope.
        ZFrame client = msg.unwrap();
        msg.addFirst(serviceFrame.duplicate());
        msg.addFirst(MDP.C_CLIENT.newFrame());
        msg.wrap(client);
        msg.send(socket);
    }

    /**
     * Send heartbeats to idle workers if it's time
     */
    public synchronized void sendHeartbeats() {
        // Send heartbeats to idle workers if it's time
        if (System.currentTimeMillis() >= heartbeatAt) {
            for (Worker worker : waiting) {
                sendToWorker(worker, MDP.S_HEARTBEAT, null, null);
            }
            heartbeatAt = System.currentTimeMillis() + HEARTBEAT_INTERVAL;
        }
    }

    /**
     * Look for & kill expired workers. Workers are oldest to most recent, so we
     * stop at the first alive worker.
     */
    public synchronized void purgeWorkers() {
        Iterator<Worker> iterator = waiting.iterator();
        while(iterator.hasNext()){
            Worker w = iterator.next();
            if (w.expiry < System.currentTimeMillis()){
                iterator.remove();
                deleteWorker(w, false);
            }
        }
    }

    /**
     * This worker is now waiting for work.
     */
    public synchronized void workerWaiting(Worker worker) {
        // Queue to broker and service waiting lists
        waiting.addLast(worker);
        worker.service.waiting.addLast(worker);
        worker.expiry = System.currentTimeMillis() + HEARTBEAT_EXPIRY;
        dispatch(worker.service, null);
    }

    /**
     * Dispatch requests to waiting workers as possible
     */
    private void dispatch(Service service, ZMsg msg) {
        ExceptionHandler.checkAssert( (service != null) );
        if (msg != null)// Queue message if any
            service.requests.offerLast(msg);
        purgeWorkers();
        while (!service.waiting.isEmpty() && !service.requests.isEmpty()) {
            msg = service.requests.pop();
            Worker worker = service.waiting.pop();
            waiting.remove(worker);
            sendToWorker(worker, MDP.S_REQUEST, null, msg);
            msg.destroy();
        }
    }

    /**
     * Send message to worker. If message is provided, sends that message. Does
     * not destroy the message, this is the caller's job.
     */
    public void sendToWorker(Worker worker, MDP command, String option,
                             ZMsg msgp) {

        ZMsg msg = msgp == null ? new ZMsg() : msgp.duplicate();

        // Stack protocol envelope to start of message
        if (option != null)
            msg.addFirst(new ZFrame(option));
        msg.addFirst(command.newFrame());
        msg.addFirst(MDP.S_ORCHESTRATOR.newFrame());

        // Stack routing envelope to start of message
        msg.wrap(worker.address.duplicate());
        Log4J.debug(this, "sending to worker " + worker.toString());
        msg.send(socket);
    }
}
