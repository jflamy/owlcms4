---
name: convert-jxls-template
description: "Use when: converting OWLCMS Excel templates from JXLS1 markup cells to JXLS3 cell comments, editing jx:area or jx:each commands, removing legacy loop rows, preserving XLSX styles and merges, or diagnosing converted template layout changes. Keywords: JXLS1, JXLS3, template conversion, jx:area, jx:each, lastCell, Excel comments, Medals-A4.xlsx."
---

# Convert JXLS Templates

## Scope

Use this skill when converting an existing OWLCMS workbook from JXLS1 XML-like
markup in cells to JXLS3 commands stored in cell comments.

## Core Structural Rule

JXLS1 command cells are rows in the worksheet. JXLS3 comments are metadata on
cells. Do not merely clear legacy command text: physically remove command-only
rows that are not part of the intended output layout.

Recalculate every `lastCell` after removing those rows. `lastCell` is the
bottom-right coordinate of a rectangular template area, not a generated-row
count.

## Required Workflow

1. Inspect all populated cells, comments, merged ranges, row dimensions, print
   settings, and breaks in the original workbook.
2. Identify matching opening and closing JXLS1 tags and distinguish command-only
   rows from intentional spacer rows.
3. Always start with the legacy JXLS1 workbook. Write a guarded conversion
  script beside the templates it owns and assert the expected legacy cells
  before modifying anything. Do not use previously converted output as input.
4. Remove markup rows conceptually, calculate the old-to-new row mapping, and
   place JXLS3 comments on surviving cells.
5. Save atomically to a temporary workbook, reopen it, and validate the result
   before replacing the source template.
6. Run the script's check-only mode after conversion.
7. Generate a report with representative data and inspect repeated sections,
   ordering, fills, borders, merged cells, and pagination.
8. Exercise the empty-data path and confirm the dialog receives the expected
   validation error.

## JXLS3 Comment Rules

- Put `jx:area(lastCell="...")` in a comment on `A1`. OWLCMS detects JXLS3 by
  checking that exact cell.
- A `jx:each` comment belongs on the top-left cell of the rectangular area it
  repeats.
- The outer loop area must fully contain nested loop areas.
- Remove `${...}` wrappers from command attributes. For example,
  `items="${athletes}"` becomes `items="athletes"`.
- Set an explicit `var` for grouped loops. Prefix the JXLS3 `groupBy` property
  with that variable, for example `var="group"
  groupBy="group.medalingSortCode"`.
- Omit `groupOrder` when the Java producer already supplies the required order.
  JXLS3 then preserves first-appearance order.
- JXLS1 `varStatus` and JXLS3 `varIndex` are not equivalent objects. Add
  `varIndex` only when the template actually needs a numeric index.

## Preserving Workbook Formatting

Spreadsheet row deletion is not only a value operation.

- Record merged ranges before editing.
- Unmerge before deleting rows.
- Delete rows and remap row dimensions.
- Recreate merged ranges only after deletion. Recreating a future-position
  merge before deletion can clear styles from unrelated cells temporarily
  occupying that position.
- Validate fills across every cell of repeated headers, including blank cells.
- Reopen the saved workbook before considering the conversion valid.
- Preserve paper size, orientation, margins, print area, repeated rows, column
  widths, row heights, borders, and number formats.

## Normalizing Legacy Headers

When the recognized legacy header is present, remove the obsolete federation
block from `A1:A4`. The same pattern may contain federation formulas or empty
cells in that first-column area.

- Put the competition-name label in the first column and its value in the second
  column as two distinct, unmerged cells, matching the detail rows below.
- Move the detail fields below the name left by the same offset as the original
  name field; do not move table content or unrelated rows.
- Right-align labels moved into the first column so they remain visually paired
  with their values.
- Remove decorative header underlines instead of extending them across the new
  full-width layout.
- Preserve the existing relative arrangement of site, city, date, and organizer
  fields beneath the name.
- Guard the transformation with exact checks for the name label and value cells.
  Leave unknown header arrangements untouched or fail explicitly.
- Include the resulting title merge and absence of federation values in
  post-save validation.

## Medal Templates

The maintained converter is:

```bash
python3 owlcms/scripts/convert-jxls1-to-jxls3.py
```

Validate without modifying files:

```bash
python3 owlcms/scripts/convert-jxls1-to-jxls3.py --check
```

The medal conversion removes original rows 9, 12, 14, and 15. The resulting
command areas are:

- `A1:J11`: complete template area
- `A9:J11`: grouped category block, beginning with the intentional spacer
- `A11:J11`: repeated medalist row

The script is intentionally strict. Update its assertions deliberately if the
legacy template structure changes; do not weaken them to accept unknown layouts.

## Validation Checklist

- No cell value contains `<jx:` or `</jx:`.
- `A1` contains the `jx:area` comment.
- Every command's `lastCell` uses post-deletion coordinates.
- Nested command rectangles are contained by their parents.
- Merged ranges use post-deletion coordinates.
- Repeated category headers retain complete background fills.
- Group ordering matches the producer's comparator.
- A4 and Letter variants produce equivalent content and retain their paper
  settings.
- Empty input propagates `StopProcessingException` to the dialog callback.

## Do Not

- Do not convert templates by manually clearing markup cells.
- Do not infer new `lastCell` coordinates from the old final row alone.
- Do not regenerate an established workbook from scratch unless preserving its
  existing styles and print configuration is impractical.
- Do not run Maven or a project build without explicit human consent.