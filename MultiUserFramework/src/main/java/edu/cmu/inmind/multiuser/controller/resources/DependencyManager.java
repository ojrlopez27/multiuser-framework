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


/**
 * Created by oscarr on 3/7/17.
 */
public class DependencyManager {
    private AbstractModule[] modules;
    private Injector injector;
    @Inject ProcessOrchestratorFactory orchestratorFactory;
    private static DependencyManager instance;

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
    }

    public static void reset(){
        if( instance != null ) {
            instance.injector = null;
            instance.modules = null;
            instance.orchestratorFactory = null;
            instance = null;
        }
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
}
