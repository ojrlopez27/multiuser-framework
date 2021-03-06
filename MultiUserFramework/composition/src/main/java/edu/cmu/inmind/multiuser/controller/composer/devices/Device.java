package edu.cmu.inmind.multiuser.controller.composer.devices;

import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.composer.services.*;
import edu.cmu.inmind.multiuser.controller.log.Log4J;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static edu.cmu.inmind.multiuser.controller.composer.group.User.CLOUD;

/**
 * Created by oscarr on 4/26/18.
 */
public abstract class Device {
    public enum TYPES{ PHONE, TABLET, SERVER, SMARTWATCH}

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

    public Device(BehaviorNetwork network, String belongsToUser){
        this.network = network;
        this.state = network.getState();
        this.behServMap = new HashMap<>();
        this.belongsToUser = belongsToUser;
        this.name = belongsToUser + Behavior.TOKEN + getType().toString().toLowerCase();
    }

    public abstract TYPES getType();

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
        List<String> states = new ArrayList<>();
        if( batteryLevel >= 7 ) states.add(addPrefix("high-battery"));
        else if( batteryLevel >= 3 ) states.add(addPrefix("medium-battery"));
        else states.add(addPrefix("low-battery"));

        if( isGPSturnedOn ) states.add( addPrefix("gps-turned-on" ));
        else states.add(addPrefix("gps-turned-off"));

        if( latency > 100 ) states.add( addPrefix("high-latency" ));
        else if( latency > 50 ) states.add( addPrefix("medium-latency" ));
        else states.add( addPrefix("low-latency" ));

        addStates( states.toArray( new String[states.size()] ) );
    }

    public synchronized boolean executeService(String serviceName){
        Log4J.info(this, String.format("%s device is executing service: %s", name, serviceName));
        boolean perfomed = behServMap.get(serviceName).snd.execute();
        //TODO: each subclass has to do something with the actual service
        return perfomed;
    }

    public synchronized void addStates(String... states){
        network.setState(Arrays.asList(states));
    }

    private String addPrefix(String premise){
        return name + Behavior.TOKEN + premise;
    }

    public void addServices(List<Behavior> behaviors, Map<String, String>... optionalMappings) {
        for (Behavior behavior : behaviors) {
            Service service = Service.getService(behavior, name, state);
            if(name.contains(CLOUD)){
                behavior = behavior.groundByReplacing(CLOUD, optionalMappings[0]);
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
