package edu.cmu.inmind.multiuser.controller.composer.devices;

import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;

/**
 * Created by oscarr on 6/21/18.
 */
public class SmartwatchDevice extends Device {
    public SmartwatchDevice(BehaviorNetwork network, String belongsTo){
        super(network, belongsTo);
    }

    @Override
    public TYPES getType() {
        return TYPES.SMARTWATCH;
    }

    @Override
    public synchronized boolean executeService(String serviceName){
        return super.executeService(serviceName);
    }
}
