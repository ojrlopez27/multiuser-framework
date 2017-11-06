package performance;

import edu.cmu.inmind.multiuser.test.PerformanceTestMain;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.util.concurrent.*;

import static org.awaitility.Awaitility.*;


/**
 * Created by oscarr on 10/17/17.
 */
public class PerformanceTests {
    private PrintWriter printWriter;
    private PerformanceTestMain ptm;
    // constants:
    private final boolean    doMultipleExperiments = false;
    private final long       timeout = 1000 * 60 * 5 * 10; // 50 minutes

    @Test(timeout = timeout)
    public void testPerformanceOneAgent() throws Throwable{
        ptm = new PerformanceTestMain();
        if(doMultipleExperiments) {
            runMultipleExperiments();
        }
        ptm.runAgents();

        //awaitility
        await().atMost(timeout, TimeUnit.MILLISECONDS).until( () -> ptm.getReceivedMsgs() == ptm.getTotalMessages());

        ptm.getTimesAndRelease(doMultipleExperiments, printWriter);

        if(doMultipleExperiments) {
            printWriter.flush();
            printWriter.close();
        }
    }

    private void runMultipleExperiments() throws Throwable{
        printWriter = new PrintWriter( new File("results.txt") );
        int[] numAgents = new int[]{1, 10};
        int[] numMsgs = new int[]{1, 10};
        for (int agents : numAgents) {
            for (int messages : numMsgs) {
                for (int i = 0; i < 3; i++) {
                    //ptm.runAgents(agents, messages);
                    Thread.sleep(2000);
                }
            }
        }
    }
}
