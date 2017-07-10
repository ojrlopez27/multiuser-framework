package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx01;

/**
 * Created by oscarr on 4/10/17.
 *
 * You can programmatically control what to do with the message. For instance, you can
 * translate the input that comes from android client into a known object (e.g.,
 * SaraInput object).
 */
public class Ex01_MessageTranslation extends Main {

    /**
     * This method controls the whole app. If shutdown is entered, it will completely stop
     * the system.
     */
    public static void main(String args[]) throws Throwable {
        new Ex01_MessageTranslation().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        // let's create all necessary components for SARA:
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx01.class)
                        // you can either load classes from the config.properties file:
                        .addPlugin(Utils.getProperty("NLUComponent"), SaraCons.ID_NLU)
                        .build()
        };
    }
}
