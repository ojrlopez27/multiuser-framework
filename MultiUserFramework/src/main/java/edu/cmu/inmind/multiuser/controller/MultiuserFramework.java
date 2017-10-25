package edu.cmu.inmind.multiuser.controller;


import edu.cmu.inmind.multiuser.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ServiceInfo;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.session.Session;
import edu.cmu.inmind.multiuser.controller.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 3/20/17.
 * Use this class to control the lifecycle of the MUF framework.
 */
public class MultiuserFramework implements DestroyableCallback {
    private String id;
    private SessionManager sessionManager;
    private AtomicBoolean stopping = new AtomicBoolean(false);
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
        }else if( config.getExceptionLogger() != null ){
            ExceptionHandler.setLog( config.getExceptionLogger() );
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
        if( client == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "client: " + client) );
        }
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
        try {
            if (config.isTCPon()) {
                sessionManager.start();
            } else {
                Log4J.info(this, "start orchestrator");
            }
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    void stop(){
        try {
            if (!stopping.getAndSet(true)) {
                if (hooks != null) {
                    for (ShutdownHook hook : hooks) {
                        hook.execute();
                    }
                }

                if (config.isTCPon()) {
                    sessionManager.stop();
                } else {
                    session.close( this );
                }
            }
        }catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
    }

    @Override
    public void destroyInCascade(Object destroyedObj) throws Throwable {
        //TODO some logic to release resources
        sessionManager = null;
        session = null;
        config = null;
        dependencyManager = null;
        client = null;
        hooks = null;
        Log4J.info(this, "Gracefully destroying...");
    }
}
