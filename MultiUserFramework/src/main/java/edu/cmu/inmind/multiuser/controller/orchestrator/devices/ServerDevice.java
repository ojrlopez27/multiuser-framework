package edu.cmu.inmind.multiuser.controller.orchestrator.devices;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.orchestrator.group.User;

import java.util.Collection;

/**
 * Created by oscarr on 4/27/18.
 */
public class ServerDevice extends Device {
    private Collection<User> users;

    public ServerDevice(String name, BehaviorNetwork network, Collection<User> users){
        super(name, network, name);
        this.users = users;
    }

    @Override
    public synchronized void executeService(String serviceName){
        super.executeService(serviceName);
    }
}
