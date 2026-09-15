# Lenient Birth-Date Parsing for Registration Upload — Spec

Status: draft
Scope: registration spreadsheet upload, birth-date column only
Owner file: `app.owlcms.spreadsheet.NRegistrationFileProcessor` (+ `RAthlete`, `DateTimeUtils`)

## Problem

Typed birth dates in registration spreadsheets only parse when they match the
user's locale short-date pattern or ISO 8601. For Ecuador (`es-EC`) Java's SHORT
pattern is `d/M/yy` → normalized to `d/M/yyyy`, so only slash-separated,
day-first dates or ISO dates parse. Users who typed other delimiters (e.g. `.`)
or the other field order got `Upload.WrongDateFormat` even though their intent
was unambiguous.

Dot separators are not actually the Ecuadorian convention (that is `dd/mm/aaaa`
with slashes); dots are a European convention. But real spreadsheets are
inconsistent, so we want a robust, file-scoped heuristic rather than a per-cell
locale guess.

## Goal

Accept `/`, `.`, and `-` delimiters and both `DMY` / `MDY` field orders, deciding
the order **once per file** by evidence, so ambiguous dates inherit a decision
proven by their neighbours instead of being guessed per row.

## Model: order voting

The whole design reduces to voting on the field order for the file.

- Every birth date with a component in **13–31** (a value that can only be a day)
  casts one vote for an order: `DMY` or `MDY`. A single such date is enough to
  fix the order for the entire file.
- Once the order is fixed, **all** dates — including ambiguous ones (both
  day/month parts ≤ 12) — are parsed in that order, without complaint.
- ISO dates (`yyyy-…`, 4-digit first token) are always accepted and do **not**
  participate in voting.

### Outcomes

| Situation | Outcome |
|---|---|
| Any 2-digit year in the birth column | **Reject sheet outright** |
| Order votes unanimous | Use the voted order for all dates |
| Order votes conflict (`DMY` and `MDY` both proven) | **Reject sheet outright** (inconsistent file) |
| No votes (every date has both parts ≤ 12) | **Locale order** + **warning**, no rejection |
| ISO rows (`yyyy-…`) | Always accepted, exempt from the single-format rule |
| Mixed delimiters (`/ . -`), order consistent | Tolerated |

Notes:

- **Year is always 4 digits.** Any 2-digit year anywhere in the birth column
  aborts the whole upload with an error naming the offending cell(s). This also
  keeps 2-digit years out of the vote entirely.
- **Delimiter never changes meaning**, so delimiter mixing is tolerated; only the
  day/month order is constrained, because a wrong order silently corrupts the
  birth month (affecting Masters age groups).

## Algorithm

1. **Detection pass** (after the athlete header row is parsed, before row
   application). The POI workbook is fully in memory, so this re-walks the same
   live `Sheet` cells — no separate buffering of date strings is required.
   For each non-empty birth cell:
   - Read via `cellToString` (genuine Excel date cells are already normalized to
     `yyyy-MM-dd` upstream and count as ISO).
   - Tokenize on any of `/ . -`.
   - Skip bare years and Excel serials (handled elsewhere; no order evidence).
   - If a 2-digit year is present → record a fatal error (sheet rejected).
   - If first token is a 4-digit year → ISO, no vote.
   - Otherwise identify the 4-digit year, and if one of the remaining two tokens
     is 13–31, cast a `DMY`/`MDY` vote.
2. **Resolve order**:
   - unanimous votes → that order;
   - conflicting votes → reject sheet;
   - no votes → locale order (from `localizedShortDatePattern`) + warning.
3. **Application pass** (existing per-row loop): parse each birth date against the
   resolved order, accepting any delimiter; construct the `LocalDate` to validate
   calendar correctness (e.g. reject month 15 or day 31 in a 30-day month).

## Architecture / placement

- Detection lives inside `readAthletes`: a first loop discovers the header row and
  the birth column (`delayedSetterColumns[BIRTHDATE]`), tallies votes into a field
  (e.g. `detectedDateOrder`), then the existing loop applies dates using it.
- Because `WorkbookFactory.create(...)` returns an in-memory workbook
  (XSSF/HSSF DOM), iterating rows twice costs no extra I/O and needs no buffering.
- **Scope stays birth-date only** (`RAthlete` path). Competition header dates keep
  using `parseExcelDate` (single values, no consensus available, and they also
  support fractional Excel serials that this heuristic does not).

## Open / decided items

- Conflicting votes → reject sheet. **Decided.**
- No-evidence ambiguous → locale order + warning, no reject. **Decided.**
- 2-digit year anywhere → reject sheet. **Decided.**
- Mixed delimiters with consistent order → tolerated. **Decided.**

## Testing

Unit tests should cover each branch:

- Single day≥13 fixes order; ambiguous dates then inherit it.
- Unanimous DMY; unanimous MDY.
- Conflicting votes → reject.
- No votes → locale order + warning (verify locale drives the result).
- 2-digit year anywhere → reject.
- Mixed delimiters, consistent order → accepted.
- ISO mixed with delimited dates → ISO accepted, order from delimited votes.
- Calendar validation (month 15, Feb 30) → rejected per row / per sheet as
  appropriate.
