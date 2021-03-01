#/bin/bash -wq
(echo "# DockerDesktop setup"; cat base/nginx.yaml ; echo "---" ; kubectl kustomize overlays/kubesail-dd ) > dd_setup.yaml
(echo "# k3s setup "; cat base/nginx.yaml ; echo "---" ; kubectl kustomize overlays/k3s ) > k3s_setup.yaml
(echo "# k3s setup "; kubectl kustomize overlays/k3d ) > k3d_setup.yaml
(echo "# k3d nocert setup "; kubectl kustomize overlays/local-nocerts ) > k3d_nocert.yaml