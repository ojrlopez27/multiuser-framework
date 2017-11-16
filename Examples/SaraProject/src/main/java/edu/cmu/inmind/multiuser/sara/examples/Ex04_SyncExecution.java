package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.NLGComponent;
import edu.cmu.inmind.multiuser.sara.component.SocialReasonerComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx04;

/**
 * Created by oscarr on 4/10/17.
 *
 * You can execute each component in your system (e.g., NLUComponent, TR, SR, NLGComponent)
 * synchronously. This is an example of how to execute your components sequentially and it
 * assumes all components run synchronously (i.e., they do NOT run on separate threads):
 */
public class Ex04_SyncExecution extends Main {

    /**
     * This method controls the whole app. If shutdown is entered, it will completely stop
     * the system.
     */
    public static void main(String args[]) throws Throwable {
        new Ex04_SyncExecution().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        // let's create all necessary components for SARA:
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx04.class)
                        // you can either load classes from the config.properties file:
                        .addPlugin(Utils.getProperty("NLUComponent"), SaraCons.ID_NLU)
                                // or you can directly specify the complete name of your component
                        .addPlugin("edu.cmu.inmind.multiuser.sara.component.TaskReasonerComponent", SaraCons.ID_TR)
                                // or you can directly refer to classes in your project
                        .addPlugin(SocialReasonerComponent.class, SaraCons.ID_SR)
                        .addPlugin(NLGComponent.class, SaraCons.ID_NLG)
                        .build()
        };
    }
}
