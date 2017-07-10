package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.HowToLogComponent;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx12;

/**
 * Created by oscarr on 4/10/17.
 *
 * This example illustrates how to log all the messages and events passed through the framework.
 */
public class Ex12_Loggers extends Main {

    public static void main(String args[]) throws Throwable{
        new Ex12_Loggers().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx12.class)
                        .addPlugin(HowToLogComponent.class, SaraCons.ID_LOG_COMPONENT)
                        .build()
        };
    }
}
