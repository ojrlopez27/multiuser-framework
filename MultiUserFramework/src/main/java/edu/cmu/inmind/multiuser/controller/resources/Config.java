package edu.cmu.inmind.multiuser.controller.resources;

import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 3/23/17.
 */
public class Config {
    private int sessionManagerPort;
    private String pathLogs;
    private int defaultNumOfPoolInstances;
    private long sessionTimeout;
    private static Config instance;
    private static String serverAddress;
    private static boolean shouldShowException;

    private Config( Builder builder) {
        this.sessionManagerPort = builder.sessionManagerPort;
        this.pathLogs = builder.pathLogs;
        this.defaultNumOfPoolInstances = builder.defaultNumOfPoolInstances;
        this.sessionTimeout = builder.sessionTimeout;
        this.serverAddress = builder.serverAddress;
        this.shouldShowException = builder.shouldShowException;
    }

    public static int getSessionManagerPort() {
        return instance.sessionManagerPort;
    }

    public static String getPathLogs() {
        return instance.pathLogs;
    }

    public static int getNumOfInstancesPool() {
        return instance.defaultNumOfPoolInstances;
    }

    public static long getSessionTimeout() {
        return instance.sessionTimeout;
    }

    public static String getServerAddress() {
        return serverAddress;
    }

    public static boolean isShouldShowException() {
        return shouldShowException;
    }

    public static class Builder{
        private int sessionManagerPort = 5555;
        private String pathLogs= "";
        private int defaultNumOfPoolInstances = 10;
        private long sessionTimeout = 1000 * 60 * 5; // set to 5 minutes by default
        private boolean shouldShowException = false;
        public String serverAddress = "127.0.0.1";

        public Builder setSessionManagerPort(int sessionManagerPort) {
            this.sessionManagerPort = sessionManagerPort;
            return this;
        }

        public Builder setPathLogs(String pathLogs) {
            this.pathLogs = pathLogs;
            return this;
        }

        public Builder setDefaultNumOfPoolInstances(int defaultNumOfPoolInstances) {
            this.defaultNumOfPoolInstances = defaultNumOfPoolInstances;
            return this;
        }

        public Config build(){
            return instance = new Config( this );
        }

        public Builder setSessionTimeout(long timeout, TimeUnit minutes) {
            this.sessionTimeout = minutes.toMillis( timeout );
            return this;
        }

        public Builder setSessionTimeout(long sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public Builder setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder setShouldShowException(boolean shouldShowException) {
            this.shouldShowException = shouldShowException;
            return this;
        }
    }

}
