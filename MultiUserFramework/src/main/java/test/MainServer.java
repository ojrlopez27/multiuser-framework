package test;

import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.muf.MUFLifetimeManager;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;


import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 10/16/18.
 */
class MainServer {

    public static void main(String args[]) throws Exception{
        MUFLifetimeManager.startFramework(
                new PluginModule[]{
                        new PluginModule.Builder( MyOrchestrator.class, MyComponent.class, "test").build()},
                        new Config.Builder()
                                // you can add values directly like this:
                                .setSessionManagerPort(5555)
                                .setDefaultNumOfPoolInstances(10)
                                // or you can refer to values in your config.properties file:
                                .setPathLogs(CommonUtils.getProperty("pathLogs", "/logs"))
                                .setSessionTimeout(5, TimeUnit.MINUTES)
                                .setServerAddress("tcp://127.0.0.1")
                                .setExceptionTraceLevel(Constants.SHOW_ALL_EXCEPTIONS)
                                .setCorePoolSize(1000)
                                .setNumOfSockets(1)
                                .build());
        CommonUtils.sleep(2000); //give some time to initialize the MUF
    }
}
