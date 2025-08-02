#!/bin/bash -
fly deploy . --app owlcms-next --config owlcms-next.toml --ha=false --image-label $1