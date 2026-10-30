"""
Lists the translation keys that no source tree uses: candidates for deletion
from the shared translation sheet.

Usage (from the repository root):
    python3 scripts/find-unused-translation-keys.py . ../owlcms_68 ../tracker ../tracker-core

Every tree that shares the translation sheet must be listed. The keys are the
union of shared/src/main/resources/i18n/translation4.csv in the trees that have
one. Prints one "key<TAB>English" line per unused key.

A key counts as used when it appears in a source or template file (including
the XML inside .xlsx/.xls/.docx/.odt/.ods templates) and:
- it appears as a whole token (covers keys held in variables or enum names);
- it starts with a literal joined with + ("Record." + lift, X.class.getSimpleName() + ".")
  or with a template literal (`Prefix.${x}`);
- it starts with a dotted literal followed by "." ("AuditCheck.Integrity" passed
  to code that appends "." + value);
- it is a token followed by a literal suffix joined with + (key + "Confirm");
- it contains other characters (/, spaces) and appears verbatim.

The result is a candidate list: check each family by hand before deleting.
"""
import csv
import os
import re
import sys
import zipfile

CSV = 'shared/src/main/resources/i18n/translation4.csv'
TEXT = ('.java', '.js', '.mjs', '.cjs', '.ts', '.svelte', '.html', '.htm', '.ftl', '.css', '.json',
        '.properties', '.xml', '.vue', '.jsx', '.tsx', '.txt', '.yaml', '.yml')
ZIPS = ('.xlsx', '.xls', '.xlsm', '.docx', '.odt', '.ods')
SKIP_DIRS = {'.git', 'node_modules', 'target', 'build', 'dist', '.svelte-kit', 'docs', 'compliance',
             'i18n', '.vscode', 'playwright', '.gradle', 'coverage'}

TOKEN = re.compile(r'[A-Za-z0-9_][A-Za-z0-9_.\-]*')
PREFIX = re.compile(r'["\'`]([A-Za-z][A-Za-z0-9_.\-/]*[._\-][A-Za-z0-9_.\-/]*)["\'`]\s*\+')
TEMPLATE_PREFIX = re.compile(r'`([A-Za-z0-9_.\-]+)\$\{')
CLASS_PREFIX = re.compile(r'(\w+)\.class\.getSimpleName\(\)\s*\+\s*"([^"]*)"')
DOTTED_LITERAL = re.compile(r'"([A-Za-z][A-Za-z0-9_\-]*\.[A-Za-z0-9_.\-]*[A-Za-z0-9_])"')
SUFFIX = re.compile(r'\+\s*["\']([A-Za-z0-9_.\-]+)["\']')
PLAIN_KEY = re.compile(r'[A-Za-z0-9_.\-]+')


class Corpus:
    def __init__(self):
        self.tokens = set()
        self.prefixes = set()
        self.suffixes = set()
        self.texts = []

    def add(self, text):
        for token in TOKEN.findall(text):
            parts = token.strip('.-').split('.')
            for i in range(1, len(parts) + 1):
                self.tokens.add('.'.join(parts[:i]))
        self.prefixes.update(PREFIX.findall(text))
        self.prefixes.update(TEMPLATE_PREFIX.findall(text))
        self.prefixes.update(name + literal for name, literal in CLASS_PREFIX.findall(text))
        self.prefixes.update(literal + '.' for literal in DOTTED_LITERAL.findall(text))
        self.suffixes.update(SUFFIX.findall(text))
        self.texts.append(text)

    def scan(self, root):
        for dirpath, dirnames, filenames in os.walk(root):
            dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
            for name in filenames:
                path = os.path.join(dirpath, name)
                try:
                    if name.endswith(TEXT) and os.path.getsize(path) < 5_000_000:
                        with open(path, encoding='utf-8', errors='ignore') as f:
                            self.add(f.read())
                    elif name.endswith(ZIPS):
                        with zipfile.ZipFile(path) as z:
                            for member in z.namelist():
                                if member.endswith('.xml'):
                                    self.add(z.read(member).decode('utf-8', 'ignore'))
                except (OSError, zipfile.BadZipFile):
                    pass

    def uses(self, key):
        if key in self.tokens:
            return True
        if any(key.startswith(p) for p in self.prefixes if len(p) > 1):
            return True
        if any(key.endswith(s) and key[:-len(s)] in self.tokens for s in self.suffixes if len(s) < len(key)):
            return True
        return not PLAIN_KEY.fullmatch(key) and any(key in t for t in self.texts)


def main(roots):
    english = {}
    for root in roots:
        path = os.path.join(root, CSV)
        if os.path.exists(path):
            with open(path, encoding='utf-8') as f:
                for row in csv.reader(f):
                    if row and row[0] and row[0] != 'key':
                        english.setdefault(row[0], row[1] if len(row) > 1 else '')
    corpus = Corpus()
    for root in roots:
        corpus.scan(root)
    unused = [k for k in sorted(english) if not corpus.uses(k)]
    for key in unused:
        print(key + '\t' + english[key].replace('\n', ' ')[:90])
    print(f'{len(unused)} unused of {len(english)} keys', file=sys.stderr)


if __name__ == '__main__':
    main(sys.argv[1:] or ['.'])
