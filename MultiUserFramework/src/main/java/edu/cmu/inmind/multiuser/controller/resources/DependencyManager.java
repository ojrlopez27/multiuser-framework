package edu.cmu.inmind.multiuser.controller.resources;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import edu.cmu.inmind.multiuser.common.DestroyableCallback;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestratorFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Created by oscarr on 3/7/17.
 */
public class DependencyManager {
    private Injector injector;
    private static DependencyManager instance;
    @Inject ProcessOrchestratorFactory orchestratorFactory;
    /**
     * ZMQ docs: You should create and use exactly one context in your process. Technically, the context is the
     * container for all sockets in a single process, and acts as the transport for inproc sockets, which are the
     * fastest way to connect threads in one process. If at runtime a process has two contexts, these are like separate
     * ZeroMQ instances
     */
    private static ZContext context;
    private static ConcurrentHashMap<DestroyableCallback, Boolean> destroyables = new ConcurrentHashMap<>();
    private static CopyOnWriteArrayList<ZMQ.Socket> sockets = new CopyOnWriteArrayList<>();
    private static CopyOnWriteArrayList<ZContext> contexts = new CopyOnWriteArrayList<>();

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

    public static ZContext getContext(DestroyableCallback contextOwner) {
        if( context == null ){
            context = new ZContext();
        }
        // contexts keeps a record about which owner has released its context.
        // at initialization, nobody has released it.
        destroyables.put(contextOwner, false);
        ZContext ctx = ZContext.shadow(context);
        contexts.add(ctx);
        return ctx;
    }

    public static void setIamDone(DestroyableCallback contextOwner){
        if( destroyables.get(contextOwner) != null )
            destroyables.put(contextOwner, true);
    }

    public static ZMQ.Socket createSocket(ZContext ctx, int type){
        ZMQ.Socket socket = ctx.createSocket(type);
        sockets.add(socket);
        return socket;
    }

    public void release() throws Throwable{
        if( instance != null ) {
            instance.injector = null;
            instance.orchestratorFactory = null;
            if(context != null && destroyables != null){
                boolean allTerminated;
                do{
                    allTerminated = true;
                    for(DestroyableCallback key : destroyables.keySet() ){
                        if( !destroyables.get(key) ){
                            key.destroyInCascade(null);
                            allTerminated = false;
                            Utils.sleep(50);
                            break;
                        }
                    }
                }while( !allTerminated );

                try {
                    for (ZContext ctx : contexts) {
                        ctx.destroy();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                context.destroy();
            }
            instance = null;
        }
    }
}
