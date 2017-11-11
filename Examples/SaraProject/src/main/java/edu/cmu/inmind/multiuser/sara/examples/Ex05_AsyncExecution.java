package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.NLGComponent;
import edu.cmu.inmind.multiuser.sara.component.SocialReasonerComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx05;

/**
 * Created by oscarr on 4/10/17.
 *
 * You can also execute a set of components in parallel and asynchronously (that is, in
 * separate threads):
 */
public class Ex05_AsyncExecution extends Main {

    /**
     * This method controls the whole app. If shutdown is entered, it will completely stop
     * the system.
     */
    public static void main(String args[]) throws Throwable {
        new Ex05_AsyncExecution().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        // let's create all necessary components for SARA:
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx05.class)
                        .addPlugin(Utils.getProperty("NLUComponent"), SaraCons.ID_NLU)
                        .addPlugin("edu.cmu.inmind.multiuser.sara.component.TaskReasonerComponent",
                                SaraCons.ID_TR)
                        .addPlugin(SocialReasonerComponent.class, SaraCons.ID_SR)
                        .addPlugin(NLGComponent.class, SaraCons.ID_NLG)
                        .build()
        };
    }
}
