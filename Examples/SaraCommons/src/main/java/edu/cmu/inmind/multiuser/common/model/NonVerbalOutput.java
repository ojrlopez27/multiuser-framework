package edu.cmu.inmind.multiuser.common.model;

/**
 * Created by oscarr on 3/3/17.
 */
public class NonVerbalOutput {
    private boolean smiling;
    private boolean gazeAtPartner;

    public boolean isSmiling() {
        return smiling;
    }

    public void setSmiling(boolean smiling) {
        this.smiling = smiling;
    }

    public boolean isGazeAtPartner() {
        return gazeAtPartner;
    }

    public void setGazeAtPartner(boolean gazeAtPartner) {
        this.gazeAtPartner = gazeAtPartner;
    }
}
