package edu.cmu.inmind.multiuser.controller.composer.services;

import edu.cmu.inmind.multiuser.controller.composer.bn.Behavior;
import edu.cmu.inmind.multiuser.controller.composer.bn.BehaviorNetwork;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by oscarr on 4/26/18.
 */
public abstract class Service {
    protected Behavior behavior; // each behavior maps one and only one service
    protected ConcurrentSkipListSet<String> state; //we need state to update changes on it
    protected BehaviorNetwork network;
    protected String deviceName;
    protected String user;

    public Service(){}

    public Service(String deviceName, Behavior behavior, ConcurrentSkipListSet<String> state){
        this.behavior = behavior;
        this.state = state;
        this.network = behavior.getNetwork();
        this.deviceName = deviceName;
        this.user = deviceName.split(Behavior.TOKEN)[0];
    }

    /**
     * This method executes whatever the service is supposed to do, according to the simulation step (which
     * can be ignored for some services).
     * @param simulationStep
     * @return it returns whether the service performed any action. For instance, if simulationStep condition
     * is not met, then it will not perform any action and will return false, otherwise true.
     */
    public abstract boolean execute(int simulationStep);

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    public static Service getService(Behavior behavior, String name, ConcurrentSkipListSet<String> state) {
        behavior.setUserName(name.substring(0, name.contains(Behavior.TOKEN)? name.indexOf(Behavior.TOKEN) : name.length()));
        if( behavior.getName().equals("get-self-location") ) return new LocationService(name, behavior, state);
        else if( behavior.getName().equals("find-place-location") ) return new FindPlaceService(name, behavior, state);
        else if( behavior.getName().equals("get-distance-to-place") ) return new DistanceCalculatorService(name, behavior, state);
        else if( behavior.getName().equals("calculate-nearest-place") ) return new WhoIsNearestService(name, behavior, state);
        else if( behavior.getName().equals("share-grocery-list") ) return new ShareGroceryListService(name, behavior, state);
        else if( behavior.getName().equals("do-grocery-shopping") ) return new DoGroceryShoppingService(name, behavior, state);
        else if( behavior.getName().equals("do-beer-shopping") ) return new DoBeerShoppingService(name, behavior, state);
        else if( behavior.getName().equals("go-home-decor") ) return new GoHomeDecoService(name, behavior, state);
        else if( behavior.getName().equals("organize-party") ) return new OrganizePartyService(name, behavior, state);
        else if( behavior.getName().equals("go-pharmacy") ) return new GoPharmacyService(name, behavior, state);
        return null;
    }
}
