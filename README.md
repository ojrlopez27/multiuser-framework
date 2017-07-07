
```java
MultiuserFramework muf = MultiuserFrameworkContainer.startFramework(
                TestUtils.getModules(TestOrchestrator.class ),
                TestUtils.createConfig( serverAddress, ports[0] ).setTCPon( isTPCon ), null );
```


