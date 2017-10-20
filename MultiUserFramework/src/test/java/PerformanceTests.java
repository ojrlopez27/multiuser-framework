import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.MultiuserFrameworkContainer;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.junit.Test;

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
    static boolean useExternalMUF = true;
    final long timeout = 1000 * 60 * 3;
    boolean verbose = false;

    @Test(timeout = timeout)
    public void testPerformanceOneAgent() throws Throwable{
        runAgents(1, 1000);
    }


    private void runAgents(int numAgents, int numMessages) throws Throwable{
        int totalMessages = numMessages * numAgents;
        Agent[] agents = new Agent[numAgents];

        // creates a MUF and set TCP to on or off
        if( !useExternalMUF ) {
            muf = MultiuserFrameworkContainer.startFramework(
                    TestUtils.getModulesPerf(PerformanceTestOrchestrator.class),
                    TestUtils.createConfig("tcp://127.0.0.1", 5555));
            Utils.sleep(2000); //give some time to initialize the MUF
        }

        // let's create a client(s) that sends messages to MUF
        for( int i = 0; i < numAgents; i++) {
            String agentId = "agent-" + i;
            agents[i] = new Agent( agentId, numMessages,
                    new ClientCommController.Builder()
                        .setServerAddress("tcp://34.203.160.208:5666")
                        .setServiceName( agentId )
                        .setClientAddress("tcp://34.203.160.208:5666")
                        .setRequestType(Constants.REQUEST_CONNECT)
                        .build() );
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
        if( !useExternalMUF ) MultiuserFrameworkContainer.stopFramework( muf );
    }






    class Agent{
        private String agentId;
        private ClientCommController ccc;
        private int numMessages;
        int receivedMessages = 0;

        Agent(String agentId,  int numMessages, ClientCommController ccc){
            this.agentId = agentId;
            this.ccc = ccc;
            this.numMessages = numMessages;

            ccc.setResponseListener(message -> {
                try {
                    if( message.contains(Constants.SESSION_INITIATED) ){
                        initializedAgents.incrementAndGet();
                        if(verbose)
                            Log4J.debug(this, String.format("initialized agent %s  total: %s", agentId,
                                    initializedAgents.get() ) );
                    }else if( !message.contains(Constants.SESSION_CLOSED) && !message.contains(Constants.SESSION_RECONNECTED)){
                        receivedMsgs.incrementAndGet();
                        receivedMessages++;
                        if( receivedMessages != Integer.valueOf( message ) ){
                            Log4J.error(this, String.format("receivedMessages: %s and payload: %s", receivedMessages,
                                    message));
                        }
                        if(receivedMsgs.get() % 10 == 0)//if(verbose)
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
                    ccc.send(agentId, message);
                    if (verbose)
                        Log4J.debug(this, String.format("%s sends: %s ", agentId, message));
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
