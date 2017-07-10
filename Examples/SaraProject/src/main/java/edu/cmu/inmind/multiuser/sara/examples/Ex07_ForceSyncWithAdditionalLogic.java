package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.AsyncComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx07;

/**
 * Created by oscarr on 4/10/17.
 *
 * This is a variation of Ex06_ForceSync. If you want (besides to sync your async components)
 * to add any logic after synchronizing each of your async components and before calling the next
 * one (i.e., at the notifyNext method) you have to pass a list of SynchronizableEvent objects
 * (and add your logic inside these events) along with your async components.
 */
public class Ex07_ForceSyncWithAdditionalLogic extends Main {

    /**
     * This method controls the whole app. If shutdown is entered, it will completely stop the system.
     */
    public static void main(String args[]) throws Throwable{
        new Ex07_ForceSyncWithAdditionalLogic().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        // let's create all necessary components for SARA:
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx07.class)
                        .addPlugin(AsyncComponent.class, SaraCons.ID_ASYNC)
                        .build()
        };
    }
}
