"""Add a start-number column next to the lot number in protocol templates.

Run: python3 owlcms/scripts/add-start-number-to-protocols.py [--check]

The templates listed below only showed the lot number. A column is inserted at
B; merges that span column B are widened and everything to the right is
shifted. The print area is extended by one column; the templates fit to page
width, so existing column widths are kept.
"""

import argparse
import copy
import os
import re
import tempfile
from pathlib import Path
from zipfile import ZipFile

from openpyxl import load_workbook
from openpyxl.comments import Comment
from openpyxl.workbook.defined_name import DefinedName
from openpyxl.utils import get_column_letter, range_boundaries


TEMPLATES = Path(__file__).resolve().parent.parent / "src/main/resources/templates"
INSERTED = 2
START_HEADER = '${t.get("Results.Start")}'
LOT_HEADER = '${t.get("Lot")}'
START_VALUE = "${l.startNumber}"
LOT_VALUE = "${l.lotNumber}"

# Medal-style layout: A=membership, B=lot, C=last name, D=first name.
# The new column B holds the start number; the lot number moves to C.
MEDALS = {
    "source": {"B6": LOT_HEADER, "B10": LOT_VALUE, "C10": "${l.lastName}"},
    "area": "V23",
    "result": {"B6": START_HEADER, "B10": START_VALUE, "C6": LOT_HEADER, "C10": LOT_VALUE, "D10": "${l.lastName}"},
    "style_source": {None: 3},
    "new_merges": ("B6:B7",),
    "widths": {"B": 5.43},
    "print_area": "A:W",
}

# All-sessions layout: A=lot, B:D=last name. A becomes the start number and
# the new column B holds the lot number.
ALL_SESSIONS = {
    "source": {"A6": LOT_HEADER, "A10": LOT_VALUE, "B10": "${l.lastName.toUpperCase()}"},
    "area": "Y27",
    "result": {"A6": START_HEADER, "A10": START_VALUE, "B6": LOT_HEADER, "B10": LOT_VALUE},
    "style_source": {None: 3, 6: 1, 7: 1, 10: 1},
    "new_merges": ("B6:B7",),
    "widths": {"B": 5.43},
    "print_area": "A:Z",
}

LAYOUTS = {
    "emptyProtocol/Empty3Medals-A4.xlsx": MEDALS,
    "emptyProtocol/Empty3Medals-LETTER.xlsx": MEDALS,
    "protocol/PanAm3Medals-A4.xlsx": MEDALS,
    "protocol/PanAm3Medals-LETTER.xlsx": MEDALS,
    "protocol/SnCJTot-A4.xlsx": MEDALS,
    "protocol/SnCJTot-LETTER.xlsx": MEDALS,
    "protocol/PanAmProtocol_AllSessions-A4.xlsx": ALL_SESSIONS,
}

CELL_REF = re.compile(r"\b([A-Z]{1,3})(\d+)\b")


def shift_column(column):
    return column + 1 if column >= INSERTED else column


def shift_ref(ref):
    def replace(match):
        letters, row = match.groups()
        index = range_boundaries(f"{letters}{row}")[0]
        return f"{get_column_letter(shift_column(index))}{row}"
    return CELL_REF.sub(replace, ref)


def remap_merge(merged_range):
    min_col, min_row, max_col, max_row = merged_range.bounds
    if max_col < INSERTED:
        new_min, new_max = min_col, max_col
    elif min_col < INSERTED:
        new_min, new_max = min_col, max_col + 1
    else:
        new_min, new_max = min_col + 1, max_col + 1
    return f"{get_column_letter(new_min)}{min_row}:{get_column_letter(new_max)}{max_row}"


def copy_cell_style(source, target):
    target._style = copy.copy(source._style)


def shift_column_dimensions(sheet):
    dimensions = sorted(sheet.column_dimensions.items(), key=lambda item: item[1].min or 0, reverse=True)
    for letter, dimension in dimensions:
        if (dimension.min or 0) < INSERTED:
            continue
        del sheet.column_dimensions[letter]
        moved = copy.copy(dimension)
        moved.min = dimension.min + 1
        moved.max = dimension.max + 1
        moved.index = get_column_letter(moved.min)
        sheet.column_dimensions[moved.index] = moved
    inserted = copy.copy(sheet.column_dimensions[get_column_letter(INSERTED + 1)])
    inserted.min = inserted.max = INSERTED
    inserted.index = get_column_letter(INSERTED)
    sheet.column_dimensions[inserted.index] = inserted


def shift_validations(sheet):
    for validation in sheet.data_validations.dataValidation:
        validation.sqref = type(validation.sqref)(" ".join(shift_ref(str(r)) for r in validation.sqref.ranges))


def shift_conditional_formatting(sheet):
    old = sheet.conditional_formatting
    rules = [(str(cf.sqref), cf.rules) for cf in old]
    old._cf_rules.clear()
    for sqref, cf_rules in rules:
        for rule in cf_rules:
            old.add(" ".join(shift_ref(part) for part in sqref.split()), rule)


def shift_commands(sheet):
    for row in sheet.iter_rows():
        for cell in row:
            if cell.comment is not None and "jx:" in cell.comment.text:
                text = re.sub(
                    r'lastCell="([A-Z]+\d+)"',
                    lambda m: f'lastCell="{shift_ref(m.group(1))}"',
                    cell.comment.text,
                )
                cell.comment = Comment(text, cell.comment.author or "owlcms")


def print_area(layout):
    return "Results!$" + layout["print_area"].replace(":", ":$")


def validate_source(sheet, layout, path):
    errors = [
        f"{ref}: expected {expected!r}, found {sheet[ref].value!r}"
        for ref, expected in layout["source"].items()
        if sheet[ref].value != expected
    ]
    comment = sheet["A1"].comment
    if comment is None or f'lastCell="{layout["area"]}"' not in comment.text:
        errors.append(f'A1: expected jx:area lastCell="{layout["area"]}"')
    if errors:
        raise ValueError(f"Unexpected source layout in {path}:\n  " + "\n  ".join(errors))


def convert(path, layout):
    workbook = load_workbook(path)
    sheet = workbook["Results"]
    validate_source(sheet, layout, path)

    merged_ranges = list(sheet.merged_cells.ranges)
    for merged_range in merged_ranges:
        sheet.unmerge_cells(str(merged_range))

    sheet.insert_cols(INSERTED)
    shift_column_dimensions(sheet)
    shift_validations(sheet)
    shift_conditional_formatting(sheet)
    shift_commands(sheet)

    style_source = layout["style_source"]
    for row in range(1, sheet.max_row + 1):
        source_column = style_source.get(row, style_source[None])
        copy_cell_style(sheet.cell(row, source_column), sheet.cell(row, INSERTED))

    for merged_range in merged_ranges:
        sheet.merge_cells(remap_merge(merged_range))
    for cell_range in layout["new_merges"]:
        sheet.merge_cells(cell_range)

    for ref, value in layout["result"].items():
        sheet[ref] = value
    for letter, width in layout["widths"].items():
        sheet.column_dimensions[letter].width = width
    # openpyxl cannot parse or set column-only print areas such as A:W.
    sheet.defined_names["Print_Area"] = DefinedName("Print_Area", attr_text=print_area(layout))

    with tempfile.NamedTemporaryFile(dir=path.parent, suffix=".xlsx", delete=False) as temporary:
        temporary_path = Path(temporary.name)
    try:
        workbook.save(temporary_path)
        validate_template(temporary_path, layout)
        os.replace(temporary_path, path)
    finally:
        if temporary_path.exists():
            temporary_path.unlink()


def validate_template(path, layout):
    workbook = load_workbook(path)
    sheet = workbook["Results"]
    errors = [
        f"{ref}: expected {expected!r}, found {sheet[ref].value!r}"
        for ref, expected in layout["result"].items()
        if sheet[ref].value != expected
    ]
    area = shift_ref(layout["area"])
    comment = sheet["A1"].comment
    if comment is None or f'lastCell="{area}"' not in comment.text:
        errors.append(f'A1: expected jx:area lastCell="{area}"')
    for row in sheet.iter_rows():
        for cell in row:
            if cell.comment is not None and f'lastCell="{layout["area"]}"' in cell.comment.text:
                errors.append(f"{cell.coordinate}: lastCell was not shifted")
    merges = {str(r) for r in sheet.merged_cells.ranges}
    for cell_range in layout["new_merges"]:
        if cell_range not in merges:
            errors.append(f"missing merge {cell_range}")
    with ZipFile(path) as archive:
        workbook_xml = archive.read("xl/workbook.xml").decode()
    expected_print_area = f'<definedName name="_xlnm.Print_Area" localSheetId="0">{print_area(layout)}</definedName>'
    if expected_print_area not in workbook_xml:
        errors.append(f"print area: expected {print_area(layout)}")
    for letter, width in layout["widths"].items():
        if sheet.column_dimensions[letter].width != width:
            errors.append(f"column {letter}: expected width {width}")
    if errors:
        raise ValueError(f"Invalid template {path}:\n  " + "\n  ".join(errors))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Validate converted templates without modifying them")
    args = parser.parse_args()
    for name, layout in LAYOUTS.items():
        path = TEMPLATES / name
        if args.check:
            validate_template(path, layout)
            print(f"Checked {path}")
        else:
            convert(path, layout)
            print(f"Converted {path}")


if __name__ == "__main__":
    main()
