package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.AsyncComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx06;

/**
 * Created by oscarr on 4/10/17.
 *
 * If your components are asynchronous by nature (that is, they run on their own separate
 * threads) or you run them in parallel (i.e., you use executeAsync) you can force them
 * to sync by calling forceSync method. Let's assume you have a list of 10 async components,
 * however, this is just for illustration purposes since you MUST register your components
 * in advance when you start the MultiuserFramework.
 *
 * Take a look at the implementation of AsyncComponent and you will realize the presence
 * of two key elements:
 *
 * 1) You have to use the @ForceSync annotation and add an arbitrary unique id to it (e.g.,
 * id = "sync-example"), this id will be necessary to sync your components.
 *
 * 2) You have to call the notifyNext method in the specific point you want to sync your
 * async components.
 */
public class Ex06_ForceSync extends Main {

    /**
     * This method controls the whole app. If shutdown is entered, it will completely
     * stop the system.
     */
    public static void main(String args[]) throws Throwable {
        new Ex06_ForceSync().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        // let's create all necessary components for SARA:
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx06.class)
                        .addPlugin(AsyncComponent.class, SaraCons.ID_ASYNC)
                        .build()
        };
    }
}
