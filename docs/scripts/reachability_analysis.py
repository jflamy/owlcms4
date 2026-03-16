#!/usr/bin/env python3
import re
from pathlib import Path

DOCS = Path(__file__).resolve().parent.parent
REPORTS_DIR = DOCS / 'scripts' / 'archive' / 'reports'
EXCLUDED_PREFIXES = ('obsolete/',)


def is_included_markdown(path: Path) -> bool:
    rel = str(path.relative_to(DOCS)).replace('\\', '/')
    return not rel.startswith(EXCLUDED_PREFIXES)


md_files = {
    str(p.relative_to(DOCS)).replace('\\', '/'): p
    for p in DOCS.rglob('*.md')
    if is_included_markdown(p)
}
link_re = re.compile(r"\[.*?\]\((?!https?://)([^)\#]+)(?:#[^)]+)?\)")
href_re = re.compile(r"<a[^>]+href=[\"']([^\"']+)[\"']", re.IGNORECASE)
img_re = re.compile(r"(?:!\[.*?\]|<img[^>]+src=)\(?[\"']?([^\)\"'>]+)[\"']?\)?")


def normalize_md_path(path: Path) -> str:
    return str(path).replace('\\', '/')


def resolve_doc_target(source: Path, target: str) -> str | None:
    target = target.strip()
    if not target or target.startswith('mailto:'):
        return None

    # Docsify-style hash routes such as "#/Countries" or "./#/Countries" point to docs-root pages.
    if '#/' in target:
        target = target.split('#/', 1)[1].strip()
    else:
        target = target.split('#', 1)[0].split('?', 1)[0].strip()

    if not target:
        return None

    tgt_path = Path(target)
    if tgt_path.suffix == '':
        tgt_path = Path(str(tgt_path) + '.md')
    if tgt_path.suffix.lower() != '.md':
        return None

    if target.startswith('/'):
        resolved = DOCS / tgt_path.relative_to('/')
    else:
        resolved = (source.parent / tgt_path).resolve()

    try:
        rel = normalize_md_path(resolved.relative_to(DOCS))
    except ValueError:
        return None

    if rel in md_files:
        return rel
    return None

links = {name: set() for name in md_files}
images_ref = {}
for name, p in md_files.items():
    text = p.read_text(encoding='utf-8', errors='ignore')
    for raw_target in [m.group(1) for m in link_re.finditer(text)] + [m.group(1) for m in href_re.finditer(text)]:
        target = resolve_doc_target(p, raw_target)
        if target:
            links[name].add(target)
    imgset = set()
    for m in img_re.finditer(text):
        src = m.group(1).strip()
        if src.startswith('http') or src.startswith('data:'):
            continue
        img_path = Path(src)
        if src.startswith('/'):
            resolved_img = DOCS / img_path.relative_to('/')
        else:
            resolved_img = (p.parent / img_path).resolve()
        try:
            imgset.add(normalize_md_path(resolved_img.relative_to(DOCS)))
        except ValueError:
            continue
    images_ref[name] = imgset

sidebar = DOCS / '_sidebar.md'
start_nodes = set()
if sidebar.exists():
    st = sidebar.read_text(encoding='utf-8', errors='ignore')
    for raw_target in [m.group(1) for m in link_re.finditer(st)] + [m.group(1) for m in href_re.finditer(st)]:
        target = resolve_doc_target(sidebar, raw_target)
        if target:
            start_nodes.add(target)
if 'index.md' in md_files:
    start_nodes.add('index.md')

reachable = set()
stack = list(start_nodes)
while stack:
    cur = stack.pop()
    if cur in reachable:
        continue
    reachable.add(cur)
    for tgt in links.get(cur, set()):
        if tgt not in reachable:
            stack.append(tgt)

all_md = set(md_files.keys())
all_md.discard('_sidebar.md')
unreachable = sorted(all_md - reachable)
reachable_sorted = sorted(reachable)

all_images = sorted([str(p.relative_to(DOCS)).replace('\\','/') for p in (DOCS/'img').rglob('*') if p.is_file()] + [str(p.relative_to(DOCS)).replace('\\','/') for p in (DOCS/'nimg').rglob('*') if p.is_file()])
referenced = {img: {'used_by_reachable': set(), 'used_by_unreachable': set()} for img in all_images}

for md, imgs in images_ref.items():
    for img in imgs:
        imgn = img
        # direct match
        if imgn in referenced:
            if md in reachable:
                referenced[imgn]['used_by_reachable'].add(md)
            else:
                referenced[imgn]['used_by_unreachable'].add(md)
        else:
            # try basename match
            b = Path(imgn).name
            matches = [x for x in all_images if x.endswith('/'+b) or x==b]
            for m in matches:
                if md in reachable:
                    referenced[m]['used_by_reachable'].add(md)
                else:
                    referenced[m]['used_by_unreachable'].add(md)

used_reachable = [img for img,data in referenced.items() if data['used_by_reachable']]
used_only_unreachable = [img for img,data in referenced.items() if (not data['used_by_reachable']) and data['used_by_unreachable']]
unused = [img for img,data in referenced.items() if (not data['used_by_reachable']) and (not data['used_by_unreachable'])]

REPORTS_DIR.mkdir(parents=True, exist_ok=True)
(REPORTS_DIR/'unreachable_reanalysis.txt').write_text('\n'.join(unreachable) + '\n')
(REPORTS_DIR/'reachable_list.txt').write_text('\n'.join(reachable_sorted) + '\n')
(REPORTS_DIR/'image_refs_by_reachability.txt').write_text('USED_BY_REACHABLE:\n' + '\n'.join(sorted(used_reachable)) + '\n\nUSED_ONLY_BY_UNREACHABLE_PAGES:\n' + '\n'.join(sorted(used_only_unreachable)) + '\n\nUNUSED_IMAGES:\n' + '\n'.join(sorted(unused)) + '\n')
print('Wrote reports to docs/scripts/archive/reports')
