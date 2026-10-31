# Stale-Save Check for Configuration Singletons

Plan, 2026-10-31, fourth revision — simplified. Prerequisite of the "Imports are
allowed" switch ([130](130-ACCESS_CONTROL_REMAINING.md#database-import)). No Java
has been changed by this document.

## Premise

`Config`, `Competition` and `RecordConfig` are each one row, edited rarely, by one or
two people. The only protection they need is: *refuse a save made from a form that was
opened before someone else saved, and say who that was*.

Two columns per entity do that:

| Column | Type | Meaning |
| --- | --- | --- |
| `lastSavedAt` | `Long` (epoch ms) | Changed by every save made through the repository |
| `lastSavedBy` | `String` | `AuditOrigin` actor label of that save |

A form captures `lastSavedAt` when it opens. On Save, inside one lock: reload the row,
compare, refuse if different, otherwise write the values and a new stamp. That is
exactly what JPA `@Version` would do, without Hibernate's transient/detached rules,
and the author comes for free and survives a restart.

Earlier revisions used `@Version`, a timestamp version, embeddables and an in-process
"generation counter". All of that is dropped.

### Migrations stamp too

Every persistent write to these rows stamps them, migrations included, so the rule
has no exceptions. Two migrations `em.merge` these entities outside the repositories:

- `ChampionshipRepository.migrateCompetitionIfNeeded` calls `stamp(AuditOrigin.system(
  "migration"))` on the `Competition` before its `em.merge`. Without the stamp, a
  Competition draft loaded before the migration would pass the stale check and write
  `migrated = false` plus the old legacy columns back. Consequence if that happened:
  the next `ensureCompetitionTemplate()` re-runs `migrateToChampionship`, which only
  *validates* the legacy values against the template (it never writes the template):
  if the template is unchanged it simply re-migrates; if the template was edited in
  between, the check fails, an error is logged, and the Competition's legacy values
  stay in force until the template matches again. No corruption, but template edits
  would appear ignored.
- `UtcNormalizationMigration` stamps the `Config` and `Competition` it merges, for
  consistency only.

Exposure is near zero either way: `Main` (~line 635, "database not empty" branch)
calls `reconcileFromAgeGroups` → `ensureCompetitionTemplate` at every startup, before
any screen exists; `UtcNormalizationMigration` runs at startup and only once (guarded
by `Config.isLocalDateTimeUtcNormalized`); after importing an old JSON,
`FormatDetector` migrates the freshly imported row, and any form opened before the
import is already refused by the import's stamp.

The published object is not a concern: drafts come from `loadForEdit()` and `apply`
reloads the row, so the published instance's stamp is never used for a check, and
`syncCurrentAfterMigration` already copies the migrated field values.

## Behavior

On a stale save the form stays open with the user's values and shows one centered
error notification:

> These settings were saved by {0} after you opened this page. Your changes were not
> saved. Reload the page and try again.

(`{0}` is `lastSavedBy`; when it is blank the sentence omits "by {0}".) Same message
for all three entities. No field list, no values, no override. "Not saved" is
literal: the refused Save wrote nothing.

Actions that save these settings immediately count as "someone else saved". The
release note says so:

> Settings pages (System Settings, Access Control, Competition, Records) now refuse to
> save if the same settings were changed after you opened the page, and say who
> changed them; reload the page and redo your change. Some actions save these
> settings immediately and count as such a change: uploading a registration file,
> saving the team schedule, choosing a different document template or age-group file,
> changing the start-number order, hiding empty championships, the IWF compliance
> switch, the video colour override, and uploading or clearing local overrides.

Two `Config` writers are one-time events and are deliberately left out of the note:
the PIN-hash upgrade (only when `PasswordHasher.needsRehash` says the stored hash is
outdated) and Remember Browser (only when no secret exists yet).

Opening a document download dialog does **not** count: see change 6.

## The irreducible changes

These are bugs or hazards in today's code that no stale-check design can skip.

### 1. Forms edit a detached copy

System Settings (`ConfigContent` → `ConfigEditingFormFactory`), Access Control
(`AccessControlForm`) and the Competition page (`CompetitionContent` →
`CompetitionEditingFormFactory`) bind directly to `getCurrent()`. The check is
meaningless if the form has already mutated the published object before Save.

- Each repository gets `loadForEdit()`: a fresh row from the database, never the
  published instance. The three pages pass it to their form.
- `EditChampionshipsPanel` takes a `Competition` parameter. On the Competition page
  it receives the form's draft and the `hideEmptyChampionships` checkbox only sets
  the field. `ChampionshipsContent` (standalone) passes a `loadForEdit()` copy and
  saves on change as today.
- The browser-derived default paper size (`ConfigEditingFormFactory` ~line 572)
  saves `Config.getCurrent()` while the form is open, which would make the form stale
  on arrival. Set the value on the draft instead; Save persists it.
- `RecordsConfigContent` passes the result of `addMissing` (below) to the form.

### 2. Remove the duplicate footer saves

`ConfigEditingFormFactory` line ~169 and `CompetitionEditingFormFactory` line ~176
each save a second time from the footer callback after the CRUD pipeline's `update`
already saved. The second save would be refused by the first. Delete both; move the
locale reset that follows the Config one into `update`'s success path.

### 3. V2 export must stop saving `Config`

**Why it saves today.** `CompetitionDataV2.setConfig(Config)` is a Jackson setter
written for *import*: when the file is deserialized, it calls `Config.setCurrent(
config)` so the imported locale and settings are in force while the rest of the file
is read. `Config.setCurrent` persists. `fromDatabase()` — the *export* side — reuses
that same public setter to fill the DTO, so every export writes `Config` to the
database. `setCompetition` has the same dual use (its `Competition.setCurrent` does
not persist, but it does republish).

**Why it matters.** `WebSocketEventForwarder.sendDatabase()` runs a V2 export on every
tracker connection open and from `sendDatabaseToAll()` after platform, championship
and age-group edits. With a stamp on the row, every tracker reconnect would mark
`Config` as "saved" and invalidate every open settings form.

**Fix.** Give V2 the split V1 already has (`setConfigForExport` /
`setCompetitionForExport`, private, assignment only), use them in `fromDatabase()`,
and set `appVersion` on that export copy, not on the published `Config`. The Jackson
import setters then become publish-only (in-memory, no save): the import's own
`restore()` persists a few lines later anyway.

### 4. One lock around compare + write + publish

`static final Object SINGLETON_SAVE_LOCK` in `data.jpa`, shared by the three
repositories (the Competition page saves `Competition` and a `Config` feature switch
in one action, so one lock, not three).

```
synchronized (SINGLETON_SAVE_LOCK) {
  tx begin
    row = em.find(entity, draft.id)           // null -> StaleSingletonException
    if (!Objects.equals(row.lastSavedAt, draft.lastSavedAt))
        -> StaleSingletonException(row.lastSavedBy)
    draft.stamp(origin)    // lastSavedAt = max(now, row.lastSavedAt + 1), lastSavedBy = label
    merged = em.merge(draft)                  // copies draft onto the managed row
  tx commit
  publish merged (carry runtime state, below)
}
post-commit side effects: TimeZone.setDefault, forwarder restart, access-mode audit,
account initialization, rankings, FOP events, UI
```

The comparison uses the stamp the draft was loaded with; `Objects.equals` on `Long`
makes "never saved" (null) equal to null. Merge-first matches what
`ConfigRepository.save` already does for the Local Override BLOB.

Stamping the draft itself means a form that stays open after a successful Save
(`AccessControlForm`) holds the new stamp and can save again. If the commit then
fails, the draft carries a stamp that is not in the database and its next Save is
refused; the user reloads. Accepted.

Holding the lock through publication is what stops an older committed object from
replacing a newer one in memory. Nothing inside the lock may wait on a UI, the
network or another lock.

`Config.getCurrent()` reads `current` under the `Config.class` monitor, while
publication writes it under `SINGLETON_SAVE_LOCK`: two different monitors, no
happens-before. Make `Config.current` and `Competition.competition` `volatile` (or
make the publish methods `static synchronized` on the entity class). `Competition.
getCurrent()` is not synchronized at all today. `RecordConfig`'s static is only
written and immediately returned, never read elsewhere; no change. There is no lock
inversion: `getCurrent()`'s lazy load never takes `SINGLETON_SAVE_LOCK`.

The transaction helper used here rolls back on failure and propagates every exception
(`JPAService.runInTransaction` swallows PostgreSQL exceptions and returns `null`;
add a strict variant for this path only).

### 5. Immediate writers use `apply`

`SecuritySettings`, `AccessUtils.upgradePin`, `RememberServlet`, the IWF toggle,
`LocalOverrideUploadDialog`, the Clear button, `TemplateSelectionFormFactory`,
`DocumentDownloadDialog`, `JXLSDownloader` (with the guard of change 6),
`TeamScheduleElement`, `AgeGroupContent`, `NRegistrationFileProcessor`,
`ProtocolScenario`, `VideoNavigationContent` (below), `Main.startMQTT` (~line 406,
`mqttInternal`) and `Main` ~line 602 (UTC flag) all do `getCurrent().setX(...);
save(getCurrent())` on an existing row. Two of them racing share one object.

Not in this list — they build a new object and belong to `create`:
`ProdData` / `DemoData` / `BenchmarkData` (`new Competition()`), `Main` ~line 640
and `Config.initConfig` (`new Config()`), and the `RecordConfig` default row.
`CompetitionEditingFormFactory.saveStartNumberOrderConfiguration` is handled by the
combined save below, not by a plain `apply`.

Each repository gets `apply(Consumer<T> change, AuditOrigin)`: inside the lock, load
the row, run the consumer on it, stamp, commit, publish. The consumer re-checks its
own precondition on the row it was given ("secret still absent", "hash still
outdated"). `apply` cannot be stale — it reads the row it writes — but it invalidates
every open form, which is the intended coarse behavior.

### 6. Download dialogs must not save when nothing changed

`JXLSDownloader.createDialog()` calls `processTemplateSelection(…, found)` to
preselect the stored template, and `processTemplateSelection` ends with
`CompetitionRepository.save(current)` (~line 307). So **every opening** of a download
dialog saves `Competition` — 18 buttons on 10 pages, including the Announcer page and
the results pages. During a competition, any results download would invalidate every
open Competition form.

Fix: in `processTemplateSelection`, write (through `apply`) only when the chosen
template name differs from the one stored on the row. On opening it never does, so
the dialog no longer saves. A genuine template change still saves and still counts as
a change (release note). `DocumentDownloadDialog` already saves only from its
value-change listener; apply the same "only if different" guard there for symmetry.

## Repository API

| Operation | Use | Stale check |
| --- | --- | --- |
| `T loadForEdit()` | forms | — |
| `T saveEdited(T draft, AuditOrigin)` | form Save | draft's stamp vs row's; missing row → stale |
| `T apply(Consumer<T>, AuditOrigin)` | immediate writers on an existing row | — (reads then writes under the lock) |
| `T create(T fresh, AuditOrigin)` | `-reset`, initial data, `RecordConfig` default row | refuses if a row exists |

Each entity also gets a package-private `stamp(AuditOrigin)` that sets
`lastSavedAt = max(now, previous + 1)` and `lastSavedBy`. The repository operations
call it; import calls it directly before its existing `em.merge` (see Import).

`Config.setCurrent`, `CompetitionRepository.save` and `RecordConfig.setCurrent` are
removed once callers migrate (≈40 Config call sites including tests). A transitional
`Config.publish(Config)` (in-memory only) is needed for the import setters above and
for `Main`'s startup publication.

The draft carries the `lastSavedAt` it was loaded with; there is no separate
"expected stamp" parameter. `@JsonIgnore` on both columns so exports do not carry
them; a null stamp in the database means "never saved" and compares equal to a draft
loaded with null.

### Runtime state carried across publication

A freshly loaded `Config` lacks transient state the published one holds. Before
publishing, copy from the previous published instance: `managedForwardingActivity`,
`mqttConfig`, `traceMemory`, `useCompetitionDate`. For `Competition`: `simulation`,
`rankingsInvalid` (the two caches may start empty, as today).

`Config` has getters that normalize the object they read (`getFeatureSwitchJson`,
`getForwardingDestinations`); they are idempotent and exempt from the "do not mutate
the published object" rule, which is about setters and the binder.

### Competition + Config in one Save

`CompetitionEditingFormFactory.update` saves `Competition` and then `Config`'s
`BW_CLASS_THEN_AGE_GROUP` switch. That switch is also editable on the System Settings
feature tab (`ConfigEditingFormFactory` ~line 354 excludes only
`TEAM_POINTS_TOTAL_ONLY`), so a plain `apply` would silently overwrite a concurrent
change made there.

At form open, capture the switch's value alongside the radio. On Save, under **one**
`SINGLETON_SAVE_LOCK` acquisition, in this order:

1. If the radio still equals its opening value, `Config` is not involved; go to 3.
2. Otherwise load the `Config` row and compare its `BW_CLASS_THEN_AGE_GROUP` value
   with the value captured at open. If different → `StaleSingletonException`
   (with the row's `lastSavedBy`); nothing has been written yet.
3. `Competition` stamp check (as in `saveEdited`). If stale → refuse; nothing written.
4. Write `Competition`; then, if step 2 ran, write `Config` with the new switch value.
5. Publish both.

Comparing the switch *value* rather than `Config.lastSavedAt` means a PIN-hash
upgrade at login or a Remember Browser secret does not block a start-number change.
Because every check runs before any write, and every writer takes the same lock, the
"Your changes were not saved" message is literal here too. The two writes remain two
transactions; a database failure between them is the only partial outcome, reported
as an ordinary error.

### Same-page immediate saves

| Page | Immediate save | Resolution |
| --- | --- | --- |
| Administration | IWF toggle saves `Config` beside `AccessControlForm` | after `apply`, the form reloads and `readBean`s **only if `binder.hasChanges()` is false**; a dirty form is left to conflict |
| Records configuration | `addMissing` at page build and after upload/clear | `addMissing` saves only when the order changed and returns the saved instance; the page/callback adopts it (re-read if clean) |
| System Settings | Local Override upload / Clear | unchanged: both reload the page |
| Administration | access-mode switch | ends sessions only after its `apply` commits |

### Video colour overrides

`VideoNavigationContent` sets `videoColorOverrides` / `enableColorOverrides` on the
published `Config` without saving. Today the value reaches the database only because
something else saves the published object — notably the V2 export on every tracker
connection. Under this plan that stops, and every save publishes an object loaded
from the database, so the next `Config` save anywhere would silently reset the colour
on the video displays.

Decision: the checkbox and the colour picker each call `ConfigRepository.apply(...)`.
The choice is persisted, survives a restart, and is exported as it already is. The
page is rarely used, so one write per change is acceptable; the change counts as a
`Config` save (release note). Not added to the runtime-carry list.

## Import

Nothing structural changes. `apply` does not fit: the import holds a fully
deserialized object to write wholesale, and none of the three entities has a
field-by-field copy method. `create` would fit `Config` and `Competition` (their rows
are deleted by `removeAll` first) but not `RecordConfig`, whose row survives. The
smallest uniform change is to stamp the deserialized object and keep today's writes
and transaction boundaries:

- `Config`: `restore()` calls `Config.setCurrent(config)` in its own transaction.
  Replace with `config.stamp(origin)` followed by that same save path (insert, since
  the row was deleted).
- `Competition`: `competition.stamp(origin)` just before the existing
  `em.merge(competition)` inside the restore transaction (insert).
- `RecordConfig`: `recordConfig.stamp(origin)` before the existing
  `em.merge(updated.getRecordConfig())` (update onto the surviving id-1 row; plain
  `merge` handles this without `@Version`).

`restore()` takes no `AuditOrigin` today. `FormatDetector.importData(…, importOrigin)`
has one; pass it down to both `CompetitionData.restore` and `CompetitionDataV2.restore`.
`Main`'s startup JSON load passes `AuditOrigin.system("startup import")`.

A form opened before the import captured the old stamp and is refused afterward —
including when the row was deleted and recreated, because the new stamp is a fresh
timestamp, never a repeat. Imported JSON never carries the two columns.

## Attribution and notification

`StaleSingletonException(type, String savedBy)` is thrown by `saveEdited`. The
existing session `ErrorHandler` in `AppShell` handles it before the generic branch:
translate with the session locale, `NotificationUtils.centeredError`, log at `info`
without a stack trace. Since the exception propagates out of `update()`, the CRUD
pipeline's "Saved" notification and grid callback do not run. Direct Save buttons
(`AccessControlForm`, `RecordContent` dialog) call `saveEdited` on the UI thread and
are caught the same way. No worker thread is introduced.

`lastSavedBy` is the `AuditOrigin` actor rendered as `user@station` in Accounts
mode, the station/page in PIN mode, or `system` for `AuditOrigin.system`. Never the
session fingerprint.

Translation keys (`ConcurrentEdit.Settings.*`) go through the TSV process.

## Tests

Non-UI tests only, H2. PostgreSQL by manual acceptance.

| Test | Asserts |
| --- | --- |
| two drafts, each type | first `saveEdited` succeeds; second throws stale with the first's `lastSavedBy`; row unchanged |
| apply vs draft | an `apply` commits; a draft opened before it is refused |
| missing row | Competition row deleted → `saveEdited` throws stale; no second row inserted |
| publication order | two threads saving: the later commit is the one published |
| failed commit | published instance and runtime state unchanged; the draft's next save is refused |
| form stays open | after a successful `saveEdited`, the same draft saves again without conflict |
| runtime carry | after `saveEdited`, forwarding activity / `mqttConfig` / `simulation` equal the previous published instance |
| export does not save | V1 and V2 `exportData` leave `lastSavedAt` unchanged |
| download dialog does not save | opening a `JXLSDownloader` dialog with the stored template leaves `Competition.lastSavedAt` unchanged; choosing a different template changes it |
| video colour | after an `apply` of the colour, an unrelated `Config` save keeps the colour |
| migration stamps | after `migrateCompetitionIfNeeded`, a Competition draft loaded before it is refused |
| import | after V1/V2 restore, each singleton has a new stamp and `lastSavedBy` from the import origin; a draft from before is refused; old exports without the columns restore |
| same millisecond | two consecutive saves get strictly increasing stamps |
| addMissing | returns saved instance when changed, `this` when not; no write when unchanged |
| combined save, radio unchanged | `Config` not read or written |
| combined save, radio changed | switch unchanged elsewhere → both written; switch changed elsewhere → stale, neither written; unrelated `Config` save (PIN upgrade) → both written |
| combined save, Competition stale | refused before any write; `Config` untouched |
| null stamp | existing row with null stamp loads, edits and saves normally |

Browser acceptance, two sessions: System Settings vs Access Control; two Competition
forms; template change then Competition Save; download dialog opened from the
Announcer page then Competition Save (must succeed); IWF toggle then Access Control
Save clean and dirty; Records upload then Save.

## Order

1. Two columns on each entity; `StaleSingletonException`; strict transaction helper;
   `SINGLETON_SAVE_LOCK`.
2. **V2 export split and download-dialog guard** — first functional changes, because
   they are the ones that would otherwise produce false conflicts in production.
3. Repository API with runtime-state carry; old methods delegate temporarily.
4. Immediate writers → `apply` (video colours included); creators → `create`;
   migrations stamp; import stamps before its existing writes and receives the import
   `AuditOrigin`.
5. Forms → `loadForEdit` / `saveEdited`; delete the two footer saves; paper size,
   championships checkbox, IWF refresh-if-clean, `addMissing` return value.
6. `AppShell` handler, translations, release note (text under Behavior).
7. Remove the old save methods; focused tests; browser acceptance.
8. Then item 130's IMPORT permission and `importsAllowed`.

## Cross-check

None open. Resolved 2026-10-31: video colours use `apply`; release-note text is under
Behavior; migrations stamp (the earlier "need no change" claim was wrong, and its
stated consequence — overwriting the template — was also wrong; see "Migrations stamp
too"); download dialogs no longer save on opening.
