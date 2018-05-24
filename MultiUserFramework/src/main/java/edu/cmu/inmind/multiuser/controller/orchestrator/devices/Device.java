package edu.cmu.inmind.multiuser.controller.orchestrator.devices;

import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.orchestrator.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.orchestrator.services.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 4/26/18.
 */
public class Device {
    public enum TYPES{ PHONE, TABLET, SERVER}

    protected String name;
    protected Map<String, Pair<Behavior, Service>> behServMap;
    protected CopyOnWriteArrayList<String> state;
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
        //buildMap(name);
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

    public synchronized void executeService(String serviceName){
        System.out.println(String.format("*** %s device is executing service: %s", name, serviceName));
        behServMap.get(serviceName).snd.execute();
        //TODO: each subclass has to do something with the actual service
    }

    public synchronized void addState(String premise){
        if( !state.contains(premise) ) state.add(premise);
    }

    private String addPrefix(String premise){
        return name + Behavior.TOKEN +premise;
    }

    private void buildMap(String name){
        try {
            for (Behavior behavior : network.getBehByPrefix(name)) {
                behServMap.put(behavior.getName(), new Pair(behavior, getService(behavior)));
            }
        }catch (Throwable e){e.printStackTrace();}
    }

    private Service getService(Behavior behavior) {
        if( behavior.getName().equals("get-self-location") ) return new LocationService(name, behavior, state);
        else if( behavior.getName().equals("find-place-location") ) return new FindPlaceService(name, behavior, state);
        else if( behavior.getName().equals("get-distance-to-place") ) return new DistanceCalculatorService(name, behavior, state);
        else if( behavior.getName().equals("calculate-nearest-place") ) return new WhoIsNearestService(name, behavior, state);
        return null;
    }

    public void addServices(List<Behavior> behaviors, Map<String, String>... optionalMappings) {
        for (Behavior behavior : behaviors) {
            Service service = getService(behavior);
            if(name.equals("server")){
                behavior = behavior.groundByReplacing(optionalMappings[0]);
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
