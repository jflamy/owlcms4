#!/usr/bin/env python3
"""Convert the OWLCMS medal templates from JXLS1 tags to JXLS3 comments."""

import argparse
import copy
import os
from pathlib import Path
import tempfile

from openpyxl import load_workbook
from openpyxl.comments import Comment
from openpyxl.styles import Border
from openpyxl.utils.cell import range_boundaries


TEMPLATE_NAMES = ("Medals-A4.xlsx", "Medals-LETTER.xlsx")
REMOVED_ROWS = (9, 12, 14, 15)
FEDERATION_HEADER_VALUES = (
    "${competition.federation}",
    "${competition.federationAddress}",
    "${competition.federationWebSite}",
    "${competition.federationEMail}",
)
LEGACY_COMPETITION_NAME_LABEL = '${t.get("Competition.competitionName")} :'
COMPETITION_LABEL = '${t.get("Competition")} :'
COMPETITION_NAME_VALUE = "${competition.competitionName}"
EXPECTED_LEGACY_CELLS = {
    "A9": '<jx:forEach items="${athletes}" groupBy="medalingSortCode">',
    "A11": "${group.item.category}",
    "A12": '<jx:forEach items="${group.items}" var="l" varStatus="lifterLoop">',
    "A13": "${l.rankingText}",
    "A14": "</jx:forEach>",
    "A15": "</jx:forEach>",
}
EXPECTED_JXLS3_COMMENTS = {
    "A1": 'jx:area(lastCell="J11")',
    "A9": 'jx:each(items="athletes" var="group" groupBy="group.medalingSortCode" lastCell="J11")',
    "A11": 'jx:each(items="group.items" var="l" lastCell="J11")',
}


def remapped_row(row):
    return row - sum(removed < row for removed in REMOVED_ROWS)


def remove_merged_ranges(sheet):
    ranges = list(sheet.merged_cells.ranges)
    for merged_range in ranges:
        sheet.unmerge_cells(str(merged_range))
    remapped_ranges = []
    for merged_range in ranges:
        min_col, min_row, max_col, max_row = range_boundaries(str(merged_range))
        if any(min_row <= removed <= max_row for removed in REMOVED_ROWS):
            raise ValueError(f"Merged range {merged_range} intersects a removed markup row")
        remapped_ranges.append(
            (min_col, remapped_row(min_row), max_col, remapped_row(max_row))
        )
    return remapped_ranges


def restore_merged_ranges(sheet, ranges):
    for min_col, min_row, max_col, max_row in ranges:
        sheet.merge_cells(
            start_row=min_row,
            start_column=min_col,
            end_row=max_row,
            end_column=max_col,
        )


def has_legacy_header(sheet):
    federation_values = tuple(sheet.cell(row, 1).value for row in range(1, 5))
    return (
        federation_values in (FEDERATION_HEADER_VALUES, (None, None, None, None))
        and sheet["D1"].value == LEGACY_COMPETITION_NAME_LABEL
        and sheet["E1"].value == COMPETITION_NAME_VALUE
    )


def normalize_header(sheet):
    if not has_legacy_header(sheet):
        return False

    label_style = copy.copy(sheet["D1"]._style)
    title_style = copy.copy(sheet["E1"]._style)
    for row in range(1, 5):
        cell = sheet.cell(row, 1)
        cell.value = None
        cell.hyperlink = None
    for column in range(4, 11):
        sheet.cell(1, column).value = None
    sheet["A1"] = COMPETITION_LABEL
    sheet["A1"]._style = label_style
    sheet["B1"] = COMPETITION_NAME_VALUE
    sheet["B1"]._style = title_style
    return True


def move_header_details_left(sheet):
    if (
        sheet["D2"].value != '${t.get("Competition.competitionSite")} :'
        or sheet["D3"].value != '${t.get("Competition.competitionDate")} :'
    ):
        return False

    source_columns = (4, 5, 6, 7, 8, 9, 10)
    target_columns = (1, 2, 3, 6, 7, 8, 9)
    cells = {
        (row, target_column): (
            sheet.cell(row, source_column).value,
            copy.copy(sheet.cell(row, source_column)._style),
            copy.copy(sheet.cell(row, source_column).hyperlink),
            copy.copy(sheet.cell(row, source_column).comment),
        )
        for row in range(2, 4)
        for source_column, target_column in zip(source_columns, target_columns)
    }
    for row in range(2, 4):
        for column in range(1, 11):
            cell = sheet.cell(row, column)
            cell.value = None
            cell.hyperlink = None
            cell.comment = None
    for (row, column), (value, style, hyperlink, comment) in cells.items():
        cell = sheet.cell(row, column)
        cell.value = value
        cell._style = style
        cell.hyperlink = hyperlink
        cell.comment = comment
    sheet["B3"] = "${session.competitionShortDateTime}"
    for coordinate in ("A2", "F2", "A3", "F3"):
        alignment = copy.copy(sheet[coordinate].alignment)
        alignment.horizontal = "right"
        sheet[coordinate].alignment = alignment
    for row in sheet.iter_rows(min_row=1, max_row=3, min_col=1, max_col=10):
        for cell in row:
            cell.border = Border()
    return True


def remap_row_dimensions(sheet):
    dimensions = [(row, copy.copy(dimension)) for row, dimension in sheet.row_dimensions.items()]
    sheet.row_dimensions.clear()
    for row, dimension in dimensions:
        if row in REMOVED_ROWS:
            continue
        new_row = remapped_row(row)
        dimension.index = new_row
        sheet.row_dimensions[new_row] = dimension


def validate_legacy(sheet, path):
    mismatches = [
        f"{cell}: expected {expected!r}, found {sheet[cell].value!r}"
        for cell, expected in EXPECTED_LEGACY_CELLS.items()
        if sheet[cell].value != expected
    ]
    if mismatches:
        raise ValueError(f"Unexpected legacy layout in {path}:\n  " + "\n  ".join(mismatches))


def validate_converted(sheet, path):
    expected_values = {
        "A9": None,
        "A10": "${group.item.category}",
        "A11": "${l.rankingText}",
    }
    errors = [
        f"{cell}: expected {expected!r}, found {sheet[cell].value!r}"
        for cell, expected in expected_values.items()
        if sheet[cell].value != expected
    ]
    for cell, expected in EXPECTED_JXLS3_COMMENTS.items():
        actual = sheet[cell].comment.text if sheet[cell].comment else None
        if actual != expected:
            errors.append(f"{cell} comment: expected {expected!r}, found {actual!r}")
    markup_cells = [
        cell.coordinate
        for row in sheet.iter_rows()
        for cell in row
        if isinstance(cell.value, str) and "<jx:" in cell.value
    ]
    if markup_cells:
        errors.append(f"legacy markup remains in {', '.join(markup_cells)}")
    expected_fill = "FFDDDDDD"
    missing_fill = [
        cell.coordinate
        for cell in sheet[10][:10]
        if cell.fill.fill_type != "solid" or cell.fill.fgColor.rgb != expected_fill
    ]
    if missing_fill:
        errors.append(f"category header fill missing in {', '.join(missing_fill)}")
    expected_title = {"A1": COMPETITION_LABEL, "B1": COMPETITION_NAME_VALUE}
    for cell, expected in expected_title.items():
        if sheet[cell].value != expected:
            errors.append(f"{cell}: expected {expected!r}, found {sheet[cell].value!r}")
    expected_details = {
        "A2": '${t.get("Competition.competitionSite")} :',
        "B2": "${competition.competitionSite}",
        "F2": ' ${t.get("Competition.competitionCity")} : ',
        "G2": "${competition.competitionCity}",
        "A3": '${t.get("Competition.competitionDate")} :',
        "B3": "${session.competitionShortDateTime}",
        "F3": '${t.get("Competition.competitionOrganizer")} : ',
        "G3": "${competition.competitionOrganizer}",
    }
    for cell, expected in expected_details.items():
        if sheet[cell].value != expected:
            errors.append(f"{cell}: expected {expected!r}, found {sheet[cell].value!r}")
    misaligned_labels = [
        cell
        for cell in ("A1", "A2", "F2", "A3", "F3")
        if sheet[cell].alignment.horizontal != "right"
    ]
    if misaligned_labels:
        errors.append(f"header labels not left-aligned in {', '.join(misaligned_labels)}")
    bordered_header_cells = [
        cell.coordinate
        for row in sheet.iter_rows(min_row=1, max_row=3, min_col=1, max_col=10)
        for cell in row
        if any(
            side is not None and side.style is not None
            for side in (cell.border.left, cell.border.right, cell.border.top, cell.border.bottom)
        )
    ]
    if bordered_header_cells:
        errors.append(f"header underlining remains in {', '.join(bordered_header_cells)}")
    obsolete_federation_values = [
        cell.coordinate
        for row in sheet.iter_rows(min_row=1, max_row=4, max_col=10)
        for cell in row
        if cell.value in FEDERATION_HEADER_VALUES
    ]
    if obsolete_federation_values:
        errors.append(f"federation header remains in {', '.join(obsolete_federation_values)}")
    expected_merges = {"A6:J6", "D8:E8", "D11:E11"}
    actual_merges = {str(merged_range) for merged_range in sheet.merged_cells.ranges}
    if actual_merges != expected_merges:
        errors.append(f"merged ranges: expected {sorted(expected_merges)}, found {sorted(actual_merges)}")
    if errors:
        raise ValueError(f"Invalid JXLS3 layout in {path}:\n  " + "\n  ".join(errors))


def is_converted(sheet):
    comment = sheet["A1"].comment
    return comment is not None and comment.text == EXPECTED_JXLS3_COMMENTS["A1"]


def save_atomically(workbook, sheet_name, path):
    with tempfile.NamedTemporaryFile(dir=path.parent, suffix=".xlsx", delete=False) as temporary:
        temporary_path = Path(temporary.name)
    try:
        workbook.save(temporary_path)
        reopened = load_workbook(temporary_path)
        validate_converted(reopened[sheet_name], temporary_path)
        os.replace(temporary_path, path)
    finally:
        if temporary_path.exists():
            temporary_path.unlink()


def convert(path, check_only=False):
    workbook = load_workbook(path)
    if "Results" not in workbook.sheetnames:
        raise ValueError(f"Results sheet not found in {path}")
    sheet = workbook["Results"]

    if is_converted(sheet):
        if check_only:
            validate_converted(sheet, path)
            print(f"Checked {path}")
            return
        raise ValueError(f"Template is already JXLS3; start from the legacy JXLS1 template: {path}")
    if check_only:
        raise ValueError(f"Template is still JXLS1: {path}")

    validate_legacy(sheet, path)
    merged_ranges = remove_merged_ranges(sheet)
    remap_row_dimensions(sheet)
    for row in sorted(REMOVED_ROWS, reverse=True):
        sheet.delete_rows(row)
    restore_merged_ranges(sheet, merged_ranges)
    if not normalize_header(sheet):
        raise ValueError(f"Recognized report header not found in {path}")
    if not move_header_details_left(sheet):
        raise ValueError(f"Recognized report details not found in {path}")

    for cell, text in EXPECTED_JXLS3_COMMENTS.items():
        sheet[cell].comment = Comment(text, "owlcms")

    validate_converted(sheet, path)
    save_atomically(workbook, "Results", path)
    print(f"Converted {path}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("templates", nargs="*", type=Path, help="Templates to convert")
    parser.add_argument("--check", action="store_true", help="Validate without modifying files")
    args = parser.parse_args()

    owlcms_directory = Path(__file__).resolve().parent.parent
    template_directory = owlcms_directory / "src/main/resources/templates/medals"
    templates = args.templates or [template_directory / name for name in TEMPLATE_NAMES]
    for template in templates:
        convert(template.resolve(), args.check)


if __name__ == "__main__":
    main()