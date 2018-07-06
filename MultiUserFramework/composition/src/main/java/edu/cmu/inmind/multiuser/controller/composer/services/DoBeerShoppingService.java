package edu.cmu.inmind.multiuser.controller.composer.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by oscarr on 5/25/18.
 */
public class DoBeerShoppingService extends Service {

    public DoBeerShoppingService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(Object... params) {
        network.triggerPostconditions(behavior);
        return true;
    }
}