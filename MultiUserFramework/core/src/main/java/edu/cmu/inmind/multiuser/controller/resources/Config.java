package edu.cmu.inmind.multiuser.controller.resources;

import edu.cmu.inmind.multiuser.controller.common.Constants;
import edu.cmu.inmind.multiuser.controller.exceptions.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.log.MessageLog;
import edu.cmu.inmind.multiuser.controller.orchestrator.ProcessOrchestrator;

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
    private String pathExceptionLogger;
    private MessageLog exceptionLogger;
    private boolean executeExit;
    private boolean isTCPon;
    private String serviceConfigPath;
    private Class<? extends ProcessOrchestrator> orchestrator;
    private int numOfSockets;
    private int corePoolSize;


    private Config( Builder builder) {
        this.sessionManagerPort = builder.sessionManagerPort;
        this.pathLogs = builder.pathLogs;
        this.defaultNumOfPoolInstances = builder.defaultNumOfPoolInstances;
        this.sessionTimeout = builder.sessionTimeout;
        this.serverAddress = builder.serverAddress;
        this.exceptionTraceLevel = builder.exceptionTraceLevel;
        this.pathExceptionLogger = builder.pathExceptionLogger;
        this.exceptionLogger = builder.exceptionLogger;
        this.executeExit = builder.executeExit;
        this.isTCPon = builder.isTCPon;
        this.orchestrator = builder.orchestrator;
        this.serviceConfigPath = builder.serviceConfigPath;
        this.numOfSockets = builder.numOfSockets;
        this.corePoolSize = builder.corePoolSize;
        ExceptionHandler.setExceptionTraceLevel(this.exceptionTraceLevel);
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

    public String getPathExceptionLogger() {
        return pathExceptionLogger;
    }

    public MessageLog getExceptionLogger() {
        return exceptionLogger;
    }

    public String getServiceConfigPath() {
        return serviceConfigPath;
    }

    public boolean executeExit() {
        return executeExit;
    }

    public int getDefaultNumOfPoolInstances() {
        return defaultNumOfPoolInstances;
    }

    public boolean isExecuteExit() {
        return executeExit;
    }

    public boolean isTCPon() {
        return isTCPon;
    }

    public int getNumOfSockets() {
        return numOfSockets;
    }

    public Config setNumOfSockets(int numOfSockets) {
        this.numOfSockets = numOfSockets;
        return this;
    }

    public Class<? extends ProcessOrchestrator> getOrchestrator() {
        return orchestrator;
    }

    public Config setSessionManagerPort(int sessionManagerPort) {
        this.sessionManagerPort = sessionManagerPort;
        return this;
    }

    public Config setPathLogs(String pathLogs) {
        this.pathLogs = pathLogs;
        return this;

    }

    public Config setDefaultNumOfPoolInstances(int defaultNumOfPoolInstances) {
        this.defaultNumOfPoolInstances = defaultNumOfPoolInstances;
        return this;
    }

    public Config setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
        return this;
    }

    public Config setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
        return this;
    }

    public Config setExceptionTraceLevel(int exceptionTraceLevel) {
        Config.exceptionTraceLevel = exceptionTraceLevel;
        return this;
    }

    public Config setPathExceptionLogger(String pathExceptionLogger) {
        this.pathExceptionLogger = pathExceptionLogger;
        return this;
    }

    public Config setExceptionLogger(MessageLog exceptionLogger) {
        this.exceptionLogger = exceptionLogger;
        return this;
    }

    public Config setExecuteExit(boolean executeExit) {
        this.executeExit = executeExit;
        return this;
    }

    public Config setJsonServicesConfig(String path) {
        this.serviceConfigPath = path;
        return this;
    }

    public Config setTCPon(boolean TCPon) {
        isTCPon = TCPon;
        return this;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public Config setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    /** ========================================================== **/

    public static class Builder{
        private int sessionManagerPort = 5555;
        private String pathLogs= "";
        private int defaultNumOfPoolInstances = 10;
        private long sessionTimeout = 1000 * 60 * 5; // set to 5 minutes by default
        private int exceptionTraceLevel = Constants.SHOW_ALL_EXCEPTIONS;
        private String pathExceptionLogger;
        private MessageLog exceptionLogger;
        private boolean executeExit;
        private boolean isTCPon = true;
        private String serverAddress = "127.0.0.1";
        private String serviceConfigPath;
        private Class<? extends ProcessOrchestrator> orchestrator;
        public int numOfSockets = 1;
        private int corePoolSize = 0;

        public Builder setSessionManagerPort(int sessionManagerPort) {
            if( sessionManagerPort <= 0 ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "sessionManagerPort: "
                        + sessionManagerPort));
            }
            this.sessionManagerPort = sessionManagerPort;
            return this;
        }

        public Builder setPathLogs(String pathLogs) {
            ExceptionHandler.checkPath(pathLogs);
            this.pathLogs = pathLogs;
            return this;
        }

        public Builder setDefaultNumOfPoolInstances(int defaultNumOfPoolInstances) {
            if( defaultNumOfPoolInstances <= 0){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                        "defaultNumOfPoolInstances: " + defaultNumOfPoolInstances));
            }
            this.defaultNumOfPoolInstances = defaultNumOfPoolInstances;
            return this;
        }

        public Config build(){
            return new Config( this );
        }

        public Builder setSessionTimeout(long timeout, TimeUnit minutes) {
            if( timeout <= 0 || minutes == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                        "timeout: " + timeout, "minutes: " + minutes));
            }
            this.sessionTimeout = minutes.toMillis( timeout );
            return this;
        }

        public Builder setSessionTimeout(long sessionTimeout) {
            if( sessionTimeout <= 0){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL,
                        "timeout: " + sessionTimeout));
            }
            this.sessionTimeout = sessionTimeout;
            return this;
        }

        public Builder setServerAddress(String serverAddress) {
            ExceptionHandler.checkIpAddress(serverAddress, sessionManagerPort);
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder setExceptionTraceLevel(int exceptionTraceLevel) {
            if( exceptionTraceLevel <= 0 || exceptionTraceLevel > 4){
                ExceptionHandler.handle( new MultiuserException( ErrorMessages.UNKNOWN_EXCEPTION_TRACE_LEVEL,
                        exceptionTraceLevel) );
            }
            this.exceptionTraceLevel = exceptionTraceLevel;
            return this;
        }

        public Builder setPathExceptionLogger(String pathExceptionLogger) {
            ExceptionHandler.checkPath(pathExceptionLogger);
            this.pathExceptionLogger = pathExceptionLogger;
            return this;
        }

        public Builder setExceptionLogger(MessageLog exceptionLogger) {
            if( exceptionLogger == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "exceptionLogger: "
                        + exceptionLogger) );
            }
            this.exceptionLogger = exceptionLogger;
            return this;
        }

        public Builder setExecuteExit(boolean executeExit) {
            this.executeExit = executeExit;
            return this;
        }

        public Builder setTCPon(boolean TCPon) {
            isTCPon = TCPon;
            return this;
        }

        public Builder setOrchestrator(Class<? extends ProcessOrchestrator> orchestrator) {
            if( orchestrator == null ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "orchestrator: "
                        + orchestrator) );
            }
            this.orchestrator = orchestrator;
            return this;
        }

        public Builder setJsonServicesConfig(String path) {
            ExceptionHandler.checkPath(path);
            this.serviceConfigPath = path;
            return this;
        }

        public Builder setNumOfSockets(int numOfSockets){
            if( numOfSockets < 1 || numOfSockets > 200 ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.INCORRECT_NUM_SOCKETS) );
            }
            this.numOfSockets = numOfSockets;
            return this;
        }

        public Builder setCorePoolSize(int corePoolSize){
            if( corePoolSize < 100 ){
                ExceptionHandler.handle( new MultiuserException(ErrorMessages.INCORRECT_CORE_POOL_SIZE) );
            }
            this.corePoolSize = corePoolSize;
            return this;
        }
    }

}
