#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage: owlcms/scripts/zipcurrent.sh <installed-version-dir>

Packages the CURRENT development environment data into an owlcms control
panel installed version directory, replacing what is there:

  - local override:  read from OWLCMS_LOCALDIR in .vscode/.env.mac
                     (falls back to ./local if unset)
                     copied to <installed-version-dir>/local
  - database:        owlcms/database  (H2 files, e.g. owlcms-h2v2.mv.db)
                     copied to <installed-version-dir>/database

Existing local/ and database/ directories at the target are replaced.

Example:
  owlcms/scripts/zipcurrent.sh "$HOME/Library/Application Support/owlcms/67.3.0-rc02"
USAGE
}

if [[ $# -ne 1 || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 1
fi

TARGET_DIR="${1%/}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${REPO_ROOT}/.vscode/.env.mac"
DB_SRC="${SCRIPT_DIR}/../database"

if [[ ! -d "${TARGET_DIR}" ]]; then
  echo "ERROR: target directory not found: ${TARGET_DIR}" >&2
  exit 1
fi

# --- resolve local override source from .env.mac ---
LOCAL_SRC=""
if [[ -f "${ENV_FILE}" ]]; then
  # grab OWLCMS_LOCALDIR=... (ignore commented lines), strip quotes and whitespace
  LOCAL_SRC="$(grep -E '^[[:space:]]*OWLCMS_LOCALDIR[[:space:]]*=' "${ENV_FILE}" | tail -n 1 \
    | sed -E 's/^[^=]*=[[:space:]]*//' \
    | sed -E 's/^[[:space:]]*//; s/[[:space:]]*$//' \
    | sed -E 's/^"(.*)"$/\1/; s/^'"'"'(.*)'"'"'$/\1/')"
fi

if [[ -z "${LOCAL_SRC}" ]]; then
  LOCAL_SRC="${REPO_ROOT}/local"
  echo "OWLCMS_LOCALDIR not set in ${ENV_FILE}, using ${LOCAL_SRC}"
else
  echo "OWLCMS_LOCALDIR from $(basename "${ENV_FILE}"): ${LOCAL_SRC}"
fi

if [[ ! -d "${LOCAL_SRC}" ]]; then
  echo "ERROR: local override directory not found: ${LOCAL_SRC}" >&2
  exit 1
fi

if [[ ! -d "${DB_SRC}" ]]; then
  echo "ERROR: development database directory not found: ${DB_SRC}" >&2
  exit 1
fi

echo "Target:  ${TARGET_DIR}"
echo ""

# --- copy local ---
echo "Replacing ${TARGET_DIR}/local ..."
rm -rf "${TARGET_DIR}/local"
cp -R "${LOCAL_SRC}" "${TARGET_DIR}/local"

# --- copy database ---
echo "Replacing ${TARGET_DIR}/database ..."
rm -rf "${TARGET_DIR}/database"
mkdir -p "${TARGET_DIR}/database"
cp -R "${DB_SRC}/." "${TARGET_DIR}/database/"

echo ""
echo "Done. Copied:"
echo "  local:    ${LOCAL_SRC}"
echo "  database: ${DB_SRC}"
echo "  into:     ${TARGET_DIR}"
