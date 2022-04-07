## Testing Kubernetes Locally with Docker Desktop

First, you need to build the jars required to build a docker image.   We want the same jars as will be used in production.

```bash
mvn -Dmaven.test.skip=true -P production -pl owlcms,publicresults install 
```

Then we build the docker images and install them to the local docker image.  This does not push them to the remote docker repository

```bash
mvn -pl owlcms install
```

Then we can make sure that the local yaml is up to date

```bash
mvn -DyamlOnly=true clean package
```

Finally we can update the kubernetes config

```bash
kubectl apply -k target/k8s/overlays/dd
```

After a change, in config, it may be necessary to restart the pods

```bash
kubectl scale --replicas=0 deployment.apps/owlcms
kubectl scale --replicas=1 deployment.apps/owlcms
```

