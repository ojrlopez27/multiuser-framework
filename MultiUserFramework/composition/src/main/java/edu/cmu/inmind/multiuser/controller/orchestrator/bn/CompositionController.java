package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

import edu.cmu.inmind.multiuser.controller.common.CommonUtils;
import edu.cmu.inmind.multiuser.controller.common.Pair;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.Device;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.PhoneDevice;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.ServerDevice;
import edu.cmu.inmind.multiuser.controller.orchestrator.devices.TabletDevice;
import edu.cmu.inmind.multiuser.controller.orchestrator.group.User;

import java.util.*;

import static edu.cmu.inmind.multiuser.controller.orchestrator.devices.Device.SERVER;
import static edu.cmu.inmind.multiuser.controller.orchestrator.group.User.ADMIN;

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
    private List<String> users;
    private List<Double>[] activations;


    public CompositionController(String bnJsonFile){
        try {
            network = CommonUtils.fromJsonFile(bnJsonFile, BehaviorNetwork.class);
            serviceMap = network.map();
            devices = new ArrayList<>();
            usersMap = new HashMap<>();
            behaviorsActivatedByUser = new HashMap<>();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void createUsers(String... names){
        users = new ArrayList<>();
        for(String name : names){
            usersMap.put(name, new User(name));
            users.add(name);
        }
        usersMap.put(ADMIN, new User(ADMIN));
    }

    public Device createDevice(String userName, Device.TYPES type) {
        String deviceName = userName + Behavior.TOKEN + type.toString().toLowerCase();
        Device device = type.equals( Device.TYPES.PHONE )? new PhoneDevice( deviceName, network, userName )
                : type.equals( Device.TYPES.TABLET )? new TabletDevice( deviceName, network, userName )
                : new ServerDevice(SERVER, network, ADMIN, usersMap.values());
        devices.add( device );
        usersMap.get(userName).addDevice(device);
        return device;
    }

    /**
     * User abstract behaviors to instantiate grounded behaviors. Remove abstract behaviors
     * from network, and add newly created grounded behaviors.
     */
    public void instantiateServices(Pair<List<String>, List<String>>... mappings) {
        // let's remove all abstract behaviors
        network.getBehaviors().clear();

        for(Pair mapping : mappings) {
            List<String> services = (List<String>) mapping.snd;
            List<Behavior> behaviors = new ArrayList<>();
            for (String service : services) {
                behaviors.add(serviceMap.get(service));
            }
            for(String user : (List<String>)mapping.fst) {
                for (Device device : usersMap.get(user).getDevices()) {
                    // let's install the services (behaviors) to each device
                    if(user.equals(ADMIN)){
                        Map<String, String> userMappings = new HashMap<>();
                        int idx = 1;
                        for(User userObj : usersMap.values()){
                            if(!userObj.getName().equals(ADMIN)){
                                userMappings.put("user" + (idx), userObj.getName() );
                                idx++;
                            }
                        }
                        device.addServices(behaviors, userMappings);
                    }else{
                        device.addServices(behaviors);
                    }
                    // let's add grounded behaviors created in previous step
                    network.addBehaviors(device.getBehaviors());
                }
            }
        }
        network.sortBehaviorsByName();
        serviceMap = network.map();
        deviceServiceMap = generateMap();
        activations = new List[serviceMap.values().size()];
        for(int i = 0; i < activations.length; i++){
            activations[i] = new ArrayList<>();
        }
    }

    public void endMeansAnalysis() {
        network.endMeansAnalysis( users );
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
        int beh = network.selectBehavior();
        for(int i = 0; i < network.getBehaviors().size(); i++){
            Behavior behavior = network.getBehaviors().get(i);
            if(beh > -1) activations[i].add( behavior.getActivation() );
        }
        return beh;
    }

    public boolean executeBehavior(int idx, int simulationStep) {
        // executing service
        Behavior selectedBehavior = network.getBehaviors().get(idx);
        boolean performed = deviceServiceMap.get(selectedBehavior)
                .executeService(selectedBehavior.getName(), simulationStep);

        // keep a record of those (abstract) services that have been activated per user so
        // we don't activate them in the future (e.g., if location is executed by phone, we
        // don't need it to be re-calculated by tablet.
        String[] id = splitBehName(selectedBehavior.getName());
        List<String> behaviorsActivated = behaviorsActivatedByUser.get(id[0]);
        if( behaviorsActivated == null ) behaviorsActivated = new ArrayList<>();
        if( !behaviorsActivated.contains(id[2]) ) behaviorsActivated.add( id[2] );
        behaviorsActivatedByUser.put(id[0], behaviorsActivated);
        return performed;
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

    /**
     * The BN may loop when it doesn't find enough sates that trigger preconditions of any behavior.
     * In order to avoid this issue, we will find a behavior which is the next most plausible behavior
     * to be activated in terms of highest activation (except those behaviors that have been already
     * activated). Once a plausible behavior is found, then we seek for those preconditions that are
     * missing from the current state, and return them. The external caller may decide whether to ask
     * the user for these missing conditions, auto-generate them, or any other strategy.
     * @return
     */
    public List<String> nextPlausibleBehavior() {
        List<String> missingStr = new ArrayList();
        List<Behavior> behaviors = network.getBehaviorsSorted();
        for(Behavior beh : behaviors){
            String[] id = splitBehName(beh.getName());
            if( behaviorsActivatedByUser.get(id[0]) == null
                    || !behaviorsActivatedByUser.get(id[0]).contains(id[2]) ){
                List<Premise> missing = beh.getMissingStates();
                for(Premise premise : missing){
                    missingStr.add(premise.getLabel());
                }
                break;
            }
        }
        return missingStr;
    }

    /**
     * We need to split behavior's name into three sections: e.g., bob-phone-get-self-location:
     * result[0]:    user name -> bob
     * result[1]:    device type -> phone
     * result[2]:    service name -> get-self-location
     * @param behName
     * @return
     */
    private String[] splitBehName(String behName){
        String[] result = new String[3];
        for(int i = 0, fromIndex = 0; i < 3; i++ ){
            int pos = behName.indexOf(Behavior.TOKEN, fromIndex);
            if( pos != -1 ) {
                result[i] = i == 2? behName.substring(fromIndex) : behName.substring(fromIndex, pos);
                fromIndex = pos+1;
            }
            else break;
        }
        return result;
    }

    public void addState(List<String> states) {
        network.setState(states);
    }

    public void removeState(String state) {
        network.getState().remove(state);
    }

    public List<Behavior> getServices() {
        return new ArrayList<>( network.getBehaviors() );
    }

    public List<Double>[] getNormalizedActivations() {
        double highest = network.getHighestActivation();
        double currentBehActivation = getActivationBeh();
        int idxActivated = network.getIdxBehActivated();
        if( highest > currentBehActivation ) {
            double belowActivated = currentBehActivation * 0.9;
            for (int i = 0; i < activations.length; i++) {
                Double lastActivation = activations[i].remove(activations[i].size() - 1);
                if (i != idxActivated)
                    lastActivation = lastActivation * belowActivated / highest;
                activations[i].add(lastActivation);
            }
        }
        return activations;
    }

    public double getThreshold() {
        return network.getTheta();
    }

    public String getBehActivated() {
        if( network.getIdxBehActivated() == -1) return null;
        return network.getBehaviorActivated().getShortName();
    }

    public double getActivationBeh() {
        if(network.getIdxBehActivated() == -1) throw new IllegalStateException("There should be a winner behavior");
        return network.getBehaviorActivated().getActivation();
    }

    public void addGoal(String goal) {
        network.getGoals().add(goal);
    }
}