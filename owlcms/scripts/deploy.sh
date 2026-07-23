#!/usr/bin/env bash

set -euo pipefail

usage() {
	cat <<EOF
Usage: $0 [--help]

Builds the OWLCMS Docker context using the root POM revision, then deploys the
shared root Dockerfile to Fly.io. Creates a missing Fly application; its first
deployment uses a single machine (--ha=false).

Environment overrides:
  FLY_APP     Fly application name (default: owlcms-next)
  REGION      Fly primary region (default: yyz)
  FLY_CONFIG  Fly TOML template (default: owlcms-docker/fly.toml)

Example:
  FLY_APP=my-owlcms REGION=ord $0
EOF
}

if [[ $# -eq 1 && ( "$1" == "--help" || "$1" == "-h" ) ]]; then
	usage
	exit 0
fi

if [[ $# -ne 0 ]]; then
	usage >&2
	exit 2
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
revision="$(xmllint --xpath 'string(/*[local-name()="project"]/*[local-name()="properties"]/*[local-name()="revision"])' "${repo_root}/pom.xml")"
fly_config_template="${FLY_CONFIG:-${repo_root}/owlcms-docker/fly.toml}"
fly_app="${FLY_APP:-owlcms-next}"
region="${REGION:-yyz}"
docker_context="${repo_root}/owlcms-docker/target/docker-context"

if [[ -z "${revision}" ]]; then
	echo "ERROR: Could not read the revision property from ${repo_root}/pom.xml." >&2
	exit 1
fi

if [[ ! -f "${fly_config_template}" ]]; then
	echo "ERROR: Fly configuration template not found: ${fly_config_template}" >&2
	exit 1
fi

if ! command -v fly >/dev/null 2>&1; then
	echo "ERROR: flyctl is not installed or not on PATH." >&2
	exit 1
fi

if ! fly_apps="$(fly apps list --quiet | awk 'NF { print $1 }')"; then
	echo "ERROR: Could not list Fly applications. Check your flyctl login and permissions." >&2
	exit 1
fi

if ! grep -Fqx -- "${fly_app}" <<< "${fly_apps}"; then
	echo "Creating Fly application: ${fly_app}"
	fly apps create "${fly_app}" --yes
fi

cd "${repo_root}"
mvn -Pproduction -pl owlcms-docker -am package \
	-Dmaven.test.skip=true \
	-Drevision="${revision}"

if [[ ! -f "${docker_context}/owlcms.jar" || ! -f "${docker_context}/classes/logback.xml" ]]; then
	echo "ERROR: Maven did not prepare the Docker context." >&2
	exit 1
fi
fly deploy . \
	--app "${fly_app}" \
	--config "${fly_config_template}" \
	--primary-region "${region}" \
	--ha=false \
	--image-label "${revision}"