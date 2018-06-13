package edu.cmu.inmind.multiuser.controller.composer.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;

import static edu.cmu.inmind.multiuser.controller.composer.simulation.SimuConstants.S9_CLOSER_TO_GROCERY;


/**
 * Created by oscarr on 4/30/18.
 */
public class WhoIsNearestService extends Service{

    public WhoIsNearestService(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public boolean execute(int simulationStep) {
        if( simulationStep == S9_CLOSER_TO_GROCERY) {
            network.triggerPostconditions(behavior, Arrays.asList("bob-is-closer-to-place"));
            return true;
        }
        return false;
    }
}
