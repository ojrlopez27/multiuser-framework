package edu.cmu.inmind.multiuser.controller.composer.simulation;

import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.composer.bn.CompositionController;
import edu.cmu.inmind.multiuser.controller.composer.devices.Device;
import edu.cmu.inmind.multiuser.controller.composer.services.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.cmu.inmind.multiuser.controller.composer.group.User.CLOUD;
import static edu.cmu.inmind.multiuser.controller.composer.simulation.SimuConstants.*;

/**
 * Created by oscarr on 4/26/18.
 */
public class TestDemo {
    private static CompositionController compositionController;

    public static void main(String args[]) throws Exception{
        // this is our composition controller
        compositionController = new CompositionController("behavior-network.json");

        // create users
        compositionController.createUsers("alice", "bob");

        // create devices
        compositionController.createDevice("bob", Device.TYPES.PHONE).setGPSturnedOn(false);
        compositionController.createDevice("bob", Device.TYPES.TABLET).setBatteryLevel(6);
        compositionController.createDevice("alice", Device.TYPES.PHONE);
        compositionController.createDevice(CLOUD, Device.TYPES.SERVER);

        // create services
        compositionController.instantiateServices(
                getMapOfServices(),
                new Pair<>(Arrays.asList("bob", "alice"), getUserServices()),
                new Pair<>(Arrays.asList(CLOUD), getServerServices() ));

        // set system/user goals and states
        compositionController.addState("bob-party-not-organized", "alice-party-not-organized" );
        compositionController.setGoals(   "grocery-shopping-done", "whatever" ); // "organize-party-done"
        // let's extract xxx-required preconditions
        compositionController.endMeansAnalysis();

        // decision-making cycle
        int step = S2_ALICE_LOCATION;
        while( !compositionController.hasMoreGoals() ) {
            compositionController.updateDeviceState();
            int idx = compositionController.selectService()[0];
            if( idx >= 0 ){
                if( compositionController.executeService(idx) ) {
                    step++;
                }
                addEventToState(step);
            }
        }
        System.exit(0);
    }

    private static Map<String,Class<? extends Service>> getMapOfServices() {
        Map<String, Class<? extends Service>> map = new HashMap<>();
        map.put("get-self-location", LocationService.class);
        map.put("find-place-location", FindPlaceService.class);
        map.put("get-distance-to-place", DistanceCalculatorService.class);
        map.put("calculate-nearest-place", WhoIsNearestService.class);
        map.put("share-grocery-list", ShareGroceryListService.class);
        map.put("do-grocery-shopping", DoGroceryShoppingService.class);
        map.put("do-beer-shopping", DoBeerShoppingService.class);
        map.put("go-home-decor", GoHomeDecoService.class);
        map.put("organize-party", OrganizePartyService.class);
        map.put("go-pharmacy", GoPharmacyService.class);
        return map;
    }


    private static void addEventToState(int simulationStep) {
        switch (simulationStep){
            case S8_ALICE_SHOP_LIST:
                compositionController.addState("bob-grocery-shopping-not-done",
                        "alice-grocery-shopping-not-done" );
                break;
            case S11_ALICE_ADD_PREF:
                compositionController.removeState("alice-is-closer-to-place");
                compositionController.removeState("bob-is-closer-to-place");
                compositionController.removeState("alice-place-location-provided");
                compositionController.addState("alice-place-location-required",
                        "alice-place-name-provided" );
                break;
            case S12_ALICE_DO_GROCERY:
                compositionController.addState("alice-close-to-organic-supermarket" );
                break;
            case S13_BOB_FIND_BEER:
                compositionController.removeState("bob-place-location-provided");
                compositionController.addState("bob-place-location-required",
                        "bob-place-name-provided",
                        "bob-beer-shopping-not-done" );
                break;
            case S14_BOB_GO_BEER_SHOP:
                compositionController.removeState("bob-grocery-shopping-required");
                compositionController.removeState("alice-grocery-shopping-required");
                compositionController.removeState("bob-grocery-shopping-not-done");
                compositionController.addState("bob-driver-license-provided",
                        "bob-is-closer-to-place",
                        "bob-beer-shopping-not-done",
                        "bob-beer-shopping-required" );
                break;
            case S15_BOB_FIND_HOME_DECO:
                compositionController.removeState("bob-place-location-provided");
                compositionController.addState( "bob-place-location-required",
                        "bob-place-name-provided");
                break;
            case S16_BOB_GO_HOME_DECO:
                compositionController.addState("bob-is-closer-to-place",
                        "bob-buy-decoration-required");
                break;
            case S17_ALICE_HEADACHE:
                compositionController.removeState("bob-place-location-provided");
                compositionController.removeState("bob-buy-decoration-required");
                compositionController.addState("bob-place-location-required",
                        "bob-place-name-provided",
                        "bob-somebody-has-headache",
                        "bob-no-medication-at-home" );
                break;
            case S18_BOB_COUPONS:
                compositionController.addState("bob-has-coupons");
                break;
            case S19_BOB_GO_HOME_DECO:
                compositionController.addState("bob-buy-decoration-required");
                break;
            case S20_ALICE_GO_HOME_DECO:
                compositionController.addState("alice-buy-decoration-required",
                        "alice-is-closer-to-place");
                break;
        }
    }


    private static List<String> getUserServices(){
        return Arrays.asList(
                "get-self-location",
                "find-place-location",
                "get-distance-to-place",
                "share-grocery-list",
                "do-grocery-shopping",
                "do-beer-shopping",
                "go-home-decor",
                "go-pharmacy");
    }

    private static List<String> getServerServices(){
        return Arrays.asList(
                "calculate-nearest-place",
                "organize-party");
    }
}
