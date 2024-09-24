#!/bin/bash -
fly deploy . --local-only --dockerfile DockerfilePR --env OWLCMS_RESTARTSIZE=512 --app owlcms-next-results --config owlcms-next-results.toml --ha=false 
#--image-label $1