package edu.cmu.inmind.multiuser.controller.session;

import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.resources.ResourceLocator;

/**
 * Created by oscarr on 6/14/18.
 */
public class CrossSessionChoreographer {
    private static CrossSessionChoreographer instance;

    private CrossSessionChoreographer(){}

    public static CrossSessionChoreographer getInstance(){
        if(instance == null){
            instance = new CrossSessionChoreographer();
        }
        return instance;
    }

    public void passMessage(String sessionFrom, String sessionTo, String message, String messageId){
        try {
            SessionMessage sessionMessage = new SessionMessage();
            sessionMessage.setSessionId(sessionFrom);
            sessionMessage.setMessageId(messageId);
            sessionMessage.setRequestType(Constants.CROSS_SESSION_MESSAGE);
            sessionMessage.setPayload(message);
            ResourceLocator.getSession(sessionTo).getOrchestrator().process(CommonUtils.toJson(sessionMessage));
        }catch (Throwable throwable){
            throwable.printStackTrace();
        }
    }



    public void passMessage(String sessionFrom, String message, String messageId){
        for(String sessionId : ResourceLocator.getSessions().keySet()){
            if(!sessionFrom.equals(sessionId)){
                passMessage(sessionFrom, sessionId, message, messageId);
            }
        }
    }
}
