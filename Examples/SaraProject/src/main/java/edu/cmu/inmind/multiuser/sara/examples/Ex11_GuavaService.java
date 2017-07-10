package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.GuavaServiceComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx11;

/**
 * Created by oscarr on 4/10/17.
 *
 * Every PluggableComponent you create (NLGComponent, NLUComponent, etc.) behaves as a Guava Service:
 * @https://github.com/google/guava/wiki/ServiceExplained
 * The lifecyle of a Guava Service has the following states:
 * NEW -> STARTING -> RUNNING -> STOPPING -> TERMINATED and FAILED (anytime)
 * If you need to wait for service transitions to complete then you can implement several methods as
 * explained in this example. Also, your guava service may behave async or sync.
 */
public class Ex11_GuavaService extends Main {

    public static void main(String args[]) throws Throwable{
        new Ex11_GuavaService().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx11.class)
                        .addPlugin(GuavaServiceComponent.class, SaraCons.ID_GUAVA_SERVICE)
                        .build()
        };
    }
}
