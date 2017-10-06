package edu.cmu.inmind.multiuser.controller.session;

import edu.cmu.inmind.multiuser.common.ErrorMessages;
import edu.cmu.inmind.multiuser.controller.communication.ServiceInfo;
import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import edu.cmu.inmind.multiuser.controller.exceptions.MultiuserException;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;

/**
 * Created by oscarr on 3/14/17.
 * This class is used to control a remote service that connects to the MUF. Usually, this remote service
 * behaves as a local component, that's the reason why it has a PluggableComponent instance.
 *
 */
public class ServiceComponent{
    private Class<? extends PluggableComponent> component;
    private String serviceURL;
    private ZMsgWrapper msgTemplate;
    private String[] subMessages;
    private ServiceInfo serviceInfo;

    public ServiceComponent(Class<? extends PluggableComponent> component, ServiceInfo serviceInfo, ZMsgWrapper msgTemplate) {
        if( serviceInfo == null || serviceInfo.getSlaveMUFAddress() == null || serviceInfo.getSlaveMUFAddress().isEmpty()){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "serviceInfo: " + serviceInfo,
                    "url: " + serviceInfo.getSlaveMUFAddress()) );
        }
        this.serviceInfo = serviceInfo;
        this.component = component;
        this.serviceURL = serviceInfo.getSlaveMUFAddress();
        this.msgTemplate = msgTemplate;
    }

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public ServiceComponent setServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
        setServiceURL( serviceInfo.getSlaveMUFAddress() );
        return this;
    }

    public ServiceComponent setServiceURL(String serviceURL) {
        if(  serviceURL == null || serviceURL.isEmpty()){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "serviceURL: " + serviceURL) );
        }
        this.serviceURL = serviceURL;
        return this;
    }

    public ServiceComponent setComponent(Class<? extends PluggableComponent> component) {
        if(  component == null ){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "component: " + component) );
        }
        this.component = component;
        return this;
    }

    public Class<? extends PluggableComponent> getComponent() {
        return component;
    }

    public String getServiceURL() {
        return serviceURL;
    }

    public ZMsgWrapper getMsgTemplate() {
        return msgTemplate;
    }

    public void setMsgTemplate(ZMsgWrapper msgTemplate) {
        this.msgTemplate = msgTemplate;
    }

    public void setSubMessages(String[] subMessages) {
        if(  subMessages == null || subMessages.length <= 0){
            ExceptionHandler.handle( new MultiuserException(ErrorMessages.ANY_ELEMENT_IS_NULL, "subMessages: " + subMessages) );
        }
        this.subMessages = subMessages;
    }

    public String[] getSubMessages() {
        return subMessages;
    }
}