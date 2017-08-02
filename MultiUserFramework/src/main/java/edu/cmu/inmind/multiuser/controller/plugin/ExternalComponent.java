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
            setClientCommController( new ClientCommController.Builder()
                    .setServerAddress(serviceURL)
                    .setServiceName(sessionId)
                    .setClientAddress( clientAddress )
                    .setMsgTemplate( zMsgWrapper )
                    .setSubscriptionMessages( messages )
                    .setRequestType( Constants.REQUEST_CONNECT )
                    .build() );
            getClientCommController().receive(this);
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
        SessionMessage sessionMessage = new SessionMessage();
        sessionMessage.setSessionId( getSessionId() );
        sessionMessage.setRequestType( Constants.REQUEST_DISCONNECT );
        getClientCommController().send( getSessionId(), sessionMessage );
        super.shutDown();
    }

    @Override
    public void close( String sessionId ) throws Throwable{
        super.close( getSessionId() );
    }

    @Override
    public void process(String message) {
        SessionMessage sessionMessage = Utils.fromJson( message, SessionMessage.class );
        blackboard().post( this, sessionMessage.getMessageId(), sessionMessage.getPayload() );
    }
}
