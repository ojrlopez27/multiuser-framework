package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.log.Log4J;

import java.util.Scanner;

/**
 * Created by oscarr on 9/17/17.
 */
public class MockClient {

    public static void main(String args[]){
        String sessionId = args[0];
        ClientCommController client =  new ClientCommController.Builder()
                .setServerAddress("tcp://127.0.0.1:5555")
                .setServiceName( sessionId )
                .setClientAddress( "tcp://127.0.0.1:5555" )
                .setRequestType( Constants.REQUEST_CONNECT )
                .setTCPon( true )
                .setMuf( null ) //when TCP is off, we need to explicitly tell the client who the MUF is
                .build();
        // this method will be executed asynchronuously, so we need to add a delay before stopping the MUF
        client.setResponseListener(message -> {
            SessionMessage sessionMessage = Utils.fromJson(message, SessionMessage.class);
            Log4J.info(ResponseListener.class, "Received message: " + sessionMessage.getPayload());
        });

        Scanner scanner = new Scanner(System.in);
        String input = "";
        while( !input.equals("stop") ){
            System.out.println("Enter a command:");
            input = scanner.nextLine();
            if( input.equals("stop") ) {
                break;
            }else if(input.equals("dm")){
                client.send(sessionId, new SessionMessage("MSG_START_DM",""));
            }else if(input.equals("sr")){
                client.send(sessionId, new SessionMessage("MSG_SR",""));
            }else if( input.equals("disconnect") ){
                client.send( sessionId, new SessionMessage(Constants.REQUEST_DISCONNECT, ""+ System.currentTimeMillis(), sessionId) );
            }else if( input.equals("asr") ){
                SessionMessage sessionMessage = new SessionMessage();
                sessionMessage.setMessageId("MSG_ASR");
                sessionMessage.setPayload( "{\"utterance\": \"I like action movies\", \"confidence\": 1.0}");
                client.send( sessionId, sessionMessage );
            }
        }
    }
}
