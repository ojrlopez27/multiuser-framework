package edu.cmu.inmind.multiuser.controller;

import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.session.SessionManager;

/**
 * Created by oscarr on 3/20/17.
 * Use this class to control the lifecycle of the MUF framework.
 */
public class MultiuserFramework{
    private String id;
    private SessionManager sessionManager;
    private boolean stopping;

    public String getId() {
        return id;
    }

    MultiuserFramework(SessionManager sessionManager, String id){
        ClassLoader.getSystemClassLoader().setPackageAssertionStatus("zmq",false);
        this.sessionManager = sessionManager;
        this.id = id;
        addShutDown();
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

    void start( ){
        sessionManager.start();
    }

    void stop(){
        Log4J.info(this, "1...");
        if( !stopping ) {
            stopping = true;
            try {
                Log4J.info(this, "2...");
                sessionManager.stop();
                Log4J.info(this, "3...");
            } catch (Throwable e) {
                Log4J.info(this, "4...");
                ExceptionHandler.handle(e);
            }
            Log4J.info(this, "5...");
        }
        Log4J.info(this, "6...");
    }
}
