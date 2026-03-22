"""
Generate JXLS3 TeamResults templates (A4 and Letter variants).

Three sheets per file: Men, Women, Mixed.
Post-processing in JXLSTeamResultsSheet.postProcess() removes sheets
whose bean list was absent (empty gender group).

The template uses nested jx:each to iterate:
  - Outer: team items (TeamTreeItem where team != null)
  - Inner: team members (TeamTreeItem where athlete != null)

Per-sheet display control beans (prefix m/w/mw):
  mShowPoints / wShowPoints / mwShowPoints  - Boolean: show the points column
  mScoringTitle / wScoringTitle / mwScoringTitle - String: score column heading

Beans available:
  mTeamItems / wTeamItems / mwTeamItems - List<TeamTreeItem>
  Each TeamTreeItem has:
    - getName(), getGender(), getPoints(), getScore(), getCounted(), getSize()
    - getSortedTeamMembers() -> List<TeamTreeItem> (athlete children)
  Each member TeamTreeItem has:
    - getName(), getCategory(), getPoints(), getScore()
    - isDone()
  Also: competition, championship, ageGroupPrefix, gender, t (translations)

Display logic per sheet:
  Points col (E):  team sum hidden when score-based (xxxShowPoints); member points always shown
  Score  col (G):  always shown, hide individual zeros
"""
import xlsxwriter
import os

TEMPLATE_DIR = os.path.dirname(os.path.abspath(__file__))

# Paper sizes: A4 = 9, US Letter = 1
VARIANTS = [
    ('TeamResults-A4.xlsx', 9),
    ('TeamResults-Letter.xlsx', 1),
]

for filename, paper_size in VARIANTS:
    output = os.path.join(TEMPLATE_DIR, filename)
    wb = xlsxwriter.Workbook(output, {'strings_to_urls': False})

    # ---- Styles (must be created per workbook) ----
    title_fmt = wb.add_format({'font_name': 'Arial', 'bold': True, 'font_size': 14})
    header_fmt = wb.add_format({'font_name': 'Arial', 'bold': True, 'font_size': 11})
    team_fmt = wb.add_format({
        'font_name': 'Arial', 'bold': True, 'font_size': 11,
        'bg_color': '#E2EFDA'
    })
    team_right_fmt = wb.add_format({
        'font_name': 'Arial', 'bold': True, 'font_size': 11,
        'bg_color': '#E2EFDA', 'align': 'right'
    })
    col_hdr_fmt = wb.add_format({
        'font_name': 'Arial', 'bold': True, 'font_size': 11,
        'bg_color': '#D9E1F2', 'border': 1
    })
    data_fmt = wb.add_format({
        'font_name': 'Arial', 'font_size': 10, 'border': 1
    })
    data_right_fmt = wb.add_format({
        'font_name': 'Arial', 'font_size': 10, 'border': 1, 'align': 'right'
    })
    team_score_fmt = wb.add_format({
        'font_name': 'Arial', 'bold': True, 'font_size': 11,
        'bg_color': '#E2EFDA', 'align': 'right', 'num_format': '0.00'
    })
    data_score_fmt = wb.add_format({
        'font_name': 'Arial', 'font_size': 10, 'border': 1, 'align': 'right',
        'num_format': '0.00'
    })


    def build_sheet(sheet_title, items_bean, gender_key, prefix):
        """
        Build one team-results sheet.
        items_bean: "mTeamItems" / "wTeamItems" / "mwTeamItems"
        gender_key: "Gender.M" / "Gender.F" / "Gender.MF"
        prefix: "m" / "w" / "mw"
        """
        ws = wb.add_worksheet(sheet_title)
        ws.set_landscape()
        ws.set_paper(paper_size)
        ws.set_margins(left=0.4, right=0.4, top=0.6, bottom=0.4)
        ws.fit_to_pages(1, 0)

        col_widths = [4, 22, 4, 20, 10, 8, 12]
        for i, w in enumerate(col_widths):
            ws.set_column(i, i, w)

        showPts = prefix + 'ShowPoints'
        scoringTitle = prefix + 'ScoringTitle'

        # Row 0 (1 in Excel): title + jx:area
        ws.write(0, 0, '${competition.competitionName}', title_fmt)
        ws.write_comment('A1', 'jx:area(lastCell="G7")', {'author': 'owlcms', 'width': 300, 'height': 100})

        # Row 1 (2): championship alone left, ageGroupPrefix + gender together
        ws.write(1, 0,
                 '${championship != null ? championship.name : ""}',
                 header_fmt)
        ws.write(1, 3,
                 '${ageGroupPrefix != null ? ageGroupPrefix : ""}'
                 ' ${t.get("' + gender_key + '")}',
                 header_fmt)

        # Row 2 (3): outer loop — team summary
        ws.write(2, 0, '', team_fmt)
        ws.write_comment('A3', 'jx:each(items="' + items_bean + '" var="team" lastCell="G7")',
                         {'author': 'owlcms', 'width': 400, 'height': 100})
        ws.write(2, 1, '${team.name}', team_fmt)
        ws.write(2, 2, '', team_fmt)
        ws.write(2, 3, '', team_fmt)
        # Points: show only when showPoints is true; hide 0
        ws.write(2, 4,
                 '${' + showPts + ' && team.points != 0 ? team.points : ""}',
                 team_right_fmt)
        ws.write(2, 5, '${team.counted}/${team.size}', team_right_fmt)
        # Score: show only when NOT points-based (score-based section); hide 0
        ws.write(2, 6,
                 '${!' + showPts + ' && team.score != 0 ? team.score : ""}',
                 team_score_fmt)

        # Row 3 (4): column headers
        ws.write(3, 0, '#', col_hdr_fmt)
        ws.write(3, 1, '${t.get("Name")}', col_hdr_fmt)
        ws.write(3, 2, '', col_hdr_fmt)
        ws.write(3, 3, '${t.get("Results.Category")}', col_hdr_fmt)
        ws.write(3, 4, '${t.get("Results.Points")}', col_hdr_fmt)
        ws.write(3, 5, '${t.get("Done")}', col_hdr_fmt)
        ws.write(3, 6, '${' + scoringTitle + '}', col_hdr_fmt)

        # Row 4 (5): inner loop — member row
        ws.write(4, 0, '', data_fmt)
        ws.write_comment('A5', 'jx:each(items="team.sortedTeamMembers" var="member" lastCell="G5")',
                         {'author': 'owlcms', 'width': 400, 'height': 100})
        ws.write(4, 1, '${member.name}', data_fmt)
        ws.write(4, 2, '${member.gender}', data_fmt)
        ws.write(4, 3, '${member.category}', data_fmt)
        # Points: always show individual place points (hide 0)
        ws.write(4, 4,
                 '${member.points != null && member.points != 0 ? member.points : ""}',
                 data_right_fmt)
        ws.write(4, 5, '${member.done ? t.get("Done") : ""}', data_fmt)
        # Score: always show, hide 0/null
        ws.write(4, 6,
                 '${member.score != null && member.score != 0 ? member.score : ""}',
                 data_score_fmt)

        # Row 5 (6): spacer (inside outer loop)
        ws.write(5, 0, '')

        # Row 6 (7): empty row (end of jx:area)
        ws.write(6, 0, '')

    # =============================================
    # Build all three sheets
    # =============================================
    build_sheet('Men',   'mTeamItems',  'Gender.M',  'm')
    build_sheet('Women', 'wTeamItems',  'Gender.F',  'w')
    build_sheet('Mixed', 'mwTeamItems', 'Gender.MF', 'mw')

    wb.close()
    print(f'Created {output} — 3 sheets (Men, Women, Mixed) via xlsxwriter')
