package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.sara.component.PoolExComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx14;

import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 4/10/17.
 *
 * Your components (PluggableComponents) may behave as stateful or stateless components
 * just by adding a class annotation. An intermediate state is a pool component, that is,
 * it behaves as a stateless component in the sense that it shouldn't keep any state of
 * the system and behaves as a stateful component in the sense that you can have multiple
 * instances of the same class. Therefore, the purpose of a pool component is to load balance
 * since a unique instance (stateless) may collapse any moment if the process it does is
 * heavy. Take a look at PoolExComponent and see that the only thing you have to do is add a
 * PoolComponent annotation.
 * Note: we have defined a small pool of 2 instances (see below in createConfig method).
 * Now you will have to run at least 3 clients (e.g., 3 different phones) and you will realize
 * that:
 * 1) Client1 will be assigned PoolComponent1
 * 2) Client2 will be assigned PoolComponent2
 * 3) Client3 will be assigned PoolComponent1 (this time, PoolComponent1 is shared by clients 1 and 3)
 */
public class Ex14_PoolComponent extends Main {

    public static void main(String args[]) throws Throwable{
        new Ex14_PoolComponent().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx14.class)
                        .addPlugin(PoolExComponent.class, SaraCons.ID_POOL)
                        .build()
        };
    }

    @Override
    protected Config createConfig() {
        return new Config.Builder()
                .setSessionManagerPort(5555)
                // let's use a small pool to see the results easily
                .setDefaultNumOfPoolInstances(2)
                .setPathLogs(Utils.getProperty("pathLogs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress("127.0.0.1")
                .build();
    }
}
