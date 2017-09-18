package edu.cmu.inmind.multiuser.controller.plugin;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;

/**
 * Created by oscarr on 4/4/17.
 * This component communicate with external services automatically, that is, external service subscribe to a list
 * of messages and this component sends and receives changes on Blackboard.
 */
@BlackboardSubscription( messages = {} )
@StateType( state = Constants.STATEFULL )
public class ExternalComponent extends PluggableComponent implements ResponseListener{

    public ExternalComponent(String serviceURL, String clientAddress, String sessionId, ZMsgWrapper zMsgWrapper,
                             String[] messages){
        try {
            Utils.addOrChangeAnnotation(getClass().getAnnotation(BlackboardSubscription.class), "messages", messages);
            Flowable.just(this).subscribe(externalComponent -> {
                setClientCommController( new ClientCommController.Builder()
                        .setServerAddress(serviceURL)
                        .setServiceName(sessionId)
                        .setClientAddress( clientAddress )
                        .setMsgTemplate( zMsgWrapper )
                        .setSubscriptionMessages( messages )
                        .setRequestType( Constants.REQUEST_CONNECT )
                        .setResponseListener(this)
                        .build() );
            });
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    @Override
    public void execute() {
    }

    @Override
    public void onEvent(BlackboardEvent event) {
        try {
            String sessionId = getSessionId();
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setSessionId( sessionId );
            sessionMessage.setRequestType(event.getStatus());
            sessionMessage.setMessageId(event.getId());
            sessionMessage.setPayload( Utils.toJson(event.getElement() ));
            getClientCommController().setShouldProcessReply( true );
            getClientCommController().send( sessionId, sessionMessage );
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    @Override
    public void startUp(){
        super.startUp();
    }

    @Override
    public void shutDown() {
        sendCloseMessage();
        super.shutDown();
    }

    /**
     * We need to send this message to the Python Dialogue System on a separate thread, otherwise
     * we will get some TimeOut exceptions because the communication process takes longer than the
     * shuttingdown process
     */
    private void sendCloseMessage(){
        new Thread("send-message-close-python-dialogue") {
            public void run(){
                SessionMessage sessionMessage = new SessionMessage();
                sessionMessage.setRequestType( Constants.REQUEST_DISCONNECT );
                sessionMessage.setSessionId(getSessionId());
                getClientCommController().send( getSessionId(), sessionMessage );
                Utils.sleep(1000);
            }
        }.start();
    }

    @Override
    public void process(String message) {
        SessionMessage sessionMessage = Utils.fromJson( message, SessionMessage.class );
        if( sessionMessage.getMessageId() == null || sessionMessage.getMessageId().isEmpty() ){
            String msg = "This message from Python (or any other external server) has an empty or null id. Make " +
                    "sure you send a message with a proper id, otherwise it won't be delivered through the Blackboard. " +
                    "Message: " + sessionMessage.getPayload();
            Log4J.error(this, msg );
            System.err.println(msg);
        }
        blackboard().post( this, sessionMessage.getMessageId(), sessionMessage.getPayload() );
    }
}
