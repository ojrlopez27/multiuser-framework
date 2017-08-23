package edu.cmu.inmind.multiuser.sara.examples;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.SaraCons;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.MultiuserFramework;
import edu.cmu.inmind.multiuser.controller.MultiuserFrameworkContainer;
import edu.cmu.inmind.multiuser.controller.ShutdownHook;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.sara.log.ExceptionLogger;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 3/20/17.
 */
public class Main{
    protected MultiuserFramework muf;

    /**
     * This method controls the whole app. If shutdown is entered, it will completely stop the system.
     */
    public static void main(String args[]) throws Throwable {
        new Main().execute();
    }

    protected void execute() throws Throwable{
         execute( null );
    }

    protected void execute(List<ShutdownHook> hooks) throws Throwable{
        // starting the Multiuser framework
        muf = MultiuserFrameworkContainer.startFramework(
                createModules(), createConfig(), null );
        if( hooks != null ){
            for( ShutdownHook hook : hooks ){
                muf.addShutDownHook( hook );
            }
        }

        // just in case you force the system to close or an unexpected error happen.
        Runtime.getRuntime().addShutdownHook(new Thread("ShutdownThread") {
            public void run() {
                MultiuserFrameworkContainer.stopFramework( muf );
            }
        });

        // you can use a loop like this in order to gracefully shutdown the system.
        String command = "";
        while (!command.equals("shutdown")) {
            System.err.println("Type \"shutdown\" to stop:");
            Scanner scanner = new Scanner(System.in);
            command = scanner.nextLine();
            if (command.equals(SaraCons.SHUTDOWN)) {
                MultiuserFrameworkContainer.stopFramework( muf );
            }
        }
    }

    protected PluginModule[] createModules() {
        //TODO
        return null;
    }

    protected Config createConfig() {
        return new Config.Builder()
                // you can add values directly like this:
                .setSessionManagerPort(5555)
                .setDefaultNumOfPoolInstances(10)
                        // or you can refer to values in your config.properties file:
                .setPathLogs(Utils.getProperty("pathLogs"))
                .setSessionTimeout(5, TimeUnit.MINUTES)
                .setServerAddress("127.0.0.1") //use IP instead of 'localhost'
                .setExceptionTraceLevel( Constants.SHOW_MUF_EXCEPTIONS)
                // if using FileLogger, just specify the path to store the logs
                //.setPathExceptionLogger(Utils.getProperty("pathLogs"))
                // if using your own implementation of MessageLog:
                .setExceptionLogger( getExceptionLogger() )
                .build();
    }

    protected MessageLog getExceptionLogger(){
        MessageLog log =  new ExceptionLogger();
        log.setId( "any-arbitrary-id" );
        log.setPath( Utils.getProperty("pathExceptionLog") );
        return log;
    }

}
