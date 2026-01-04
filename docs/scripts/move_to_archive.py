#!/usr/bin/env python3
"""
Safely move Markdown and image files listed in archive/reports into
archive/md/ and archive/img/ respectively.

Rules:
- For MD: expect filenames (e.g. `OBSReplays.md`). Move if exists under docs/.
- For images: prefer exact relative path (img/... or nimg/...). If missing, try to
  locate by basename across docs/ (exclude archive/ and .git/). If exactly one
  match is found, move that file and preserve a sensible subpath under
  archive/img/. If multiple matches, do NOT move and log ambiguity.

Produces archive/reports/move_log.txt
"""

from pathlib import Path
import shutil

DOCS = Path(__file__).resolve().parent
ARCHIVE_MD = DOCS / 'archive' / 'md'
ARCHIVE_IMG = DOCS / 'archive' / 'img'
REPORTS = DOCS / 'archive' / 'reports'
LOG = REPORTS / 'move_log.txt'
MD_LIST = REPORTS / 'md_to_archive.txt'
IMG_LIST = REPORTS / 'images_to_archive.txt'

def log(msg: str):
    with LOG.open('a', encoding='utf-8') as f:
        f.write(msg + '\n')
    print(msg)

def ensure_dirs():
    ARCHIVE_MD.mkdir(parents=True, exist_ok=True)
    ARCHIVE_IMG.mkdir(parents=True, exist_ok=True)
    REPORTS.mkdir(parents=True, exist_ok=True)
    if not LOG.exists():
        LOG.write_text('Move log - generated\n', encoding='utf-8')

def move_md_files():
    if not MD_LIST.exists():
        log(f'MD list not found: {MD_LIST}')
        return
    for line in MD_LIST.read_text(encoding='utf-8').splitlines():
        md = line.strip()
        if not md:
            continue
        # sanitize
        md = md.replace('\r','')
        src = DOCS / md
        if src.exists():
            dest = ARCHIVE_MD / src.name
            shutil.move(str(src), str(dest))
            log(f'MOVED MD: {src} -> {dest}')
        else:
            log(f'MISSING MD: {src}')


def find_unique_by_basename(basename: str):
    # search under DOCS for files matching basename, excluding archive and .git
    matches = []
    for p in DOCS.rglob(basename):
        # skip anything under archive/ or .git
        if 'archive' in p.parts or '.git' in p.parts:
            continue
        matches.append(p)
    return matches


def move_image_files():
    if not IMG_LIST.exists():
        log(f'Image list not found: {IMG_LIST}')
        return
    for line in IMG_LIST.read_text(encoding='utf-8').splitlines():
        img = line.strip()
        if not img:
            continue
        img = img.replace('\r','')
        src = DOCS / img
        if src.exists():
            # preserve subpath after img/ or nimg/
            parts = img.split('/')
            if parts[0] in ('img','nimg'):
                sub = Path('/'.join(parts[1:]))
            else:
                sub = Path('/'.join(parts))
            dest_dir = ARCHIVE_IMG / sub.parent
            dest_dir.mkdir(parents=True, exist_ok=True)
            dest = dest_dir / src.name
            shutil.move(str(src), str(dest))
            log(f'MOVED IMG: {src} -> {dest}')
            continue
        # try basename search
        b = Path(img).name
        matches = find_unique_by_basename(b)
        if len(matches) == 1:
            found = matches[0]
            # create dest subpath mirroring original folder name (if possible)
            # use the relative path under DOCS, strip leading folders until we find 'img' or 'nimg', else use parent
            try:
                rel = found.relative_to(DOCS)
                if rel.parts[0] in ('img','nimg'):
                    sub = Path('/'.join(rel.parts[1:]))
                else:
                    sub = Path('/'.join(rel.parts))
            except Exception:
                sub = found.name
            dest_dir = ARCHIVE_IMG / sub.parent
            dest_dir.mkdir(parents=True, exist_ok=True)
            dest = dest_dir / found.name
            shutil.move(str(found), str(dest))
            log(f'FOUND & MOVED IMG: {found} -> {dest} (basename match)')
        elif len(matches) > 1:
            log(f'AMBIGUOUS IMG: {img} -> multiple matches:')
            for m in matches:
                log(f'  - {m}')
        else:
            log(f'MISSING IMG: {img}')


def main():
    ensure_dirs()
    log('Starting move operation')
    move_md_files()
    move_image_files()
    log('Move operation completed')

if __name__ == '__main__':
    main()
