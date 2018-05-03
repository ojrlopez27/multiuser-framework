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

    public Service(){}

    public Service(Behavior behavior, CopyOnWriteArrayList<String> state){
        this.behavior = behavior;
        this.state = state;
        this.network = behavior.getNetwork();
    }

    public abstract void execute();
}
