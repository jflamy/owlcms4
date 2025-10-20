#!/usr/bin/env python3
"""
Extract zh-TW column from translation4.csv to identify entries needing translation.
Handles complex CSV with quoted fields properly.
"""

import csv
import sys

# Read the CSV file
csv_file = r"c:\Dev\git\owlcms_v23stable\owlcms_v23master\shared\src\main\resources\i18n\translation4.csv"

try:
    with open(csv_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        header = next(reader)
        
        # Find column indices
        key_idx = header.index('key')
        en_idx = header.index('en')
        zh_tw_idx = header.index('zh-TW')
        
        print(f"Key column: {key_idx}, English: {en_idx}, zh-TW: {zh_tw_idx}")
        print(f"Total columns: {len(header)}\n")
        
        # Collect entries
        entries = []
        for row_num, row in enumerate(reader, start=2):
            if len(row) > zh_tw_idx:
                key = row[key_idx].strip()
                en_text = row[en_idx].strip() if en_idx < len(row) else ""
                zh_text = row[zh_tw_idx].strip() if zh_tw_idx < len(row) else ""
                
                # Check if zh-TW is empty or just English
                needs_translation = (not zh_text or zh_text == en_text or zh_text.startswith(en_text[:20]))
                
                if needs_translation and en_text and not en_text.startswith('{') and len(en_text) > 3:
                    entries.append((row_num, key, en_text[:80], zh_text[:80] if zh_text else "EMPTY"))
        
        # Print first 30 entries needing translation
        print(f"Found {len(entries)} entries potentially needing zh-TW translation\n")
        print("First 30 entries:\n")
        for row_num, key, en, zh in entries[:30]:
            print(f"Line {row_num}: {key}")
            print(f"  EN: {en}")
            print(f"  ZH: {zh}\n")

except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
    sys.exit(1)
