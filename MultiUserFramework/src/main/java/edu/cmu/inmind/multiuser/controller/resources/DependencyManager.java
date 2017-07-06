package edu.cmu.inmind.multiuser.controller.resources;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
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
        return injector.getInstance( clazz );
    }

    public ProcessOrchestrator getOrchestrator( ){
        return  orchestratorFactory.create( );
    }
}
