import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;

import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 6/28/17.
 */
public class TestUtils {

    public static PluginModule[] getModules(){
        return new PluginModule[]{
                new PluginModule.Builder( TestOrchestrator.class )
                        .addPlugin(TestPluggableComponent.class, "test")
                        .build()
        };
    }

    public static Config createConfig(String serverAddress, int port) {
        return new Config.Builder()
                // you can add values directly like this:
                .setSessionManagerPort(port)
                .setDefaultNumOfPoolInstances(10)
                // or you can refer to values in your config.properties file:
                //.setPathLogs(Utils.getProperty("pathLogs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress(serverAddress)
                .setExceptionTraceLevel(Constants.SHOW_MUF_EXCEPTIONS)
                .build();
    }
}
