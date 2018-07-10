package edu.cmu.inmind.multiuser.controller.composer.group;

import edu.cmu.inmind.multiuser.controller.composer.devices.Device;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oscarr on 5/22/18.
 */
public class User {
    /** Services that runs on the cloud are under the CLOUD ownership **/
    public final static String CLOUD = "cloud";
    private String name;
    private List<Device> devices = new ArrayList<>();

    public User(String name) {
        this.name = name;
    }

    public void addDevice(Device device){
        devices.add(device);
    }

    public List<Device> getDevices() {
        return devices;
    }

    public Device getDevice(Device.TYPES deviceType){
        for(Device device : devices){
            if(device.getType().equals(deviceType)) return device;
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
