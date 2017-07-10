package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.TaskReasonerComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx09;

/**
 * Created by oscarr on 4/10/17.
 *
 * This example is an extension of Ex08_RemoteService. Basically, a remote service subscribes
 * to SARA server but this time you don't need to create an intermediary component
 * (RemoteNLUComponent) because the Blackboard will automatically update the remote service.
 * The only thing you need to do is to uncomment the corresponding code in Main at
 * DialogueSystem project.
 */
public class Ex09_RemoteServiceAutomaticSubscription extends Main {

    public static void main(String args[]) throws Throwable {
        new Ex09_RemoteServiceAutomaticSubscription().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx09.class)
                        .addPlugin(TaskReasonerComponent.class, SaraCons.ID_TR)
                        .build()
        };
    }
}
