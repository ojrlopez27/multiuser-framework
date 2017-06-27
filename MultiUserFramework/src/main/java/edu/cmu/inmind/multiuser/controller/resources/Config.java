package edu.cmu.inmind.multiuser.controller.resources;

import edu.cmu.inmind.multiuser.common.Constants;

import java.util.concurrent.TimeUnit;

/**
 * Created by oscarr on 3/23/17.
 */
public class Config {
    private int sessionManagerPort;
    private String pathLogs;
    private int defaultNumOfPoolInstances;
    private long sessionTimeout;
    private String serverAddress;
    private static int exceptionTraceLevel;
    private boolean executeExit;

    private Config( Builder builder) {
        this.sessionManagerPort = builder.sessionManagerPort;
        this.pathLogs = builder.pathLogs;
        this.defaultNumOfPoolInstances = builder.defaultNumOfPoolInstances;
        this.sessionTimeout = builder.sessionTimeout;
        this.serverAddress = builder.serverAddress;
        this.exceptionTraceLevel = builder.exceptionTraceLevel;
        this.executeExit = builder.executeExit;
    }

    public int getSessionManagerPort() {
        return sessionManagerPort;
    }

    public String getPathLogs() {
        return pathLogs;
    }

    public int getNumOfInstancesPool() {
        return defaultNumOfPoolInstances;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public static int getExceptionTraceLevel() {
        return exceptionTraceLevel;
    }

    public boolean executeExit() {
        return executeExit;
    }

    public static class Builder{
        private int sessionManagerPort = 5555;
        private String pathLogs= "";
        private int defaultNumOfPoolInstances = 10;
        private long sessionTimeout = 1000 * 60 * 5; // set to 5 minutes by default
        private int exceptionTraceLevel = Constants.SHOW_ALL_EXCEPTIONS;
        private boolean executeExit;
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
            return new Config( this );
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

        public Builder setExceptionTraceLevel(int exceptionTraceLevel) {
            this.exceptionTraceLevel = exceptionTraceLevel;
            return this;
        }

        public Builder setExecuteExit(boolean executeExit) {
            this.executeExit = executeExit;
            return this;
        }
    }

}
