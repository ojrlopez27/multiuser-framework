package edu.cmu.inmind.multiuser.controller.orchestrator.services;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 4/30/18.
 */
public class ServerWhoIsNeareastService extends Service {

    public ServerWhoIsNeareastService(Behavior behavior, CopyOnWriteArrayList<String> state){
        super(behavior, state);
    }


    @Override
    public void execute() {
        Random random = new Random();
        int who = random.nextInt(2);
        network.triggerPostconditions( behavior, Arrays.asList(who == 0? "bob-nearest-to-shop" : "alice-nearest-to-shop") );
    }
}
