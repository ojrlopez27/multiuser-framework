package edu.cmu.inmind.multiuser.controller.plugin;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.blackboard.Blackboard;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardEvent;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.*;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;


/**
 * Created by oscarr on 4/4/17.
 * This component communicate with external services automatically, that is, external service subscribe to a list
 * of messages and this component sends and receives changes on Blackboard.
 */
@BlackboardSubscription( messages = {} )
@StateType( state = Constants.STATEFULL )
public class ExternalComponent extends PluggableComponent implements ResponseListener {

    public ExternalComponent(ServiceInfo serviceInfo, String clientAddress, String sessionId, ZMsgWrapper zMsgWrapper,
                             String[] messages){
        try {
            //if we override annotations, it will affect all instances of ExternalComponent, so every
            //ExternalComponent will have the same subscription messages
            //Utils.addOrChangeAnnotation(getClass().getAnnotation(BlackboardSubscription.class), "messages", messages);
            ResourceLocator.addComponentSubscriptions( this.hashCode(), messages );
            setClientCommController( new ClientCommController.Builder(Log4J.getInstance())
                .setServerAddress(serviceInfo.getSlaveMUFAddress())
                .setServiceName(serviceInfo.getServiceName())
                .setSessionId(sessionId)
                .setSubscriptionMessages( messages )
                .setRequestType( Constants.REQUEST_CONNECT )
                .setResponseListener(this)
                .setSendAck(false)
                .build() );
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    @Override
    public void startUp(){
        super.startUp();
        Log4J.info(this, "ExternalComponent: startup has finished for service.");
    }

    @Override
    public void postCreate() {

    }
    @Override
    public void execute() {
    }

    @Override
    public void onEvent(Blackboard bb, BlackboardEvent event) {
        try {
            String sessionId = getSessionId();
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setSessionId( sessionId );
            sessionMessage.setRequestType(event.getStatus());
            sessionMessage.setMessageId(event.getId());
            sessionMessage.setPayload( CommonUtils.toJson(event.getElement() ));
            send( sessionMessage, true );
        }catch (Throwable e){
            ExceptionHandler.handle( e );
        }
    }

    @Override
    public void process(String message) {
        try {
            SessionMessage sessionMessage = CommonUtils.fromJson(message, SessionMessage.class);
            if (sessionMessage.getMessageId() == null || sessionMessage.getMessageId().isEmpty()) {
                String msg = "This message from Python (or any other external server) has an empty or null id. Make " +
                        "sure you send a message with a proper id, otherwise it won't be delivered through the Blackboard. " +
                        "Message: " + sessionMessage.getPayload();
                Log4J.error(this, msg);
            }
            if( !sessionMessage.getRequestType().equals(Constants.SESSION_CLOSED)
                    && !sessionMessage.getRequestType().equals(Constants.REQUEST_SHUTDOWN_SYSTEM) ) {
                getBlackBoard( sessionMessage.getSessionId() ).post(this, sessionMessage.getMessageId(),
                        sessionMessage.getPayload());
            }else{
                destroyInCascade( this );
            }
        }catch (Throwable e){
            ExceptionHandler.handle(e);
        }
    }
}
