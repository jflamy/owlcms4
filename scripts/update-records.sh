#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/update-records.sh <database> <records-directory> <output-directory>

Rebuilds record spreadsheets using the results in an OWLCMS competition database.

Arguments:
  database           Competition export in .json format, or an H2 .db/.mv.db file
  records-directory  Directory containing the source .xls and .xlsx record files
  output-directory   Directory where updated files are written as
                     <federation>_Records_<YYYYMMDDTHHMMSS-0400>.xlsx

The source database and record files are not modified. The output directory is
created when necessary; existing federation export files are replaced.

Examples:
  scripts/update-records.sh \
    owlcms/src/test/resources/testDatabases/demoDatabase_2025-10-16_15h11.json \
    owlcms/src/test/resources/testData/records \
    /tmp/updated-records

  scripts/update-records.sh \
    /path/to/competition.mv.db \
    /path/to/records \
    /path/to/output
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ $# -ne 3 ]]; then
  usage >&2
  exit 2
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
DATABASE="$1"
RECORDS_DIRECTORY="$2"
OUTPUT_DIRECTORY="$3"

if [[ ! -f "${DATABASE}" ]]; then
  echo "ERROR: database file does not exist: ${DATABASE}" >&2
  exit 2
fi

DATABASE_LOWER="$(printf '%s' "${DATABASE}" | tr '[:upper:]' '[:lower:]')"
case "${DATABASE_LOWER}" in
  *.json|*.db) ;;
  *)
    echo "ERROR: database must be a .json, .db, or .mv.db file: ${DATABASE}" >&2
    exit 2
    ;;
esac

if [[ ! -d "${RECORDS_DIRECTORY}" ]]; then
  echo "ERROR: records directory does not exist: ${RECORDS_DIRECTORY}" >&2
  exit 2
fi

mkdir -p "${OUTPUT_DIRECTORY}"

DATABASE="$(cd "$(dirname "${DATABASE}")" && pwd)/$(basename "${DATABASE}")"
RECORDS_DIRECTORY="$(cd "${RECORDS_DIRECTORY}" && pwd)"
OUTPUT_DIRECTORY="$(cd "${OUTPUT_DIRECTORY}" && pwd)"

cd "${REPOSITORY_ROOT}"
mvn -pl owlcms -am \
  -Dtest=RecordUpdateWorkflow \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dowlcms.recordUpdate.database="${DATABASE}" \
  -Dowlcms.recordUpdate.records="${RECORDS_DIRECTORY}" \
  -Dowlcms.recordUpdate.output="${OUTPUT_DIRECTORY}" \
  test

echo "Updated record files written to: ${OUTPUT_DIRECTORY}"
