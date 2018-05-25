package edu.cmu.inmind.multiuser.controller.orchestrator.services;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by oscarr on 5/25/18.
 */
public class GoPharmacyService extends Service {

    public GoPharmacyService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(int simulationStep) {
        network.triggerPostconditions(behavior);
        return true;
    }
}