#!/usr/bin/env python3
import re
import os
import csv
from pathlib import Path

ROOT = Path(r"c:\Dev\git\owlcms_v23")
CSV_PATH = ROOT / "shared" / "src" / "main" / "resources" / "i18n" / "translation4.csv"

# Safety: never open the canonical translations file for writing from this script.
# If someone later edits this script and attempts to write to CSV_PATH, the
# helper below will raise an explicit error instead of accidentally modifying
# `translation4.csv`.
PROTECT_CSV = True

def safe_open(path, mode='r', *args, **kwargs):
    """Open files but refuse to open CSV_PATH for any write mode when
    PROTECT_CSV is True.
    """
    p = Path(path).resolve()
    try:
        csv_resolved = CSV_PATH.resolve()
    except Exception:
        csv_resolved = None
    if PROTECT_CSV and csv_resolved is not None and p == csv_resolved:
        # modes that imply writing: w, a, x, +
        if any(ch in mode for ch in ('w', 'a', 'x', '+')):
            raise RuntimeError(f"Refusing to open '{CSV_PATH}' for mode '{mode}' (PROTECT_CSV=True)")
    return open(path, mode, *args, **kwargs)

# Match Translator.translate("literal") where the first argument is a single
# string literal. This intentionally does NOT match cases like
# Translator.translate("prefix" + var) or Translator.translate(someVar).
# We capture the inner string contents (group 2) while allowing escaped
# characters inside the Java string literal.
translator_re = re.compile(r'Translator\.translate\s*\(\s*("((?:\\.|[^"\\])*)")\s*(?:,|\))')
# enum constants: lines inside enum with UPPER_CASE names followed by (, or ;
enum_const_re = re.compile(r'^\s*([A-Z0-9_]+)\s*(?:\(|,|;)', re.M)

code_keys = set()
enum_keys = set()

# Walk Java files
for dirpath, dirnames, filenames in os.walk(ROOT):
    # skip target directories to be faster
    if 'target' in dirpath.split(os.sep):
        continue
    for fn in filenames:
        if not fn.endswith('.java'):
            continue
        fp = Path(dirpath) / fn
        try:
            text = fp.read_text(encoding='utf-8')
        except Exception:
            try:
                text = fp.read_text(encoding='latin-1')
            except Exception:
                continue
        for m in translator_re.finditer(text):
            # m.group(2) is the inner contents of the Java string literal
            raw = m.group(2)
            # unescape common Java escapes (\n, \t, \", \\ and unicode escapes)
            try:
                # Use python's unicode_escape to decode backslash escapes.
                unescaped = bytes(raw, 'utf-8').decode('unicode_escape')
            except Exception:
                unescaped = raw
            code_keys.add(unescaped)
        # If this is PreCompetitionTemplates.java, extract enum constants
        if fn == 'PreCompetitionTemplates.java' or 'PreCompetitionTemplates' in fn:
            for m in enum_const_re.finditer(text):
                enum_keys.add(m.group(1))

# Parse CSV keys (first column)
csv_keys = set()
if CSV_PATH.exists():
    try:
        # use safe_open to ensure we never accidentally open the CSV for writing
        with safe_open(CSV_PATH, 'r', newline='') as csvfile:
            reader = csv.reader(csvfile)
            for row in reader:
                if not row:
                    continue
                key = row[0].strip()
                if key:
                    csv_keys.add(key)
    except Exception as e:
        print(f"Error reading CSV: {e}")
else:
    print(f"CSV not found at {CSV_PATH}")

# Add enum keys into code_keys (since enum.name() is used as key in some places)
all_code_keys = set(code_keys) | set(enum_keys)

missing_in_csv = sorted([k for k in all_code_keys if k not in csv_keys])
unused_in_code = sorted([k for k in csv_keys if k not in all_code_keys])

# Print summaries
print("# Summary from extract_i18n_keys.py")
print(f"Total literal keys found in Java sources: {len(code_keys)}")
print(f"Total enum keys from PreCompetitionTemplates: {len(enum_keys)}")
print(f"Total CSV keys: {len(csv_keys)}")
print(f"Total distinct code keys (literals + enum): {len(all_code_keys)}")
print()
print("--- Keys used in code (sample, sorted) ---")
for k in sorted(all_code_keys)[:200]:
    print(k)
print()
print("--- Keys missing in CSV (code -> CSV) ---")
for k in missing_in_csv:
    print(k)
print()
print("--- Keys in CSV but not referenced in code (CSV -> code) (sample) ---")
for k in unused_in_code[:200]:
    print(k)

# Also write results to files for review
outdir = ROOT / 'tools' / 'i18n_extraction_output'
outdir.mkdir(parents=True, exist_ok=True)
with (outdir / 'code_keys.txt').open('w', encoding='utf-8') as f:
    for k in sorted(all_code_keys):
        f.write(k + '\n')
with (outdir / 'csv_keys.txt').open('w', encoding='utf-8') as f:
    for k in sorted(csv_keys):
        f.write(k + '\n')
with (outdir / 'missing_in_csv.txt').open('w', encoding='utf-8') as f:
    for k in missing_in_csv:
        f.write(k + '\n')
with (outdir / 'unused_in_code.txt').open('w', encoding='utf-8') as f:
    for k in unused_in_code:
        f.write(k + '\n')

print(f"\nWrote outputs to {outdir}")
