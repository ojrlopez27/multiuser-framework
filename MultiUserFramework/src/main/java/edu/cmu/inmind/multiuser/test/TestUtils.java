package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.composer.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;

import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 6/28/17.
 */
public class TestUtils {

    public static PluginModule[] getModules(Class<? extends ProcessOrchestratorImpl> orchestrator){
        return new PluginModule[]{
                new PluginModule.Builder( orchestrator, TestPluggableComponent.class, "test")
                        .build()
        };
    }


    public static PluginModule[] getModulesPerf(Class<? extends ProcessOrchestratorImpl> orchestrator){
        return new PluginModule[]{
                new PluginModule.Builder( orchestrator, PerformanceTestPC.class, "test")
                        .addPlugin(PerformanceTestPCStateless.class, "stateless")
                        .build()
        };
    }

    @Deprecated
    public static PluginModule[] getModules(Class<? extends ProcessOrchestratorImpl> orchestrator, Class[] pluggins){
        PluginModule.Builder builder = new PluginModule.Builder( orchestrator );
        for( Class clazz : pluggins ){
            builder.addPlugin( clazz, clazz.getSimpleName() );
        }
        return new PluginModule[]{ builder.build() };
    }

    public static Config createConfig(String serverAddress, int port) {
        return new Config.Builder()
                // you can add values directly like this:
                .setSessionManagerPort(port)
                .setDefaultNumOfPoolInstances(10)
                // or you can refer to values in your config.properties file:
                .setPathLogs(CommonUtils.getProperty("pathLogs", "/logs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress(serverAddress)
//                .setJsonServicesConfig("services.json")
                .setExceptionTraceLevel(Constants.SHOW_ALL_EXCEPTIONS)
                .setCorePoolSize(1000)
                .setNumOfSockets(1)
                .build();
    }

    public static Config createConfigWithServices(String serverAddress, int port) {
        return new Config.Builder()
                // you can add values directly like this:
                .setSessionManagerPort(port)
                .setDefaultNumOfPoolInstances(10)
                // or you can refer to values in your config.properties file:
                .setPathLogs(CommonUtils.getProperty("pathLogs", "/logs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress(serverAddress)
//                .setJsonServicesConfig("services.json")
                .setExceptionTraceLevel(Constants.SHOW_ALL_EXCEPTIONS)
                .setCorePoolSize(1000)
                .setNumOfSockets(1)
                .build()
                .setJsonServicesConfig("services.json");
    }
}
