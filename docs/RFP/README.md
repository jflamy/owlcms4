# RFP compliance tables

The compliance tables in `OTIS_Proposal_NN.docx` are rebuilt from the
`requirementNarrative` paragraphs (`[RFP §x.y] …`) and the justification text that
follows each one. Short versions of the table text are kept in a Markdown file,
`OTIS_Proposal_NN.md`, next to the matching document.

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
<!-- hash: d61eb12c10e5 -->
FIX: optional reminder about a problem in the proposal text

**Requirement** (RFP §2.1)

> original requirement text (read-only, for reference)

R: short requirement shown in the table

**Justification**

> original justification text (read-only, for reference)

J: short justification shown in the table
```

- Edit only the `R:`, `J:` and `FIX:` lines, and keep each on a single line.
- Don't edit the `## ID | ref` headings or the `<!-- hash -->` comments. The rebuild
  uses them to match summaries to rows and to detect text that has changed.
- An empty `R:` or `J:` keeps the full original text in the table.
- A `FIX:` line is a reminder to correct the proposal itself. Rebuilds print it as a
  warning, and extracts list it under "Open problems" at the top of the `.md`.
  Delete the line once the proposal is fixed.
- If a requirement's text changes in the proposal, its slots are emptied on the next
  extract. The old summary is kept in a `STALE` comment, and the rebuild uses the
  original text until the row is summarized again.

## Recipe A: edit summaries in the `.md`

1. Edit `OTIS_Proposal_01.md`.
2. If you also changed proposal text in Word, refresh the `.md` first. This keeps your
   `.md` summaries and marks changed rows as stale.

   ```bash
   python3 compliance_summaries.py extract OTIS_Proposal_01.docx OTIS_Proposal_01.md
   ```

3. Rebuild:

   ```bash
   python3 rebuild_compliance.py OTIS_Proposal_01.docx OTIS_Proposal_02.docx --summaries OTIS_Proposal_01.md
   ```

   This writes `OTIS_Proposal_02.docx` and `OTIS_Proposal_02.md`.

Don't use `--from-table` in this recipe. It replaces the `.md` summaries with the Word
table cells.

## Recipe B: edit summaries in the Word table

1. Edit the table cells, the proposal text, or both, directly in
   `OTIS_Proposal_01.docx`. In the Requirement column, keep the ID at the start of
   the cell.
2. Read the table back into the `.md`:

   ```bash
   python3 compliance_summaries.py extract OTIS_Proposal_01.docx OTIS_Proposal_01.md --from-table
   ```

   `FIX:` lines and hashes are kept from the existing `OTIS_Proposal_01.md`.

3. Rebuild:

   ```bash
   python3 rebuild_compliance.py OTIS_Proposal_01.docx OTIS_Proposal_02.docx --summaries OTIS_Proposal_01.md
   ```

## Rules

- In each round, edit summaries either in the `.md` (recipe A) or in the Word table
  (recipe B), not both. Recipe A overwrites Word table edits; recipe B overwrites
  `.md` edits.
- Always pass explicit source and output paths. The script can choose the paths
  itself only when `OTIS_Proposal_IWF.docx` or a numbered `OTIS_Proposal_NN.docx`
  exists.
- Tests:

  ```bash
  python3 -m unittest test_rebuild_compliance test_heading_numbering test_compliance_summaries
  ```
