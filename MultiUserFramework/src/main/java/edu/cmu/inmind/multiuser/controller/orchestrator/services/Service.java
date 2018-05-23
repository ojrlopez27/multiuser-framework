package edu.cmu.inmind.multiuser.controller.orchestrator.services;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.orchestrator.bn.BehaviorNetwork;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 4/26/18.
 */
public abstract class Service {
    protected Behavior behavior; // each behavior maps one and only one service
    protected CopyOnWriteArrayList<String> state; //we need state to update changes on it
    protected BehaviorNetwork network;
    protected String deviceName;

    public Service(){}

    public Service(String deviceName, Behavior behavior, CopyOnWriteArrayList<String> state){
        this.behavior = behavior;
        this.state = state;
        this.network = behavior.getNetwork();
        this.deviceName = deviceName;
    }

    public abstract void execute();

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }
}
