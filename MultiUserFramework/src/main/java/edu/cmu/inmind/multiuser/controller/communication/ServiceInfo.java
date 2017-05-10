package edu.cmu.inmind.multiuser.controller.communication;

/**
 * Created by oscarr on 4/4/17.
 */
public class ServiceInfo {
    private String serverAddress;
    private String serviceName;
    private String clientAddress;
    private String requestType;
    private ZMsgWrapper msgWrapper;
    private String[] msgSubscriptions;
    private ResponseListener responseListener;

    public ServiceInfo(Builder builder) {
        this.serverAddress = builder.serverAddress;
        this.serviceName = builder.serviceName;
        this.clientAddress = builder.clientAddress;
        this.requestType = builder.requestType;
        this.msgWrapper = builder.msgWrapper;
        this.msgSubscriptions = builder.msgSubscriptions;
        this.responseListener = builder.responseListener;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClientAddress() {
        return clientAddress;
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
        private String serverAddress;
        private String serviceName;
        private String clientAddress;
        private String requestType;
        private ZMsgWrapper msgWrapper;
        private String[] msgSubscriptions;
        private ResponseListener responseListener;

        public Builder setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        public Builder setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder setClientAddress(String clientAddress) {
            this.clientAddress = clientAddress;
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
