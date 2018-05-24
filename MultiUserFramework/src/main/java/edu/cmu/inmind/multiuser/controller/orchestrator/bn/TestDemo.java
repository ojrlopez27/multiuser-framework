package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.Device;
import java.util.Arrays;
import java.util.List;

/**
 * Created by oscarr on 4/26/18.
 */
public class TestDemo {
    public static void main(String args[]) throws Exception{
        // this is our composition controller
        CompositionController compositionController = new CompositionController("behavior-network.json");

        // create users
        compositionController.createUsers("alice", "bob");

        // create devices
        compositionController.createDevice("bob", Device.TYPES.PHONE).setGPSturnedOn(false);
        compositionController.createDevice("bob", Device.TYPES.TABLET).setBatteryLevel(6);
        compositionController.createDevice("alice", Device.TYPES.PHONE);
        compositionController.createDevice("server", Device.TYPES.SERVER);

        // create services
        compositionController.instantiateServices(
                new Pair(Arrays.asList("bob", "alice"),
                        Arrays.asList("get-self-location", "find-place-location", "get-distance-to-place")),
                new Pair(Arrays.asList(CompositionController.SERVER),
                        Arrays.asList("calculate-nearest-place")) );

        // set system/user goals
        compositionController.setGoals( Arrays.asList(  "nearest-person-calculated")); //party-organization-done
        // let's extract xxx-required preconditions
        compositionController.endMeansAnalysis();

        // decision-making cycle
        while( !compositionController.hasMoreGoals() ) {
            compositionController.updateDeviceState();
            int idx = compositionController.selectBehavior();
            if( idx >= 0 ){
                compositionController.executeBehavior(idx);
            }else{
                List<String> missing = compositionController.nextPlausibleBehavior();
                for(String miss : missing){
                    compositionController.addState(miss, "");
                    System.out.println("Adding missing state: " + miss);
                }
            }
        }
        System.exit(0);
    }
}
