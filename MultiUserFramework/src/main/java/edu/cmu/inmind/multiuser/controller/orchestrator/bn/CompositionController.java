package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

import edu.cmu.inmind.multiuser.controller.common.Pair;
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
    public final static String SERVER = "server";


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
        usersMap.put(SERVER, new User(SERVER));
    }

    public Device createDevice(String userName, Device.TYPES type) {
        String deviceName = userName + "-" + type.toString().toLowerCase();
        Device device = type.equals( Device.TYPES.PHONE )? new PhoneDevice( deviceName, network, userName )
                : type.equals( Device.TYPES.TABLET )? new TabletDevice( deviceName, network, userName )
                : new ServerDevice(SERVER, network, usersMap.values());
        devices.add( device );
        usersMap.get(userName).addDevice(device);
        return device;
    }

    /**
     * User abstract behaviors to instantiate grounded behaviors. Remove abstract behaviors
     * from network, and add newly created grounded behaviors.
     */
    public void instantiateServices(Pair... mappings) {
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
                    if(user.equals(SERVER)){
                        Map<String, String> userMappings = new HashMap<>();
                        int idx = 1;
                        for(User userObj : usersMap.values()){
                            if(!userObj.getName().equals(SERVER)){
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
        String[] id = splitBehName(selectedBehavior.getName());
        List<String> behaviorsActivated = behaviorsActivatedByUser.get(id[0]);
        if( behaviorsActivated == null ) behaviorsActivated = new ArrayList<>();
        if( !behaviorsActivated.contains(id[2]) ) behaviorsActivated.add( id[2] );
        behaviorsActivatedByUser.put(id[0], behaviorsActivated);
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

    private String[] splitBehName(String behName){
        String[] result = new String[3];
        // id[0] = user id, id[1] = abstract behavior
        String[] id = behName.contains(Behavior.TOKEN)? behName.split(Behavior.TOKEN)
                : new String[]{SERVER + "-server", behName};
        String[] user = id[0].split("-");
        result[0] = user[0];
        result[1] = user[1];
        result[2] = id[1];
        return result;
    }

    public void addState(String state, String value) {
        network.getState().add(state);
        //TODO: we need to do something with the value, like put it on the WM for further processing
    }
}
