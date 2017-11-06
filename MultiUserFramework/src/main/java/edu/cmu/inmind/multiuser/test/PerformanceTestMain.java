package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.muf.MUFLifetimeManager;
import edu.cmu.inmind.multiuser.controller.muf.MultiuserController;

import java.io.PrintWriter;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oscarr on 11/4/17.
 */
public class PerformanceTestMain {

    private AtomicInteger receivedMsgs = new AtomicInteger(0);
    private AtomicInteger initializedAgents = new AtomicInteger(0);
    private AtomicInteger sentMessages = new AtomicInteger(0);
    private int totalMessages;
    private MultiuserController muf;
    private PerformanceTestAgent[] agents;
    private AtomicInteger[] ids;
    private long time;

    // constants:
    private static final boolean    useExternalMUF = false;
    private static final int        offset = 0;
    private static final boolean    useScanner = false;
    private static final long       delaySendMsg = 100;
    private static final boolean    verbose = false;
    private static final int        numAgents = 100;
    private static final int        numMessages = 10;
    private static final String     url = useExternalMUF? "tcp://34.203.160.208" : "tcp://127.0.0.1";
    private static final long       delayMUF = 1000;
    private static final int        portMUF = 5555;
    private static final long       delayAgentCreation = 100;

    public void runAgents() throws Throwable{
        totalMessages = numMessages * numAgents;
        agents = new PerformanceTestAgent[numAgents];
        ids = new AtomicInteger[numAgents];

        // creates a MUF and set TCP to on or off
        if( !useExternalMUF ) {
            muf = MUFLifetimeManager.startFramework(
                    TestUtils.getModulesPerf(PerformanceTestOrchestrator.class),
                    TestUtils.createConfig(url, portMUF));
            Utils.sleep(delayMUF); //give some time to initialize the MUF
        }

        // let's create a client(s) that sends messages to MUF
        for(int i = offset; i < (offset + numAgents); i++) {
            String agentId = "agent-" + i;
            agents[i - offset] = new PerformanceTestAgent( agentId, numMessages, url, portMUF, delaySendMsg,
                    initializedAgents, sentMessages, receivedMsgs, ids, verbose);
            ids[i - offset] = new AtomicInteger(i - offset);
            Utils.sleep(delayAgentCreation);
        }

        if( useScanner ) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Press any hey...");
            scanner.nextLine();
        }

        // let's run the agents...
        time = System.currentTimeMillis();
        for(int i = 0; i < numAgents; i++ ) {
            final int idx = i;
            Utils.execute(agents[idx]);
        }
    }

    public void getTimesAndRelease(boolean doMultipleExperiments, PrintWriter printWriter){
        time = System.currentTimeMillis() - time;
        System.out.println("Total time: " + time + " and total received: " + receivedMsgs.get()+ " total: " + totalMessages );
        System.out.println("Average per message: " + (time / (double) totalMessages) );

        long total = 0;
        for(PerformanceTestAgent agent : agents ){
            total += agent.getTotalTime();
        }
        System.out.println("Total time by sections: " + total);
        if(doMultipleExperiments)
            printWriter.write( String.format("%s\t%s\t%s\n", numAgents, numMessages, total ) );

        for(PerformanceTestAgent agent : agents){
            agent.destroy();
        }
        if( !useExternalMUF ) {
            MUFLifetimeManager.stopFramework(muf);
            muf = null;
        }
    }

    public int getReceivedMsgs() {
        return receivedMsgs.get();
    }

    public int getTotalMessages(){
        return totalMessages;
    }




    //-----------------------------------------------------

    public static void main(String args[]) throws Throwable{
        PerformanceTestMain ptm = new PerformanceTestMain();
        ptm.runAgents();

        do{
            Utils.sleep(50);
        }while( ptm.getReceivedMsgs() < ptm.getTotalMessages() );

        ptm.getTimesAndRelease(false, null);
    }
}
