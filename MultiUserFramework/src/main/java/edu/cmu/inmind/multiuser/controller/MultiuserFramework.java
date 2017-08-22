package edu.cmu.inmind.multiuser.controller;


import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ServiceInfo;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.controller.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oscarr on 3/20/17.
 * Use this class to control the lifecycle of the MUF framework.
 */
public class MultiuserFramework{
    private String id;
    private SessionManager sessionManager;
    private boolean stopping;
    private Session session;
    private Config config;
    private DependencyManager dependencyManager;
    private ClientCommController client;
    private List<ShutdownHook> hooks;

    MultiuserFramework(String id, PluginModule[] modules, Config config, ServiceInfo serviceInfo) throws Throwable{
        ClassLoader.getSystemClassLoader().setPackageAssertionStatus("zmq",false);
        this.id = id;
        this.config = config;
        addShutDown();
        if( config.getPathExceptionLogger() != null ){
            ExceptionHandler.setLog( config.getPathExceptionLogger() );
        }
        if( config.isTCPon() ){
            this.sessionManager = new SessionManager(modules, config, serviceInfo);
        }else{
            dependencyManager = DependencyManager.getInstance( modules );
            session = dependencyManager.getComponent(Session.class);
            session.setConfig( config );
            session.setId( "session-with-tcp-off", null, null );
        }
    }

    public String getId() {
        return id;
    }

    public ProcessOrchestrator getOrchestrator() {
        return session.getOrchestrator();
    }

    public void setClient(ClientCommController client) {
        this.client = client;
        this.session.setClient( this.client );
    }

    public ClientCommController getClient() {
        return client;
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

    public MultiuserFramework addShutDownHook(ShutdownHook shutdownHook){
        if( hooks == null ){
            hooks = new ArrayList<>();
        }
        hooks.add( shutdownHook );
        return this;
    }

    void start( ){
        if( config.isTCPon() ) {
            sessionManager.start();
        }else{
            Log4J.info(this, "start orchestrator");
        }
    }

    void stop(){
        if( !stopping ) {
            if( hooks != null ){
                for( ShutdownHook hook : hooks ){
                    hook.execute();
                }
            }
            stopping = true;
            DependencyManager.reset();
            try {
                if( config.isTCPon() ) {
                    sessionManager.stop();
                }else {
                    session.close();
                }
            } catch (Throwable e) {
                ExceptionHandler.handle(e);
            }
        }
    }
}
