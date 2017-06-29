package edu.cmu.inmind.multiuser.controller;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorImpl;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.session.SessionManager;

/**
 * Created by oscarr on 3/20/17.
 * Use this class to control the lifecycle of the MUF framework.
 */
public class MultiuserFramework{
    private String id;
    private SessionManager sessionManager;
    private boolean stopping;
    private ProcessOrchestrator orchestrator;
    private Config config;

    MultiuserFramework(PluginModule[] modules, Config config, String id) {
        this.id = id;
        this.config = config;
        this.orchestrator = DependencyManager.getInstance( modules ).getOrchestrator();
        addShutDown();
    }

    MultiuserFramework(SessionManager sessionManager, String id){
        ClassLoader.getSystemClassLoader().setPackageAssertionStatus("zmq",false);
        this.sessionManager = sessionManager;
        this.id = id;
        addShutDown();
    }

    public String getId() {
        return id;
    }

    public ProcessOrchestrator getOrchestrator() {
        return orchestrator;
    }

    private void addShutDown() {
        Runtime.getRuntime().addShutdownHook(new Thread("FrameworkShutdownThread-" + id) {
            public void run() {
                try {
                    MultiuserFramework.this.stop();
                }catch (Throwable e){
                    ExceptionHandler.handle(e);
                }
            }
        });
    }

    void start( ){
        if( config.isTCPon() ) {
            sessionManager.start();
        }else{
            Log4J.info(this, "start orchestrator");
        }
    }

    void stop(){
        Log4J.info(this, "1...");
        if( !stopping ) {
            stopping = true;
            try {
                Log4J.info(this, "2...");
                sessionManager.stop();
                Log4J.info(this, "3...");
            } catch (Throwable e) {
                Log4J.info(this, "4...");
                ExceptionHandler.handle(e);
            }
            Log4J.info(this, "5...");
        }
        Log4J.info(this, "6...");
    }
}
