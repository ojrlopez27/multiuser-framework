package edu.cmu.inmind.multiuser.common;

/**
 * Created by oscarr on 10/5/17.
 *
 * We use this class to gracefully shutdown components in cascade. When the lowest objects in the
 * three hierarchy are closed, then callback messages are triggered to close higher objects in the three
 * (e.g. ClientCommController -> PluggableComponent -> Orchestrator -> Session)
 * Take a look at: http://zguide.zeromq.org/page:all#Making-a-Clean-Exit
 */
public interface DestroyableCallback {
    void destroyInCascade(Object destroyedObj) throws Throwable;
}
