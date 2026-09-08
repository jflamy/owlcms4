# Personal Records: Tracker Plugin Implementation Plan

Status: plan only. No tracker or OWLCMS application changes made.

Companion: [Personal Best Records implementation plan](personal-best-records.md).

## Recommendation

Live scoreboard plugins render the separate PB block already supplied by OWLCMS.
Book plugins exclude Personal rows from their ordinary record sections. No PB
computation, identity matching, new storage, or new wire discriminator is needed
for either path.

PB improvements produce no notifications on OWLCMS or tracker scoreboards or
attempt boards. Updating PB values and their supplied highlights is display data,
not a new-record announcement. Ordinary-record notifications remain unchanged.

Preserve incoming PB data in the hub and transport. Filtering belongs at the book
consumer boundary, not ingestion. No changes to the main PB implementation plan
are part of this task.

There are different representations on the wire. Identify Personal records using
the marker appropriate to each representation, before ordinary grouping or display.

## Identification Contract

| Representation | Reliable Personal marker | Initial action |
| --- | --- | --- |
| Live UPDATE `records.recordTable` display block | `recordClass === "recordBoxPersonal"` | Extract separately and render the supplied PB values, highlights, and title |
| Raw database `records[]` record event | `recordFederation` is exactly `Personal` or starts with `Personal:` with a nonempty suffix | Exclude before eligibility, grouping, summary, or rendering |
| Structured decision record events, if supplied | Same raw federation predicate, provided the field is present | Exclude Personal events from notification triggers |
| Aggregate `recordKind` / `recordMessage` | No reliable Personal marker currently | Producer must derive notification status from ordinary records only |

Do not recognize Personal records from a translated title, athlete name, membership
ID, hash prefix, `ageGrp = PB`, or the `0..999` bounds. These are not the type contract.
In particular, `recordName` becomes athlete identity in raw Personal rows; it is not
a safe display label or a reliable type discriminator.

The federation predicate must match the convention adopted by the OWLCMS plan.
Use one helper wherever raw records are filtered, not slightly different checks
in each plugin. The live display block has its own explicit marker and does not
need to expose the originating Personal federations or athlete identity.

### Existing Core Data Access

`competitionHub.getDatabaseState().records` is the full raw collection. Tracker-core's
V2 parser preserves incoming rows; there is no dedicated typed records getter.
Book helpers select and group these rows. Use one shared raw-row predicate for the
exact, case-sensitive Personal family; null, `Personal:`, and `Personalized` do not
match. The predicate can be exposed by tracker-core for reuse by consumers.

Live scoreboards instead consume `competitionHub.getFopUpdate({ fopName }).records`.
OWLCMS has already selected the PBs for the displayed athlete. The existing
`recordBoxPersonal` marker suffices; do not add a new scope schema or rederive PBs
from the database. Preserve the complete block through the server-side helper.

Tracker-core's exported `extractRecordsFromUpdate` is a legacy helper that filters
raw rows for nonempty `groupNameString`; it is not the tracker-local display-block
extractor. Do not substitute one for the other.

## 1. Live Record Boxes: Small, Local Change

Verified path in the tracker repository:

1. `src/lib/server/standard-scoreboard-helpers.js` obtains `fopUpdate.records`.
2. `src/lib/server/records-extractor.js::extractRecordsFromUpdate` parses a string
   or object and converts blocks into federation/category rows.
3. `src/lib/components/StandardScoreboard.svelte` passes these rows to
   `src/lib/components/RecordsSection.svelte`.
4. Lifting-order, start-order, and rankings plugins share that component. The
   attempt-bar helper also calls the shared server helper, but its inspected page
   does not render `RecordsSection`.

Current incompatibility: the extractor ignores `recordClass` and interprets every
block's `records[]` using indices from the top-level `recordNames` array. The PB
block has one value row of its own, not one row per ordinary record name.

A read-only Node probe against the current extractor confirmed:

- PB block with `recordNames = []`: returns no records.
- Same PB block with `recordNames = ["World", "National"]`: assigns PB values to
  World and adds an empty Personal Best category under National.

Implementation steps:

1. Extract `recordClass === "recordBoxPersonal"` blocks separately before ordinary
   federation-indexed iteration. Retain their supplied values, highlights, and
   `cat` title. Keep the ordinary extractor's existing output contract where
   practical; expose the PB block separately through the shared scoreboard helper.
2. Preserve `recordNames` and ordinary row indices. Never interpret the PB value
   row as the first ordinary federation or synthesize empty ordinary categories.
3. Pass the PB block through `standard-scoreboard-helpers.js` and
   `StandardScoreboard.svelte` to `RecordsSection.svelte`. Render it as a separate
   PB section using the existing record display styling, without identity labels.
   Svelte displays supplied data only; no PB calculations or selection logic.
4. Update visibility checks so PB-only data renders even when the ordinary records
   array is empty. Preserve existing break/no-current-athlete behavior. Ensure
   cached responses, athlete changes, and jury reversals carry the latest block
   and clear it when it is absent.
5. Keep string/object payload support and legacy ordinary flat-format behavior.
   No PB block means no PB section; do not infer PBs from unmarked legacy data.
6. Verify shared scoreboard consumers. Do not add a records section to attempt
   boards that do not currently display one, or introduce PB announcements there.

The PB title is already translated by OWLCMS in
`RecordFilter.buildRecordJson`: `Translator.translate("Record.PersonalBest")`
becomes the block's `cat` value. Identify by `recordClass`, display the supplied
translated title, and preserve supplied lift highlights.

## 2. Raw Records: Filter Before Category Processing

Adjacent document consumers also need protection once Personal rows enter the
exported competition database. These are not live record-box consumers.

Verified tracker locations:

- `src/plugins/books/iwf-helpers/records-extraction.js::extractRecords` selects raw
  records using federation, gender, age, and weight, without Personal ID matching.
- `extractNewRecords` selects provisional session records.
- `keepLargestRecordsBySummaryCategory` groups without `recordName` (athlete
  identity), so Personal records for different athletes could collapse together.
- IWF startbook and results-book helpers consume these utilities and also have
  whole-competition record sections that need their call sites checked.

For the initial migration, exclude raw Personal rows at each records entry point
before ordinary selection/grouping. Do not teach these category-based routines to
match Personal identities yet. Ensure all-record, session-record, and new-record
sections apply the same exclusion. Do not delete records from hub/database storage
just to hide them; keep filtering at the consumer boundary.

## 3. No Personal-Best Notifications

PB improvements must not trigger banners, new-record scenes, or other record
notifications on OWLCMS or tracker scoreboards/attempt boards. Do not implement a
Personal notification stream or reserve a future PB-notification UI in this plan.

Verified OWLCMS paths:

- `monitors/websocket/ForwarderPayloadBuilder.populateRecordInfo` sets
  `recordKind = new` whenever `fop.getNewRecords()` is nonempty, otherwise `attempt`
  when challenged records are nonempty. The message is generic and translated.
- `monitors/WebSocketEventForwarder.populateRecordInfo` contains equivalent logic.

When Personal rows enter challenged/new-record lists, these aggregate checks must
exclude them before deciding notification kind and message. This is an upstream
integration requirement, not a reason to add a new wire type: PB-only gives
`recordKind = none`; simultaneous ordinary/PB outcomes report only the ordinary
outcome. An ordinary attempt plus a new PB must remain an ordinary attempt, not
be promoted to a new-record notification. Preserve full internal lists for PB
persistence and reversals, and preserve PB display blocks and raw database rows.

Verify the same no-PB-notification rule on OWLCMS's own display paths and on
update, decision, and jury messages. This file records the integration dependency;
the main plan and application code are not being edited here. Tracker cannot
reliably repair an undifferentiated aggregate using a translated message or the
presence of a PB box. With correct producer aggregates, keep tracker notification
behavior unchanged, including clearing stale status.

One adjacent OBS path needs follow-up: tracker
`src/plugins/OBS/shared/helpers-core.js::resolveVisibleVariant` chooses
`newRecordVisible` whenever `decision.records` is a nonempty array. Trace how that
array is produced before implementation. If it contains raw record objects, exclude
Personal rows from the trigger without mutating stored data. Otherwise verify that
the producer supplies ordinary-only notification inputs. Its actual PB payload has
not been verified. A PB improvement must not select a new-record scene.

## 4. Initial Implementation Order

1. Confirm/freeze the two existing markers: display `recordBoxPersonal` and raw
   Personal federation family. Capture representative OWLCMS messages as fixtures.
2. Separate the PB block in the local display extractor and pass it through the
   shared helper/components. Render it on existing record-displaying scoreboards.
3. Add one shared raw Personal predicate and apply exclusion to book record
   consumers, including summaries. Leave the full database collection intact.
4. Verify the ordinary-only notification producer contract and tracker/attempt-board
   behavior, including mixed outcomes and reversals. Track any producer fix as an
   integration dependency; do not introduce PB notifications.
5. Check OBS/new-record triggers and shared plugin consumers for the same rule.
6. Document the plugin policy: scoreboards display supplied PBs, books omit them,
   and PB improvements never generate record notifications.

The active live helper imports tracker-local `records-extractor.js`, not a
tracker-core extractor. Do not assume changing the shared package alone fixes the
bundled scoreboards. Inspect tracker-core only as needed for decision/raw-record
transport and any consumers that actually import its helpers.

## 5. Focused Acceptance Checks

| Fixture / workflow | Expected result |
| --- | --- |
| Ordinary records only | Existing values, names, category order, highlights unchanged |
| Ordinary blocks plus PB block | Ordinary output unchanged; PB rendered separately with supplied values and highlights |
| PB-only, empty `recordNames` | PB section visible with no ordinary grid or parsing failure |
| PB-only with stale ordinary names | PB section visible; no empty federation/category rows synthesized |
| Localized PB `cat` label | Supplied translated title displayed; identification remains structural |
| String and object UPDATE records | Same ordinary and PB display output |
| Athlete change, absent PB block, break, or no current athlete | No stale PB values; existing visibility policy respected |
| PB improvement and jury reversal | Latest supplied values/highlights displayed without a notification |
| Raw `Personal`, `Personal:CAN`, `Personal:USABARS` | Excluded from document record sections |
| Raw null federation, `Personal:`, or `Personalized` | Not classified as Personal |
| Raw ordinary federation with ageGrp PB or broad bounds | Not excluded merely by those values |
| Filtering a view or indicator | Stored incoming PB data and markers remain intact; other consumers see the original payload |
| PB section on desktop/mobile | Readable values and title without overlap; ordinary grid layout preserved |
| PB-only record attempt/improvement | No record notification on OWLCMS or tracker scoreboards/attempt boards |
| Simultaneous ordinary record and PB | Ordinary record notification preserved |
| Ordinary attempt plus new PB | Ordinary attempt notification only; PB values updated separately |
| PB reversal / athlete change | No stale PB-derived status survives |
| OBS decision containing PB-only record list | No new-record scene triggered |

Use the existing test framework. Shared live-extractor tests belong in shared test
infrastructure; book-owned tests remain colocated with the book helpers/plugins.
No need to duplicate PB identity calculation, ranking, or record adjudication in
tracker. OWLCMS remains the authority for all three.

## Effort and Verification Limits

- Live PB display: small, shared extractor/helper and record-rendering components
   plus focused tests and desktop/mobile browser checks.
- Book record exclusion: small to moderate; several entry points need coverage.
- No-PB-notification verification: integration risk if producer aggregates include
   Personal rows; verify ordinary-only status without adding a new protocol.

Verified by source inspection and a read-only synthetic extractor probe. No tracker
source was changed, no builds or full tests were run, and no browser was launched.
This is an initial assessment, not a full audit of custom plugins or tracker-core.