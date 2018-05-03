package edu.cmu.inmind.multiuser.controller.orchestrator.bn;

/**
 * Created by oscarr on 8/27/17.
 */
public class Premise {
    private String label;
    private double weight;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Premise(String label, double weight) {
        this.label = label;
        this.weight = weight;
    }

    @Override
    public String toString(){return label;}
}
