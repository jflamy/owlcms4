# Kubernetes Deployment

This page discusses several options for deploying under Kubernetes.  First we discuss local development options for testing, using Docker Desktop under Windows with WSL2, and k3s under WSL2.  If you run Linux natively, you can use the corresponding Linux packages.

For actual deployment, we propose using the KubeSail service.  KubeSail is a low-cost service hosted on Amazon AWS, but with a much simpler setup than AWS, Azure or Google Cloud.  The "hobby" tier is sufficient to run owlcms (7 US$ per month).

Should anyone provide us with a step-by-step recipe for other providers, we will add it to this page.

## WSL2 Preparation (Windows 10)

If working under Windows WSL2, the following steps are required for preparation

1. The Linux subsystem is NAT-ed and changes IP address on every reboot.  Install the go-wsl2-host service from https://github.com/shayne/go-wsl2-host/releases

2. Add a `~/.wsl2hosts` files with a space-separated list the host aliases you need, for example

   ```
   o.jflamy.dev r.jflamy.dev o.local r.local
   ```

## Docker Desktop Kubernetes

Docker Desktop includes a Kubernetes cluster.

### Without certificates

#### Initial Setup

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


#### For each release

Obtain the k8s.zip file for the release, unzip and go to that folder.  You can generate the same content by using the  `mvn -DyamlOnly=true clean package` command in the source directory (the output will be in target/k8s)

```bash
export KUBECONFIG=~/.kube/config
kubectl kustomize k8s/overlays/local-nocerts | kubectl apply -f -
```

### Full deployment with certificates

For a more complete deployment using an ingress controller and running both owlcms and publicresults, you can look at the following recipe.

#### Initial Setup with certificates

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

#### For each release

Obtain the k8s.zip file for the release, unzip and go to that folder.  You can generate the same content by using the  `mvn -DyamlOnly=true clean package` command in the source directory (the output will be in target/k8s)

```bash
export KUBECONFIG=~/.kube/config
kubectl kustomize k8s/overlays/local-jflamy-dev | kubectl apply -f -
```

## K3S Deployment

This setup is excellent for running on Linux, or for local testing on Windows WSL2 (under Windows, you would need to setup port forwarding to port 443 to be able to connect from outside the local machine). 

#### Initial Setup with certificates

1. If running on Windows, make sure you have installed the `go-wsl2-hosts` as explained at the top of this page.

2. Install k3s on WSL2 Ubuntu

   - download the k3s binary from https://github.com/rancher/k3s/releases

   - On WSL2, there is no systemd, so start it manually in a new Ubuntu window (it will hog the console)

     ```
     sudo ./k3s server
     ```

   - Copy the k3s.yaml file to ~/.kube

3. There is no need to install an ingress controller, traefik is installed by default

4. For the remaining steps, point to the correct cluster configuration

   ```bash
   export KUBECONFIG=~/.kube/k3s.yaml
   ```

5. Generate certificates manually for your development domain. 

   - Since the development environment is not visible to the outside, we cannot use an http challenge with letsencrypt, so we generate a wildcard certificate manually.

   - You can follow this [tutorial](https://www.digitalocean.com/community/tutorials/how-to-acquire-a-let-s-encrypt-certificate-using-dns-validation-with-acme-dns-certbot-on-ubuntu-18-04) for generating the wildcard certificates

6. Go to the directory containing your generated certificate, and generate the two secrets for TLS authentication.

   ```bash
   kubectl create secret tls o-jflamy-dev --key privkey.pem --cert fullchain.pem
   kubectl create secret tls r-jflamy-dev --key privkey.pem --cert fullchain.pem
   ```

#### For each release

Obtain the k8s.zip file for the release, unzip and go to that folder.  You can generate the same content by using the  `mvn -DyamlOnly=true clean package` command in the source directory (the output will be in target/k8s)

```bash
export KUBECONFIG=~/.kube/k3s.yaml
kubectl kustomize k8s/overlays/local-jflamy-dev | kubectl apply -f -
```

## KubeSail Deployment (from template)

KubeSail (https://kubesail.com) is a cloud KaaS (Kubernetes as a service) provider.

#### Initial Setup

- Create your cluster.  Select the options to use nginx and to create a certificate manager for LetsEncrypt
- Follow their instructions to define your own custom domain
- Create your instance of owlcms and publicresults, connected to one another.
  1. From the your application, go to the Templates section at the bottom.  Search for "owlcms4" using the search bar.
  2. Go to the bottom of the page that appears.  Fill in the parameters with the recommended values as shown in the description.
  3. Launch Template

#### For each release

1. Go back to the template section for your cluster.

2. Update the version number, and Launch Template again.

3. If you get red boxes about not being able to apply the changes, do the following

   1. Go to the Resources page

   2. Click on the owlcms deployment in the list, select the Delete button and confirm.

   3. Click on the publicresults deployment in the list, select the Delete button and confirm.

   4. Go back to the Template section, Launch Template.

      

## KubeSail Deployment (kubectl)

The following instructions are for advanced users who wish to have a "configuration as code" declarative setup.  The following instructions assume working knowledge of the `kustomize` app (which is usually invoked as `kubectl kustomize`)

#### Initial Setup

1. Create your cluster using the options for nginx ingress and cert-manager for LetsEncrypt.

2. Kubesail uses cert-manager HTTP challenges.

   - follow their instructions to define your own custom domain
   - the examples in kubesail-jflamy-dev use the secrets populated automatically by kubesail.

3. Kubesail can use nginx as an ingress controller.  Select that option when creating your cluster

4. You need to capture the cluster configuration file -- see the "Details" section fory your cluster.  For example, if `~/.kube/sailconfig` is available for a kubesail.com cluster, then deployment can take place with

   ```bash
   export KUBECONFIG=~/.kube/sailconfig
   ```

5. Obtain the k8s.zip file for the release, unzip and go to that folder. Create a folder for yourSite (for example k8s/overlays/kubesail-myOwn).  Copy the files from jflamy-dev and edit the ingress file to reflect your own. 

#### For each release

 For each release, edit the version number in the base deployments.yaml.

```bash
export KUBECONFIG=~/.kube/sailconfig
kubectl kustomize k8s/overlays/kubesail-myOwn | kubectl apply -f -
```