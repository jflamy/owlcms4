# Interactive Java JVM Monitoring on Kubernetes using JMX

It is occasionally necessary to inspect the behavior of a Java application, in particular its memory usage or CPU usage.  In the Java ecosystem, the free VisualVM or jconsole tools are often used. These tools also allow seeing the thread usage and drill down/filter interactively, and complement log-based monitoring.

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

