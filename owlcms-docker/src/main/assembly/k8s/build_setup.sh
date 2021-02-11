#/bin/bash -wq
(echo "# DockerDesktop setup"; cat base/nginx.yaml ; echo "---" ; kubectl kustomize overlays/kubesail-dd ) > dd_setup.yaml
(echo "# k3s setup "; cat base/nginx.yaml ; echo "---" ; kubectl kustomize overlays/k3s ) > k3s_setup.yaml
