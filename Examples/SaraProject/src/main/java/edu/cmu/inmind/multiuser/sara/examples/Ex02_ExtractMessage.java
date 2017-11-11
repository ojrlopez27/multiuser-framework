package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx02;

/**
 * Created by oscarr on 4/10/17.
 *
 * Description: this is a simple scenario that illustrates:
 * 1) how to use your own implementation of a Message Logger,
 * 2) how to extract messages coming from the client
 * 3) how to respond to the client
 */
public class Ex02_ExtractMessage extends Main {

    /**
     * This method controls the whole app. If shutdown is entered, it will completely
     * stop the system.
     */
    public static void main(String args[]) throws Throwable{
        new Ex02_ExtractMessage().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        // let's create all necessary components for SARA:
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx02.class)
                        // you can either load classes from the config.properties file:
                        .addPlugin(Utils.getProperty("NLUComponent"), SaraCons.ID_NLU)
                        .build()
        };
    }
}
