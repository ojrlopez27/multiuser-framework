package edu.cmu.inmind.multiuser.controller.orchestrator.devices;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.BehaviorNetwork;

/**
 * Created by oscarr on 4/26/18.
 */
public class PhoneDevice extends Device {
    public PhoneDevice(String name, BehaviorNetwork network, String belongsToUser){
        super(name, network, belongsToUser);
    }

    @Override
    public synchronized void executeService(String serviceName){
        super.executeService(serviceName);
    }
}
