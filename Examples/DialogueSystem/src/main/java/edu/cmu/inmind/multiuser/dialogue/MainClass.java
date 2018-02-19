package edu.cmu.inmind.multiuser.dialogue;

import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ServiceInfo;
import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.muf.MUFLifetimeManager;
import edu.cmu.inmind.multiuser.controller.muf.MultiuserController;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.dialogue.components.NLUComponent;
import edu.cmu.inmind.multiuser.dialogue.orchestrator.DialogueOrchestrator;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 3/31/17.
 */
public class MainClass {
    private static String saraServerAddress = Utils.getProperty("saraServerAddress");
    private static String dialogueAddress = Utils.getProperty("dialogueAddress");
    private static MultiuserController muf;
    public static boolean isMasterMUFCallingMe = true;
    public static boolean usePythonDialogue = false;

    /**
     * This method controls the whole app. If shutdown is entered, it will completely stop the system.
     */
    public static void main(String args[]) throws Throwable{
        muf = MUFLifetimeManager.startFramework(createModules(), createConfig(),
                isMasterMUFCallingMe? null : createServiceInfo() );

        // just in case you force the system to close or an unexpected error happen.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                MUFLifetimeManager.stopFramework(muf);

            }
        });

        String command = "";
        while( !command.equals("shutdown") ){
            System.err.println("Type \"shutdown\" to stop:");
            Scanner scanner = new Scanner(System.in);
            command = scanner.nextLine();
            if( command.equals( SaraCons.SHUTDOWN ) ){
                MUFLifetimeManager.stopFramework(muf);
            }
        }
    }

    private static PluginModule[] createModules() {
        // let's create all necessary components for Dialogue:
        return new PluginModule[]{
                new PluginModule.Builder( DialogueOrchestrator.class )
                        .addPlugin(NLUComponent.class, SaraCons.MSG_ASR)
                        .build()
        };
    }

    private static Config createConfig(){
        return new Config.Builder()
                .setSessionManagerPort(Integer.valueOf(Utils.getProperty("SessionManagerPort")))
                .setDefaultNumOfPoolInstances(Integer.valueOf(Utils.getProperty("NumOfInstancesPool")))
                .setPathLogs(Utils.getProperty("pathLogs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .build();
    }

    /**
     * We need to register this remote service (NLU) through SARA server
     */
    private static ServiceInfo createServiceInfo() {
        // TODO: if you register this way (i.e., no msgSubscription array), then you will need to define a local component
        // TODO: at SaraProject that defines a ConnectRemoteService annotation and forwards messages to the DialogueSystem.
        // TODO: uncomment the lines below for Example08_RemoteService.
//        return new ServiceInfo.Builder()
//                .setServerAddress(saraServerAddress)
//                .setServiceName(SaraCons.NLU_SERVICE)
//                .setClientAddress(dialogueAddress)
//                .setRequestType(Constants.REGISTER_REMOTE_SERVICE)
//                .setMsgWrapper( new ZMsgWrapper() )
//                .setResponseListener(message -> {
//                    System.out.println("This is a message from SARA server: " + message);
//                })
//                .build();


        // TODO: Or you can subscribe directly with SaraProject (no local component at SaraProject is needed) to all the
        // TODO: messages you want to be updated to. Uncomment the lines below for
        // TODO: Example09_RemoteServiceAutomaticSubscription and Ex15_WholePipeline. Note that this time we are
        // TODO: including a list of subscriptions setMsgSubscriptions(new String[]{SaraCons.MSG_ASR}
        return new ServiceInfo.Builder()
                .setMasterMUFAddress( saraServerAddress)
                .setServiceName(SaraCons.NLU_SERVICE)
                .setSlaveMUFAddress(dialogueAddress)
                .setRequestType(Constants.REGISTER_REMOTE_SERVICE)
                .setMsgWrapper( new ZMsgWrapper() )
                .setMsgSubscriptions(new String[]{SaraCons.MSG_ASR})
                // use this listener to process control messages from SARA such as shutdown the system, etc.
                .setResponseListener(message -> {
                    Log4J.info(MainClass.class, "This is the response from SARA server: " + message);
                })
                .build();
    }

}
