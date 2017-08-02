package edu.cmu.inmind.multiuser.controller.communication;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by oscarr on 8/1/17.
 */
public class ServiceInfoContainer {
    private List<ServiceInfo> services = new ArrayList<>();


    public List<ServiceInfo> getServices() {
        return services;
    }

    public void setServices(List<ServiceInfo> services) {
        this.services = services;
    }
}
