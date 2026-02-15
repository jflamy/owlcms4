import re
import zipfile

path = 'src/main/resources/templates/competitionBook/TeamsGlobalScoring-A4.xlsx'

with zipfile.ZipFile(path) as z:
    for name in z.namelist():
        if not name.endswith('.xml'):
            continue
        text = z.read(name).decode('utf-8', 'ignore')
        if '${' in text or 'jx:forEach' in text:
            print('PART', name)
            exprs = sorted(set(re.findall(r'\$\{[^}]+\}', text)))
            for expr in exprs:
                print('  ', expr)
            tags = sorted(set(re.findall(r'jx:[^\"]+', text)))
            for tag in tags:
                print('  ', tag)
