#/bin/bash -wq
(cat base/nginx.yaml ; echo "---" ; kubectl kustomize overlays/kubesail-dd ) > dd_setup.yaml
(cat base/nginx.yaml ; echo "---" ; kubectl kustomize overlays/k3s ) > k3s_setup.yaml
