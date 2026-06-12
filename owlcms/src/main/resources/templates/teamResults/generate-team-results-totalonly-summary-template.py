"""
Generate JXLS3 TeamResults "Total Only Summary" templates (A4 and Letter variants).

These templates show one row per team only and use total-only points:
  - no member-detail loop
  - no spacer rows between teams
  - team points come from team.totalOnlyPoints

Points and score columns remain in E and G so JXLSTeamResultsSheet.postProcess()
can keep using the existing column-hide logic.
"""
import os

import xlsxwriter

TEMPLATE_DIR = os.path.dirname(os.path.abspath(__file__))

# Paper sizes: A4 = 9, US Letter = 1
VARIANTS = [
    ("TeamResults-TotalOnly-Summary-A4.xlsx", 9),
    ("TeamResults-TotalOnly-Summary-Letter.xlsx", 1),
]

for filename, paper_size in VARIANTS:
    output = os.path.join(TEMPLATE_DIR, filename)
    wb = xlsxwriter.Workbook(output, {"strings_to_urls": False})

    title_fmt = wb.add_format({"font_name": "Arial", "bold": True, "font_size": 14})
    header_fmt = wb.add_format({"font_name": "Arial", "bold": True, "font_size": 11})
    team_fmt = wb.add_format({
        "font_name": "Arial", "bold": True, "font_size": 11,
        "bg_color": "#E2EFDA"
    })
    team_right_fmt = wb.add_format({
        "font_name": "Arial", "bold": True, "font_size": 11,
        "bg_color": "#E2EFDA", "align": "right"
    })
    col_hdr_fmt = wb.add_format({
        "font_name": "Arial", "bold": True, "font_size": 11,
        "bg_color": "#D9E1F2", "border": 1
    })
    team_score_fmt = wb.add_format({
        "font_name": "Arial", "bold": True, "font_size": 11,
        "bg_color": "#E2EFDA", "align": "right", "num_format": "0.00"
    })

    def build_sheet(sheet_title, items_bean, gender_key, prefix):
        ws = wb.add_worksheet(sheet_title)
        ws.set_landscape()
        ws.set_paper(paper_size)
        ws.set_margins(left=0.4, right=0.4, top=0.6, bottom=0.4)
        ws.fit_to_pages(1, 0)

        col_widths = [4, 22, 4, 20, 10, 18, 12]
        for i, width in enumerate(col_widths):
            ws.set_column(i, i, width)

        show_points = prefix + "ShowPoints"
        scoring_title = prefix + "ScoringTitle"
        team_size = prefix + "TeamSize"

        # Row 0 (1 in Excel): title + jx:area
        ws.write(0, 0, "${competition.competitionName}", title_fmt)
        ws.write_comment("A1", 'jx:area(lastCell="G4")', {
            "author": "owlcms", "width": 300, "height": 100
        })

        # Row 1 (2): championship alone left, ageGroupPrefix + gender together
        ws.write(1, 0,
                 '${championship != null ? championship.name : ""}',
                 header_fmt)
        ws.write(1, 3,
                 '${ageGroupPrefix != null ? ageGroupPrefix : ""}'
                 ' ${t.get("' + gender_key + '")}',
                 header_fmt)

        # Row 2 (3): summary headers
        ws.write(2, 0, "", col_hdr_fmt)
        ws.write(2, 1, '${t.get("Name")}', col_hdr_fmt)
        ws.write(2, 2, "", col_hdr_fmt)
        ws.write(2, 3, "", col_hdr_fmt)
        ws.write(2, 4, '${t.get("Results.Points")}', col_hdr_fmt)
        ws.write(2, 5, '${t.get("TeamResults.Status")}', col_hdr_fmt)
        ws.write(2, 6, '${' + scoring_title + '}', col_hdr_fmt)

        # Row 3 (4): one team row per iteration, no detail rows or spacer rows
        ws.write(3, 0, "", team_fmt)
        ws.write_comment("A4", 'jx:each(items="' + items_bean + '" var="team" lastCell="G4")', {
            "author": "owlcms", "width": 400, "height": 100
        })
        ws.write(3, 1, "${team.name}", team_fmt)
        ws.write(3, 2, "", team_fmt)
        ws.write(3, 3, "", team_fmt)
        ws.write(3, 4,
                 '${' + show_points + ' && team.totalOnlyPoints != 0 ? team.totalOnlyPoints : ""}',
                 team_right_fmt)
        ws.write(3, 5,
                 '${team.counted}/${' + team_size + ' != 0 ? ' + team_size + ' : team.size}',
                 team_right_fmt)
        ws.write(3, 6,
                 '${!' + show_points + ' && team.score != 0 ? team.score : ""}',
                 team_score_fmt)

    build_sheet("Men", "mTeamItems", "Gender.M", "m")
    build_sheet("Women", "wTeamItems", "Gender.F", "w")
    build_sheet("Mixed", "mwTeamItems", "Gender.MF", "mw")

    wb.close()
    print(f"Created {output} — 3 total-only summary sheets (Men, Women, Mixed) via xlsxwriter")
