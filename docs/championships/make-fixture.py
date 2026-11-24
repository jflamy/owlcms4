# Builds the "Junior and Senior Masters" fixture from the masters demo database:
#  - masters age groups get the 2026-08 categories (AgeGroups_2026-08.xlsx);
#  - athletes are spread over ages 30 to 64, two weight categories per age slice,
#    each registered and participating in the category that fits age and body weight.
#
#   python3 make-fixture.py

import copy
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'owlcms/src/test/resources/testDatabases/mastersDemoDatabase_2025-03-20_11h03.json'
DST = ROOT / 'owlcms/src/test/resources/testDatabases/oldVeryOldMasters_2026.json'

CATS = {'M': [60, 65, 70, 75, 85, 95, 110, 999], 'F': [49, 53, 57, 61, 69, 77, 86, 999]}
COMPETITION_YEAR = 2025

# per age slice: two categories, two athletes each (men) / one each (women)
MEN = {30: (85, 95), 35: (75, 85), 40: (85, 95), 45: (85, 110), 50: (95, 110), 55: (85, 95), 60: (75, 85)}
WOMEN = {30: (61, 69), 35: (69, 77), 40: (61, 69), 45: (69, 77), 50: (69, 86), 55: (61, 77), 60: (69, 77)}
EXTRA_WOMEN = [('Moreau', 'Claire'), ('Tremblay', 'Julie'), ('Nguyen', 'Linh'), ('Kowalski', 'Anna'), ('Fischer', 'Petra')]

d = json.loads(SRC.read_text(encoding='utf-8'))
next_id = max(c['id'] for g in d['ageGroups'] for c in g['categories']) + 1000
max_athlete_id = max(a['id'] for a in d['athletes'])

# 1. masters age groups with the 2026 categories
cat_by_group = {}
for g in d['ageGroups']:
    if g.get('championshipName') != 'Masters':
        continue
    gender = g['gender']
    letter = 'M' if gender == 'M' else 'F'
    cats, low = [], 0.0
    previous = CATS[gender][-2]
    for upper in CATS[gender]:
        bound = f'>{previous}' if upper == 999 else str(upper)
        cats.append({
            'id': next_id, 'maximumWeight': float(upper), 'minimumWeight': low, 'active': g['active'],
            'ageGroup': g['key'], 'code': f"{g['code']}_{letter}{upper}", 'gender': gender,
            'name': f"{g['code']} {bound}", 'qualifyingTotal': 0, 'wrJr': 0, 'wrSr': 0, 'wrYth': 0,
            'translatedGender': g['translatedGender'], 'upperBound': bound,
        })
        next_id += 1000
        low = float(upper)
    g['categories'] = cats
    cat_by_group[(gender, g['minAge'])] = (g, cats)


def category_for(gender, age, weight):
    for (g_gender, min_age), (g, cats) in cat_by_group.items():
        if g_gender == gender and g['minAge'] <= age <= g['maxAge']:
            return g, next(c for c in cats if c['minimumWeight'] < weight <= c['maximumWeight'])
    raise ValueError(f'no age group for {gender} {age}')


def weight_in(gender, upper, k):
    cats = CATS[gender]
    i = cats.index(upper)
    low = 0 if i == 0 else cats[i - 1]
    return round(low + (upper - low) * (0.35 + 0.4 * (k % 2)), 2)


# 2. enough women for two per slice
women = [a for a in d['athletes'] if a['gender'] == 'F']
for n, (last, first) in enumerate(EXTRA_WOMEN):
    clone = copy.deepcopy(women[n % len(women)])
    max_athlete_id += 1
    clone.update(id=max_athlete_id, lastName=last, firstName=first, lotNumber=100 + n)
    d['athletes'].append(clone)


# 3. ages, body weights, registration category and the one participation
def place(athletes, plan, gender):
    slots = [(band, upper) for band, pair in plan.items() for upper in pair for _ in range(2 if gender == 'M' else 1)]
    assert len(slots) == len(athletes), (gender, len(slots), len(athletes))
    for k, (a, (band, upper)) in enumerate(zip(athletes, slots)):
        age = band + 2
        a['fullBirthDate'] = [COMPETITION_YEAR - age, 1, 15]
        a['bodyWeight'] = weight_in(gender, upper, k)
        g, cat = category_for(gender, age, a['bodyWeight'])
        a['category'] = cat['id']
        a['ageGroupDisplayName'] = g['code']
        template = a['participations'][0]
        p = {key: (0 if key.endswith('Rank') else value) for key, value in template.items()}
        p.update(athlete=a['id'], category=cat['id'], id={'athleteId': a['id'], 'categoryId': cat['id']},
                 teamMember=True, championshipType='MASTERS')
        a['participations'] = [p]


place([a for a in d['athletes'] if a['gender'] == 'M'], MEN, 'M')
place([a for a in d['athletes'] if a['gender'] == 'F'], WOMEN, 'F')

DST.write_text(json.dumps(d, ensure_ascii=False, indent=1), encoding='utf-8')

summary = {}
for a in d['athletes']:
    cat = next(c for g in d['ageGroups'] for c in g['categories'] if c['id'] == a['category'])
    summary.setdefault(cat['name'], []).append(f"{a['lastName']} {a['bodyWeight']}")
for name in sorted(summary, key=lambda s: (s[0], int(s[1:3]), s)):
    print(f'{name:10} {len(summary[name])}  {", ".join(summary[name])}')
print(f'\n{len(d["athletes"])} athletes -> {DST.relative_to(ROOT)}')
