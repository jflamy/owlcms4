# 200 Implementation Plan: Age-group editing and championship reassignment

Status: done 2026-11-04, archived.

Implements [200-AGEGROUP_EDIT_CONFIRMATION.md](200-AGEGROUP_EDIT_CONFIRMATION.md) (the spec).
The spec says what; this plan says how and in what order. Section references (§) are to the
spec. Where they disagree, stop and ask the maintainer.

## Rules for this work

- Read `AGENTS.md` and `.github/copilot-instructions.md`. In particular:
  - Never run Maven while the dev server is up. Check with
    `curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost:8080/`
    (000 means down). If it is up, ask the maintainer to stop it and wait.
  - Focused Maven test runs are pre-authorized (skill `run-maven-test`); read the Surefire
    reports. Never `mvn package` or `install`.
  - Never commit, push or stage without the maintainer's explicit go. Never add a Copilot
    `Co-authored-by` trailer.
  - Never edit `shared/src/main/resources/i18n/translation4.csv`; new keys go into a TSV
    (skill `update-translations`). No hard-coded UI strings. No fully qualified class names.
  - Capture `UI`, locale and `AuditOrigin` on the UI thread and pass them down. No
    `OwlcmsSession`/`UI.getCurrent()` in domain code. No blocking work in `ui.access`.
  - Logging: skill `owlcms-logging-format`.
- Tests: skill `add-test-case`. No Vaadin objects in tests. All domain logic below must be
  callable without Vaadin.
- After editing Java, check the Problems panel for touched files (skill `verify-java-fix`).
- Stop at the end of each phase, report, and wait for the maintainer.

## Decisions taken by this plan (confirm at phase 1 report)

1. **Stamp storage.** `lastSavedAt` (`Long`) and `lastSavedBy` (`String`, 255) columns on
   `AgeGroup`, the object being edited (spec §3.8). Not exported.
2. **Stale kind.** Add `StaleSingletonException.Kind.AGE_GROUP`. The existing `AppShell` error
   handler and `NotificationUtils.staleSettings` then report it with no new UI.
3. **§9 open decision** is implemented as proposed there (buttons disabled while saving,
   a pre-commit failure closes the confirmation dialog, retry starts over, a stale draft must
   reload). Post-commit refresh failures are a separate outcome, not a failed save (3.2).
4. **Imports create fresh rows.** Loading definitions (predefined or uploaded file) and database
   import (V1 and V2 JSON) create age groups with a fresh stamp; a draft of a replaced row is
   refused because its row is gone.
5. **Post-commit ordering.** Keep spec §11 step 8: commit definitions and assignments, then
   refresh counts and rankings. Do not move persisted ranking updates into the save transaction
   without first agreeing a spec change. A refresh failure must not replay a committed save.
6. **Definition reloads are special (maintainer confirmed 2026-11-03).** Keep their replacement
  workflow outside the new session guard and single-transaction guarantee, including the
  legacy reassignment calls. Fresh-row stamping still applies (3.3).
7. **Unchanged submissions are accepted saves.** `NONE` changes no definitions or assignments,
  but goes through `write` and advances the stamp, as required by spec §3.8. Cancel never saves.
8. **No new locking mechanism.** Age-group stamping reuses the existing `SingletonSaves.LOCK`,
  `findFresh`, `nextStamp`, `savedByLabel` and exception-propagating `inTransaction` pattern
  already used for `Config`, `Competition` and `RecordConfig`. The stale-check API is extended
  to a new `AGE_GROUP` kind; it is not a new synchronization or save framework.
9. **Session guard scope.** The guard protects athletes already placed in a started session,
   because a platform caches those athletes and can recreate participations removed by a
   definition save. An unrelated upcoming championship remains editable: fixing Masters must
   work while children are lifting. Reuse the existing `LiftingSessionGuard` rule: refuse only
   when the save can change an athlete assigned to a session that is in progress on any
   platform. Compute that scope from the *stored* data, before classification, so it also
   catches moves into or out of a session/championship. An Open championship appears in every
   session, so an Open boundary fix is normally impossible during competition; there is no
   override. The athlete finishes in the wrong category and the supervisor fixes the boundary
   and reassigns during a quiet period.

## Phase 1: Infrastructure

### 1.1 Age-group stamp

- `AgeGroup`: add `lastSavedAt`, `lastSavedBy` (`@JsonIgnore`), getters, and
  `stamp(AuditOrigin, Long previous)` (copy `Competition.stamp`). Add them to the V2 mixins as
  ignored and list them in `V2FormatCoverageTest`.
- `AgeGroupRepository.loadForEdit(Long id)`: fresh copy via `SingletonSaves.findFresh`,
  categories initialized, detached.
- `AgeGroupRepository.assertCurrent(Long id, Long draftStamp)`: read-only preflight using a
  fresh row, throwing the same stale exception for a missing row or mismatched stamp. Run it
  before the confirmation flow and on each retry. It reserves nothing; the check inside
  `write` remains authoritative.
- `AgeGroupRepository.write(Long id, Long draftStamp, AuditOrigin origin,
  BiFunction<EntityManager, AgeGroup, T> work)`: under `SingletonSaves.LOCK`, in one
  `SingletonSaves.inTransaction`: read the row fresh; if missing or `lastSavedAt` differs from
  `draftStamp`, throw `StaleSingletonException(AGE_GROUP, row != null ? row.getLastSavedBy() :
  null)`; run `work.apply(em, managedRow)`; stamp the surviving row with
  `row.stamp(origin, row.getLastSavedAt())`; return the result after commit. A delete callback
  removes the row instead of stamping it. Missing rows never fall back to creation.
- Rows created by `AgeGroupDefinitionReader`, `AgeGroupRepository.add`, and both JSON imports get
  `stamp(origin, null)`. UI ADD uses the atomic create service in 3.3, not the edit-only `write`.
  The three `reloadDefinitions` overloads and `insertAgeGroups` take an `AuditOrigin`: UI callers
  pass the one captured on the UI thread, startup passes `AuditOrigin.system(...)`.
- Rows that existed before the upgrade have a null `lastSavedAt`. A draft loaded from such a row
  carries null, the comparison treats null = null as current, and the first save stamps the row.
- `write` owns the lock and transaction. Its callback and all helpers use the supplied
  `EntityManager`; they must not call either `SingletonSaves.inTransaction` or
  `JPAService.runInTransaction`. Use the exception-propagating `SingletonSaves.inTransaction`
  only at the outer transaction boundary.

### 1.2 Session check

- Scope: protect athletes already assigned to a session that is in progress on any platform
  (decision 9). This deliberately differs from refusing when any platform is busy. Editing a
  championship used only by upcoming or unrelated sessions remains possible.
- Use `OwlcmsFactory.getFOPsWithSessionInProgress()` as the low-level detector, then compute
  the affected-athlete scope from stored data before classification: athletes whose stored
  participations or session assignment intersect the championship/age groups being changed,
  including moves into or out of those definitions. Refuse only if any such athlete belongs to
  an in-progress session; the message names each conflicting session and platform.
- Because Open athletes may belong to every session, an Open boundary change is refused until
  no session containing affected athletes is in progress. No in-session override is provided;
  the athlete lifts in the wrong category and the fix is applied in a quiet period.
- `SessionsInProgressException` carries the conflicting session/platform pairs, not merely all
  FOPs. Definition reloads remain exempt (3.3); championship renaming has its separate
  stamping-only contract in 3.3.

### 1.3 Tests — new `owlcms/src/test/java/app/owlcms/tests/AgeGroupStampTest.java`

Copy setup from `data/config/SingletonStaleSaveTest`. Cover: two drafts of one age group, the
second is refused and writes nothing; saved-by label is reported; a save of another age group
does not refuse the draft; a draft whose row was deleted is refused and inserts nothing; stamps
strictly increase; the stamp is not in V1/V2 exports. Verify preflight refusal and a write that
becomes stale after a successful preflight; the transactional check must still refuse it.

## Phase 2: Fit rule and reassignment engine (domain only)

New package `app.owlcms.data.agegroup.reassign`. No Vaadin imports.

### 2.1 Snapshot — `AthleteSnapshot` (record)

Per athlete, taken inside the transaction: id, gender, age (`Athlete.getAge()`), body weight
(`getBodyWeight()`, else `getPresumedBodyWeight()` **before** any category change), entry total
(`getQualifyingTotal()`), participations (category id → championship name, teamMember,
mixedTeamMember), and registration category id. Copy the collections so membership and weights
cannot change when managed definitions are updated. Pass this snapshot explicitly to `plan`;
do not reconstruct it from mutated entities in step 6.

### 2.2 Fit rule — `CategoryFit`

`FitResult fit(AthleteSnapshot a, Category c)` returning FITS or a `LeaveReason`
(`AGE`, `GENDER`, `QT`, `INACTIVE`, `MISSING_BIRTH_DATE`, `MISSING_BODY_WEIGHT`), per §2:
active age group; age in range; gender (category gender equals athlete gender, as
`CategoryRepository.filteringWhere` does; check `Gender` for the gender-inclusive variant and
reuse it); `min < bw <= max`; `!c.requiresEntryTotalConfirmation(total)`.
For one athlete and a set of categories, missing birth date and missing body weight are checked
first and reported as such. Otherwise rank the candidates by closeness — gender match, then age
band containing the athlete, then active, then weight in bounds, then QT met — lexicographically
in that order; the best candidate's first failing criterion is the reported reason (so a
deactivated age group that would otherwise fit reports `INACTIVE`, not `AGE`). Ties between
overlapping age groups go to the most preferred category (2.3 registration preference). With no
candidate at all (empty target list), the reason is `INACTIVE`.

### 2.3 Reassigner — `ChampionshipReassigner`

`ReassignmentPlan plan(EntityManager em, Map<Long, AthleteSnapshot> snapshots,
Map<String, List<Category>> targetCategoriesByChampionship,
Map<String, List<Category>> reasonOnlyCategoriesByChampionship,
Map<String, Boolean> mixedTeamsByChampionship, Mode mode)` and
`void apply(EntityManager em, ReassignmentPlan plan)`.

- The caller captures snapshots before changing definitions, then builds the target map from
  the final category definitions. Exclude every category marked for deletion, even while its
  entity remains attached. Include championship keys with an empty target list. Inactive final
  definitions never pass the fit rule, but the caller passes their categories in the separate
  reason-only map so a deactivation reports `INACTIVE`; deleted categories appear in neither
  map.
- Resolve effective mixed-team settings from championship rows and their competition template
  using the supplied `EntityManager`; pass the resulting scalar policy map to the engine.
  A newly materialized championship must be read in that same transaction. Do not call
  `AgeGroup.isMixedTeams()`, `AgeGroup.getChampionship()`, `Championship.of` or `findStored`
  inside the save/preview traversal: they can query through another transaction or bootstrap
  missing championships through the global cache. No transaction-local entity may be published
  to that cache before commit. Store the resolved policy in the executable plan for `apply`.
- Mode `WITHIN` (§4): athletes = those whose snapshot has a participation in that championship,
  including athletes linked only to categories marked for deletion. Target = every fitting
  category in that championship's final target list. Never add athletes outside it, including
  when processing several championships in one action.
- Mode `ALL` (§5 "Recompute all eligibilities"): the caller snapshots every athlete; target =
  every fitting category in all championships' final lists.
- Plan holds per athlete: kept, removed, added categories; whether they leave/join each
  championship and why. An executable plan is local to the transaction that computed it.
- Apply:
  - kept → same `Participation` row, team flag and ranks unchanged; clear `mixedTeamMember`
    only if the transaction-local policy says that its championship no longer has mixed teams.
  - removed → remove the `Participation` from both collections, then `em.remove` (pattern in
    `AgeGroupRepository.removeParticipations`).
  - added → `athlete.addEligibleCategory(c, !FeatureSwitch.EXPLICIT_TEAMS.isEffective(), false)`.
  - registration category: for every athlete in scope, the first of all their participations
    (every championship) sorted by `Category.specificityComparator` then
    `RegistrationPreferenceComparator`; `null` if none. Use `setCategory` (not
    `computeCategory`, which overwrites the presumed weight), then `computeMainRankings()`.
    `Category.specificityComparator` reads `ageGroup.getCategories().size()`, which still
    counts categories marked for deletion until step 7. The medal-order tie-breaker also reads
    that count indirectly through `getMedalingSortCode` and `AgeGroup.scoreCodeCore`. Add a
    registration-preference variant that uses final counts for both stages, preserving the
    existing ordering otherwise. Its count lookup covers all age groups of the athlete's
    remaining participations, including unchanged championships outside the reassignment scope;
    override the edited age group's count with its kept-and-added count. Leave the existing
    comparators unchanged for other callers.
- Convert preview results to an immutable `ReassignmentPreview` before closing the transaction:
  athlete ids and names, championship names, gains/losses and reason enums. Copy every collection;
  no managed entities, lazy collections or executable plan escape to the UI. Apply always
  computes a new plan and applies it within the same fresh transaction.
- No `removeBrokenParticipationsAndCategories`, no `resetCodeMap`, no nested transactions.

### 2.4 Tests — new `ChampionshipReassignmentTest`

Fixture: `testDatabases/multiagegroups_owlcms.mv.db` or `mixedTestsJRSR.mv.db`, loaded as in
`ChampionshipDeleteTest`. Add athletes programmatically where a case is missing. Cover the §10
groups **Championship scope**, **No recruiting**, **QT**, **Missing data**, **Team flags**,
**Registration category**, and the plan/apply consistency part of **Dry run**. Include an athlete
linked only to a deleted category: they remain in scope, but that category is never a target.
Read the immutable preview after its transaction closes and verify Apply does not reuse it.
Cover a cold championship cache and a missing legacy championship row: preview/plan/apply do
not bootstrap or publish data through global getters, and preview rollback leaves no new row.
Cover registration-preference ties involving deleted categories and an unchanged championship.

## Phase 3: Definition saves (domain)

New `app.owlcms.data.agegroup.AgeGroupDefinitionService`. Edits and deletes of an existing row
take `(Long draftStamp, AuditOrigin origin, …)` and run through `AgeGroupRepository.write`.
Creation and multi-row changes have their own single outer transaction under the same lock
(3.3); they do not nest calls to `write`.

### 3.1 Change classification — `AgeGroupChange.classify(AgeGroup stored, AgeGroup scratch)`

Returns `NONE`, `DIRECT` (code/name, already-gendered, medals), `REASSIGN` (age range, gender,
active, any QT, added or removed upper limit), `CHAMPIONSHIP_ONLY`, or `CHAMPIONSHIP_MIXED`
(championship plus anything else). Compare categories by upper limit (`getMaximumWeight`),
never by id or code. Duplicate upper limits in the scratchpad → `IllegalArgumentException`
(the UI validates first).

### 3.2 `save(draftStamp, origin, AgeGroup scratch, Decision decision)`

`Decision` = `APPLY` or `LATER`. Run preflight checks before confirmation; inside `write`,
repeat stale/session checks and reclassify against the fresh stored row. The domain service,
not just the UI, enforces the classification:

- `NONE`: accepted save through `write`; mutate no definition fields or assignments, but
  advance the stamp and commit. Older drafts then become stale, as for any accepted save (§3.8).
- `DIRECT`: steps 3 and 5 only (copy fields, recompute category codes), then stamp. No snapshot,
  no reassignment, regardless of `Decision`.
- `CHAMPIONSHIP_ONLY`: the shared `EntityManager` helper also used by `moveToChampionship` (3.3):
  materialize the destination if needed, change the name, stamp. Every participation and
  registration is preserved, regardless of `Decision`.
- `CHAMPIONSHIP_MIXED`: refuse without writing.
- `REASSIGN`: follow the steps below; run reassignment only with `APPLY`. Refuse `LATER` when
  categories are removed.

Implement §11 steps 1–7 inside one `write`; step 8 runs only after it has committed:

1. Validate and classify as above before mutating any entity.
2. For `REASSIGN`, snapshot the championship's athletes (2.1), including athletes linked only
   to categories being removed. Do not reload their presumed weights after changing definitions.
3. Copy fields: code, gender, minAge, maxAge, active, championshipName, medals,
   alreadyGendered. **Not** scoring. Create the championship row here if it does not exist yet
   (a legacy age group may never have been materialized; the editor no longer does it on open).
   Move the logic of `ChampionshipRepository.materializeForAgeGroup` into an `em` variant;
   the current one opens its own transaction and resets the championship cache.
4. Match by upper limit: copy QT; create missing ones and attach them with `stored.addCategory`;
   mark the others for deletion without removing them yet. Build the final category list from
   kept and added categories only.
5. Recompute `minimumWeight` from that final list's sorted upper limits, never from the list
   containing marked deletions. Synchronize child category gender with the stored age group
   and recompute codes (`getComputedCode`).
6. Only `REASSIGN` with `APPLY` → `ChampionshipReassigner.plan` in `WITHIN` mode using the step-2
   snapshots and final target map, then `apply` with the same `EntityManager`. `LATER` and
   non-reassigning classifications never call the engine.
7. Assert that no participation or registration category points to any marked category, then
   call `stored.removeCategory(category)` for each, triggering `orphanRemoval` on the managed
   collection. `getCategories()` returns a copy: removing from its result does not persist.
8. After commit, in this order: `Competition.getCurrent().setRankingsInvalid(true)`,
   `Championship.reset()` if a championship row was created, `Championship.recomputeParticipantCounts()`,
   `BestAthleteRankingService.recomputeAll(em)` inside its own `SingletonSaves.inTransaction`
   (the no-argument `recomputeAll()` uses `JPAService.runInTransaction`, which swallows
   failures), then `RankingConfig.updateMustCompute()`. Run each step even if an earlier one
   failed, and collect the failures into the result.

**Post-commit outcome.** A transaction failure propagates and leaves the scratchpad available
for a fresh attempt. Once commit succeeds, return a committed result with any refresh failure
reported separately; never present it as a rolled-back save or replay the definition change.
Recovery retries only the step-8 refreshes against committed data. This outcome contract also
applies to create, delete, championship changes and both global actions.

Also: `ReassignmentPreview preview(AgeGroup scratch)` checks the stamp/session first, then
captures snapshots, builds proposed definitions and computes the plan in a dedicated
rollback-only transaction. Do not call `apply`, stamp rows or invoke post-commit helpers.
Build the immutable summary before closing the transaction; never return an executable plan.
The preview commits nothing. The accepted save repeats the computation from fresh data.

### 3.3 Other writers

- `create(AgeGroup scratch, AuditOrigin origin)` → explicit ADD path, not `write` on a missing
  id. Use the existing `SingletonSaves.LOCK` and one exception-propagating transaction: apply
  the chosen session rule, validate definitions (including duplicate code/gender and upper
  limits), construct a new stored age group and categories by copying allowed values,
  materialize its championship with the same `EntityManager`, stamp and persist. Never merge
  the scratchpad. Preserve existing ADD assignment behavior; creation does not rebuild athlete
  participations. After commit use the outcome/refresh handling in 3.2. Cancel or failure
  leaves no new rows, including championships.
  Route `AgeGroupEditingFormFactory.add` through this method, replacing its separate
  materialization and repository-add transactions.
- Active toggle: no separate service method. The page builds a `loadForEdit` copy with only
  `active` changed and goes through `preview` and `save` like the editor (5.3).
- `delete(draftStamp, origin, ageGroupId)` → existing `AgeGroupRepository.delete(em, ag)` logic
  through `write` (a stale or missing row is refused); drop the trailing
  `removeBrokenParticipationsAndCategories()`. The repaired registration category uses the
  same preference as 2.3, not `specificityComparator` alone as `remainingRegistrationCategory`
  does today.
- `moveToChampionship(origin, Map<Long, Long> stampsById, name)` → body of the current
  `AgeGroupRepository.moveToChampionship`, one transaction under the existing save lock, the
  chosen session rule first, every row freshly checked and stamped; never one nested `write`
  per row. Preflight all draft stamps before confirmation. Use the shared `EntityManager`
  helper for destination materialization and movement. `SelectAgeGroupsDialog` captures the
  stamps of the listed age groups when it opens.
- `ChampionshipRepository.rename` is also an age-group writer: propagate `AuditOrigin` through
  `Championship.rename` and its UI caller (`ChampionshipDetailsForm`). Under the same save lock
  and in one transaction, read affected age groups fresh, change the championship name and stamp
  each changed age group. No session check: §3.7 does not list renaming, and participations do
  not change. Publish the championship cache change only after commit. Open drafts of those
  age groups must become stale; drafts of unrelated age groups remain current.
- `reassignWithin(origin)` and `recomputeAll(origin)` → Phase 2 over all championships, one
  transaction under the existing save lock, the chosen session rule first, no stamping. Capture
  snapshots and build the final target and mixed-team policy maps in that transaction, then
  plan and apply. Matching `preview…` methods return immutable summaries only; Apply never
  accepts the earlier preview as input.
- **Definition reload exception:** the three `AgeGroupRepository.reloadDefinitions(...)`
  overloads retain their special replacement workflow, including
  `resetParticipations(false, true)`. They are exempt from the chosen session rule and the
  single-transaction guarantee; do not route them through the ordinary save/global-action flow.
  Newly created rows still receive fresh stamps (1.1), and drafts of replaced rows are refused.
  This exception does not apply to editor saves, ADD, Active toggles, age-group deletes,
  championship moves or either global action.
- JSON database imports (`CompetitionData`, `CompetitionDataV2`) stamp the rows they create
  (1.1). `ChampionshipRepository.deleteWithAssociatedAgeGroups` needs no stamp (rows are deleted).

### 3.4 Tests — new `AgeGroupDefinitionSaveTest`

All §10 bullets under **Data preservation**, **Classification**, **Category identity and
remapping**, **Scratchpad and copy-back**, **Later**, **Atomicity** (inject a failure with a
test-only hook, e.g. a package-private `Runnable afterStep` set by the test), **During the
competition** (put a FOP in session-in-progress state the way
`owlcms/src/test/java/app/owlcms/init/LiftingSessionStatusTest.java` does), **Stale-save check
per age group**, **Championship change**.
The scratchpad is built as the editor will build it: detached copies of the age group and
categories, categories with **new** ids and empty participation lists. This is the 40 → 0
regression.

Add focused cases for the reviewed contracts: `NONE` advances the stamp without changing
definitions, category ids, participations, flags or registration categories;
`DIRECT` and `CHAMPIONSHIP_ONLY` with `APPLY` never reassign; deleted categories are absent from
final lower-limit/preference calculations and disappear after commit; ADD creates all rows
atomically and Cancel creates none; a pre-upgrade row with a null stamp can be saved once;
championship rename invalidates affected drafts only; a refresh failure after commit reports
a committed save, and refresh recovery never repeats participation changes or advances stamps.

## Phase 4: QT at registration (§2)

- `NAthleteRegistrationFormFactory`: the `v3a` validator checks only `categoryField`. Extend to
  every selected eligible category; keep "Ignore errors" as the override. List QT-failing
  categories in the eligible checkbox group, marked (label suffix via a translated key).
- Extract one non-UI import eligibility/finalization helper covering explicit category lists
  (`RAthlete.processEligibilityAndTeams`) and shorthand/inferred categories
  (`RAthlete.findByAgeBW` / `setCategoryHeuristics`). `doFindEligibleCategories` intentionally
  ignores QT so the athlete form can offer exceptions: do not change that candidate query to
  filter QT globally. Instead the import helper checks the candidates' QTs, skips each
  category failing only QT, warns with athlete/category details and imports the rest of the
  line. Other validation failures still throw. Team and mixed-team sets contain surviving
  categories only. Surface translated warnings through the existing import error collector.
- Preserve an explicit registration category when it survives. If QT removes it, choose the
  preferred surviving category using the shared registration preference (or none), and report
  that final choice in the warning. Shorthand imports choose the preferred surviving candidate.
  Carry the resolved registration category through to `NRegistrationFileProcessor`'s final
  persistence step, using the existing import state holder as appropriate. Its current
  `eligibles.stream().findFirst()` assignment must not overwrite that resolved choice. Do not
  reorder every explicit input list merely to force a preferred first category: a surviving
  explicit registration choice must stay intact.
- Tests: extend `NRegistrationFileProcessorCategoryAssignmentTest`; for the form, extract the
  check into a non-UI helper and test it. Cover explicit and shorthand QT failures, all
  candidates failing QT, a surviving explicit registration choice, and a skipped first
  category whose preferred survivor is not first in the remaining input. Assert final stored
  registration categories and team flags, not just `RAthlete`'s intermediate values.

## Phase 5: UI

In scope for this implementation. Load skill `vaadin-skills` first.

### 5.1 Editor (`AgeGroupEditingFormFactory`, `CategoryGridField`)

- On edit open: `OwlcmsCrudGrid.showForm` passes the selected grid item to `buildNewForm`, and
  `performOperationAndCallback` writes the binder into that same object. Override the update
  path of the age-group grid so the form is shown with a `loadForEdit` copy rather than the
  grid item. The copy is fetched in a worker; only installing it and building the form run in
  a short `ui.access`. The grid item is never mutated, so a cancelled or failed edit leaves
  the grid showing stored values.
  On ADD open: build a new detached scratchpad; do not look up its generated id as a stored row.
  Capture `UI`, locale and `AuditOrigin.fromUi(...)` on the UI thread.
  Remove `ChampionshipRepository.materializeIfRequired` from `add`, `buildNewForm` and `update`
  (no writes before acceptance). The "edit championship details" button may still materialize,
  since it is an explicit user action.
- Before confirmation and on each retry, run `assertCurrent` for existing drafts and the
  session preflight in a worker. A stale draft cannot proceed to gap checks or a preview.
  These checks do not replace the authoritative checks inside the write transaction.
- `CategoryGridField.react`: refuse a duplicate upper limit with a translated message.
  Fix the false comment in `generateModelValue`.
- Override `confirmUpdate` (hook already in `OwlcmsCrudFormFactory`) so the CRUD form reports
  success only after commit. The override does not run the `update` runnable: it starts a
  worker for the flow below and calls `completed` exactly once, in a short `ui.access` on the
  captured `UI`, and only for a committed result. Keep the existing `saveCallBack` /
  `refreshGrid` completion: after the worker has finished the save, its quick grid query is
  acceptable under the UI lock. Do not run a save, preview, bulk reassignment or multi-step
  refresh inside `ui.access`. `update()` is then no longer reached for this form. Dispatch on
  `AgeGroupChange.classify`:
  - `NONE` → `save` (stamp only), close.
  - `DIRECT` → `save`, close.
  - `CHAMPIONSHIP_ONLY` → confirmation (age group, both championships, athletes moving, age-gap
    warnings for both) → `save` → close.
  - `CHAMPIONSHIP_MIXED` → refusal dialog, nothing written, editor stays open.
  - `REASSIGN` → age-gap dialog (Continue/Cancel) → if participants, `preview` then
    Apply/Later/Cancel dialog listing leavers with reasons (Later disabled when categories are
    removed) → `save`. Highlight "Reassign within championships" after Later.
- All DB work in a background executor; results applied in a short `ui.access`.
- Route ADD through `create`; provide narrowly scoped deferred ADD/DELETE completion handling
  as needed, since `confirmUpdate` covers UPDATE only. A completed ADD/DELETE likewise reports
  through `ui.access` and lets the normal grid callback issue its quick refresh query. Other
  CRUD forms keep their behavior.
- `StaleSingletonException`, `SessionsInProgressException` and other pre-commit failures close
  the confirmation dialog and leave the editor's scratchpad available. Stale drafts must reload.
- A committed save with a refresh failure completes/closes the editor as saved and shows a
  separate translated warning. Do not offer Apply again on that consumed draft. Recovery
  retries only post-commit refresh work, not the save (3.2).

### 5.2 Age-gap check

Domain helper `AgeGapCheck.gaps(championship, proposed age groups)` → per gender, missing age
ranges over active age groups. Unit-test it (§10 **Age gap**).

### 5.3 Page (`AgeGroupContent`)

- Grid rows carry their age group's stamp. Keep the normal grid callback/`findAll` refresh:
  the short query needed to repaint the grid after a completed action may run under
  `ui.access`. Saves, previews, definition loads and global actions remain worker work; only
  their final UI completion enters `ui.access`.
- Active checkbox: show stored value; do not mutate the grid item. In a worker, `loadForEdit`
  the row, check that its stamp matches the grid row's, set `active` on that copy and run the
  5.1 `REASSIGN` flow with it; revert the checkbox on Cancel/failure.
- Delete: age-group-specific confirmation text (`AgeGroup.Delete.Warning`); single dialog.
- Replace "Reassign athletes" and "Keep Main Registration Category" with
  "Reassign within championships" and "Recompute all eligibilities", each preview → list →
  Apply/Cancel.
- Load predefined / upload: keep the special definition-reload behavior and exemptions in 3.3;
  the new rows are stamped (1.1).

### 5.4 Translations

One TSV `shared/src/main/resources/i18n/agegroup_reassignment_translations.tsv` with the §8
keys plus: leave reasons (6), QT-exception marker, championship-change confirmation and refusal,
two global-action labels/warnings, gain/lose dry-run texts, session-in-progress refusal
(`{0}` = list of "session (platform)"), duplicate upper limit, and the committed-save/refresh-failed
warning. Include import-warning keys for a QT-skipped category (athlete, category, entry total,
required QT), a replacement registration category (athlete, skipped and replacement categories),
and no remaining registration category. Reuse existing keys only where their meaning matches;
all warnings shown through the import collector are translated, not just the checkbox marker.

## Phase 6: Legacy removal and documentation

- Remove `AgeGroup.forceSave`/`setForceSave`/`getForceSave`, `AssignedAthletesException`,
  `AgeGroupRepository.save`'s reassignment branch and `cleanUp`, `requiresAthleteReassignment`,
  `AgeGroup.reassignmentHashCode`/`Category.reassignmentHashCode` if unused.
- `AthleteRepository.resetParticipations`: retain the special definition-reload callers (3.3)
  and remaining callers outside the ordinary age-group actions (`DemoData`, `BenchmarkData`,
  `RegistrationContent:1172`). Audit those other callers and replace with `recomputeAll` where
  it is a user action; do not migrate definition reloads as part of this work. Keep
  `removeBrokenParticipationsAndCategories` at startup (`Main`) and import only.
- Update `docs/030-PreCompetitionSetup/040-DefineAgeGroupsAndCategories.md` (§"Reassigning the
  athletes") and `docs/100-AdvancedTopics/040-AgeGroupDefinitions.md`.
- Release note (skill `update-release-notes`).

## Phase 7: Browser acceptance

Delegated to the `Visual Scenario` agent. Skill `open-browser-1920x1080`. Run the §10
**Browser checks** on a running server, plus one end-to-end QT edit on an age group with
participants: ids and team flags preserved, leavers listed, Later then "Reassign within
championships". Record results in this file.

## Parked ideas

- Age group page as a tree grid: championships as parent rows in their configured order, their age groups
  beneath, and an "inactive" group holding the age groups that are not in use (an age group belongs to
  exactly one championship). It would make the current sort (championship order, gender, minimum age,
  maximum age) visible instead of implicit. Not started; to be decided after Phase 7.

## Interim option

If only the data loss must ship first (spec §11 "Interim delivery"): Phase 1.1 is not needed;
do 3.1 (by upper limit) and step 4/5 of 3.2 inside the existing `AgeGroupRepository.save`,
copying QTs onto stored categories and creating/removing by upper limit, with the existing
confirmation dialog. Ask the maintainer before choosing this path.
