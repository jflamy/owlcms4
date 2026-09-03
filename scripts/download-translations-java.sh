#!/usr/bin/env bash
set -euo pipefail

REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${REPOSITORY_ROOT}/.vscode/.env.mac"

if [[ -z "${GOOGLE_SHEETS_API_KEY:-}" && -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

if [[ $# -gt 1 ]]; then
  echo "Usage: $0 [destination.csv]" >&2
  exit 2
fi

if [[ -z "${GOOGLE_SHEETS_API_KEY:-}" ]]; then
  echo "ERROR: GOOGLE_SHEETS_API_KEY is not set and ${ENV_FILE} was not usable." >&2
  exit 4
fi

cd "${REPOSITORY_ROOT}"
DESTINATION="${1:-shared/src/main/resources/i18n/translation4.csv}"
MAVEN_ARGS=(
  -pl shared
  -DskipTests
  compile
  exec:java
  -Dexec.mainClass=app.owlcms.i18n.TranslationDownload
  "-Dexec.args=${DESTINATION}"
)
mvn "${MAVEN_ARGS[@]}"