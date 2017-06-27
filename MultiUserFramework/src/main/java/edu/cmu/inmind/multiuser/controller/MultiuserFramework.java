package edu.cmu.inmind.multiuser.controller;

import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.session.SessionManager;

/**
 * Created by oscarr on 3/20/17.
 * Use this class to control the lifecycle of the MUF framework.
 */
public class MultiuserFramework{
    private String id;
    private SessionManager sessionManager;
    private boolean stopping;

    MultiuserFramework( SessionManager sessionManager, String id){
        this.sessionManager = sessionManager;
        this.id = id;
        addShutDown();
    }

    private void addShutDown() {
        Runtime.getRuntime().addShutdownHook(new Thread("FrameworkShutdownThread-" + id) {
            public void run() {
                try {
                    MultiuserFramework.this.stop();
                }catch (Exception e){
                    ExceptionHandler.handle(e);
                }
            }
        });
    }

    public void start( ){
        sessionManager.start();
    }

    public void stop(){
        if( !stopping ) {
            stopping = true;
            try {
                sessionManager.stop();
            } catch (Exception e) {
                ExceptionHandler.handle(e);
            }
        }
    }
}
