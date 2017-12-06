package edu.cmu.inmind.multiuser.controller.common;

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
            "make sure you include the message id (also, external services that are included in your services.json may " +
            "return messages with empty/null message ids).";
    public static final String NOBODY_IS_SUBSCRIBED = "There are no components/orchestrators subscribed to message: %s. " +
            "Check your services.json file OR make sure your components have the @BlackboardSubscription annotation";
    public static final String PAIR_VALUE_NULL = "One (or both) of the Pair variables is null. var1: &s, var2: %s";
    public static final String COMPONENTS_NULL = "The list of components is empty or null.";
    public static final String BLACKBOARD_SUBSCRIBER_NULL = "Blackboard subscriber is null";

    public static final String BLACKBOARD_MESSAGES_NULL = "The list of subscription messages is empty or null";
    public static final String BLACKBOARD_SENDER_NULL = "Blackboard sender is null";
    public static final String BLACKBOARD_EVENT = "Blackboard event contains a null or empty parameter. status: %, " +
            "id: %s, element: %s, sessionId: %s";
    public static final String NO_BLACKBOARD = "There is no blackboard associated to this session id: %s. Try using" +
            "addBlackboard() method first";
    public static final String NO_SUBSCRIPTION_MESSSAGES = "The service '%s' has no subscription messages. Please " +
            "check your services.json file";
    public static final StringBuffer ANY_ELEMENT_IS_NULL = new StringBuffer("One or more of these elements is/are null (or empty):");
    public static final String INCORRECT_IP_ADDRESS = "The ip address: %s is incorrect. The correct format is: tcp://xxx.xxx.xxx.xxx";
    public static final String UNKNOWN_EXCEPTION_TRACE_LEVEL = "Unknown Exception trace level: %s. It should be a value " +
            "between 1-4";
    public static final String DUPLICATED_SERVICE_DEFINITION = "The service %s has been defined in multiple places. " +
            "Check your services.json file or any annotation ConnectRemoteService, you can only use one method for services" +
            "(services.json is preferred)";
    public static final String SERVICES_FILE_EMPTY = "The json file that contains remote service configuration " +
            "(e.g., services.json, on path: %s) is empty. Check whether the json attribute \"services\": is empty";
    public static final String MASTER_ADDRESS_IS_NULL = "You have to specify the ip address for the master MUF " +
            "when creating the ServiceInfo object.";
    public static final String NOT_EXPECTED_DESTROYED_OBJ = "The destroyed object has an unexpected type. Received: %s, Expected: %s";
    public static final String INCORRECT_NUM_SOCKETS = "A valid number of sockets should be a value between 1 - 200";
    public static final String INCORRECT_CORE_POOL_SIZE = "A minimum of 100 threads is required to properly execute the MUF";
    public static final String SESSION_MESSAGE_IS_EMPTY = "you are passing an empty string message when calling your " +
            "process orchestrator's sendResponse method. You should send a non-empty string message.";
    public static final String MASTER_SLAVE_NOT_NULL = "Both Master and Slave MUF address cannot be null nor empty. " +
            "Current values are: %s and %s.";
    public static final String CLIENT_NOT_CONNECTED = "The client is still connecting, you cannot send messages right now. " +
            "If you are sending a message inside the ResponseListener.process method, then check you are not sending a " +
            "message when you just receive a SESSION_INITIATED message. You can only send messages when the message received " +
            "is different from SESSION_INITIATED (if sending from inside ResponseListener) or when you do it outside the " +
            "ResponseListener.process().";
}
