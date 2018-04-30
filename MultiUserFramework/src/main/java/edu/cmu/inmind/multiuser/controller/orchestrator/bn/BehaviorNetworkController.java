package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by oscarr on 4/28/16.
 */
public class BehaviorNetworkController {
    protected BehaviorNetwork network;
//    protected List<String> states = new Vector<>();
    protected CopyOnWriteArrayList<String> states = new CopyOnWriteArrayList<>();
    protected List<String> goals = new Vector<>();
    protected List<Behavior> modules = new Vector<>();
    protected int NUM_BEHAVIORS;
    protected int NUM_VARIABLES;
    protected String title;
    protected String[] series;

    public BehaviorNetworkController() {}

    protected String name;

    public BehaviorNetwork getBN() {
        return network;
    }

    public void setBN(BehaviorNetwork bn) {
        this.network = bn;
    }

    public BehaviorNetwork createBN(){
        return null;
    }

    public String[] getSeries() {
        return series;
    }

    public String getTitle() {
        return title;
    }

    public String getName(){
        return name;
    }

    public String extractState( String state ){return null;};

    public String removeState( String state ){return null;}

    public String getStates() {
        String stateString = "";
        for( String state : states ){
            stateString += state + ":";
        }
        return stateString;
    }

    public String getDelList(){
        String deleteString = "";
        try {
            if (network.isRemovePrecond()) {
                for (List<Premise> preconds : network.getModules().get(network.getIdxBehActivated()).getPreconditions()) {
                    for (Premise precond : preconds) {
                        deleteString += precond.getLabel() + ":";
                    }
                }
            } else {
                List<String> deleteList = network.getModules().get(network.getIdxBehActivated()).getDeleteList();
                for (String state : deleteList) {
                    deleteString += state + ":";
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return deleteString;
    }

    public String getAddList(){
        List<String> addList = network.getModules().get( network.getIdxBehActivated() ).getAddList();
        String addString = "";
        for( String state : addList ){
            if( !state.contains("_history") ) {
                addString += state + ":";
            }
        }
        return addString;
    }

    public boolean isOverThreshold(){
        double threshold = network.getTheta();
        for(double activation : network.getActivations()){
            if( activation > threshold){
                return true;
            }
        }
        return false;
    }

    public CopyOnWriteArrayList<String> getStatesList() {
        return network.getState();
    }

    public BehaviorNetwork getNetwork() {
        return network;
    }

    public int getSize() {
        return network.getModules().size();
    }
}
