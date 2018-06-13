package edu.cmu.inmind.multiuser.controller.resources;

import edu.cmu.inmind.multiuser.controller.communication.DestroyableCallback;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

//added to remove dependency from core, instead ResourceLocator in commons uses this ZMQ
public class CommonsResourceLocator {


    /** ======================== ZeroMQ Contexts =================================== **/

    /**
     * ZMQ docs: You should create and use exactly one context in your process. Technically, the context is the
     * container for all sockets in a single process, and acts as the transport for inproc sockets, which are the
     * fastest way to connect threads in one process. If at runtime a process has two contexts, these are like separate
     * ZeroMQ instances
     */
    public static ZContext context;
    public static ConcurrentHashMap<DestroyableCallback, Boolean> destroyables = new ConcurrentHashMap<>();
    public static CopyOnWriteArrayList<ZMQ.Socket> sockets = new CopyOnWriteArrayList<>();
    public static CopyOnWriteArrayList<ZContext> contexts = new CopyOnWriteArrayList<>();


    public static ZContext getContext(DestroyableCallback contextOwner) {
        if( context == null ){
            context = new ZContext();
        }
        // contexts keeps a record about which owner has released its context.
        // at initialization, nobody has released it.
        if(contextOwner!=null )
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
}
