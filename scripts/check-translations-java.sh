#!/usr/bin/env bash
set -euo pipefail

REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TRANSLATION_CSV="shared/src/main/resources/i18n/translation4.csv"
ENV_FILE="${REPOSITORY_ROOT}/.vscode/.env.mac"

if [[ -z "${GOOGLE_SHEETS_API_KEY:-}" && -f "${ENV_FILE}" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
fi

if [[ -z "${GOOGLE_SHEETS_API_KEY:-}" ]]; then
  echo "ERROR: GOOGLE_SHEETS_API_KEY is not set and ${ENV_FILE} was not usable." >&2
  exit 4
fi

cd "${REPOSITORY_ROOT}"
if [[ ! -f "${TRANSLATION_CSV}" ]]; then
  echo "ERROR: ${TRANSLATION_CSV} not found." >&2
  exit 4
fi

REMOTE_TMP="$(mktemp)"
trap 'rm -f "${REMOTE_TMP}"' EXIT

echo "Downloading Google Sheets translations and comparing them with ${TRANSLATION_CSV}..."
mvn -pl shared -DskipTests compile exec:java \
  -Dexec.mainClass=app.owlcms.i18n.TranslationComparison \
  -Dexec.args="${TRANSLATION_CSV} ${REMOTE_TMP}"