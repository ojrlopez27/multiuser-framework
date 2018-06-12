package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

/**
 * Created by oscarr on 8/27/17.
 */
public class Premise {
    private String label;
    private double weight;
    /** Some premises are device dependant, like battery level, gps, etc. */
    private boolean dependsOnDevice;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isDependsOnDevice() {
        return dependsOnDevice;
    }

    public void setDependsOnDevice(boolean dependsOnDevice) {
        this.dependsOnDevice = dependsOnDevice;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Premise(String label, double weight, boolean dependsOnDevice) {
        this.label = label;
        this.weight = weight;
        this.dependsOnDevice = dependsOnDevice;
    }

    @Override
    public String toString(){return label;}
}
