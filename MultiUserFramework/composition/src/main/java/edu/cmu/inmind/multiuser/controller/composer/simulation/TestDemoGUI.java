package edu.cmu.inmind.multiuser.controller.composer.simulation;


import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;
import edu.cmu.inmind.multiuser.controller.composer.bn.CompositionController;
import edu.cmu.inmind.multiuser.controller.composer.devices.Device;
import edu.cmu.inmind.multiuser.controller.composer.ui.BNGUIVisualizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static edu.cmu.inmind.multiuser.controller.composer.group.User.ADMIN;

/**
 * Created by oscarr on 4/26/18.
 */
public class TestDemoGUI {
    private static CompositionController compositionController;
    private static BNGUIVisualizer plot;
    private static boolean shouldPlot = true;
    private static List<String> correctSeqOfServices = Arrays.asList(
            "alice-phone-get-self-location",
            "bob-tablet-get-self-location",
            "alice-phone-find-place-location",
            "alice-phone-get-distance-to-place",
            "bob-phone-find-place-location",
            "bob-phone-get-distance-to-place",
            "server-admin-calculate-nearest-place",
            "alice-phone-share-grocery-list",
            "bob-phone-do-grocery-shopping",
            "alice-phone-find-place-location",
            "alice-phone-do-grocery-shopping",
            "bob-tablet-find-place-location",
            "bob-phone-do-beer-shopping",
            "bob-phone-find-place-location",
            "bob-phone-go-home-decor",
            "bob-phone-go-pharmacy",
            "bob-tablet-go-home-decor",
            "alice-phone-go-home-decor");
    private static int seqIdx = 0;

    public static void main(String args[]) throws Exception{

        shouldPlot = true;
        init();

        // decision-making cycle
        int step = SimuConstants.SimSteps.S1_ALICE_LOCATION.ordinal();
        Scanner scanner = new Scanner(System.in);
        while( !compositionController.hasMoreGoals() ) {
            if(!plot.isPaused()) {
                runOneStep(step);
                step++;
            }
            //scanner.nextLine();
            CommonUtils.sleep(1000);
        }
        System.exit(0);
    }

    private static void checkCorrectSequence(int idx) {
        if( !compositionController.getServices().get(idx).getName().equals( correctSeqOfServices.get(seqIdx) ) ) {
            throw new IllegalStateException(
                    String.format("Incorrect sequence of behaviors/services. It should be '%s' and it received '%s'",
                            correctSeqOfServices.get(seqIdx), compositionController.getServices().get(idx).getName()));
        }
        seqIdx++;
    }

    public static int runOneStep(int step) {
        compositionController.updateDeviceState();
        int idx = compositionController.selectService();
        if( idx >= 0 ){
            //checkCorrectSequence(idx);
            compositionController.executeService(idx, step);
        }
        addEventToState(step);
        if(shouldPlot) refreshPlot();
        return idx;
    }


    public static void init(){
        // this is our composition controller
        compositionController = new CompositionController("behavior-network.json");

        // create users
        compositionController.createUsers("alice", "bob");

        // create devices
        compositionController.createDevice("bob", Device.TYPES.PHONE).setGPSturnedOn(false);
        compositionController.createDevice("bob", Device.TYPES.TABLET).setBatteryLevel(6);
        compositionController.createDevice("alice", Device.TYPES.PHONE);
        compositionController.createDevice( ADMIN, Device.TYPES.SERVER);

        // create services
        compositionController.instantiateServices(
                new Pair<>(Arrays.asList("bob", "alice"), getUserServices()),
                new Pair<>(Arrays.asList(ADMIN), getServerServices() ));

        // set system/user goals and states
        compositionController.addState(Arrays.asList("bob-party-not-organized", "alice-party-not-organized" ));
        compositionController.setGoals( Arrays.asList(  "grocery-shopping-done", "whatever" )); // "organize-party-done"
        // let's extract xxx-required preconditions
        compositionController.endMeansAnalysis();

        // create the gui
        if(shouldPlot) plot = createGUI( compositionController.getNetwork() );
    }


    private static void addEventToState(final int simulationStep) {
        if( simulationStep < SimuConstants.SimSteps.values().length ) {
            SimuConstants.SimSteps step = SimuConstants.SimSteps.values()[simulationStep];
            switch (step) {
                case S7_CLOSER_TO_GROCERY:
                    compositionController.addState(Arrays.asList("bob-grocery-shopping-not-done",
                            "alice-grocery-shopping-not-done"));
                    compositionController.addGoal("organize-party-done");
                    break;
                case S9_BOB_DO_GROCERY:
                    compositionController.removeState("alice-is-closer-to-place");
                    compositionController.removeState("bob-is-closer-to-place");
                    compositionController.removeState("alice-place-location-provided");
                    compositionController.addState(Arrays.asList("alice-place-location-required",
                            "alice-place-name-provided"));
                    break;
                case S10_ALICE_ADD_PREF:
                    compositionController.addState(Arrays.asList("alice-close-to-organic-supermarket"));
                    break;
                case S12_BOB_FIND_BEER:
                    compositionController.removeState("bob-place-location-provided");
                    compositionController.removeState("bob-grocery-shopping-required");
                    compositionController.removeState("alice-grocery-shopping-required");
                    compositionController.addState(Arrays.asList("bob-place-location-required",
                            "bob-place-name-provided",
                            "bob-beer-shopping-not-done",
                            "bob-beer-shopping-required"));
                    break;
                case S13_BOB_GO_BEER_SHOP:
                    compositionController.removeState("bob-grocery-shopping-required");
                    compositionController.removeState("alice-grocery-shopping-required");
                    compositionController.removeState("bob-grocery-shopping-not-done");
                    compositionController.addState(Arrays.asList("bob-driver-license-provided",
                            "bob-is-closer-to-place",
                            "bob-beer-shopping-not-done",
                            "bob-beer-shopping-required"));
                    break;
                case S14_BOB_FIND_HOME_DECO:
                    compositionController.removeState("bob-place-location-provided");
                    compositionController.addState(Arrays.asList("bob-place-location-required",
                            "bob-place-name-provided"));
                    break;
                case S15_BOB_GO_HOME_DECO:
                    compositionController.addState(Arrays.asList("bob-is-closer-to-place",
                            "bob-buy-decoration-required"));
                    break;
                case S15_1_BOB_MOVE_HOME_DECO:
                    compositionController.removeState("bob-is-closer-to-place");
                    compositionController.removeState("bob-buy-decoration-required");
                    break;
                case S16_ALICE_HEADACHE:
                    compositionController.removeState("bob-place-location-provided");
                    compositionController.removeState("bob-buy-decoration-required");
                    compositionController.addState(Arrays.asList(
                            "bob-place-name-provided",
                            "bob-somebody-has-headache",
                            "bob-no-medication-at-home",
                            "bob-is-closer-to-place"));
                    break;
                case S17_BOB_COUPONS:
                    compositionController.addState(Arrays.asList("bob-has-coupons"));
                    break;
                case S18_BOB_GO_HOME_DECO:
                    compositionController.addState(Arrays.asList("bob-buy-decoration-required"));
                    break;
                case S19_ALICE_GO_HOME_DECO:
                    compositionController.addState(Arrays.asList("alice-buy-decoration-required",
                            "alice-is-closer-to-place"));
                    break;
            }
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


    private static BNGUIVisualizer createGUI(BehaviorNetwork network) {
        String title = "Service Composition";
        List<Behavior> services = new ArrayList<>(network.getBehaviors());
        String[] series = new String[services.size() + 1];
        for(int i = 0; i < services.size(); i++){
            series[i] = services.get(i).getShortName();
        }
        series[series.length-1] = "Threshold";
        return BNGUIVisualizer.start(title, series, network);
    }

    private static void refreshPlot() {
        plot.setDataset(compositionController.getNormalizedActivations(),
                compositionController.getThreshold(),
                compositionController.getBehActivated(),
                compositionController.getActivationBeh(),
                compositionController.isExecutable());
    }
}