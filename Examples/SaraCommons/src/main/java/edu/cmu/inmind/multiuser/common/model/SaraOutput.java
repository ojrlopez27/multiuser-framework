package edu.cmu.inmind.multiuser.common.model;


import java.util.ArrayList;

/**
 * Created by oscarr on 3/3/17.
 */
public class SaraOutput {
    private VerbalOutput verbal;
    private NonVerbalOutput nonVerbal;
    private SocialIntent socialIntent;
    private String status = "";
    private UserIntent userIntent;
    private String systemIntent;

    public SaraOutput() {
        verbal = new VerbalOutput("", "");
        nonVerbal = new NonVerbalOutput();
        socialIntent = new SocialIntent(0, "", "");
        userIntent = new UserIntent("", new ArrayList<String>());
    }

    public VerbalOutput getVerbal() {
        return verbal;
    }

    public void setVerbal(VerbalOutput verbal) {
        this.verbal = verbal;
    }

    public NonVerbalOutput getNonVerbal() {
        return nonVerbal;
    }

    public void setNonVerbal(NonVerbalOutput nonVerbal) {
        this.nonVerbal = nonVerbal;
    }

    public SocialIntent getSocialIntent() {
        return socialIntent;
    }

    public void setSocialIntent(SocialIntent socialIntent) {
        this.socialIntent = socialIntent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserIntent getUserIntent() {
        return userIntent;
    }

    public void setUserIntent(UserIntent userIntent) {
        this.userIntent = userIntent;
    }

    public String getSystemIntent() {
        return systemIntent;
    }

    public void setSystemIntent(String systemIntent) {
        this.systemIntent = systemIntent;
    }

}
