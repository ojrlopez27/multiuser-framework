
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
                      .addPlugin(YourPluggableComponent.class, "test")
                      .build()
      };
}
```

And also you can create a Config object like this:

```java
public Config createConfig(String serverAddress, int port) {
    return new Config.Builder()
            // you can add values directly like this:
            .setSessionManagerPort(port)
            .setDefaultNumOfPoolInstances(10)
            // or you can refer to values in your config.properties file:
            //.setPathLogs(Utils.getProperty("pathLogs"))
            .setSessionTimeout(5, TimeUnit.MINUTES)
            .setServerAddress(serverAddress)
            .setExceptionTraceLevel(Constants.SHOW_ALL_EXCEPTIONS)
            .build();
}
```
