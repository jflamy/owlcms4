#!/bin/bash -
fly deploy . --local-only --dockerfile DockerfilePR --env OWLCMS_RESTARTSIZE=768 --app nextpr --config owlcms-next-results.toml --ha=false