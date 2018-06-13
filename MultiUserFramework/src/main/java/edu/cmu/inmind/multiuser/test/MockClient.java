package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import java.util.Scanner;

/**
 * Created by oscarr on 9/17/17.
 */
public class MockClient {

    public static void main(String args[]) throws Exception {
        try {
            String sessionId = args[0];
            String ipAddress = args[1];
            ClientCommController client = new ClientCommController.Builder(Log4J.getInstance())
                    .setServerAddress("tcp://"+ipAddress+":5555")
                    .setSessionId(sessionId)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setResponseListener(new ResponseListener() {
                        @Override
                        public void process(String message) {
                            SessionMessage sessionMessage = CommonUtils.fromJson(message, SessionMessage.class);
                            Log4J.info(ResponseListener.class, "Received message: " + message);
                        }
                    })
                    .build();

            Scanner scanner = new Scanner(System.in);
            String input = "";
            while (!input.equals("stop") && !input.equals("disconnect")) {
                System.out.println("Enter a command:");
                input = scanner.nextLine();
                if (input.equals("stop")) {
                    break;
                } else if (input.equals("dm")) {
                    client.send(sessionId, new SessionMessage("MSG_START_DM", ""));
                }
                else if(input.equals("start"))
                {
                    client.send(sessionId, new SessionMessage("MSG_START_SESSION", "RESET_NONE"));
                }
                else if (input.equals("sr")) {
                    client.send(sessionId, new SessionMessage("MSG_SR", ""));
                } else if (input.equals("disconnect")) {
                    client.disconnect(sessionId);
                } else if (input.equals("asr")) {
                    SessionMessage sessionMessage = new SessionMessage();
                    sessionMessage.setMessageId("MSG_ASR");
                    sessionMessage.setPayload("{\"utterance\": \"I like action movies\", \"confidence\": 1.0}");
                    client.send(sessionId, sessionMessage);
                }
                else
                {
                    SessionMessage sessionMessage = new SessionMessage();
                    sessionMessage.setMessageId("MSG_ASR");
                    sessionMessage.setPayload("{\"utterance\": "+""+ "\""+input.toString()+"\", \"confidence\": 1.0}");
                    client.send(sessionId, sessionMessage);
                }
            }
            System.exit(0);
        } catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
    }

}
