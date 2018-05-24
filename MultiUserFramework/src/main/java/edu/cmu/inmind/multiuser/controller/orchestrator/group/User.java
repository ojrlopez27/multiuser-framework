package edu.cmu.inmind.multiuser.controller.orchestrator.group;

import edu.cmu.inmind.multiuser.controller.orchestrator.devices.Device;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oscarr on 5/22/18.
 */
public class User {
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

    public String getName() {
        return name;
    }
}
