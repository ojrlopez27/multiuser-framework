package edu.cmu.inmind.multiuser.test;

import edu.cmu.inmind.multiuser.common.Constants;
import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.common.Utils;
import edu.cmu.inmind.multiuser.controller.communication.ClientCommController;
import edu.cmu.inmind.multiuser.controller.communication.ResponseListener;
import edu.cmu.inmind.multiuser.controller.communication.SessionMessage;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.Log4J;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by oscarr on 9/17/17.
 */
public class MockClient {

    public static void main(String args[]) throws Exception {
        try {
            String sessionId = args[0];
            ClientCommController client = new ClientCommController.Builder()
                    .setServerAddress("tcp://34.203.160.208:5666") //5666
                    .setClientAddress("tcp://34.203.160.208:5666")
//                    .setServerAddress("tcp://127.0.0.1:5555")
//                    .setClientAddress("tcp://127.0.0.1:5555")
                    .setServiceName(sessionId)
                    .setRequestType(Constants.REQUEST_CONNECT)
                    .setTCPon(true)
                    .setMuf(null) //when TCP is off, we need to explicitly tell the client who the MUF is
                    .setResponseListener(message -> {
                        SessionMessage sessionMessage = Utils.fromJson(message, SessionMessage.class);
                        Log4J.info(ResponseListener.class, "Received message: " + message);
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
                } else if (input.equals("sr")) {
                    client.send(sessionId, new SessionMessage("MSG_SR", ""));
                } else if (input.equals("disconnect")) {
                    client.send(sessionId, new SessionMessage(Constants.REQUEST_DISCONNECT, "" + System.currentTimeMillis(), sessionId));
                } else if (input.equals("asr")) {
                    SessionMessage sessionMessage = new SessionMessage();
                    sessionMessage.setMessageId("MSG_ASR");
                    sessionMessage.setPayload("{\"utterance\": \"I like action movies\", \"confidence\": 1.0}");
                    client.send(sessionId, sessionMessage);
                }
            }
            System.exit(0);
        } catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
    }

}
