package edu.cmu.inmind.multiuser.common.model;

/**
 * Created by oscarr on 3/3/17.
 */
public class SocialIntent {
    private double rapportScore;
    private String rapportLevel;
    private String userConvStrategy;

    public SocialIntent(double rapportScore, String rapportLevel, String userConvStrategy) {
        this.rapportScore = rapportScore;
        this.rapportLevel = rapportLevel;
        this.userConvStrategy = userConvStrategy;
    }

    public double getRapportScore() {
        return rapportScore;
    }

    public void setRapportScore(double rapportScore) {
        this.rapportScore = rapportScore;
    }

    public String getRapportLevel() {
        return rapportLevel;
    }

    public void setRapportLevel(String rapportLevel) {
        this.rapportLevel = rapportLevel;
    }

    public String getUserConvStrategy() {
        return userConvStrategy;
    }

    public void setUserConvStrategy(String userConvStrategy) {
        this.userConvStrategy = userConvStrategy;
    }
}
