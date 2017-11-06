package edu.cmu.inmind.multiuser.controller.muf;


import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.controller.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ServiceInfo;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.PluginModule;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;
import edu.cmu.inmind.multiuser.controller.session.SessionImpl;
import edu.cmu.inmind.multiuser.controller.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by oscarr on 3/20/17.
 * Use this class to control the lifecycle of the MUF framework.
 */
public class MultiuserController implements DestroyableCallback {
    private String id;
    private SessionManager sessionManager;
    private AtomicBoolean stopping = new AtomicBoolean(false);
    private SessionImpl session;
    private Config config;
    private DependencyManager dependencyManager;
    private ClientCommController client;
    private List<ShutdownHook> hooks;

    MultiuserController(String id, PluginModule[] modules, Config config, ServiceInfo serviceInfo) throws Throwable{
        ClassLoader.getSystemClassLoader().setPackageAssertionStatus("zmq",false);
        Utils.initThreadExecutor( config.getCorePoolSize() );
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
            session = dependencyManager.getComponent(SessionImpl.class);
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
                    MultiuserController.this.stop();
                }catch (Throwable e){
                    ExceptionHandler.handle(e);
                }
            }
        });
    }

    public MultiuserController addShutDownHook(ShutdownHook shutdownHook){
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
            close(null);
        }catch (Throwable e){
            e.printStackTrace();
        }
    }

    @Override
    public void close(DestroyableCallback callback) throws Throwable {
        try {
            if (!stopping.getAndSet(true)) {
                if (hooks != null) {
                    for (ShutdownHook hook : hooks) {
                        hook.execute();
                    }
                }

                if (config.isTCPon()) {
                    sessionManager.close(this);
                } else {
                    session.close( this );
                }
            }
        }catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
    }

    @Override
    public void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable {
        //TODO some logic to release resources
        sessionManager = null;
        session = null;
        config = null;
        dependencyManager = null;
        client = null;
        hooks = null;
        ResourceLocator.setIamDone( this );
        Log4J.info(this, "Gracefully destroying...");
    }
}
