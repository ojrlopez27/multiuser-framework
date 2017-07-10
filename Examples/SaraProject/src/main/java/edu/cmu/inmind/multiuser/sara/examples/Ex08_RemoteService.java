package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.RemoteNLUComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx08;

/**
 * Created by oscarr on 4/10/17.
 *
 * You can create local components that communicate with remote services. This example
 * illustrates how SARA can communicate with a external (remote or local) NLU system.
 * The main idea behind this example is to demonstrate how you can intercept messages
 * (in this case MSG_ASR messages), process them, and finally forward them to the
 * remote service.
 *
 * Take a lok at RemoteNLUComponent to see the different ways you can communicate with
 * your remote service:
 *
 * 1) explicitly by calling sendAndReceive method anywhere in your PluggableComponent code
 * 2) implicitly in the onEvent method.
 *
 * It is also important to notice that the service provider (remote NLU) must be running
 * and be registered before clients (phones) connect to the framework, take a look at
 * the DialogueSystem project that is included in the examples folder. So, run the projects
 * in this order:
 *
 * 1) Run SaraProject
 * 2) Run DialogueSystem: look at Main and uncomment the corresponding lines for this
 * example
 * 3) Run Android Client
 */
public class Ex08_RemoteService extends Main {

    public static void main(String args[]) throws Throwable{
        new Ex08_RemoteService().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx08.class)
                        .addPlugin(RemoteNLUComponent.class, SaraCons.ID_NLU)
                        .build()
        };
    }
}
