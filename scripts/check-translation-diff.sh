#!/usr/bin/env bash
set -euo pipefail

# Compares local translation4.csv with Google Sheets source
# and reports which keys differ in which languages

TRANSLATION_CSV="shared/src/main/resources/i18n/translation4.csv"
GOOGLE_SHEET_URL="https://docs.google.com/spreadsheets/d/1ZRfYHCARnPCnUEVZYo3Y_7qJGS9z7NRVg-Se7z3lHtE/export?format=csv"
PROMPT_DOWNLOAD="${PROMPT_DOWNLOAD:-true}"

if [[ ! -f "${TRANSLATION_CSV}" ]]; then
  echo "ERROR: ${TRANSLATION_CSV} not found" >&2
  exit 1
fi

REMOTE_TMP=$(mktemp)
trap "rm -f ${REMOTE_TMP}" EXIT

echo "Downloading Google Sheets translation (this may take a moment)..."
curl -sL --retry 3 --retry-delay 2 --max-time 120 --connect-timeout 30 \
  -o "${REMOTE_TMP}" "${GOOGLE_SHEET_URL}"

# Validate download: must be non-empty CSV, not an HTML error page
if [[ ! -s "${REMOTE_TMP}" ]]; then
  echo "ERROR: Download failed - empty file" >&2
  exit 1
fi
if head -c 200 "${REMOTE_TMP}" | grep -qi '<html'; then
  echo "ERROR: Download returned HTML instead of CSV (auth or rate limit issue)" >&2
  exit 1
fi
# Sanity check: first line should contain 'key' as header
if ! head -1 "${REMOTE_TMP}" | grep -q 'key'; then
  echo "ERROR: Downloaded file does not look like a valid translation CSV" >&2
  echo "First line: $(head -1 "${REMOTE_TMP}")" >&2
  exit 1
fi

echo "Comparing translations..."
echo ""

# Run comparison script (separate file to avoid Git Bash heredoc issues)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
set +e
python "${SCRIPT_DIR}/compare-translations.py" "${REMOTE_TMP}"
PYTHON_EXIT=$?
set -e

if [[ ${PYTHON_EXIT} -ne 0 ]]; then
  if [[ "${PROMPT_DOWNLOAD}" == "true" ]]; then
    echo ""
    read -p "Download updated translation4.csv from Google Sheets? (y/N): " -r DOWNLOAD
    if [[ "${DOWNLOAD}" =~ ^[Yy]$ ]]; then
      echo "Downloading updated translation4.csv..."
      cp "${REMOTE_TMP}" "${TRANSLATION_CSV}"
      echo "✓ Updated ${TRANSLATION_CSV}"
      echo "  Review changes with: git diff ${TRANSLATION_CSV}"
    else
      echo "Skipped download."
    fi
  fi
fi
