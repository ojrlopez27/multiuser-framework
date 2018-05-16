import edu.cmu.inmind.multiuser.client.DummyClient;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

/**
 * Created by oscarr on 5/8/18.
 */
public class TestDummy {

    /**
     * The simplest test we can do with a dummy client, we use default values for server address (localhost),
     * sessionId (my-session-id) and ResponseListener (MyResponseListener class that just prints out the response
     * from server).
     */
    @Test
    public void testBasicSend(){
        DummyClient dummy = new DummyClient();
        dummy.test();
        dummy.disconnect();
    }

    /**
     * Now we can pass some parameters to DummyClient. We take care of async calls by using awaitility library.
     */
    @Test
    public void testCustomSend(){
        final AtomicBoolean canDisconnect = new AtomicBoolean(false);
        long timeout = 10000;

        final DummyClient dummy = new DummyClient("tcp://128.237.99.154:5555", "my-test",
                new ResponseListener() {
            @Override
            public void process(String message) {
                System.out.println("This is the response from server: " + message);
                Log4J.debug("Test", "This is the response from server: " + message);
                // if we receive a message after the session has been initiated, then we are done and we can disconnect
                if( !message.contains(Constants.SESSION_INITIATED) ){
                    canDisconnect.set(true);
                }
            }
        });


        await().atMost(timeout, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return dummy.getIsConnected();
            }
        });

        // let's send a message
        dummy.send("message from client");

        // let's wait for server to reply, then disconnect
        await().atMost(timeout, TimeUnit.MILLISECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return canDisconnect.get();
            }
        });
        dummy.disconnect();
    }
}
