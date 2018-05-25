package edu.cmu.inmind.multiuser.controller.orchestrator.devices;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.orchestrator.bn.BehaviorNetwork;

import static edu.cmu.inmind.multiuser.controller.orchestrator.simulation.SimuConstants.S3_BOB_LOCATION;

/**
 * Created by oscarr on 4/26/18.
 */
public class TabletDevice extends Device {
    public TabletDevice(String name, BehaviorNetwork network, String belongsToUser){
        super(name, network, belongsToUser);
    }

    @Override
    public synchronized boolean executeService(String serviceName, int simulationStep){
        boolean performed = super.executeService(serviceName, simulationStep);
        String prefix = belongsToUser + Behavior.TOKEN;
        if(simulationStep <= S3_BOB_LOCATION)
            addState(prefix + "place-name-provided");
        return performed;
    }
}
