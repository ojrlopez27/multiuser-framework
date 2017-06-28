package edu.cmu.inmind.multiuser.common;

/**
 * Created by oscarr on 6/27/17.
 */
public class ErrorMessages {

    public static final String NO_REMOTE_ANNOTATION = "PluggableComponent %s has not defined a ConnectRemoteService " +
            "annotation, thus no messages can be sent to remote services";
    public static final String OBJECT_NULL = "Parameter %s cannot be null nor empty!" ;
    public static final String PREFIX_NOT_MAPPED = "Message prefix %s has not been mapped with any component in the Module";
    public static final String ATTRIBUTE_NULL = "Attribute %s is null. It must be defined somewhere by using %s";
    public static final String COMP_MUTUAL_EXCLUSIVE = "PluggableComponent annotations (Stateless, Stateful and Pool) " +
            "are mutually exclusive, so you should use only one of these.";
    public static final String COMP_NO_ANNOTATIONS = "PluggableComponent component must have one of these annotations: " +
            "StatelessComponent, StatefulComponent or PoolComponent";
    public static final String SERVICE_NOT_REGISTERED = "Service %s is not registered!";
    public static final String FRAMEWORK_ALREADY_EXIST = "Framework with address: %s has been already created. Stop it " +
            "before trying to start it again.";
    public static final String USE_TCP_INSTEAD = "TCP is on so you should use method %s";
}
