package edu.cmu.inmind.multiuser.controller.session;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.ServerCommController;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.orchestrator.OrchestratorListener;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;
import edu.cmu.inmind.multiuser.controller.resources.Config;
import edu.cmu.inmind.multiuser.controller.resources.DependencyManager;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by oscarr on 3/3/17.
 */
public class Session implements Runnable, OrchestratorListener{
    private String id;
    private String status;
    private ProcessOrchestrator orchestrator;
    private Thread thread;
    private boolean isClosed;
    private InactivityTimer timer;
    private ServerCommController sessionCommController;
    private ZMsgWrapper replyMsg = new ZMsgWrapper();

    public Session() {
        this.thread = new Thread( this );
        this.timer = new InactivityTimer();
    }

    public String getId() {
        return id;
    }

    public void setId(String id, ZMsgWrapper msg) {
        if( this.id == null ){
            Log4J.info(this, "A new session has been created with id: " + id);
            this.sessionCommController = new ServerCommController( Constants.FULL_ADDRESS, id, msg);
            this.thread.start();
        }
        this.id = id;
    }

    public List<PluggableComponent> getComponents(){
        return orchestrator.getComponents();
    }

    public void pause(){
        status = Constants.SESSION_PAUSED;
        orchestrator.pause();
        Log4J.info(this, String.format("Session with id: %s has been paused.", id));
        //TODO: add some logic when session is paused
    }

    public void resume(){
        status = Constants.SESSION_RESUMED;
        orchestrator.resume();
        Log4J.info(this, String.format("Session with id: %s has been resumed.", id));
        //TODO: add some logic when session is resumed
    }

    public void close() throws Exception{
        if( !isClosed ) {
            Log4J.info(this, String.format("Closing session: %s", id));
            isClosed = true;
            status = Constants.SESSION_CLOSED;
            orchestrator.close();
            sessionCommController.send(new SessionMessage(Constants.SESSION_CLOSED));
            sessionCommController.close();
            sessionCommController = null;
            orchestrator = null;
            System.gc();
            thread.interrupt();
            thread = null;
            timer.cancel();
            timer.purge();
            Log4J.info(this, String.format("Session: %s has been disconnected!", id));
        }
    }

    private void initialize() throws Exception{
        Log4J.info(this, String.format("Initializing session: %s.", id));
        orchestrator = DependencyManager.getInstance().getOrchestrator();
        orchestrator.initialize(this);
        orchestrator.start();
        orchestrator.subscribe(this);
        status = Constants.SESSION_INITIATED;
    }

    /**
     * While session is active, this method will be processing incoming information from client
     */
    @Override
    public void run(){
        try {
            initialize();
            while (!status.equals(Constants.SESSION_CLOSED) && !thread.isInterrupted()) {
                ZMsgWrapper request = sessionCommController.receive( replyMsg.getMsg() );
                if (request == null)
                    break; //Interrupted
                replyMsg = request; //  Echo is complex :-)
                String message = replyMsg.getMsg().peekLast().toString();
                stopTimer();
                if (message.contains(Constants.REQUEST_DISCONNECT)) {
                    status = Constants.SESSION_CLOSED;
                } else {
                    orchestrator.process(message);
                }
            }
            close();
        }catch (Exception e){
            ExceptionHandler.handle(e);
        }
    }

    @Override
    public void processOutput(SessionMessage output) {
        sessionCommController.send(output);
        timer.schedule(new InactivityCheck(), Config.getSessionTimeout());
    }

    private void stopTimer(){
        timer.stopTimer();
    }

    class InactivityCheck extends TimerTask{
        @Override
        public void run() {
            try {
                close();
            }catch (Exception e){
                ExceptionHandler.handle( e );
            }
        }
    }

    class InactivityTimer extends Timer{
        private TimerTask inactivityCheck;

        @Override
        public void schedule(TimerTask task, long delay) {
            inactivityCheck = task;
            super.schedule(task, delay);
        }

        public void stopTimer(){
            if( inactivityCheck != null ) {
                inactivityCheck.cancel();
            }
        }
    }
}
