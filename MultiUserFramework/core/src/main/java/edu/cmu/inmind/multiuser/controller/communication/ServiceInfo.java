package edu.cmu.inmind.multiuser.controller.communication;

import edu.cmu.inmind.multiuser.controller.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;

/**
 * Created by oscarr on 4/4/17.
 * Use this class to specify information about a slave MUF, that is, a MUF that
 * receives messages from a master MUF
 */
public class ServiceInfo {
    /** URL of the master MUF */
    private String masterMUFAddress;
    /** name of the slave MUF */
    private String serviceName;
    /** URL of the slave MUF */
    private String slaveMUFAddress;
    /** connection, disconnection, etc. */
    private String requestType;
    /** wrapper of the messages between master and slave MUF's */
    private ZMsgWrapper msgWrapper;
    /** specify a list of messages that the slave MUF is subscribed to */
    private String[] msgSubscriptions;
    /** use this listener to process the responses from external components */
    private ResponseListener responseListener;

    public ServiceInfo(Builder builder) {
        this.masterMUFAddress = builder.masterMUFAddress;
        this.serviceName = builder.serviceName;
        this.slaveMUFAddress = builder.slaveMUFAddress;
        this.requestType = builder.requestType;
        this.msgWrapper = builder.msgWrapper;
        this.msgSubscriptions = builder.msgSubscriptions;
        this.responseListener = builder.responseListener;
    }

    public String getMasterMUFAddress() {
        if( masterMUFAddress == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.MASTER_ADDRESS_IS_NULL) );
        }
        return masterMUFAddress;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getSlaveMUFAddress() {
        return slaveMUFAddress;
    }

    public String getRequestType() {
        return requestType;
    }

    public ZMsgWrapper getMsgWrapper() {
        return msgWrapper;
    }

    public String[] getMsgSubscriptions() {
        return msgSubscriptions;
    }

    public ResponseListener getResponseListener() {
        return responseListener;
    }

    public static class Builder{
        private String masterMUFAddress;
        private String serviceName;
        private String slaveMUFAddress;
        private String requestType;
        private ZMsgWrapper msgWrapper;
        private String[] msgSubscriptions;
        private ResponseListener responseListener;

        public Builder setMasterMUFAddress(String masterMUFAddress) {
            this.masterMUFAddress = masterMUFAddress;
            return this;
        }

        public Builder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder setSlaveMUFAddress(String slaveMUFAddress) {
            this.slaveMUFAddress = slaveMUFAddress;
            return this;
        }

        public Builder setRequestType(String requestType) {
            this.requestType = requestType;
            return this;
        }

        public Builder setMsgWrapper(ZMsgWrapper msgWrapper) {
            this.msgWrapper = msgWrapper;
            return this;
        }

        public Builder setMsgSubscriptions(String[] msgSubscriptions) {
            this.msgSubscriptions = msgSubscriptions;
            return this;
        }

        public Builder setResponseListener(ResponseListener responseListener) {
            this.responseListener = responseListener;
            return this;
        }

        public ServiceInfo build(){
            return new ServiceInfo( this );
        }
    }
}
