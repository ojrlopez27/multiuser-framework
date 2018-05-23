package edu.cmu.inmind.multiuser.controller.orchestrator.services;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 5/22/18.
 */
public class FindPlaceService extends Service{
    public FindPlaceService(String deviceName, Behavior behavior, CopyOnWriteArrayList<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public void execute() {
        network.triggerPostconditions(behavior);
    }
}
