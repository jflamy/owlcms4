#!/usr/bin/env python3
import re
from pathlib import Path

DOCS = Path(__file__).resolve().parent.parent
md_files = {p.name: p for p in DOCS.glob('*.md')}
link_re = re.compile(r"\[.*?\]\((?!https?://)([^)\#]+)(?:#[^)]+)?\)")
img_re = re.compile(r"(?:!\[.*?\]|<img[^>]+src=)\(?[\"']?([^\)\"'>]+)[\"']?\)?")

links = {name: set() for name in md_files}
images_ref = {}
for name, p in md_files.items():
    text = p.read_text(encoding='utf-8', errors='ignore')
    for m in link_re.finditer(text):
        target = m.group(1).strip()
        if target.startswith('#') or target.startswith('mailto:'):
            continue
        tgt = target.split('#', 1)[0].split('?', 1)[0].strip()
        tgt_path = Path(tgt)
        if tgt_path.suffix == '':
            tgt_path = Path(str(tgt_path) + '.md')
        candidate = tgt_path.name
        if candidate in md_files:
            links[name].add(candidate)
    imgset = set()
    for m in img_re.finditer(text):
        src = m.group(1).strip()
        if src.startswith('http') or src.startswith('data:'):
            continue
        imgset.add(src.lstrip('./'))
    images_ref[name] = imgset

sidebar = DOCS / '_sidebar.md'
start_nodes = set()
if sidebar.exists():
    st = sidebar.read_text(encoding='utf-8', errors='ignore')
    for m in link_re.finditer(st):
        target = m.group(1).split('#', 1)[0].strip()
        if not target:
            continue
        tgt = Path(target)
        if tgt.suffix == '':
            tgt = Path(str(tgt) + '.md')
        if tgt.name in md_files:
            start_nodes.add(tgt.name)
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

(DOCS/'unreachable_reanalysis.txt').write_text('\n'.join(unreachable) + '\n')
(DOCS/'reachable_list.txt').write_text('\n'.join(reachable_sorted) + '\n')
(DOCS/'image_refs_by_reachability.txt').write_text('USED_BY_REACHABLE:\n' + '\n'.join(sorted(used_reachable)) + '\n\nUSED_ONLY_BY_UNREACHABLE_PAGES:\n' + '\n'.join(sorted(used_only_unreachable)) + '\n\nUNUSED_IMAGES:\n' + '\n'.join(sorted(unused)) + '\n')
print('Wrote unreachable_reanalysis.txt, reachable_list.txt, image_refs_by_reachability.txt')
