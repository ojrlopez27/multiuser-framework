package edu.cmu.inmind.multiuser.controller.composer.devices;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;

import static edu.cmu.inmind.multiuser.controller.composer.simulation.SimuConstants.S3_BOB_LOCATION;

/**
 * Created by oscarr on 4/26/18.
 */
public class TabletDevice extends Device {
    public TabletDevice(BehaviorNetwork network, String belongsToUser){
        super(network, belongsToUser);
    }

    @Override
    public TYPES getType() {
        return TYPES.TABLET;
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
