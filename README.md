# What MUF is?

The MUF is a framework that allows developers to easily scale their mono-user architectures to multi-user architectures with little effort. The MUF is written in Java (but it supports interaction with almost any programming language thanks to its communication layer that uses ZeroMQ). The most relevant features of the MUF are:

* **Session Management:** a Session Manager automatically creates a new session every time a client connects and then manages its lifecycle (i.e., connection, disconnection, control of inactivity, resources management, etc.) 

* **Low latency:** a whole communication action (i.e., to send a message from the client and receive a response from the server) takes around 10-13 ms in total. This test was made using my own computer (MacBook Pro 2.5 GHz Intel core i7, 16 GB 1600 MHz DDR) but it could be improved if you use an AWS instance. These latency range were the same even when the framework was tested with 1,000 clients. 

* **Robustness and Reliability:** the MUF supports error handling (e.g., crash errors trigger specific system recovery actions), disconnection and automatic reconnection (if network fails or communication freezes or gets locked), management of queue overflows and memory leaks, data loss prevention (when a client disconnects due to an unexpected reason, the system will keep messages in memory until the client reconnects or a timeout is met), gracefully shutdown (if system crashes and cannot be restarted, all resources such as sockets, queues, shared objects, DB, etc. are closed and released before the system exits). 

* **Simple:** all the communication complexity is hidden, that is, you don’t have to deal with details about sockets, http requests, ports, etc. You just call functions of other modules and components as if they were local objects, regardless the fact they may be services running remotely. 

* **Pluggable Architecture:** the MUF allows you to define pluggable modules that can be added anytime and using different mechanisms, so you can easily evolve your system over time and make it more flexible to changes. There are two kind of pluggable modules: Orchestrators that are in charge of arbitrating the internal processing of each request message from a client, and PluggableComponents that are your domain-specific modules (e.g., in a dialogue system domain, PluggableComponents may be ASR, NLG, NLU, DM, etc.) 

* **Component state:** you can define your components’ behavior as simple as adding an “annotation” to your code. Your components may be Stateful (it keeps an internal representation or model so a new component is created for each session), Stateless (no representation of the state, so a component is shared by multiple sessions) or Pool (you define a maximum number of instances to be created of your component). 

* **Blackboard:** the MUF uses a Blackboard system, a common knowledge base that is iteratively updated by a diverse group of specialist knowledge sources: the Pluggable modules. Blackboard updates all the listeners that are subscribed to specific messages that are posted by other components. Also, the blackboard keeps a history of all interactions between components (components never communicate directly to each other but through the Blackboard) so components may extract past messages that are stored in the Blackboard.

* **Automatic Scalability:** thanks to the MUF design and the communication patterns it uses (e.g, PUB-SUB, REQ-RESP, ROUTER-DEALER, etc.) your system can easily scale from 1 client to thousands without any extra effort. Now, if you use Amazon Lambda server (AWS) as well, then this scalability feature will be improved even further. MUF’s Philosophy: you write your app for one client and run it for thousands. 

* **User Model:** you can conveniently create your customized user models and store them on disc. A User Model component filters messages that go through the Blackboard and then extracts specific information from these messages (e.g., user preferences and interests). 

* **Sync and Async execution:** MUF provides an API so you can define different flows of control in your orchestrator, for instance, you can use an event-oriented approach, or a direct-invocation approach. Also, you can define how your components are executed (sync or asynchronously) and, in those cases where components run asynchronously, you can force the synchronization without concurrency issues. 

* **Inversion of Control:** the MUF uses dependency injection technique, so your implementation delegates the responsibility of providing its dependencies to the MUF (injector). It allows your system to be reconfigured without recompilation, make easier to unit test your components in isolation, allow concurrent or independent development, etc. Also, MUF provides you mechanisms to intercept execution of methods according to AOP (Aspect Oriented Programming). 

* **Logging:** you can define multiple levels of logging and different ways to store logs (files, json, database, etc.). All interaction that goes through the blackboard is logged by default, but you can add additional logging criteria. 

* **Contracts:** the MUD defines contracts (shared libraries with classes and global constants) to avoid issues when parsing and matching messages and content. 

* **API's:** the MUF provides different API’s that encapsulate natural patterns for communication, parsing json, serialization, common utils, etc. 

* **Multiplatform and Multilanguage:** thanks to MUF uses a potent messaging and concurrency libraries such as ZMQ, you can easily communicate the MUF implementation (Java code) with clients written in almost any programming language with low extra effort (there is an example in the github repo of how to connect Java MUF with a Python MUF, but so many other languages may be supported). Also, you can communicate your MUF with another MUF which runs on another machine, that means that your MUF may behave as a server when receiving requests from clients (phones) or as a client when it sends or forwards messages to other MUF’s and waits for responses from them, so you can get a nested architecture of MUF’s.



```java
MultiuserFramework muf = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ).setTCPon( isTPCon ), null );
```

You can create a PluginModule array like this:

```java
public PluginModule[] getModules(Class<? extends ProcessOrchestratorImpl> orchestrator){
      return new PluginModule[]{
              new PluginModule.Builder( orchestrator )
                      .addPlugin(YourPluggableComponent1.class, "id_comp_1")
                      .addPlugin(YourPluggableComponent2.class, "id_comp_2")
                      //.addPlugin....
                      .build()
      };
}
```

And also you can create a Config object like this:

```java
public Config createConfig(String serverAddress, int port) {
    return new Config.Builder()
            // you can refer to values in your config.properties file:
            setPathLogs(Utils.getProperty("pathLogs"))
            // or you can add values directly like this:
            .setServerAddress(serverAddress)
            .setSessionManagerPort(port)
            .setDefaultNumOfPoolInstances(10)           
            .setSessionTimeout(5, TimeUnit.MINUTES)
            .setExceptionTraceLevel(Constants.SHOW_ALL_EXCEPTIONS)
            //.set...
            .build();
}
```
