package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.muf.MUFLifetimeManager;
import edu.cmu.inmind.multiuser.controller.muf.MultiuserController;
import edu.cmu.inmind.multiuser.controller.muf.ShutdownHook;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 5/16/18.
 */
public class SimpleServer {
    public static void main(String args[]){
        try {
            MultiuserController multiuserController = MUFLifetimeManager.startFramework(
                    createComponents(),
                    createConfig());
            multiuserController.addShutDownHook(new ShutdownHook() {
                @Override
                public void execute() {
                    MUFLifetimeManager.stopFramework(multiuserController);
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static Config createConfig() {
        String ipaddress="";
        try {
            ipaddress = InetAddress.getLocalHost().getHostAddress();
            System.out.println(ipaddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if(ipaddress.isEmpty())
        {
            ipaddress = "127.0.0.1";
        }
        return new Config.Builder()
                .setExceptionTraceLevel( Constants.SHOW_ALL_EXCEPTIONS)
                .setSessionManagerPort(5555)
                .setDefaultNumOfPoolInstances(10)
                .setPathLogs(Utils.getProperty("pathLogs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress("tcp://"+ipaddress) //use IP instead of 'localhost'
                .build();
    }

    public static PluginModule[] createComponents() {
        return new PluginModule[]{
                new PluginModule.Builder(TestOrchestrator.class,
                        TestPluggableComponent.class, "TestPluggableComponent")
                        .build()
        };
    }
}
