# Builds the "Junior and Senior Masters" fixture as a copy of a real masters database:
# everything is kept (competition, age groups with their 2026 categories, championships, platforms), but the
# athletes are reduced to 10 per gender per 5-year age slice from 35 to 64, picked evenly across body weights.
# The age groups M30/W30 stay and have no athletes: removing them is part of the scenario.
#
#   python3 build-fixture.py [source.json]

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SRC = Path('/Users/jflamy/git/meets/competitions/dev67/2026-10 IMWA Worlds/owlcmsDatabase_2026-09-28_19h36.json')
SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SRC
DST = ROOT / 'owlcms/src/test/resources/testDatabases/oldVeryOldMasters_2026.json'

BANDS = range(35, 65, 5)
PER_BAND = 10

d = json.loads(SRC.read_text(encoding='utf-8'))
year = d['competition']['competitionDate'][0]


def age(a):
    fb = a.get('fullBirthDate')
    return year - fb[0] if isinstance(fb, list) else None


def pick(candidates, n):
    candidates = sorted(candidates, key=lambda a: (a.get('bodyWeight') or 0, a['lastName'], a['firstName']))
    if len(candidates) <= n:
        return candidates
    return [candidates[round(i * (len(candidates) - 1) / (n - 1))] for i in range(n)]


selected = []
for gender in ('F', 'M'):
    for band in BANDS:
        pool = [a for a in d['athletes'] if a['gender'] == gender and age(a) is not None and band <= age(a) < band + 5]
        chosen = pick(pool, PER_BAND)
        print(f'{gender} {band}-{band + 4}: {len(pool)} available, {len(chosen)} kept')
        selected += chosen

# the real file groups athletes into sessions; keep the sessions that are still used
used_groups = {a['group'] for a in selected if a.get('group') is not None}
d['athletes'] = selected
d['groups'] = [g for g in d['groups'] if g['id'] in used_groups]

# the real file has the 30 age groups inactive; the scenario wants them active and empty
for g in d['ageGroups']:
    if g.get('championshipName') == 'Masters' and g['code'] in ('M30', 'W30'):
        g['active'] = True


DST.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding='utf-8')

cat_name = {c['id']: c.get('categoryName') or c.get('name') for g in d['ageGroups'] for c in g['categories']}
per_cat = {}
for a in selected:
    per_cat.setdefault(cat_name[a['participations'][0]['category']], []).append(a['bodyWeight'])
print()
for name in sorted(per_cat, key=lambda s: (s[0], int(s[1:3]), float(s.split()[1].lstrip('>')))):
    print(f'{name:10} {len(per_cat[name])}')
print(f'\n{len(selected)} athletes, {len(d["groups"])} sessions -> {DST.relative_to(ROOT)}')
print('age groups without athletes:', sorted(
    g['code'] + '/' + g['gender'] for g in d['ageGroups']
    if g.get('championshipName') == 'Masters' and not any(
        p['category'] in {c['id'] for c in g['categories']} for a in selected for p in a['participations'])))
for key in ('records', 'technicalOfficials', 'technicalOfficialsTimetable', 'platforms'):
    print(key, type(d[key]).__name__, len(d[key]))
