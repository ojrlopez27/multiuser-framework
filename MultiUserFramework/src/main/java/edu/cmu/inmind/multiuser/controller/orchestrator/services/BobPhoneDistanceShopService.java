package edu.cmu.inmind.multiuser.controller.orchestrator.services;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 4/30/18.
 */
public class BobPhoneDistanceShopService extends Service {

    public BobPhoneDistanceShopService(Behavior behavior, CopyOnWriteArrayList<String> state){
        super(behavior, state);
    }


    @Override
    public void execute() {
        network.triggerPostconditions(behavior);
    }
}
