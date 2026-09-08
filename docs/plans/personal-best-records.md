# Personal Best Records: Implementation Handoff

Status: implementation plan only. No application changes have been made.

Target repository: `owlcms_68`.

## 1. Instructions to the Implementing Agent

Move live personal-best (PB) tracking to the existing record-event system. Reuse
record improvements, provisional history, jury reversals, and record workbooks.
The three athlete PB fields become seed inputs only; they are never written back.

Implement one numbered phase at a time. Read the named controlling methods before
editing; do not map the entire repository. After each phase, run the cheapest
authorized focused check before continuing. Update the checklist at the end of
this file with checks run and remaining blockers.

Follow repository instructions and applicable skills:

- Do not run Maven, builds, or deployments without explicit human consent.
- Load `verify-java-fix` before editing Java and follow its validation procedure.
- Load `add-test-case` before adding tests; do not instantiate Vaadin UI objects.
- Load the Vaadin and translation skills before changing UI code or translations.
- Never edit `translation4.csv`; use the approved translation TSV process.
- Keep I/O outside the Vaadin UI lock. Capture UI context before background work.
- Do not commit, push, remove existing diagnostic logging, or change unrelated code.
- If a step cannot be implemented with the stated semantics, stop and explain the
  concrete conflict. Do not invent an alternative identity or persistence scheme.

## 2. Agreed Requirements

1. PBs are ordinary `RecordEvent` rows; there is no parallel PB store.
2. A PB record is recognized by its `recordFederation` belonging to the reserved
   *Personal* family (see 3.1).
3. `recordName` holds the athlete identity (see 3.2). No new `RecordEvent` column.
4. `athleteName` holds the name, `nation` holds the team. No new team column.
5. Age and bodyweight bounds are `0..999`; category eligibility is irrelevant.
6. Runtime matching is by identity equality only, across all Personal federations.
   The federation is import/export provenance (e.g. Canada vs USA BARS), nothing more.
7. Identity collisions are assumed not to happen. Document the assumption; add no
   collision UI, namespace allocation, or name/team disambiguation.
8. As little persistence as possible: no new `Athlete` field; `Athlete.membership`
   is never modified; identity is computed transiently when needed.
9. PBs originate from registration (form or file) or from imported record files.
10. If a registration file contains any PB column, that import is authoritative:
    delete all Personal records, then rebuild baselines from the file.
11. Personal federations are importable/exportable with the normal record workbook.
12. Public displays show a translated "Personal Best" label, never the identity.

## 3. Design Decisions

### 3.1 Personal federation family

- Recognized names: exactly `Personal`, or `Personal:<source>` with a nonempty
  source, e.g. `Personal:CAN`, `Personal:USABARS`.
- Centralize recognition in one static predicate (`RecordEvent.isPersonalFederation`
  or a small `PersonalRecords` helper). Do not scatter prefix checks.
- Registration-generated baselines use federation `Personal`.
- Imported files keep their federation name exactly; never rename or renumber.
- An improvement during the meet is created for **each** Personal federation whose
  record for that lift was beaten, copying that federation, so every source can be
  exported back. Existing `updateRecords` already iterates challenged records, so
  this falls out naturally if each matching federation's row is in `eligibleRecords`.

### 3.2 Identity stored in `recordName`

Compute with one pure function, `PersonalRecords.identityFor(Athlete)`:

1. If `membership` is nonblank: `trim(membership)`. Preserve case and leading zeros.
   Never parse as a number.
2. Otherwise: `GEN:` + hex SHA-256 of `canonicalName|canonicalTeam|discriminator`
   where
   - `canonicalName` = `getFullName()` after NFKD normalization, removal of combining
     marks, `toUpperCase(Locale.ROOT)`, trim, whitespace collapsed to one space;
   - `canonicalTeam` = same treatment applied to `getTeam()` (empty string if null);
   - `discriminator` = first available of full birth date (ISO), birth year,
     registration category code, lot number; empty string if none.

The identity is never stored on the athlete. It is recomputed for matching, for
baseline creation, and for improvements. A name/team/birth-date correction changes
the generated identity; that is acceptable because registration reload rebuilds
all Personal records (3.4).

Reuse an existing SHA-256 usage pattern (`ForwarderPayloadBuilder` line ~514) rather
than adding a dependency.

### 3.3 Record field usage

| Field | Personal meaning |
| --- | --- |
| `recordFederation` | `Personal` or `Personal:<source>` |
| `recordName` | Identity from 3.2 |
| `recordLift` | SNATCH, CLEANJERK, TOTAL |
| `recordValue` | PB in kg |
| `athleteName` | Athlete full name at creation |
| `nation` | Athlete team at creation |
| `ageGrpLower/Upper`, `bwCatLower/Upper` | `0` / `999` |
| `ageGrp` | Fixed string `PB`, untranslated in every language. Comes from the workbook `AgeGroup` column or is injected by registration; `fillDefaults` uppercases it and requires it nonblank |
| `categoryString` | Not read from workbooks and not displayed; leave existing behavior alone |
| `bwCatString` | `">0"`, produced automatically by `syncBodyWeightCategoryString` for `0..999` |
| `gender` | Athlete gender if known, else null (metadata only; not used for matching) |
| `groupNameString` | Empty for baselines; session name for meet improvements |
| `fileName` | Import file basename, or `registration` for registration-generated rows |
| `event`, `eventLocation`, `recordDate` | As for ordinary improvements |

The existing logical key (`getKey()`, `appendLogicalKeyConditions`) remains valid
for Personal rows because gender and bounds are constant per identity. Do not add
a Personal branch to key logic unless a test proves it necessary.

### 3.4 Source precedence and reload

- **Registration file with any PB column** (`PersonalBestSnatch`,
  `PersonalBestCleanJerk`, `PersonalBestTotal` in `NRegistrationFileProcessor`'s
  header map): delete **every** record in **every** Personal federation, then create
  one baseline row per positive PB value per athlete under federation `Personal`.
  Detect by column presence, not by nonblank values, so blank cells intentionally
  clear old baselines.
- **Registration file without PB columns**: leave Personal records untouched.
- **Registration form save** for one athlete: delete that athlete's rows with
  federation `Personal` and empty `groupNameString`, then recreate from the three
  form fields. Do not touch other federations or meet improvements.
- **Record workbook import** containing Personal rows: existing official-replacement
  and provisional rules apply unchanged; Personal rows are just records.
- Non-participating athletes' PBs are irrelevant, so full deletion on registration
  reload is acceptable. Imported Personal history loaded earlier is discarded by such
  a reload; document this as intended precedence.
- If lifts have already been recorded when a registration PB reload happens, run
   a new `RecordRepository.recomputePersonalRecords()` afterwards so Personal meet
   improvements reappear. Both deletion and recreation must be Personal-only.
   Do not call `recomputeNewRecords()` here: it calls `clearNewRecords()`, which
   deletes all provisional records, including ordinary federation records.
   Ordinary baseline and provisional rows must retain their IDs and all field values.

### 3.5 Eligibility and display

- Personal rows must bypass: gender/age/bodyweight filtering in
  `RecordRepository.findFiltered` (as used by `computeDisplayableRecordsForAthlete`),
  the athlete federation-code filter in `filterEligibleRecordsForAthlete`, and
  `FieldOfPlay.hasCompleteRecordLookupProfile`.
- Personal rows must be excluded from the ordinary record boxes and from
  `RecordConfig.recordOrder`; they feed only the existing personal box
  (`recordBoxPersonal`) in `RecordFilter.buildRecordJson`.
- "Show all federation/category records" modes must not include Personal rows of
  other athletes.

## 4. Verified Starting Points

Paths relative to `owlcms/src/main/java/app/owlcms/`. Line numbers drift.

| File | Controlling code |
| --- | --- |
| `data/athlete/Athlete.java` | PB fields (~465, ~4717), `getMembership`, `getFullName`, `getTeam`, `getFullBirthDate`, `getYearOfBirth`, `getLotNumber` |
| `data/records/RecordEvent.java` | `fillDefaults`, `getKey`, `sameAs`, `syncBodyWeightCategoryString` |
| `data/records/RecordRepository.java` | `findFiltered`, `clearLoadedRecords`, `clearNewRecords`, `recomputeNewRecords`, `improveRecord`, `findAllRecordNames`, `findDistinctFederations`, `appendLogicalKeyConditions` |
| `data/records/RecordFilter.java` | `buildRecordJson` (personal box, ~line 178), `computeDisplayableRecordsForAthlete`, `filterEligibleRecordsForAthlete`, `computeChallengedRecords` |
| `data/records/RecordConfig.java` | `addMissing` |
| `data/records/RecordDefinitionReader.java` | `createRecords`, `parseRecordsFromWorkbook`, `importParsedRecords`, `SETTER_MAP` |
| `fieldofplay/FieldOfPlay.java` | `recomputeRecordsMap`, `hasCompleteRecordLookupProfile`, `recomputeRecords`, `commitCurrentDecision`, `doJuryDecision`, `updateRecords`, `createNewRecordEvent` |
| `nui/shared/NAthleteRegistrationFormFactory.java` | `createRecordForm` (~line 938), save/commit path |
| `spreadsheet/NRegistrationFileProcessor.java` | header map (`PersonalBest*` ~line 716), `updateAthletes` |
| `spreadsheet/JXLSExportRecords.java` | `keepNewest`, `sortRecords`, template lookup |
| `monitors/websocket/ForwarderPayloadBuilder.java` | existing SHA-256 usage (~line 514) |

Read `data/records/RecordImportSpec.md` before touching import semantics.

## 5. Phases

### Phase 1: Personal helper (no behavior change)

1. Add a small non-UI helper (e.g. `data/records/PersonalRecords.java`) with:
   `isPersonalFederation(String)`, `isPersonal(RecordEvent)`,
   `identityFor(Athlete)`, `canonicalize(String)`,
   `newBaseline(Athlete, Ranking lift, int value)` returning a populated
   `RecordEvent` per 3.3 with federation `Personal` and `fileName = "registration"`.
2. Unit-test: recognition (`Personal`, `Personal:CAN`, not `Personalized`),
   identity uses trimmed membership when present, generated identity is stable,
   accent/case/whitespace insensitive, changes with birth date, `0..999` bounds and
   `ageGrp = "PB"` on baselines.

Gate: tests pass; no production call sites yet.

### Phase 2: Lookup and eligibility

1. `RecordRepository`: add `findPersonalRecords(String identity)` returning all
   Personal rows whose `recordName` equals the identity (any Personal federation).
2. `RecordFilter.computeDisplayableRecordsForAthlete`: append the athlete's Personal
   rows (via `identityFor`) to the fetched list before the keep-largest map. Ensure
   `findFiltered` results never include Personal rows for other athletes; if
   `findFiltered` returns them, exclude `isPersonal` rows from its output first.
3. `RecordFilter.filterEligibleRecordsForAthlete`: pass Personal rows through
   regardless of athlete federation codes.
4. `FieldOfPlay.recomputeRecordsMap`: when `hasCompleteRecordLookupProfile` is
   false, still compute and store the athlete's Personal rows.
5. `RecordConfig.addMissing` / `RecordRepository.findAllRecordNames`: exclude
   Personal rows so identities never enter `recordOrder`.
6. `RecordFilter.buildRecordJson`: exclude Personal rows from the per-category
   table; compute the personal box from the max Personal value per lift instead of
   `getPersonalBest*()`. Keep the existing `Record.PersonalBest` label and
   `recordBoxPersonal` class. Highlight uses the existing challenged-record logic.

Gate: an athlete with no DOB/bodyweight and no ordinary records still shows a PB
box; ordinary record tests unchanged.

### Phase 3: Improvements and reversals

1. Confirm `computeChallengedRecords` now includes Personal rows (it filters on
   `eligibleRecords`, so Phase 2 suffices). Total is challenged only when
   `bestSnatch > 0` (existing `recomputeRecords` logic).
2. Confirm that `RecordEvent.newRecord` (used by `FieldOfPlay.createNewRecordEvent`)
   and `RecordRepository.improveRecord` copy `recordFederation`, `recordName`,
   bounds, and `ageGrp` (`PB`) unchanged from the beaten record, and set
   `groupNameString`, `event`, `eventLocation`, `recordDate`. Reading the code
   indicates they already do; no change expected here.
3. Confirm jury reversal removes the just-created Personal rows through the existing
   `voidableRecords` path and the previous max reappears after `recomputeRecordsMap`.
4. Add `RecordRepository.recomputePersonalRecords()` for registration PB reloads.
   Reuse the chronological replay logic, but restrict any provisional deletion and
   every candidate/improvement to the recognized Personal federation family.
   Never call the unscoped `clearNewRecords()` or `recomputeNewRecords()` from this
   path. Preserve the existing general recomputation operation for its existing
   callers. If extracting shared replay code, keep its original scope unchanged.
   Personal lookup must use computed identity and bypass ordinary federation-code
   eligibility, as in Phase 2.3. Repeated Personal replay must not create duplicates.
5. Add tests using the existing `RecordsTest` fixture style: good lift creates one
   provisional Personal row per matching federation; reversal deletes them; failed
   or equal lift creates none; C&J can improve both CJ and TOTAL.
6. Add a replay isolation test with ordinary baselines and provisional rows, including
   an imported ordinary provisional whose owner is absent from the athlete database.
   Snapshot their IDs and field values; run Personal replay twice and assert they
   are unchanged while Personal improvements are correctly rebuilt without duplicates.

Gate: matrix rows for improvements/reversals pass.

### Phase 4: Registration file import

1. In `NRegistrationFileProcessor`, detect whether the header row contained any of
   the three PB columns.
2. If yes, inside the import transaction after athletes are merged: delete all rows
   where `isPersonalFederation(recordFederation)`; then for every athlete with a
   positive `getPersonalBest*()` value create the corresponding baseline via
   `PersonalRecords.newBaseline` and persist it.
3. If lifts exist in the database, call the new
   `RecordRepository.recomputePersonalRecords()` afterwards. Do not call the
   existing general `recomputeNewRecords()` operation.
4. Refresh record maps on active FOPs once at the end, not per athlete.
5. Test with a small registration workbook fixture: PB columns present clears
   pre-existing Personal rows (including an imported `Personal:CAN` row) and creates
   baselines; PB columns absent leaves Personal rows untouched; blank cell yields no
   baseline for that lift. Include completed lifts and assert that ordinary record
   rows retain their IDs and field values across registration replacement and replay.

Gate: precedence rules of 3.4 verified by tests.

### Phase 5: Registration form

1. Keep the three fields bound to `Athlete.personalBest*` as seed inputs.
2. On successful form commit (find the existing post-save hook in
   `NAthleteRegistrationFormFactory`), in a background task: delete the athlete's
   rows with federation `Personal` and empty `groupNameString`, recreate from the
   three fields, then refresh record maps for the athlete's FOP under `ui.access`
   only for the UI part.
3. Cancel must persist nothing.
4. No new UI controls. Optionally show the current PB (max Personal value) as
   read-only helper text using an existing translation key if one fits; otherwise skip.

Gate: manual check — edit PB, save, PB box updates; cancel changes nothing.

### Phase 6: Workbook import/export

1. Import: `RecordDefinitionReader` requires no new columns and `fillDefaults`
   needs no change. Verified: it throws only when `ageGrp` is null (the declared
   `MissingGender` and `UnknownIWFBodyWeightCategory` are never thrown), uppercases
   `ageGrp`, and applies defaults only for `YTH`/`JR`/`SR`. A Personal row therefore
   needs `AgeGroup = PB`, `AgeLow = 0`, `AgeUpper = 999`, `BwLow = 0`, `BwUpper = 999`
   in the workbook. Add a reader test that a Personal row with no gender column
   imports cleanly and that `RecordEventSetters.setBwUpper` maps `999` to `999`.
2. `RecordEventSetters.setRecordName` must keep identities as text; add a test that a
   numeric cell containing a membership number is read as its string form without
   losing leading zeros where the cell is string-typed, and warn when numeric.
3. Export: `JXLSExportRecords` already exports whatever is filtered. Verify Personal
   rows pass through with federation, identity in `RecordName`, `athleteName`,
   `nation`, lift, value, and provenance. Verify `keepNewest` keeps the highest
   value per key for Personal rows; add a regression test if it does not.
4. `RecordContent` filters and `findDistinctFederations` must list Personal
   federations so they can be selected and exported. The grid may show identities;
   that is administrative, not public.
5. Round-trip test: export rows from `Personal` and `Personal:CAN` sharing no ID,
   re-import, identical rows.

Gate: round trip preserves federation, identity, and values.

### Phase 7: Documentation and acceptance

1. Update `data/records/RecordImportSpec.md` with a short Personal section
   (family recognition, identity in `RecordName`, precedence of 3.4).
2. Update `docs/2200Registration.md` and `docs/2500RecordsManagement.md` briefly.
3. Run authorized focused tests and Java validation. Ask before Maven.
4. Manual browser check: registration PB entry, scoreboard PB box, good lift PB,
   jury reversal, records export containing Personal rows.
5. Report changed files, tests, and the documented limitations (collision
   assumption; registration PB reload discards imported Personal history).

## 6. Acceptance Test Matrix

| Scenario | Required result |
| --- | --- |
| Membership `00123` | Identity `00123`, leading zeros kept |
| No membership; `José Da Silva` vs `JOSE DA  SILVA` same team/DOB | Same generated identity |
| No membership; same name/team, different DOB | Different identities |
| Personal rows in `Personal` and `Personal:CAN` for one identity | Both matched; max per lift displayed |
| Athlete with no DOB/bodyweight | PB box still shown |
| Athlete federation codes set, Personal rows present | Personal rows still eligible |
| Other athletes' Personal rows | Never displayed for current athlete |
| Identities | Never appear in `RecordConfig.recordOrder` |
| Good lift 101 over PB 100 (two federations) | One provisional row per federation |
| Jury reversal of that lift | Rows removed; 100 shown again |
| Equal or failed lift; total with no snatch | No Personal row created |
| Registration file with PB columns | All Personal rows deleted, baselines rebuilt |
| Registration file without PB columns | Personal rows untouched |
| Blank PB cell | No baseline for that lift |
| PB reload after lifts recorded | `recomputePersonalRecords` restores only Personal improvements |
| PB reload/replay with ordinary baselines and provisionals | Ordinary row IDs and all field values unchanged, including imported provisionals without participating owners |
| Personal replay repeated | No duplicate Personal improvements; ordinary records untouched |
| Form save changes PB | Only that athlete's `Personal` baselines replaced |
| Form cancel | Nothing persisted |
| Workbook export/import of Personal rows | Federation, identity, values preserved |
| Ordinary record tests | Unchanged and passing |

## 7. Documented Limitations

- Two athletes with identical membership IDs, or identical canonical
  name+team+birth discriminator with no membership, are treated as one identity.
- A registration import that includes PB columns discards all Personal records,
  including previously imported ones.
- Correcting name, team, or birth date of an athlete without a membership ID changes
  the generated identity; reload registration PBs afterwards.

## 8. Progress Checklist

- [ ] Phase 1: helper and identity
- [ ] Phase 2: lookup, eligibility, display
- [ ] Phase 3: improvements and reversals
- [ ] Phase 4: registration file import
- [ ] Phase 5: registration form
- [ ] Phase 6: workbook import/export
- [ ] Phase 7: documentation and acceptance

Checks run: none; this is a plan, not an implementation.
