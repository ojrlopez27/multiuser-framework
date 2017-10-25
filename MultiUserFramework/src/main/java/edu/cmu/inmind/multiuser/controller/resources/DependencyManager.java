package edu.cmu.inmind.multiuser.controller.resources;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorFactory;
import org.zeromq.ZContext;


/**
 * Created by oscarr on 3/7/17.
 */
public class DependencyManager {
    private AbstractModule[] modules;
    private Injector injector;
    @Inject ProcessOrchestratorFactory orchestratorFactory;
    private static DependencyManager instance;
    /**
     * ZMQ docs: You should create and use exactly one context in your process. Technically, the context is the
     * container for all sockets in a single process, and acts as the transport for inproc sockets, which are the
     * fastest way to connect threads in one process. If at runtime a process has two contexts, these are like separate
     * ZeroMQ instances
     */
    private ZContext context;

    public static DependencyManager getInstance(AbstractModule[] modules){
        if( instance == null ){
            initialize( modules );
        }
        return instance;
    }

    private static void initialize( AbstractModule[] modules ){
        if( modules == null || modules.length <= 0){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "modules: "
                    + modules) );
        }
        Injector injector = Guice.createInjector(modules);
        instance = injector.getInstance( DependencyManager.class );
        instance.injector = injector;
        instance.modules = modules;
        instance.context = new ZContext();
    }

    public static DependencyManager getInstance(){
        return instance;
    }

    public <T> T getComponent(Class<T> clazz){
        try{
            return  injector.getInstance( clazz );
        }catch (Throwable e){
            ExceptionHandler.handle( e);
        }
        return null;
    }

    public ProcessOrchestrator getOrchestrator( ){
        return  orchestratorFactory.create( );
    }

    public ZContext getContext() {
        return context;
    }

    public void release(){
        if( instance != null ) {
            instance.injector = null;
            instance.modules = null;
            instance.orchestratorFactory = null;
            if(context != null){
                context.destroy();
            }
            instance = null;
        }
    }
}
