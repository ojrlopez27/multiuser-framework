package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ClientController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class wraps the agent/client that sends requests to and receives responses from MUF
 */
public class PerformanceTestAgent implements Utils.NamedRunnable {
    private String agentId;
    private int id;
    private ClientController ccc;
    private int numMessages;
    int receivedMessages = 0;
    private ConcurrentHashMap<Integer, Long> times;
    private AtomicBoolean stop = new AtomicBoolean(true);
    private boolean verbose;
    private long delaySendMsg;
    private AtomicInteger initializedAgents = new AtomicInteger(0);
    private AtomicInteger sentMessages = new AtomicInteger(0);
    private AtomicInteger receivedMsgs = new AtomicInteger(0);
    private AtomicInteger[] ids;

    public long getTotalTime() {
        long totalTime = 0;
        for( Long time : times.values() ){
            totalTime += time;
        }
        return TimeUnit.NANOSECONDS.toMillis(totalTime);
    }

    public PerformanceTestAgent(String agentId, int numMessages, String url, int port, long delaySendMsg,
                         AtomicInteger initializedAgents, AtomicInteger sentMessages, AtomicInteger receivedMsgs,
                         AtomicInteger[] ids, boolean verbose){
        this.agentId = agentId;
        this.id = Integer.valueOf(agentId.split("-")[1]);
        this.numMessages = numMessages;
        this.times = new ConcurrentHashMap<>();
        this.delaySendMsg = delaySendMsg;
        this.initializedAgents = initializedAgents;
        this.sentMessages = sentMessages;
        this.receivedMsgs = receivedMsgs;
        this.verbose = verbose;
        this.ids = ids;
        ccc = new ClientCommController.Builder()
                .setServerAddress(url + ":" + port)
                .setServiceName( agentId )
                .setClientAddress(url + ":" + port)
                .setRequestType(Constants.REQUEST_CONNECT)
                .setResponseListener(new AgentResponseListener())
                .build();
    }

    class AgentResponseListener implements ResponseListener {
        @Override
        public void process(String message) {
            try {
                if( verbose ) {
                    Utils.printNewAddedThreads();
                    Log4J.track(this, "Active: " + Thread.activeCount());
                }
                if( message.contains(Constants.SESSION_INITIATED) ){
                    initializedAgents.incrementAndGet();
                    //if(verbose)
                    Log4J.debug(this, String.format("initialized agent %s  total: %s", agentId,
                            initializedAgents.get() ) );
                    stop.getAndSet( false );
                }else if( !message.contains(Constants.SESSION_CLOSED) && !message.contains(Constants.SESSION_RECONNECTED)){
                    Log4J.track(this, "35:" + message);
                    int key = Integer.valueOf(message.split(":")[2]);
                    long value = times.get(key);
                    times.put( key,  System.nanoTime() - value );
                    receivedMsgs.incrementAndGet();
                    receivedMessages++;
                    if(receivedMsgs.get() % 1 == 0)
                        Log4J.debug(this, String.format("%s receives: %s receivedMessages: %s total: %s", agentId,
                                message, receivedMessages, receivedMsgs.get()));
                    if(receivedMessages == numMessages ){
                        Log4J.track(this, "36:" + message);
                        ids[id].getAndSet(-1);
                        StringBuffer left =  new StringBuffer("");
                        for( AtomicInteger idAgent : ids ){
                            if(idAgent.get() != -1){
                                left.append(idAgent + ", ");
                            }
                        }
                        if( verbose )
                            Log4J.debug(this, "Left threads: " + left);
                    }
                }
            } catch (Throwable e) {
                ExceptionHandler.handle(e);
            }
        }
    }

    @Override
    public void run(){
        try {
            // let's wait until it connects
            while( stop.get() ){
                Utils.sleep(100);
            }
            String message = "";
            for (int i = 0; i < numMessages; i++) {
                //we send plain strings instead of SessionMessage to avoid json parsing
                Log4J.track(this, "3:" + message);
                times.put( (i + 1), System.nanoTime() );
                ccc.send(agentId, message);
                int sent = sentMessages.incrementAndGet();
                Utils.sleep(delaySendMsg);
                //if (verbose)
                Log4J.debug(this, String.format("%s sends: %s Total sent: %s", agentId, message, sent));
            }
            while( !stop.get() ){
                Utils.sleep(100);
            }
            Log4J.track(this, "37:" + message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void destroy(){
        ccc.close( null );
        stop.getAndSet( true );
        if( Thread.currentThread().isAlive() ){
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getName() {
        return agentId;
    }
}


