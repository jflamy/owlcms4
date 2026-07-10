#!/usr/bin/env bash
set -euo pipefail

# Refreshes translation4.csv from HEAD, then compares it with Google Sheets
# and reports which keys differ in which languages.

TRANSLATION_CSV="shared/src/main/resources/i18n/translation4.csv"
GOOGLE_SHEET_URL="https://docs.google.com/spreadsheets/d/1ZRfYHCARnPCnUEVZYo3Y_7qJGS9z7NRVg-Se7z3lHtE/export?format=csv"
PROMPT_DOWNLOAD="${PROMPT_DOWNLOAD:-true}"

if [[ ! -f "${TRANSLATION_CSV}" ]]; then
  echo "ERROR: ${TRANSLATION_CSV} not found" >&2
  exit 1
fi

REMOTE_TMP=$(mktemp)
REMOTE_RETRY_TMP=$(mktemp)
trap "rm -f ${REMOTE_TMP} ${REMOTE_RETRY_TMP}" EXIT

if command -v python3 >/dev/null 2>&1; then
  PYTHON=python3
elif command -v python >/dev/null 2>&1; then
  PYTHON=python
else
  echo "ERROR: neither 'python3' nor 'python' found on PATH" >&2
  exit 1
fi

download_translation_csv() {
  local destination="$1"

  if [[ -n "${GOOGLE_SHEETS_API_KEY:-}" ]]; then
    if ! "${PYTHON}" "${SCRIPT_DIR}/download-google-translations.py" "${destination}"; then
      echo "ERROR: Google Sheets API download failed" >&2
      exit 1
    fi
  elif ! curl -fsSL --retry 3 --retry-delay 2 --max-time 120 --connect-timeout 30 \
      -o "${destination}" "${GOOGLE_SHEET_URL}"; then
    echo "ERROR: Download failed" >&2
    exit 1
  fi
  if [[ ! -s "${destination}" ]]; then
    echo "ERROR: Download failed - empty file" >&2
    exit 1
  fi
  if head -c 200 "${destination}" | grep -qi '<html'; then
    echo "ERROR: Download returned HTML instead of CSV (auth or rate limit issue)" >&2
    exit 1
  fi
  if ! head -1 "${destination}" | grep -q 'key'; then
    echo "ERROR: Downloaded file does not look like a valid translation CSV" >&2
    echo "First line: $(head -1 "${destination}")" >&2
    exit 1
  fi
}

if ! git restore --source=HEAD --worktree -- "${TRANSLATION_CSV}"; then
  echo "ERROR: Could not refresh ${TRANSLATION_CSV} from HEAD" >&2
  exit 1
fi

echo "Downloading Google Sheets translation (this may take a moment)..."
download_translation_csv "${REMOTE_TMP}"

echo "Comparing translations..."
echo ""

# Run comparison script (separate file to avoid Git Bash heredoc issues)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
set +e
"${PYTHON}" "${SCRIPT_DIR}/compare-translations.py" "${TRANSLATION_CSV}" "${REMOTE_TMP}"
PYTHON_EXIT=$?
set -e

if [[ ${PYTHON_EXIT} -ne 0 ]]; then
  echo ""
  echo "Initial comparison differed; downloading Google Sheets again..."
  download_translation_csv "${REMOTE_RETRY_TMP}"
  set +e
  "${PYTHON}" "${SCRIPT_DIR}/compare-translations.py" "${TRANSLATION_CSV}" "${REMOTE_RETRY_TMP}"
  RETRY_EXIT=$?
  set -e

  if [[ ${RETRY_EXIT} -eq 0 ]]; then
    cp "${REMOTE_RETRY_TMP}" "${REMOTE_TMP}"
    echo "Second Google Sheets snapshot matches the local file."
    exit 0
  fi

  if [[ "${PROMPT_DOWNLOAD}" == "true" ]]; then
    echo ""
    read -p "Download updated translation4.csv from Google Sheets? (y/N): " -r DOWNLOAD
    if [[ "${DOWNLOAD}" =~ ^[Yy]$ ]]; then
      echo "Downloading updated translation4.csv..."
      if [[ -f "${TRANSLATION_CSV}" ]]; then
		BACKUP_FILE="${TRANSLATION_CSV}.bak"
        echo "Moving existing file aside: ${BACKUP_FILE}"
        mv "${TRANSLATION_CSV}" "${BACKUP_FILE}"
      fi
      if ! cp "${REMOTE_TMP}" "${TRANSLATION_CSV}"; then
        if [[ -n "${BACKUP_FILE:-}" && -f "${BACKUP_FILE}" ]]; then
          echo "Restoring backup..."
          mv "${BACKUP_FILE}" "${TRANSLATION_CSV}"
        fi
        exit 1
      fi
      echo "✓ Updated ${TRANSLATION_CSV}"
      echo "  Review changes with: git diff ${TRANSLATION_CSV}"
    else
      echo "Skipped download."
    fi
  fi
fi
