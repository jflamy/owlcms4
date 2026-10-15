# RFP compliance tables

The compliance tables in `Sportdata_Proposal.docx` are rebuilt from the
`requirementNarrative` paragraphs (`[RFP §x.y] …`) and the justification text that
follows each one. Short versions of the table text are kept in a Markdown file with
the same name, `Sportdata_Proposal.md`. Each rebuild writes a numbered copy
(`Sportdata_Proposal_01.docx`, `_02`, …) with its own `.md`.

Scripts (run from this folder):

- `compliance_summaries.py extract` copies requirement and justification text from a
  `.docx` into a `.md`.
- `rebuild_compliance.py --summaries` rebuilds the tables using the `.md` summaries.
  It writes the next `.docx` and its matching `.md`.

Neither script writes summaries. The summaries are written by hand or by the AI
assistant. Neither script overwrites an existing file.

## The `.md` format

```markdown
## 2.1-R02 | B.2.2
FIX: optional reminder about a problem in the proposal text

**Requirement** (RFP §2.1)

> original requirement text (read-only, for reference)

R: short requirement shown in the table

**Justification**

> original justification text (read-only, for reference)

J: short justification shown in the table
```

- Edit only the `R:`, `J:` and `FIX:` lines, and keep each on a single line.
- Don't edit the `## ID | ref` headings. The rebuild uses them to match summaries to
  rows.
- The quoted text is a read-only copy of the proposal, for convenience. Fix the full
  text in Word; fix the short text in the `R:`/`J:` lines. Your `R:`/`J:` lines are
  always used, even if the Word text has changed since the extract.
- An empty `R:` or `J:` keeps the full original text in the table.
- A `FIX:` line is a reminder to correct the proposal itself. Rebuilds print it as a
  warning, and extracts list it under "Open problems" at the top of the `.md`.
  Delete the line once the proposal is fixed.

## Starting a new `.md`

To start from the main body text with empty summaries, extract into a `.md` that does
not exist yet:

```bash
python3 compliance_summaries.py extract Sportdata_Proposal.docx Sportdata_Proposal.md
```

To carry summaries and `FIX:` lines over from an older `.md` with a different name,
add `--previous older.md`.

## Recipe A: edit summaries in the `.md`

1. Edit `Sportdata_Proposal.md`.
2. Optionally, after changing proposal text in Word, refresh the `.md` so the quoted
   text is current. Your summaries are kept.

   ```bash
   python3 compliance_summaries.py extract Sportdata_Proposal.docx Sportdata_Proposal.md
   ```

3. Rebuild:

   ```bash
   python3 rebuild_compliance.py Sportdata_Proposal.docx Sportdata_Proposal_01.docx --summaries Sportdata_Proposal.md
   ```

   This writes `Sportdata_Proposal_01.docx` and `Sportdata_Proposal_01.md`. The next
   round goes from `_01` to `_02` the same way.

Don't use `--from-table` in this recipe. It replaces the `.md` summaries with the Word
table cells.

## Recipe B: edit summaries in the Word table

1. Edit the table cells, the proposal text, or both, directly in
   `Sportdata_Proposal_01.docx`. In the Requirement column, keep the ID at the start
   of the cell.
2. Read the table back into the `.md`:

   ```bash
   python3 compliance_summaries.py extract Sportdata_Proposal_01.docx Sportdata_Proposal_01.md --from-table
   ```

   `FIX:` lines are kept from the existing `Sportdata_Proposal_01.md`.

3. Rebuild:

   ```bash
   python3 rebuild_compliance.py Sportdata_Proposal_01.docx Sportdata_Proposal_02.docx --summaries Sportdata_Proposal_01.md
   ```

## Rules

- In each round, edit summaries either in the `.md` (recipe A) or in the Word table
  (recipe B), not both. Recipe A overwrites Word table edits; recipe B overwrites
  `.md` edits.
- Always pass explicit source and output paths. When they are omitted, the script
  looks only for the old `OTIS_Proposal_*.docx` names, not `Sportdata_Proposal*`.
- Tests:

  ```bash
  python3 -m unittest test_rebuild_compliance test_heading_numbering test_compliance_summaries
  ```
