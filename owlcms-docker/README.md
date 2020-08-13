## Tag

When building, the image is given a tag according to what is found in `target/docker-tag/docker-tag.properties` and a `latest` tag.

## Docker

`mvn clean package` will build but not push the image to the remote directory.   
`docker run -p 8081:8080 owlcms:latest` will run the image that can then be accessed as localhost:8081
Using `-p` is necessary to forward the 8080 port of the container to the outside world.
When running the image in this way an ephemeral H2 database is created inside the container, and vanishes when the container is deleted.

### Docker Deployment

If you use `mvn clean deploy` to deploy the container to the owlcms-docker-containers.bintray.io/owlcms repository.  The repository is public, no credentials needed to pull the image.

Note: this seems to require 

```
docker login -u jflamy -p <API_KEY> owlcms-docker-containers.bintray.io
```

to have been run beforehand. Since July 2020 (docker desktop update?)


## Kubernetes

### Local Docker Studio Deployment

For a k8s deployment  of owlcms on the local Docker Studio Kubernetes, start a command line i(for example, a Git Bash) in the project directory and run

```bash
kubectl kustomize target/k8s/overlays/simple | kubectl apply -f -
```

This will assemble the various manifest files and deploy the version with the docker tag just created.

For a more complete deployment using an ingress controller and running both owlcms and publicresults

1. Install the ingress controller into Docker Desktop.  This configuration listens on localhost.

   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v0.34.1/deploy/static/provider/cloud/deploy.yaml
   ```
   
2. add lines to the hosts file `c:\windows\system32\drivers\etc\hosts`

   ```
   127.0.0.1 publicresults.local
   127.0.0.1 owlcms.local
   ```

3. Apply the customized configuration

   ```bash
   kubectl kustomize target/k8s/overlays/localhost | kubectl apply -f 
   ```

### Remote Deployment

1. To deploy elsewhere, a cluster configuration file must be available.  For example, if `~/.kube/sailconfig` is available for a kubesail.com cluster, then deployment can take place with

```
export KUBECONFIG=~/.kube/sailconfig
kubectl kustomize target/k8s/overlays/kubesail-jflamy-dev | kubectl apply -f -
```

The specificities of the cloud overlay (configuring the ingress with a custom domain) are specified in the overlays customization.

### Monitoring on Docker Desktop Kubernetes

In order to monitor using VisualVM or similar tool

1. opening a port. If running kubectl on WSL, and VisualVM on Windows, you need to tell kubectl to listen on the real IP address that Windows can reach, as follows (IP address and pod name need to be substituted with real values.)  Port `1098` is currently hard-wired in the container build.

   ```bash
   kubectl port-forward --address 172.28.119.147 owlcms-76c8b879cc-t46 1098
   ```

2. In VisualVM, use the add JMX Connection option, with the following string.  Port `1088` is currently hard-wired in the container build.

    ```
service:jmx:rmi:///jndi/rmi://172.28.119.147:1088/jmxrmi
    ```

