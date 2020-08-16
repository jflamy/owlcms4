## Building the images

- `docker` must be running

- `mvn clean package` will pull previously compiled uber jars from bintray and create docker images.

- `mvn clean deploy` to deploy the containers to the `owlcms-docker-containers.bintray.io/owlcms` repository.  The repository is public, no credentials are needed to pull the image.

  - In theory, the credentials are in the MAVEN settings, but in practice, it may be required to run the following beforehand

    ```
    docker login -u jflamy -p <API_KEY> owlcms-docker-containers.bintray.io
    ```

  - The pushed images are given a tag according to what is found in `target/docker-tag/docker-tag.properties` and a `latest` tag.

## Docker Deployment

```bash
docker run -p 8081:8080 owlcms:latest
```

will run the image that can then be accessed as http://localhost:8081
Using `-p` is necessary to forward the 8080 port of the container to the outside world.
When running the image in this way an ephemeral H2 database is created inside the container, and vanishes when the container is deleted.


## Kubernetes

### WSL2 Preparation (Windows 10)

If working under Windows WSL2, the following steps are required for preparation

1. The Linux subsystem is NAT-ed and changes IP address on every reboot.  Install the go-wsl2-host service from https://github.com/shayne/go-wsl2-host/releases

2. Add a `~/.wsl2hosts` files with a space-separated list the host aliases you need, for example

   ```
   o.jflamy.dev r.jflamy.dev o.local r.local
   ```

### Docker Desktop Kubernetes

#### Full deployment without certificates

1. Install the nginx ingress controller into Docker Desktop.  This configuration listens on localhost.

   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v0.34.1/deploy/static/provider/cloud/deploy.yaml
   ```

2. If you did not install go-wsl2-hosts, you can add lines to the hosts file `c:\windows\system32\drivers\etc\hosts`

   ```
   127.0.0.1 o.local
   127.0.0.1 r.local
   ```

3. Apply the customized configuration

   ```bash
   export KUBECONFIG=~/.kube/config
   kubectl kustomize target/k8s/overlays/local-nocerts | kubectl apply -f -
   ```

#### Full deployment with certificates

For a more complete deployment using an ingress controller and running both owlcms and publicresults, you can look at the following recipe.

1. For the remaining steps, you need to point to the correct cluster definition

   ```bash
   export KUBECONFIG=~/.kube/config
   ```
   
2. Install the nginx ingress controller into Docker Desktop.  This configuration listens on localhost.

   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v0.34.1/deploy/static/provider/cloud/deploy.yaml
   ```

3. Generate certificates manually for your development domain. 

   - Since the development environment is not visible to the outside, we cannot use an http challenge with letsencrypt, so we generate a wildcard certificate manually.

   - You can follow this [tutorial](https://www.digitalocean.com/community/tutorials/how-to-acquire-a-let-s-encrypt-certificate-using-dns-validation-with-acme-dns-certbot-on-ubuntu-18-04) for generating the wildcard certificates

4. (not needed) Copy the certificates back to Windows. 

   - The files contain secrets, so they are not readable. You will need to chown/chmod them
   - create a tar for safekeeping; use the --derefence option (-h) to create a tar file, because by default it contains symbolic links 
   - copy the files from Linux to /mnt/c/Users/...

5. Generate two secrets. Since the certificate is a wildcard, the same certificate is used for both secrets. Under the Ubuntu bash:

   ```
   kubectl create secret tls o-jflamy-dev --key privkey.pem --cert fullchain.pem
   kubectl create secret tls r-jflamy-dev --key privkey.pem --cert fullchain.pem
   ```

6. If you did not install go-wsl2-hosts, you can add lines to the hosts file `c:\windows\system32\drivers\etc\hosts`

   ```
   127.0.0.1 o.jflamy.dev
   127.0.0.1 r.jflamy.dev
   ```

7. Update and apply the customized configuration  (from the owlcms-docker build directory)

   ```bash
   mvn -DyamlOnly=true clean package
   kubectl kustomize target/k8s/overlays/local-jflamy-dev | kubectl apply -f -
   ```

### K3S Deployment

Deployment under K3S is similar to Docker Studio with the full configuration. 

1. You should install go-wsl2-hosts services because there is no port forwarding by default.

2. Install k3s on WSL2 Ubuntu

   - download the k3s binary from https://github.com/rancher/k3s/releases

   - On WSL2, there is no systemd, so start it manually in a new Ubuntu window (it will hog the console)

     ```
     sudo ./k3s server
     ```

   - Copy the k3s.yaml file to ~/.kube

3. There is no need to install an ingress controller, traefik is installed by default

4. Point to the correct cluster configuration

   ```bash
   export KUBECONFIG=~/.kube/k3s.yaml
   ```

5. Generate the two secrets for TLS authentication

   ```bash
   kubectl create secret tls o-jflamy-dev --key privkey.pem --cert fullchain.pem
   kubectl create secret tls r-jflamy-dev --key privkey.pem --cert fullchain.pem
   ```

6. Apply the customized configuration

   ```bash
   mvn -DyamlOnly=true clean package
   export KUBECONFIG=~/.kube/k3s.yaml
   kubectl kustomize target/k8s/overlays/local-jflamy-dev | kubectl apply -f -
   ```

### KubeSail Deployment (or other managed Kubernetes)

KubeSail (https://kubesail.com) is a cloud KaaS (Kubernetes as a service) provider.

1. Kubesail uses cert-manager HTTP challenges.
   - follow their instructions to define your own custom domain
   - the examples in kubesail-jflamy-dev use the secrets populated automatically by kubesail.
   
2. You need to capture a cluster configuration file from your provider.  For example, if `~/.kube/sailconfig` is available for a kubesail.com cluster, then deployment can take place with
   ```bash
   export KUBECONFIG=~/.kube/sailconfig
   ```

3. To update the configuration and apply:

   ```bash
   mvn -DyamlOnly=true clean package
   kubectl kustomize target/k8s/overlays/kubesail-jflamy-dev | kubectl apply -f -
   ```

### Monitoring on Docker Desktop Kubernetes

In order to monitor using VisualVM or similar tool

1. opening a port. If running kubectl on WSL, and VisualVM on Windows, you need to tell kubectl to listen on the real IP address that Windows can reach, as follows (IP address and pod name need to be substituted with real values.)  Port `1098` is currently hard-wired in the container build.

   ```bash
   kubectl port-forward --address 172.28.119.147 owlcms-76c8b879cc-t46 1098
   ```
```

2. In VisualVM, use the add JMX Connection option, with the following string.  Port `1088` is currently hard-wired in the container build.

```
service:jmx:rmi:///jndi/rmi://172.28.119.147:1088/jmxrmi
    ```

