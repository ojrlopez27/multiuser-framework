package edu.cmu.inmind.multiuser.controller.orchestrator.devices;

import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.orchestrator.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.orchestrator.services.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 4/26/18.
 */
public class Device {
    protected String name;
    protected Map<String, Pair<Behavior, Service>> behServMap;
    protected CopyOnWriteArrayList<String> state;
    protected BehaviorNetwork network;

    // QoS device attributes
    protected boolean isGPSturnedOn = true;
    protected int batteryLevel = 10;
    protected long latency; //depends on number of hops between peers?

    public Device(){}

    public Device(String name, BehaviorNetwork network){
        this.network = network;
        this.state = network.getState();
        this.name = name;
        buildMap(name);
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
        behServMap.get(serviceName).snd.execute();
        //TODO: each subclass has to do something with the actual service
    }

    public synchronized void addState(String premise){
        if( !state.contains(premise) ) state.add(premise);
    }

    private String addPrefix(String premise){
        return name + "-" +premise;
    }

    private void buildMap(String name){
        try {
            this.behServMap = new HashMap<>();
            for (Behavior behavior : network.getBehByPrefix(name)) {
                behServMap.put(behavior.getName(), new Pair(behavior, getService(behavior)));
            }
        }catch (Throwable e){e.printStackTrace();}
    }

    private Service getService(Behavior behavior) {
        if( behavior.getName().equals("bob-phone-do-shopping") ) return new BobPhoneDoShopService(behavior, state);
        else if( behavior.getName().equals("alice-phone-do-shopping") ) return new AlicePhoneDoShopService(behavior, state);
        else if( behavior.getName().equals("bob-phone-check-have-shopping-list") ) return new BobPhoneHasShopListService(behavior, state);
        else if( behavior.getName().equals("alice-phone-check-have-shopping-list") ) return new AlicePhoneHasShopListService(behavior, state);
        else if( behavior.getName().equals("server-who-is-nearer-to-shop") ) return new ServerWhoIsNeareastService(behavior, state);
        else if( behavior.getName().equals("bob-phone-get-distance-to-shop") ) return new BobPhoneDistanceShopService(behavior, state);
        else if( behavior.getName().equals("alice-phone-get-distance-to-shop") ) return new AlicePhoneDistanceShopService(behavior, state);
        else if( behavior.getName().equals("alice-phone-find-nearest-shop") ) return new AlicePhoneNearestShopService(behavior, state);
        else if( behavior.getName().equals("bob-phone-find-nearest-shop") ) return new BobPhoneNearestShopService(behavior, state);
        else if( behavior.getName().equals("bob-phone-location") ) return new BobPhoneLocationService(behavior, state);
        else if( behavior.getName().equals("bob-tablet-location") ) return new BobTabletLocationService(behavior, state);
        else if( behavior.getName().equals("alice-phone-location") ) return new AlicePhoneLocationService(behavior, state);
        else if( behavior.getName().equals("bob-phone-weather") ) return new BobPhoneWeatherService(behavior, state);
        return null;
    }
}
