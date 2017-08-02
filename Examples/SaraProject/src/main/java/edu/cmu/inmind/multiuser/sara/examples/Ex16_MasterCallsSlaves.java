package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.ShutdownHook;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.sara.component.*;
import edu.cmu.inmind.multiuser.sara.orchestrator.SaraOrchestratorEx15;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oscarr on 8/02/17.
 *
 * This example illustrates the whole pipeline:
 * AndroidClient (ASR) -> DialogueSystem (NLU) -> TaskReasoner -> SocialReasoner -> NLG -> AndroidClient
 * This example is pretty similar to EX15_Wholepipeline, however, unlike Ex15_WholePipeline, in this example
 * the master MUF (Sara MUF) calls its slaves MUF's (i.e., Dialogue MUF, etc.). In order to run this example,
 * you have to define the connection information of your slaves MUF's into the file services.json (i.e.,
 * replace 'services[_remove_this].json' by 'services.json'.
 */
public class Ex16_MasterCallsSlaves extends Main {

    public static void main(String args[]) throws Throwable{
        // MUF will look for the file 'services.json' so we need to rename the file
        Utils.renameFile( "services[remove_this].json", "services.json");
        List<ShutdownHook> hooks = new ArrayList<>();
        // we add a hook that will reset the state of the system, that is, it will rename the json file to
        // its original name
        hooks.add( new ShutdownHook() {
            @Override
            public void execute() {
                Utils.renameFile("services.json", "services[remove_this].json");
            }
        });
        new Ex16_MasterCallsSlaves().execute( hooks );
    }

    @Override
    protected PluginModule[] createModules() {
        return new PluginModule[]{
                new PluginModule.Builder(SaraOrchestratorEx15.class,
                        //comment out the line below if you want to use remote DialogueSystem
                        NLUComponent.class, SaraCons.ID_NLU)
                        .addPlugin(TaskReasonerComponent.class, SaraCons.ID_TR)
                        .addPlugin(SocialReasonerComponent.class, SaraCons.ID_SR)
                        .addPlugin(NLGComponent.class, SaraCons.ID_NLG)
                        .addPlugin(UserModelComponent.class, SaraCons.ID_UM)
                        .build()
        };
    }
}
