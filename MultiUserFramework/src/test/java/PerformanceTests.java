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
    static MultiuserFramework muf;
    static boolean useExternalMUF = false;
    final long timeout = 1000 * 60 * 5;
    final long delay = 10;
    boolean verbose = false;
    PrintWriter printWriter;

    @Test(timeout = timeout)
    public void testPerformanceOneAgent() throws Throwable{
//        int[] numAgents = new int[]{1, 10, 50, 100, 500, 1000, 5000, 10000};
//        int[] numMsgs = new int[]{1, 10, 100, 1000, 10000, 100000, 1000000};
        int[] numAgents = new int[]{1, 10};
        int[] numMsgs = new int[]{1, 10};
        printWriter = new PrintWriter( new File("results.txt") );
//        for( int agents : numAgents ){
//            for( int messages : numMsgs ){
//                for(int i = 0; i < 3; i++ ){
//                    runAgents(agents, messages);
//                    Thread.sleep(2000);
//                }
//            }
//        }
        runAgents(10, 1);
        printWriter.flush();
        printWriter.close();
    }


    private void runAgents(int numAgents, int numMessages) throws Throwable{
        int totalMessages = numMessages * numAgents;
        Agent[] agents = new Agent[numAgents];
        String url = useExternalMUF? "tcp://34.203.160.208" : "tcp://127.0.0.1";

        // creates a MUF and set TCP to on or off
        if( !useExternalMUF ) {
            muf = MultiuserFrameworkContainer.startFramework(
                    TestUtils.getModulesPerf(PerformanceTestOrchestrator.class),
                    TestUtils.createConfig(url, 5666));
            Utils.sleep(2000); //give some time to initialize the MUF
        }

        // let's create a client(s) that sends messages to MUF
        for( int i = 0; i < numAgents; i++) {
            String agentId = "agent-" + i;
            agents[i] = new Agent( agentId, numMessages,
                    new ClientCommController.Builder()
                        .setServerAddress(url + ":5666")
                        .setServiceName( agentId )
                        .setClientAddress(url + ":5666")
                        .setRequestType(Constants.REQUEST_CONNECT)
                        .build() );
            Thread.sleep(100);
        }

        // let's run the agents...
        long time = System.currentTimeMillis();
        for(int i = 0; i < numAgents; i++ ){
            final int idx = i;
            Utils.execObsParallel(o -> agents[idx].run());
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
        printWriter.write( String.format("%s\t%s\t%s\n", numAgents, numMessages, total ) );
        if( !useExternalMUF ) {
            MultiuserFrameworkContainer.stopFramework(muf);
            muf = null;
        }
    }






    class Agent{
        private String agentId;
        private ClientCommController ccc;
        private int numMessages;
        int receivedMessages = 0;
        private ConcurrentHashMap<Integer, Long> times;

        public long getTotalTime() {
            long totalTime = 0;
            for( Long time : times.values() ){
                totalTime += time;
                Log4J.debug(this, "totalTime: " + totalTime + "  time: " + time);
            }
            return TimeUnit.NANOSECONDS.toMillis(totalTime);
        }

        Agent(String agentId, int numMessages, ClientCommController ccc){
            this.agentId = agentId;
            this.ccc = ccc;
            this.numMessages = numMessages;
            this.times = new ConcurrentHashMap<>();

            ccc.setResponseListener(message -> {
                try {
                    //System.out.println(String.format("Thread: %s inside setResponseListener", Thread.currentThread().getName()));
                    if( message.contains(Constants.SESSION_INITIATED) ){
                        initializedAgents.incrementAndGet();
                        if(verbose)
                            Log4J.debug(this, String.format("initialized agent %s  total: %s", agentId,
                                    initializedAgents.get() ) );
                    }else if( !message.contains(Constants.SESSION_CLOSED) && !message.contains(Constants.SESSION_RECONNECTED)){
                        int key = Integer.valueOf(message);
                        long value = times.get(key);
                        times.put( key,  System.nanoTime() - value );
                        receivedMsgs.incrementAndGet();
                        receivedMessages++;
                        if( receivedMessages != key ){
                            Log4J.error(this, String.format("receivedMessages: %s and payload: %s", receivedMessages,
                                    message));
                        }
                        if(receivedMsgs.get() % 1 == 0)//if(verbose)
                            Log4J.debug(this, String.format("%s receives: %s receivedMessages: %s total: %s", agentId,
                                    message, receivedMessages, receivedMsgs.get()));
                    }
                } catch (Throwable e) {
                    ExceptionHandler.handle(e);
                }
            });
        }

        public void run(){
            try {
                for (int i = 0; i < numMessages; i++) {
                    //we send plain strings instead of SessionMessage to avoid json parsing
                    String message = "" + (i + 1);
                    times.put( (i + 1), System.nanoTime() );
                    ccc.send(agentId, message);
                    Thread.sleep(delay);
                    //if (verbose)
                        Log4J.debug(this, String.format("%s sends: %s ", agentId, message));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
