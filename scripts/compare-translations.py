#!/usr/bin/env python
"""Compare a HEAD-refreshed translation4.csv copy with a remote Google Sheets copy."""
import csv
import sys

if len(sys.argv) != 3:
    print(
        "Usage: compare-translations.py <local_csv> <remote_csv>",
        file=sys.stderr,
    )
    sys.exit(2)

LOCAL_FILE = sys.argv[1]
REMOTE_FILE = sys.argv[2]

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
local_key_order = []
for row in local_rows[1:]:
    if row and len(row) > 0:
        key = row[0]
        local_dict[key] = row
        local_key_order.append(key)

remote_dict = {}
remote_key_order = []
for row in remote_rows[1:]:
    if row and len(row) > 0:
        key = row[0]
        remote_dict[key] = row
        remote_key_order.append(key)

# Find differences
differences = []
all_keys = list(local_key_order)
for key in remote_key_order:
    if key not in local_dict:
        all_keys.append(key)

for key in all_keys:
    local_row = local_dict.get(key, [])
    remote_row = remote_dict.get(key, [])

    if key not in local_dict:
        differences.append({
            'key': key,
            'type': 'missing_local',
            'details': 'Key exists in Google Sheets but not in the HEAD-refreshed local copy'
        })
        continue

    if key not in remote_dict:
        differences.append({
            'key': key,
            'type': 'missing_remote',
            'details': 'Key exists in the HEAD-refreshed local copy but not in Google Sheets'
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
    print("No differences found - files are in sync")
    sys.exit(0)

print(f"Found {len(differences)} keys with differences:\n")

for diff in differences:
    if diff['type'] == 'missing_local':
        print(f"  {diff['key']}")
        print(f"  Missing from HEAD-refreshed local copy")
        print()
    elif diff['type'] == 'missing_remote':
        print(f"  {diff['key']}")
        print(f"  Missing from Google Sheets")
        print()
    elif diff['type'] == 'changed':
        print(f"  {diff['key']}")
        for lang_diff in diff['languages']:
            print(f"  Language: {lang_diff['lang']}")
            print(f"    Local: {lang_diff['local']}")
            print(f"    Remote: {lang_diff['remote']}")
        print()

print(f"\nSummary: {len(differences)} keys with differences")
sys.exit(1)
