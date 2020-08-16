# Kubernetes Deployment

## WSL2 Preparation (Windows 10)

If working under Windows WSL2, the following steps are required for preparation

1. The Linux subsystem is NAT-ed and changes IP address on every reboot.  Install the go-wsl2-host service from https://github.com/shayne/go-wsl2-host/releases

2. Add a `~/.wsl2hosts` files with a space-separated list the host aliases you need, for example

   ```
   o.jflamy.dev r.jflamy.dev o.local r.local
   ```

## Docker Desktop Kubernetes

Docker Desktop includes a Kubernetes cluster.

### Full deployment without certificates

1. For the remaining steps, you need to point to the correct cluster definition

   ```bash
   export KUBECONFIG=~/.kube/config
   ```

2. Install the nginx ingress controller into Docker Desktop.  This configuration listens on localhost.

   ```bash
   kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v0.34.1/deploy/static/provider/cloud/deploy.yaml
   ```

3. If you did not install go-wsl2-hosts, you can add lines to the hosts file `c:\windows\system32\drivers\etc\hosts`

   ```
   127.0.0.1 o.local
   127.0.0.1 r.local
   ```

4. Apply the customized configuration

   ```bash
   export KUBECONFIG=~/.kube/config
   kubectl kustomize target/k8s/overlays/local-nocerts | kubectl apply -f -
   ```

### Full deployment with certificates

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

7. Update and apply the customized configuration  (from the owlcms-docker/target/k8s directory or after extracting target/k8s.zip)

   ```bash
   mvn -DyamlOnly=true clean package
   kubectl kustomize overlays/local-jflamy-dev | kubectl apply -f -
   ```

## K3S Deployment

Deployment under K3S is similar to Docker Studio with the full configuration. You should install go-wsl2-hosts services because there is no port forwarding from Windows when using K3S.  Contrary to docker-desktop, you would not be able to easily connect to the cluster from outside.  Of course, if running on a native Ubuntu or a Ubuntu VM, these restrictions go away.

2. Install k3s on WSL2 Ubuntu

   - download the k3s binary from https://github.com/rancher/k3s/releases

   - On WSL2, there is no systemd, so start it manually in a new Ubuntu window (it will hog the console)

     ```
     sudo ./k3s server
     ```

   - Copy the k3s.yaml file to ~/.kube

3. There is no need to install an ingress controller, traefik is installed by default

4. For the remaining steps Point to the correct cluster configuration

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

## KubeSail Deployment (or other managed Kubernetes)

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
