package edu.cmu.inmind.multiuser.controller.orchestrator.services;

import edu.cmu.inmind.multiuser.controller.orchestrator.bn.Behavior;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 4/30/18.
 */
public class BobPhoneHasShopListService extends Service {
    private boolean hasShoppingList = true;

    public BobPhoneHasShopListService(Behavior behavior, CopyOnWriteArrayList<String> state){
        super(behavior, state);
    }

    @Override
    public void execute() {
        if( hasShoppingList ) {
            network.triggerPostconditions(behavior, Arrays.asList("shopping-list-provided" ), Arrays.asList("shopping-list-required" ) );
        }else {
            network.triggerPostconditions(behavior, Arrays.asList("shopping-list-required" ), Arrays.asList() );
        }
    }
}
