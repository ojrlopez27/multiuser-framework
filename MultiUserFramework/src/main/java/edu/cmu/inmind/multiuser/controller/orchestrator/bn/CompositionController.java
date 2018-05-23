package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

import edu.cmu.inmind.multiuser.controller.common.Utils;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.Device;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.PhoneDevice;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.ServerDevice;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.TabletDevice;
import edu.cmu.inmind.multiuser.controller.orchestrator.group.User;

import java.util.*;

/**
 * Created by oscarr on 5/22/18.
 */
public class CompositionController {
    private BehaviorNetwork network;
    private List<Device> devices;
    private HashMap<String, Behavior> serviceMap;
    private HashMap<Behavior, Device> deviceServiceMap;
    private HashMap<String, User> usersMap;
    private HashMap<String, List<String>> behaviorsActivatedByUser;


    public CompositionController(String bnJsonFile){
        try {
            network = Utils.fromJsonFile(bnJsonFile, BehaviorNetwork.class);
            serviceMap = network.map();
            devices = new ArrayList<>();
            usersMap = new HashMap<>();
            behaviorsActivatedByUser = new HashMap<>();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void createUsers(String... names){
        for(String name : names){
            usersMap.put(name, new User(name));
        }
    }

    public Device createDevice(String userName, Device.TYPES type) {
        String deviceName = userName + "-" + type.toString().toLowerCase();
        Device device = type.equals( Device.TYPES.PHONE )? new PhoneDevice( deviceName, network )
                : type.equals( Device.TYPES.TABLET )? new TabletDevice( deviceName, network )
                : new ServerDevice("server", network);
        devices.add( device );
        usersMap.get(userName).addDevice(device);
        return device;
    }

    /**
     * User abstract behaviors to instantiate grounded behaviors. Remove abstract behaviors
     * from network, and add newly created grounded behaviors.
     */
    public void instantiateServices(String... services) {
        Behavior[] behaviors = new Behavior[services.length];
        for(int i = 0; i < services.length; i++){
            behaviors[i] = serviceMap.get(services[i]);
        }
        for(Device device : devices ){
            device.addServices( behaviors );
        }
        // let's remove all abstract behaviors
        network.getBehaviors().clear();

        // let's add grounded behaviors
        for(Device device : devices ){
            network.addBehaviors( device.getBehaviors() );
        }
        serviceMap = network.map();
        deviceServiceMap = generateMap();
    }

    public void endMeansAnalysis() {
        network.endMeansAnalysis();
    }

    public void setGoals(List<String> goals) {
        network.setGoals(goals);
    }

    public boolean hasMoreGoals() {
        return network.getGoals().isEmpty();
    }

    public void updateDeviceState() {
        for(Device device : devices){
            device.updateState();
        }
    }

    public int selectBehavior() {
        return network.selectBehavior();
    }

    public void executeBehavior(int idx) {
        Behavior selectedBehavior = network.getBehaviors().get(idx);
        deviceServiceMap.get(selectedBehavior).executeService(selectedBehavior.getName());
        // id[0] = user id, id[1] = abstract behavior
        String[] id = selectedBehavior.getName().split(Behavior.TOKEN);
        String user = id[0].split("-")[0];
        List<String> behaviorsActivated = behaviorsActivatedByUser.get(user);
        if( behaviorsActivated == null ) behaviorsActivated = new ArrayList<>();
        if( !behaviorsActivated.contains(id[1]) ) behaviorsActivated.add( id[1] );
        behaviorsActivatedByUser.put(user, behaviorsActivated);
    }

    private HashMap<Behavior, Device> generateMap() {
        deviceServiceMap = new HashMap<>();
        for(Device device : devices){
            for(Behavior behavior : device.getBehaviors()){
                deviceServiceMap.put(behavior, device);
            }
        }
        return deviceServiceMap;
    }

    public List<String> nextPlausibleBehavior() {
        List<Behavior> behaviors = network.getBehaviorsSorted();
        for(int i = 0; i < behaviors.size(); i++){
            Behavior beh = behaviors.get(i);
            String[] id = beh.getName().split(Behavior.TOKEN);
            String user = id[0].split("-")[0];
            if( !behaviorsActivatedByUser.get(user).contains(id[1]) ){
                List<Premise> missing = beh.getMissingStates();
                List<String> missingStr = new ArrayList();
                for(Premise premise : missing){
                    missingStr.add(premise.getLabel());
                }
                return missingStr;
            }
        }
        return null;
    }

    public void addState(String state, String value) {
        network.getState().add(state);
        //TODO: we need to do something with the value, like put it on the WM for further processing
    }
}
