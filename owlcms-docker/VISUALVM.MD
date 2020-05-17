# Interactive Java JVM Monitoring on Kubernetes

In addition to log-based or agent-based monitoring, it is occasionally necessary to inspect interactively the behavior of a Java application, in particular its memory usage or CPU usage.  In the Java ecosystem, the free VisualVM or jconsole tools are often used. These tools also allow seeing the thread usage and drill down/filter interactively.

This note explains the steps necessary to manually inspect a Java application using the JMXMP protocol in a Kubernetes cluster.

## What about the usual way?

Exposing the default RMI protocol is extremely painful because of the way it handles ports and requires a back channel.  The general consensus is don't bother.  After wasting a couple days, I wholeheartedly agree.

## Enabling JMXMP monitoring

The simplest way to enable monitoring is to create the monitoring port ourselves.  For example, in a web frontend, add the following to open port 1098 for JMXMP monitoring:

```java
try {
    // Get the MBean server for monitoring/controlling the JVM
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    // Create a JMXMP connector server
    JMXServiceURL url = new JMXServiceURL("jmxmp", "localhost", 1098);
    JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url,
            null, mbs);
    cs.start();
} catch (Exception e) {
    e.printStackTrace();
}
```

In order for this code to compile, you will need to include a JMXMP implementation.  If using Maven, add the following to your `pom.xml`

```xml
<!-- https://mvnrepository.com/artifact/org.glassfish.external/opendmk_jmxremote_optional_jar -->
<dependency>
    <groupId>org.glassfish.external</groupId>
    <artifactId>opendmk_jmxremote_optional_jar</artifactId>
    <version>1.0-b01-ea</version>
</dependency>
```
In real life, 
- you will likely use an environment variable so you can configure the port number using a ConfigMap or Secret, and perhaps not start the connector if there is no such variable present.
- You may wish to secure the connection.   For my own purposes, I have used IP whitelisting of my development workstation in a k8s networking policy, so I have not explored this topic further.
  See https://interlok.adaptris.net/interlok-docs/advanced-jmx.html for examples on how to enable this.

### Special considerations for building the container

KubeSail uses IPv6 by default.  In the process of debugging this recipe, we have found it necessary to add the following Java flags to the Java entry point command of the Docker container.

```sh
-Djava.net.preferIPv4Stack=true
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
```

We will be checking whether they are still necessary.

## Exposing a port in Docker Desktop Kubernetes

The following instructions have been tested with the new WSL2 Docker backend which runs directly on Linux.

For Docker Desktop, the simplest way to expose a port is to use a LoadBalancer service.  This avoids having to use kubectl on every session.

```yaml
kind: Service
apiVersion: v1
metadata:
  name: owlcms
spec:
  selector:
    app: owlcms
  ports:
  - protocol: TCP
    name: http
    port: 80
    targetPort: 8080
  - protocol: TCP
    name: jmx
    port: 1098
    targetPort: 1098
  type: LoadBalancer
```

This makes it possible to open the web application on http://localhost even though the container is communicating on port 8080, and also opens up the JMX monitoring channel on port 1098.   You can change the port to suit your needs, but `targetPort` needs to match what is used by the application being monitored (see the Java above).

## Exposing a port with KubeSail

KubeSail is an affordable managed PaaS for running Kubernetes applications.  By default, only the traffic coming in through an Ingress (NGINX) is allowed to go further.  It is therefore necessary to 

1. create a Network Policy that allows traffic to flow to the monitored application container
2. expose the port for monitoring

### Creating a network policy

The following opens the 1098 port on the application to be accessed from the outside.

```yaml
kind: NetworkPolicy
apiVersion: networking.k8s.io/v1
metadata:
  name: owlcms-jmx
spec:
  podSelector:
    matchLabels:
      app: owlcms
  ingress:
  - ports:
    - port: 1098
    from: []
```

More advanced topics such as restricting monitoring to come only from a certain site are discussed in 
https://www.magalix.com/blog/kubernetes-network-policies-101

### Exposing the monitoring port

The simplest way is to use a NodePort, which makes the virtual machine open a port and expose it to the outside world.  In the case of a shared service like KubeSail, there is therefore the possibility that the chosen port is in conflict with others, and that you will need to change it.

```yaml
kind: Service
apiVersion: v1
metadata:
  name: jmx
spec:
  selector:
    app: owlcms
  ports:
  - protocol: TCP
    port: 1098
    targetPort: 1098
    nodePort: 30098
  type: NodePort
```

### Getting the address of the node

- In order to the the address of the node, you may use

  ```
  kubectl get pod -o wide
  ```
![image](https://user-images.githubusercontent.com/678663/82157544-1e37cb00-9850-11ea-86e1-da3feed3f06a.png)

- You may also install the `jq` JSON query tool, and use the following one-liner

  ```
  kubectl get pod -o json -l 'app=owlcms' | jq -r '.items[0].spec.nodeName'
  ```

  which will output the ip address (54.151.3.52) in the example

- In order to test that all is good, you can use the command.

  ```bash
  telnet 54.151.3.52 30098
  ```

  You will get garbage back, but with a recognizable `javax.management.remote.message.HandshakeBeginMessage` string at the beginning.



## Configuring VisualVM to use JMXMP

Despite feature requests to do so and some work to create a plugin, VisualVM does not ship with JMXMP configured by default.   The following recipe has been tested with VisualVM 2.0.1.

If we used Maven to enable our application, we have a copy of the required java archive (jar) in the .m2 directory.  The simple way is to add that jar to the class path using the `--cp:a` option.   This jar would normally have to be "endorsed" to be used, but starting from the directory where it is found allows things to work. 

```powershell
cd C:\Dev\.m2\repository\org\glassfish\external\opendmk_jmxremote_optional_jar\1.0-b01-ea
C:\Dev\Java\visualvm_201\bin\visualvm.exe --cp:a opendmk_jmxremote_optional_jar-1.0-b01-ea.jar
```

You can also create a shortcut on your desktop to run the command.  Use the `Start In` option to select the directory where the jmxremote jar is found.

### Adding a remote host

When accessing a remote host such as KubeSail, add it using the `add remote host` option
![image](https://user-images.githubusercontent.com/678663/82157843-d6b23e80-9851-11ea-8444-6ab5f8299fdd.png)

### Configure a connection

Under the new host (or under Local if connecting locally), right-click and use `Add JMX Connection`.   Use the accessible address.  

- If running locally, this will be localhost, and the syntax will be `service:jmx:jmxmp://localhost:1098`
- if remote, the external address obtained by querying the pod. 
  In our example, that yields`service:jmx:jmxmp://54.151.2.52:30098`  (the port is the one listed in the NodePort configuration as explained above.)

![image](https://user-images.githubusercontent.com/678663/82157887-24c74200-9852-11ea-8351-a6f2f6c8add0.png)

- Double-clicking on the new connection will open it, and the name of the monitored application will be shown.  You can then use the various tabs to monitor, examine or sample the application.

![image](https://user-images.githubusercontent.com/678663/82157944-9f905d00-9852-11ea-8331-318e88f14b2c.png)

