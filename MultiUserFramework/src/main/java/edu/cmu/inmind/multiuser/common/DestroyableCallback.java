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
    /**
     * Use this method to send requests forward to close components that belong to the
     * current DestroyableCallback instance
     * @param callback this is the instance that should be destroyed when sending
     *                     requests backward (destroyInCascade method)
     * @throws Throwable
     */
    void close(DestroyableCallback callback) throws Throwable;

    /**
     * Use this method to send requests backward to releae components that belong to the
     * current DestroyableCallback instance and the instance itself. Also, at the end of this
     * method, you should call destroyInCascade method of the callback object (the one passed
     * on close method) to release in cascade.
     * @param destroyedObj this is the instance that has been already destroyed when sending
     *                     requests forward (close method)
     * @throws Throwable
     */
    void destroyInCascade(DestroyableCallback destroyedObj) throws Throwable;
}
