#!/usr/bin/env python3
from pathlib import Path
import re

DOCS = Path(__file__).resolve().parent
REPORTS = DOCS / 'archive' / 'reports'
MOVELOG = REPORTS / 'move_log.txt'
IMG_TSV = REPORTS / 'images_to_archive.tsv'
MD_TSV = REPORTS / 'md_to_archive.tsv'

# outputs
IMG_TSV_UPD = REPORTS / 'images_to_archive_with_status.tsv'
MD_TSV_UPD = REPORTS / 'md_to_archive_with_status.tsv'
IMG_MOVED = REPORTS / 'images_moved.txt'
IMG_MISSING = REPORTS / 'images_missing.txt'
MD_MOVED = REPORTS / 'md_moved.txt'
MD_MISSING = REPORTS / 'md_missing.txt'

# parse move log
log = MOVELOG.read_text(encoding='utf-8') if MOVELOG.exists() else ''
lines = log.splitlines()
md_status = {}
img_status = {}  # key: original path -> status
# parse lines
for ln in lines:
    ln=ln.strip()
    if ln.startswith('MOVED MD:'):
        # format: MOVED MD: filename -> archive/md/
        m = re.match(r'MOVED MD: (.+?) ->', ln)
        if m:
            md = m.group(1).strip()
            md_status[md] = 'MOVED'
    elif ln.startswith('MISSING MD:'):
        m = re.match(r'MISSING MD: (.+)', ln)
        if m:
            md = m.group(1).strip()
            md_status[md] = 'MISSING'
    elif ln.startswith('MOVED IMG:'):
        # format: MOVED IMG: img/path -> archive/img/./
        m = re.match(r'MOVED IMG: (.+?) ->', ln)
        if m:
            img = m.group(1).strip()
            img_status[img] = 'MOVED'
    elif ln.startswith('FOUND & MOVED IMG:'):
        # FOUND & MOVED IMG: ./some/path -> archive/img/... (matched basename)
        m = re.match(r'FOUND & MOVED IMG: (.+?) -> .+\(matched basename', ln)
        if m:
            found = m.group(1).strip()
            # map by basename so we can attribute to original requested path
            b = Path(found).name
            img_status.setdefault(b, 'FOUND_MOVED')
    elif ln.startswith('MISSING IMG:'):
        m = re.match(r'MISSING IMG: (.+)', ln)
        if m:
            img = m.group(1).strip()
            img_status[img] = 'MISSING'
    elif ln.startswith('AMBIGUOUS IMG:'):
        m = re.match(r'AMBIGUOUS IMG: (.+?) -> multiple matches:', ln)
        if m:
            img = m.group(1).strip()
            img_status[img] = 'AMBIGUOUS'

# Update MD TSV
if MD_TSV.exists():
    updated_md_lines = []
    md_moved = []
    md_missing = []
    for line in MD_TSV.read_text(encoding='utf-8').splitlines():
        if not line.strip():
            continue
        parts = line.split('\t')
        md = parts[0].strip()
        reason = parts[1].strip() if len(parts)>1 else ''
        status = md_status.get(md,'NOT_ATTEMPTED')
        updated_md_lines.append(f"{md}\t{reason}\t{status}")
        if status=='MOVED': md_moved.append(md)
        if status in ('MISSING','NOT_ATTEMPTED'): md_missing.append(md)
    MD_TSV_UPD.write_text('\n'.join(updated_md_lines)+"\n",encoding='utf-8')
    MD_MOVED.write_text('\n'.join(md_moved)+"\n",encoding='utf-8')
    MD_MISSING.write_text('\n'.join(md_missing)+"\n",encoding='utf-8')

# Update IMG TSV
if IMG_TSV.exists():
    updated_img_lines = []
    img_moved_list = []
    img_missing_list = []
    for line in IMG_TSV.read_text(encoding='utf-8').splitlines():
        if not line.strip():
            continue
        parts = line.split('\t')
        img = parts[0].strip()
        reason = parts[1].strip() if len(parts)>1 else ''
        status = 'NOT_ATTEMPTED'
        # direct match
        if img in img_status:
            status = img_status[img]
        else:
            # check basename matches
            b = Path(img).name
            if b in img_status:
                status = img_status[b]
        updated_img_lines.append(f"{img}\t{reason}\t{status}")
        if status in ('MOVED','FOUND_MOVED'):
            img_moved_list.append(img)
        if status in ('MISSING','AMBIGUOUS','NOT_ATTEMPTED'):
            img_missing_list.append(img)
    IMG_TSV_UPD.write_text('\n'.join(updated_img_lines)+"\n",encoding='utf-8')
    IMG_MOVED.write_text('\n'.join(img_moved_list)+"\n",encoding='utf-8')
    IMG_MISSING.write_text('\n'.join(img_missing_list)+"\n",encoding='utf-8')

print('Reports updated:')
print(' -', MD_TSV_UPD)
print(' -', MD_MOVED)
print(' -', MD_MISSING)
print(' -', IMG_TSV_UPD)
print(' -', IMG_MOVED)
print(' -', IMG_MISSING)
