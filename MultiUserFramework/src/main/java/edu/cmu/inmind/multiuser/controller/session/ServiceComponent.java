package edu.cmu.inmind.multiuser.controller.session;

import edu.cmu.inmind.multiuser.controller.communication.ZMsgWrapper;
import edu.cmu.inmind.multiuser.controller.plugin.PluggableComponent;

/**
 * Created by oscarr on 3/14/17.
 */
public class ServiceComponent{
    private Class<? extends PluggableComponent> component;
    private String serviceURL;
    private ZMsgWrapper msgTemplate;
    private String[] subMessages;

    public ServiceComponent(Class<? extends PluggableComponent> component, String url, ZMsgWrapper msgTemplate) {
        this.component = component;
        this.serviceURL = url;
        this.msgTemplate = msgTemplate;
    }

    public ServiceComponent setServiceURL(String serviceURL) {
        this.serviceURL = serviceURL;
        return this;
    }

    public ServiceComponent setComponent(Class<? extends PluggableComponent> component) {
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
        this.subMessages = subMessages;
    }

    public String[] getSubMessages() {
        return subMessages;
    }
}