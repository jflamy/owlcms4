#!/usr/bin/env bash
set -euo pipefail

# Compares local translation4.csv with Google Sheets source
# and reports which keys differ in which languages

TRANSLATION_CSV="shared/src/main/resources/i18n/translation4.csv"
GOOGLE_SHEET_URL="https://docs.google.com/spreadsheets/d/1ZRfYHCARnPCnUEVZYo3Y_7qJGS9z7NRVg-Se7z3lHtE/export?format=csv"

if [[ ! -f "${TRANSLATION_CSV}" ]]; then
  echo "ERROR: ${TRANSLATION_CSV} not found" >&2
  exit 1
fi

REMOTE_TMP=$(mktemp)
trap "rm -f ${REMOTE_TMP}" EXIT

echo "Downloading Google Sheets translation..."
curl -sL "${GOOGLE_SHEET_URL}" > "${REMOTE_TMP}"

echo "Comparing translations..."
echo ""

# Use Python to parse CSV and compare (disable exit-on-error temporarily)
set +e
python3 - "${REMOTE_TMP}" <<'PYTHON'
import csv
import sys

LOCAL_FILE = "shared/src/main/resources/i18n/translation4.csv"
REMOTE_FILE = sys.argv[1]

# Read both files
with open(LOCAL_FILE, 'r', encoding='utf-8') as f:
    local_reader = csv.reader(f)
    local_rows = list(local_reader)

with open(REMOTE_FILE, 'r', encoding='utf-8') as f:
    remote_reader = csv.reader(f)
    remote_rows = list(remote_reader)

# Get header (language codes)
header = local_rows[0] if local_rows else []
languages = header[1:] if len(header) > 1 else []

# Build dictionaries keyed by translation key
local_dict = {}
for row in local_rows[1:]:
    if row and len(row) > 0:
        key = row[0]
        local_dict[key] = row

remote_dict = {}
for row in remote_rows[1:]:
    if row and len(row) > 0:
        key = row[0]
        remote_dict[key] = row

# Find differences
differences = []
all_keys = set(local_dict.keys()) | set(remote_dict.keys())

for key in sorted(all_keys):
    local_row = local_dict.get(key, [])
    remote_row = remote_dict.get(key, [])
    
    if key not in local_dict:
        differences.append({
            'key': key,
            'type': 'missing_local',
            'details': 'Key exists in Google Sheets but not in local file'
        })
        continue
    
    if key not in remote_dict:
        differences.append({
            'key': key,
            'type': 'missing_remote',
            'details': 'Key exists in local file but not in Google Sheets'
        })
        continue
    
    # Compare each language column
    changed_languages = []
    max_len = max(len(local_row), len(remote_row))
    
    for i in range(1, max_len):  # Skip first column (key)
        local_val = local_row[i] if i < len(local_row) else ''
        remote_val = remote_row[i] if i < len(remote_row) else ''
        
        if local_val != remote_val:
            lang = languages[i-1] if (i-1) < len(languages) else f'col{i}'
            changed_languages.append({
                'lang': lang,
                'local': local_val[:50] + '...' if len(local_val) > 50 else local_val,
                'remote': remote_val[:50] + '...' if len(remote_val) > 50 else remote_val
            })
    
    if changed_languages:
        differences.append({
            'key': key,
            'type': 'changed',
            'languages': changed_languages
        })

# Print results
if not differences:
    print("✓ No differences found - files are in sync")
    sys.exit(0)

print(f"Found {len(differences)} keys with differences:\n")

for diff in differences:
    if diff['type'] == 'missing_local':
        print(f"⊖ {diff['key']}")
        print(f"  Missing from local file")
        print()
    elif diff['type'] == 'missing_remote':
        print(f"⊕ {diff['key']}")
        print(f"  Missing from Google Sheets")
        print()
    elif diff['type'] == 'changed':
        print(f"≠ {diff['key']}")
        for lang_diff in diff['languages']:
            print(f"  Language: {lang_diff['lang']}")
            print(f"    Local:  {lang_diff['local']}")
            print(f"    Remote: {lang_diff['remote']}")
        print()

print(f"\nSummary: {len(differences)} keys with differences")
sys.exit(1)
PYTHON

PYTHON_EXIT=$?
set -e

if [[ ${PYTHON_EXIT} -ne 0 ]]; then
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
