## Testing Locally with Docker Desktop

First, you need to build the jars required to build a docker image.   We want the same jars as will be used in production.

```bash
mvn -Dmaven.test.skip=true -P production -pl owlcms,publicresults install 
```

Then we build the docker images and install them to the local docker image.  This does not push them to the remote docker repository.

You need to have Docker desktop configured, and DOCKER_REG environment variable set to `owlcms`. The docker desktop daemon should be running in insecure mode, listening to port 2375 without TLS.

```bash
mvn -pl owlcms-docker package
```

### Testing on Docker

```
docker run -P 8080:8080 -i owlcms/owlcms:latest
```



### Testing Kubernetes

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



### Kubernetes Deployment

This project provides `kustomize` setups to run owlcms and publicresults together in a k3d cluster running locally under docker (Linux) or Docker Desktop (Windows), or standalone in a k3s cluster running on a Linux VM.