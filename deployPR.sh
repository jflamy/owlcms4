#!/bin/bash -
fly deploy . --local-only --dockerfile DockerfilePR --app owlcms-next-results --config owlcms-next-results.toml --ha=false 
#--image-label $1