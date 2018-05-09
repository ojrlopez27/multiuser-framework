package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.Device;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.PhoneDevice;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.ServerDevice;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.TabletDevice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by oscarr on 4/26/18.
 */
public class Test {
    private static Device bobPhone, bobTablet, alicePhone, server;


    public static void main(String args[]) throws Exception{
        BehaviorNetwork network = Utils.fromJsonFile("behavior-network.json", BehaviorNetwork.class);
        HashMap<String, Behavior> serviceMap = network.map();

        List<Device> devices = new ArrayList<>();
        bobPhone = new PhoneDevice( "bob-phone", network ).setGPSturnedOn(false);
        bobTablet = new TabletDevice( "bob-tablet",  network ).setBatteryLevel(6);
        alicePhone = new PhoneDevice( "alice-phone",  network );
        server = new ServerDevice("server", network);
        devices.add( bobPhone );
        devices.add( bobTablet );
        devices.add( alicePhone );

        network.setState( Arrays.asList("shopping-not-done", "bob-weather-required" ));
        network.setGoals( Arrays.asList("shopping-done"));
//        network.setGoals(Arrays.asList("bob-weather-provided"));

        HashMap<Behavior, Device> deviceServiceMap = generateMap(serviceMap);
        network.endMeansAnalysis();

        while( !network.getGoals().isEmpty() ) {
            for(Device device : devices){
                device.updateState();
            }
            int idx = network.selectBehavior();
            if( idx >= 0 ){
                Behavior selectedBehavior = network.getModules().get(idx);
                deviceServiceMap.get(selectedBehavior).executeService(selectedBehavior.getName());
            }
        }
        System.exit(0);
    }

    private static HashMap<Behavior, Device> generateMap(HashMap<String, Behavior> serviceMap) {
        HashMap<Behavior, Device> deviceServiceMap = new HashMap<>();
        for(String serviceName : serviceMap.keySet() ){
            if(serviceName.startsWith("bob-phone"))
                deviceServiceMap.put(serviceMap.get(serviceName), bobPhone);
            else if(serviceName.startsWith("bob-tablet"))
                deviceServiceMap.put(serviceMap.get(serviceName), bobTablet);
            else if(serviceName.startsWith("alice-phone"))
                deviceServiceMap.put(serviceMap.get(serviceName), alicePhone);
            else if(serviceName.startsWith("server"))
                deviceServiceMap.put(serviceMap.get(serviceName), server);
        }
        return deviceServiceMap;
    }
}
