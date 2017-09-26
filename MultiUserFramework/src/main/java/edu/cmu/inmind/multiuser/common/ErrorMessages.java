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
    public static final String NO_RESPONSE_FROM_SERVER = "Server %s is not responding to connection request after %s " +
            "millisenconds. Please check your services.json file and see whether connection details are correct and " +
            "whether your remote server is running.";
    public static final String FILE_NOT_EXISTS = "Path: %s does not exist or is incorrect.";
    public static final String BLACKBOARD_ELEMENT_NULL = "You are posting a null object through the Blackboard. Please " +
            "check that";
    public static final String BLACKBOARD_KEY_NULL = "You are posting a null message id through the Blackboard. Please " +
            "check that";
    public static final String NOBODY_IS_SUBSCRIBED = "There are no components subscribed to message: %s. Noboby will " +
            "listen to you! Check your services.json file or the @BlackboardSubscription annotation";
    public static final String PAIR_VALUE_NULL = "One (or both) of the Pair variables is null. var1: &s, var2: %s";
    public static final String COMPONENTS_NULL = "The list of components is empty or null.";
    public static final String BLACKBOARD_SUBSCRIBER_NULL = "Blackboard subscriber is null";

    public static final String BLACKBOARD_MESSAGES_NULL = "The list of subscription messages is empty or null";
    public static final String BLACKBOARD_SUBSCRIBERS_NULL = "There are no blackboard subscribers for message: %s";
    public static final String BLACKBOARD_SENDER_NULL = "Blackboard sender is null";
    public static final String BLACKBOARD_EVENT = "Blackboard event contains a null or empty parameter. status: %, " +
            "id: %s, element: %s";
    public static final String NO_SUBSCRIPTION_MESSSAGES = "The service '%s' has no subscription messages. Please " +
            "check your services.json file";
    public static final String ANY_ELEMENT_IS_NULL = "One or more of these elements is/are null (or empty): %s, %s, %s";
    public static final String INCORRECT_IP_ADDRESS = "The ip address for %s is incorrect: %s";
    public static final String UNKNOWN_EXCEPTION_TRACE_LEVEL = "Unknown Exception trace level: %s. It should be a value " +
            "between 1-4";
    public static final String DUPLICATED_SERVICE_DEFINITION = "The service %s has been defined in multiple places. " +
            "Check your services.json file or any annotation ConnectRemoteService, you can only use one method for services" +
            "(services.json is preferred)";
    public static final String SERVICES_FILE_EMPTY = "The json file that contains remote service configuration " +
            "(e.g., services.json, on path: %s) is empty. Check whether the json attribute \"services\": is empty";
}
