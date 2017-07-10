package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx03;

/**
 * Created by oscarr on 4/10/17.
 *
 * This framework provides two approaches to process the messages that come from clients:
 *
 * 1) Event-oriented approach: every time that te blackboard is modified (e.g., insertion
 * and deletion of elements) then all the Blackboard subscribers (those components that
 * implements BlackboardListener interface, e.g., PluggableComponent components) are
 * automatically updated through the onEvent method that receives as parameter a
 * BlackboardEvent instance.
 *
 * 2) Direct-invocation approach: in this case, you are responsible of calling each component
 * in the desired order (sync or async) by calling the execute() method of your
 * ProcessOrchestrator implementation.
 *
 * If you are using the second approach and want to only activate the component that
 * corresponds to a specific message (this message must correspond to any of the keys that
 * you mapped to your PluginComponents -- see SaraCons.ID_NLU below) then call processMsg
 * message as the example below. This message should start with 'MSG_' prefix):
 */
public class Ex03_OneComponentActivation extends Main {

    /**
     * This method controls the whole app. If shutdown is entered, it will completely stop the system.
     */
    public static void main(String args[]) throws Throwable  {
        new Ex03_OneComponentActivation().execute();
    }

    @Override
    protected PluginModule[] createModules() {
        // let's create all necessary components for SARA:
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx03.class)
                        // you can either load classes from the config.properties file:
                        .addPlugin(Utils.getProperty("NLUComponent"), SaraCons.ID_NLU)
                        .build()
        };
    }
}
