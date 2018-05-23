package edu.cmu.inmind.multiuser.controller.orchestrator.services;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 4/30/18.
 */
public class AlicePhoneHasShopListService extends Service {
    private boolean hasShoppingList = true;

    public AlicePhoneHasShopListService(String deviceName, Behavior behavior, CopyOnWriteArrayList<String> state){
        super(deviceName, behavior, state);
    }

    @Override
    public void execute() {
        if( hasShoppingList ) {
            network.triggerPostconditions(behavior, Arrays.asList("shopping-list-provided" ), Arrays.asList("shopping-list-required" ) );
        }else {
            network.triggerPostconditions(behavior, Arrays.asList("shopping-list-required" ), new ArrayList<String>());
        }
    }
}
