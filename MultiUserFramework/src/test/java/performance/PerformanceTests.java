package performance;

import common.TestUtils;
import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.MultiuserFrameworkContainer;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.*;


/**
 * Created by oscarr on 10/17/17.
 */
public class PerformanceTests {
    static AtomicInteger receivedMsgs = new AtomicInteger(0);
    static AtomicInteger initializedAgents = new AtomicInteger(0);
    static AtomicInteger sentMessages = new AtomicInteger(0);
    static int totalMessages;
    static MultiuserFramework muf;
    static Agent[] agents;
    static Integer[] ids;
    static PrintWriter printWriter;
    // constants:
    static boolean          useExternalMUF = false;
    static final long       timeout = 1000 * 60 * 5; // 5 minutes
    static final long       delaySendMsg = 200;
    static final boolean    verbose = false;
    static final int        numAgents = 1;
    static final int        numMessages = 10;
    static final String     url = useExternalMUF? "tcp://34.203.160.208" : "tcp://127.0.0.1";
    static final long       delayMUF = 2000;
    static final int        portMUF = 5666;
    static final long       delayAgentCreation = 100;
    static final boolean    usePrintWriter = false;

    @Test(timeout = timeout)
    public void testPerformanceOneAgent() throws Throwable{
        if( usePrintWriter ) {
            printWriter = new PrintWriter( new File("results.txt") );
            int[] numAgents = new int[]{1, 10};
            int[] numMsgs = new int[]{1, 10};
            for (int agents : numAgents) {
                for (int messages : numMsgs) {
                    for (int i = 0; i < 3; i++) {
                        //runAgents(agents, messages);
                        Thread.sleep(2000);
                    }
                }
            }
        }
        runAgents();
        if( usePrintWriter ) {
            printWriter.flush();
            printWriter.close();
        }
    }


    private void runAgents() throws Throwable{
        totalMessages = numMessages * numAgents;
        agents = new Agent[numAgents];
        ids = new Integer[numAgents];

        // creates a MUF and set TCP to on or off
        if( !useExternalMUF ) {
            muf = MultiuserFrameworkContainer.startFramework(
                    TestUtils.getModulesPerf(PerformanceTestOrchestrator.class),
                    TestUtils.createConfig(url, portMUF));
            Utils.sleep(delayMUF); //give some time to initialize the MUF
        }

        // let's create a client(s) that sends messages to MUF
        for( int i = 0; i < numAgents; i++) {
            String agentId = "agent-" + i;
            agents[i] = new Agent( agentId, numMessages);
            ids[i] = i;
            Utils.sleep(delayAgentCreation);
        }

        // let's run the agents...
        long time = System.currentTimeMillis();
        for(int i = 0; i < numAgents; i++ ) {
            final int idx = i;
            //Utils.execute(() -> agents[idx].run());
            agents[idx].setName("agent-"+idx);
            Utils.execute(agents[idx]);
        }

        //awaitility
        await().atMost(timeout, TimeUnit.MILLISECONDS).until( () -> receivedMsgs.get() == totalMessages);
        //Utils.sleep(5000);

        Log4J.warn(this, "=== 4");
        time = System.currentTimeMillis() - time;
        System.out.println("Total time: " + time + " and total received: " + receivedMsgs.get()+ " total: " + totalMessages );
        System.out.println("Average per message: " + (time / (double) totalMessages) );

        long total = 0;
        for(Agent agent : agents ){
            total += agent.getTotalTime();
        }
        System.out.println("Total time by sections: " + total);
        if(usePrintWriter) printWriter.write( String.format("%s\t%s\t%s\n", numAgents, numMessages, total ) );

//        for(Agent agent : agents){
//            agent.destroy();
//        }
        if( !useExternalMUF ) {
            Log4J.warn(this, "=== 5");
            MultiuserFrameworkContainer.stopFramework(muf);
            muf = null;
        }
    }
    /**
     * This class wraps the agent/client that sends requests to and receives responses from MUF
     */
    class Agent extends Utils.MyRunnable{
        private String agentId;
        private int id;
        private ClientCommController ccc;
        private int numMessages;
        int receivedMessages = 0;
        private ConcurrentHashMap<Integer, Long> times;
        private boolean stop = false;

        public long getTotalTime() {
            long totalTime = 0;
            for( Long time : times.values() ){
                totalTime += time;
            }
            return TimeUnit.NANOSECONDS.toMillis(totalTime);
        }

        Agent(String agentId, int numMessages){
            this.agentId = agentId;
            this.id = Integer.valueOf(agentId.split("-")[1]);
            this.numMessages = numMessages;
            this.times = new ConcurrentHashMap<>();

            ccc = new ClientCommController.Builder()
                    .setServerAddress(url + ":" + portMUF)
                    .setServiceName( agentId )
                    .setClientAddress(url + ":" + portMUF)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setResponseListener(message -> {
                        try {
                            //Utils.printNewAddedThreads();
                            Log4J.error(this, "Active: "+Thread.activeCount());
                            if( message.contains(Constants.SESSION_INITIATED) ){
                                initializedAgents.incrementAndGet();
                                //if(verbose)
                                    Log4J.debug(this, String.format("initialized agent %s  total: %s", agentId,
                                            initializedAgents.get() ) );
                            }else if( !message.contains(Constants.SESSION_CLOSED) && !message.contains(Constants.SESSION_RECONNECTED)){
                                int key = Integer.valueOf(message);
                                long value = times.get(key);
                                times.put( key,  System.nanoTime() - value );
                                Log4J.warn(this, "=== 1");
                                receivedMsgs.incrementAndGet();
                                receivedMessages++;
                                if( receivedMessages != key ){
                                    Log4J.error(this, String.format("receivedMessages: %s and payload: %s", receivedMessages,
                                            message));
                                }
                                if(receivedMsgs.get() % 1 == 0)//if(verbose)
                                    Log4J.debug(this, String.format("%s receives: %s receivedMessages: %s total: %s", agentId,
                                            message, receivedMessages, receivedMsgs.get()));
                                if(receivedMessages == numMessages ){
                                    ids[id] = null;
                                    StringBuffer faltan =  new StringBuffer("");
                                    for( Integer id : ids ){
                                        if(id != null){
                                            faltan.append(id + ", ");
                                        }
                                    }
                                    Log4J.debug(this, "Faltan: " + faltan);
                                    Log4J.error(this, String.format("**** %s completed", agentId));
                                    Log4J.warn(this, "=== 2");
                                    destroy();
                                }
                            }
                        } catch (Throwable e) {
                            ExceptionHandler.handle(e);
                        }
                    })
                    .build();
        }

        public void run(){
            try {
                for (int i = 0; i < numMessages; i++) {
                    //we send plain strings instead of SessionMessage to avoid json parsing
                    String message = "" + (i + 1);
                    times.put( (i + 1), System.nanoTime() );
                    ccc.send(agentId, message);
                    int sent = sentMessages.incrementAndGet();
                    Utils.sleep(delaySendMsg);
                    //if (verbose)
                        Log4J.debug(this, String.format("%s sends: %s Total sent: %s", agentId, message, sent));
                }
                while( !stop ){
                    Utils.sleep(100);
                }
                Log4J.warn(this, "Chao from " + agentId);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        public void destroy(){
            Log4J.warn(this, "=== 3");
            ccc.close( null );
            stop = true;
            if( Thread.currentThread().isAlive() ){
                Thread.currentThread().interrupt();
            }
        }
    }
}
