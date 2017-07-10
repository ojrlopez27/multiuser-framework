package edu.cmu.inmind.multiuser.common.model;

/**
 * Created by oscarr on 3/7/17.
 */
public class SaraInput {
    private String ASRinput;
    private boolean isSmiling;
    private boolean isGazeAtPartner;
    private boolean isHeadNod;

    public String getASRinput() {
        return ASRinput;
    }

    public void setASRinput(String ASRinput) {
        this.ASRinput = ASRinput;
    }

    public boolean isSmiling() {
        return isSmiling;
    }

    public void setIsSmiling(boolean isSmiling) {
        this.isSmiling = isSmiling;
    }

    public boolean isGazeAtPartner() {
        return isGazeAtPartner;
    }

    public void setIsGazeAtPartner(boolean isGazeAtPartner) {
        this.isGazeAtPartner = isGazeAtPartner;
    }

    public boolean isHeadNod() {
        return isHeadNod;
    }

    public void setIsHeadNod(boolean isHeadNod) {
        this.isHeadNod = isHeadNod;
    }

}
