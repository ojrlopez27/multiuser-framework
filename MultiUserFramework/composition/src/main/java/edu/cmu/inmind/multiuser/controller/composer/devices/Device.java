package edu.cmu.inmind.multiuser.controller.composer.devices;

import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.composer.services.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by oscarr on 4/26/18.
 */
public class Device {
    public enum TYPES{ PHONE, TABLET, SERVER}
    public final static String SERVER = "server";

    protected String name;
    protected Map<String, Pair<Behavior, Service>> behServMap;
    protected ConcurrentSkipListSet<String> state;
    protected BehaviorNetwork network;
    protected String belongsToUser;

    // QoS device attributes
    protected boolean isGPSturnedOn = true;
    protected int batteryLevel = 10;
    protected long latency; //depends on number of hops between peers?

    public Device(){}

    public Device(String name, BehaviorNetwork network, String belongsToUser){
        this.network = network;
        this.state = network.getState();
        this.name = name;
        this.behServMap = new HashMap<>();
        this.belongsToUser = belongsToUser;
    }

    public Device setGPSturnedOn(boolean GPSturnedOn) {
        isGPSturnedOn = GPSturnedOn;
        return this;
    }

    public Device setBatteryLevel(int batteryLevel) {
        this.batteryLevel = batteryLevel;
        return this;
    }

    public Device setLatency(long latency) {
        this.latency = latency;
        return this;
    }

    public synchronized void updateState() {
        if( batteryLevel >= 7 ) addState(addPrefix("high-battery"));
        else if( batteryLevel >= 3 ) addState(addPrefix("medium-battery"));
        else addState(addPrefix("low-battery"));

        if( isGPSturnedOn ) addState( addPrefix("gps-turned-on" ));
        else addState(addPrefix("gps-turned-off"));

        if( latency > 100 ) addState( addPrefix("high-latency" ));
        else if( latency > 50 ) addState( addPrefix("medium-latency" ));
        else addState( addPrefix("low-latency" ));
    }

    public synchronized boolean executeService(String serviceName, int simulationStep){
        System.out.println(String.format("*** %s device is executing service: %s   at simulation step: %s"
                , name, serviceName, simulationStep));
        boolean perfomed = behServMap.get(serviceName).snd.execute(simulationStep);
        //TODO: each subclass has to do something with the actual service
        return perfomed;
    }

    public synchronized void addState(String premise){
        if( !state.contains(premise) ) state.add(premise);
    }

    private String addPrefix(String premise){
        return name + Behavior.TOKEN + premise;
    }

    public void addServices(List<Behavior> behaviors, Map<String, String>... optionalMappings) {
        for (Behavior behavior : behaviors) {
            Service service = Service.getService(behavior, name, state);
            if(name.equals(SERVER)){
                behavior = behavior.groundByReplacing(name + Behavior.TOKEN + belongsToUser, optionalMappings[0]);
            }else{
                behavior = behavior.groundByPrefix(name, belongsToUser);
            }
            service.setBehavior(behavior);
            behServMap.put(behavior.getName(), new Pair(behavior, service));
        }
    }

    public List<Behavior> getBehaviors(){
        List<Behavior> behaviors = new ArrayList<>();
        for(Pair<Behavior, Service> pair : behServMap.values()){
            behaviors.add(pair.fst);
        }
        return behaviors;
    }
}
