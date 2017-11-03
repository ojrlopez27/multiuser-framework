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
import java.util.concurrent.atomic.AtomicBoolean;
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
    static final long       timeout = 1000 * 60 * 5 * 10; // 50 minutes
    static final long       delaySendMsg = 100;
    static final boolean    verbose = false;
    static final int        numAgents = 100;
    static final int        numMessages = 10;
    static final String     url = useExternalMUF? "tcp://34.203.160.208" : "tcp://127.0.0.1";
    static final long       delayMUF = 1000;
    static final int        portMUF = 5555;
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
            Utils.execute(agents[idx]);
        }

        //awaitility
        await().atMost(timeout, TimeUnit.MILLISECONDS).until( () -> receivedMsgs.get() == totalMessages);

        time = System.currentTimeMillis() - time;
        System.out.println("Total time: " + time + " and total received: " + receivedMsgs.get()+ " total: " + totalMessages );
        System.out.println("Average per message: " + (time / (double) totalMessages) );

        long total = 0;
        for(Agent agent : agents ){
            total += agent.getTotalTime();
        }
        System.out.println("Total time by sections: " + total);
        if(usePrintWriter) printWriter.write( String.format("%s\t%s\t%s\n", numAgents, numMessages, total ) );

        for(Agent agent : agents){
            agent.destroy();
        }
        if( !useExternalMUF ) {
            MultiuserFrameworkContainer.stopFramework(muf);
            muf = null;
        }
    }
    /**
     * This class wraps the agent/client that sends requests to and receives responses from MUF
     */
    class Agent implements Utils.NamedRunnable {
        private String agentId;
        private int id;
        private ClientCommController ccc;
        private int numMessages;
        int receivedMessages = 0;
        private ConcurrentHashMap<Integer, Long> times;
        private AtomicBoolean stop = new AtomicBoolean(true);

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
            Log4J.track(this, "1:@@@:" + agentId + ".");
            ccc = new ClientCommController.Builder()
                    .setServerAddress(url + ":" + portMUF)
                    .setServiceName( agentId )
                    .setClientAddress(url + ":" + portMUF)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setResponseListener(message -> {
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
                                Log4J.track(this, "2:@@@:" + agentId + ".");
                            }else if( !message.contains(Constants.SESSION_CLOSED) && !message.contains(Constants.SESSION_RECONNECTED)){
                                Log4J.track(this, "35:" + message);
                                int key = Integer.valueOf(message.split(":")[2]);
                                long value = times.get(key);
                                times.put( key,  System.nanoTime() - value );
                                receivedMsgs.incrementAndGet();
                                receivedMessages++;
                                if(receivedMsgs.get() % 1 == 0)//if(verbose)
                                    Log4J.debug(this, String.format("%s receives: %s receivedMessages: %s total: %s", agentId,
                                            message, receivedMessages, receivedMsgs.get()));
                                if(receivedMessages == numMessages ){
                                    Log4J.track(this, "36:" + message);
                                    ids[id] = null;
                                    StringBuffer left =  new StringBuffer("");
                                    for( Integer id : ids ){
                                        if(id != null){
                                            left.append(id + ", ");
                                        }
                                    }
                                    if( verbose )
                                        Log4J.debug(this, "Left threads: " + left);
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
                // let's wait until it connects
                while( stop.get() ){
                    Utils.sleep(100);
                }
                String message = "";
                for (int i = 0; i < numMessages; i++) {
                    //we send plain strings instead of SessionMessage to avoid json parsing
                    message = "@@@:" + agentId + ":"  +(i + 1);
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
}
