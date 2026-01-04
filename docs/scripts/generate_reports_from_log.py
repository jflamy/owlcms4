#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
REPORTS = ROOT.parent / 'archive' / 'reports'
MOVELOG = REPORTS / 'move_log.txt'
IMG_TSV = REPORTS / 'images_to_archive.tsv'
MD_TSV = REPORTS / 'md_to_archive.tsv'

IMG_TSV_OUT = REPORTS / 'images_to_archive_with_status.tsv'
MD_TSV_OUT = REPORTS / 'md_to_archive_with_status.tsv'
IMG_MOVED = REPORTS / 'images_moved.txt'
IMG_MISSING = REPORTS / 'images_missing.txt'
MD_MOVED = REPORTS / 'md_moved.txt'
MD_MISSING = REPORTS / 'md_missing.txt'

log_text = MOVELOG.read_text(encoding='utf-8') if MOVELOG.exists() else ''
md_status = {}
img_status = {}

for ln in log_text.splitlines():
    ln = ln.strip()
    if ln.startswith('MOVED MD:'):
        m = re.match(r'MOVED MD: (.+?) ->', ln)
        if m:
            md_status[m.group(1).strip()] = 'MOVED'
    elif ln.startswith('MISSING MD:'):
        m = re.match(r'MISSING MD: (.+)', ln)
        if m:
            md_status[m.group(1).strip()] = 'MISSING'
    elif ln.startswith('MOVED IMG:'):
        m = re.match(r'MOVED IMG: (.+?) ->', ln)
        if m:
            img_status[m.group(1).strip()] = 'MOVED'
    elif ln.startswith('FOUND & MOVED IMG:'):
        m = re.match(r'FOUND & MOVED IMG: (.+?) ->', ln)
        if m:
            b = Path(m.group(1).strip()).name
            img_status[b] = 'FOUND_MOVED'
    elif ln.startswith('MISSING IMG:'):
        m = re.match(r'MISSING IMG: (.+)', ln)
        if m:
            img_status[m.group(1).strip()] = 'MISSING'
    elif ln.startswith('AMBIGUOUS IMG:'):
        m = re.match(r'AMBIGUOUS IMG: (.+)', ln)
        if m:
            img_status[m.group(1).strip()] = 'AMBIGUOUS'

# Process MD tsv
md_moved=[]
md_missing=[]
out_md_lines=[]
if MD_TSV.exists():
    for ln in MD_TSV.read_text(encoding='utf-8').splitlines():
        if not ln.strip():
            continue
        parts = ln.split('\t')
        md = parts[0].strip()
        reason = parts[1].strip() if len(parts)>1 else ''
        status = md_status.get(md,'NOT_ATTEMPTED')
        out_md_lines.append(f"{md}\t{reason}\t{status}")
        if status=='MOVED': md_moved.append(md)
        if status in ('MISSING','NOT_ATTEMPTED'): md_missing.append(md)
    MD_TSV_OUT.write_text('\n'.join(out_md_lines)+"\n", encoding='utf-8')
    MD_MOVED.write_text('\n'.join(md_moved)+"\n", encoding='utf-8')
    MD_MISSING.write_text('\n'.join(md_missing)+"\n", encoding='utf-8')

# Process IMG tsv
img_moved=[]
img_missing=[]
out_img_lines=[]
if IMG_TSV.exists():
    for ln in IMG_TSV.read_text(encoding='utf-8').splitlines():
        if not ln.strip():
            continue
        parts = ln.split('\t')
        img = parts[0].strip()
        reason = parts[1].strip() if len(parts)>1 else ''
        status = 'NOT_ATTEMPTED'
        if img in img_status:
            status = img_status[img]
        else:
            b = Path(img).name
            if b in img_status:
                status = img_status[b]
        out_img_lines.append(f"{img}\t{reason}\t{status}")
        if status in ('MOVED','FOUND_MOVED'):
            img_moved.append(img)
        if status in ('MISSING','AMBIGUOUS','NOT_ATTEMPTED'):
            img_missing.append(img)
    IMG_TSV_OUT.write_text('\n'.join(out_img_lines)+"\n", encoding='utf-8')
    IMG_MOVED.write_text('\n'.join(img_moved)+"\n", encoding='utf-8')
    IMG_MISSING.write_text('\n'.join(img_missing)+"\n", encoding='utf-8')

print('Generated updated reports in', REPORTS)
