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
with open(LOCAL_FILE, 'r', encoding='utf-8-sig', newline='') as f:
    local_reader = csv.reader(f)
    local_rows = list(local_reader)

with open(REMOTE_FILE, 'r', encoding='utf-8-sig', newline='') as f:
    remote_reader = csv.reader(f)
    remote_rows = list(remote_reader)

# Get header (language codes)
header = local_rows[0] if local_rows else []
remote_header = remote_rows[0] if remote_rows else []
languages = header[1:] if len(header) > 1 else []

# Build dictionaries keyed by translation key
local_dict = {}
local_key_order = []
local_key_line_numbers = {}
for line_number, row in enumerate(local_rows[1:], start=2):
    if row and len(row) > 0:
        key = row[0]
        local_key_line_numbers.setdefault(key, []).append(line_number)
        if not key:
            continue
        local_dict[key] = row
        local_key_order.append(key)

remote_dict = {}
remote_key_order = []
remote_key_line_numbers = {}
for line_number, row in enumerate(remote_rows[1:], start=2):
    if row and len(row) > 0:
        key = row[0]
        remote_key_line_numbers.setdefault(key, []).append(line_number)
        if not key:
            continue
        remote_dict[key] = row
        remote_key_order.append(key)

# Find differences
differences = []

if header != remote_header:
    differences.append({
        'key': 'Header',
        'type': 'header_changed',
        'local': header,
        'remote': remote_header,
    })

for source_name, key_line_numbers in (
    ('HEAD-refreshed local copy', local_key_line_numbers),
    ('Google Sheets', remote_key_line_numbers),
):
    if '' in key_line_numbers:
        differences.append({
            'key': 'Blank column 1',
            'type': 'blank_column_1',
            'source': source_name,
            'lines': key_line_numbers[''],
        })

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
            'type': 'remote_only',
            'details': 'Column 1 value appears only in Google Sheets'
        })
        continue

    if key not in remote_dict:
        differences.append({
            'key': key,
            'type': 'local_only',
            'details': 'Column 1 value appears only in the HEAD-refreshed local copy'
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
            lang_diff = {
                'lang': lang,
                'local': local_val[:50] + '...' if len(local_val) > 50 else local_val,
                'remote': remote_val[:50] + '...' if len(remote_val) > 50 else remote_val
            }
            changed_languages.append(lang_diff)

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
    if diff['type'] == 'header_changed':
        print("  Header")
        print("  Language column layout differs between files")
        print(f"    Local: {','.join(diff['local'])}")
        print(f"    Remote: {','.join(diff['remote'])}")
        print()
    elif diff['type'] == 'blank_column_1':
        print("  Blank column 1")
        print(f"  {diff['source']} has empty column-1 cells on line(s): {', '.join(str(line) for line in diff['lines'])}")
        print()
    elif diff['type'] == 'remote_only':
        print(f"  {diff['key']}")
        print(f"  Row only in Google Sheets")
        print()
    elif diff['type'] == 'local_only':
        print(f"  {diff['key']}")
        print(f"  Row only in HEAD-refreshed local copy")
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
