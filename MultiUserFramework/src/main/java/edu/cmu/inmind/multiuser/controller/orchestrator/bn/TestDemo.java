package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

import edu.cmu.inmind.multiuser.controller.orchestrator.devices.Device;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Created by oscarr on 4/26/18.
 */
public class TestDemo {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String args[]) throws Exception{
        // this is our composition controller
        CompositionController compositionController = new CompositionController("behavior-network.json");

        // create users
        compositionController.createUsers("alice", "bob");

        // create devices
        compositionController.createDevice("bob", Device.TYPES.PHONE).setGPSturnedOn(false);
        compositionController.createDevice("bob", Device.TYPES.TABLET).setBatteryLevel(6);
        compositionController.createDevice("alice", Device.TYPES.PHONE);

        // create services
        compositionController.instantiateServices( "get-self-location", "find-place-location", "get-distance-to-place" );

        // set system/user goals
        compositionController.setGoals( Arrays.asList("distance-to-place-provided")); //party-organization-done
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
                    System.out.println( String.format("Enter the value for %s:", miss) );
                    String state = scanner.nextLine();
                    compositionController.addState(miss, state);
                }
            }
        }
        System.exit(0);
    }
}
