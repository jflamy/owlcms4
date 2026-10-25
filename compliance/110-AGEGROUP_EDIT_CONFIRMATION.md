# PLAN — Age Group editing: confirmation dialogs and safe category save

Status: revised after implementation review and maintainer feedback. No application code changed.
Snippets are proposed contracts, not independently compilable patches. Section 8 records deferred
decisions; do not implement those by silently choosing defaults.
Repo: `owlcms_68` (Vaadin Flow 25, Hibernate JPA, H2). Follow `.github/copilot-instructions.md`
(no `mvn`/git writes without consent, no fully-qualified class names, no hard-coded UI strings).

---

## 1. Problem statement (current behavior)

Editing an age group (`AgeGroupContent` → `AgeGroupEditingFormFactory.update()` →
`AgeGroupRepository.save()`) has these defects:

| # | Defect | Where |
|---|--------|-------|
| D1 | The editor's `CategoryGridField` copies every category with `new Category(c)`, which runs `Category()` and assigns a **fresh time-based id**. `cleanUp()` matches old↔new by id, so every existing category looks obsolete and is deleted along with all its `Participation` rows — even for a QT-only change. | `CategoryGridField.setPresentationValue` L109, `CategoryListField.setPresentationValue` L84, `Category(Category)` L146, `AgeGroupRepository.cleanUp` |
| D2 | The single confirmation dialog only offers "Confirm" (destroy participations) or "Cancel". There is no "save and leave athletes as they are" path. | `AgeGroupEditingFormFactory.update` L370-390 |
| D3 | `AgeGroup.setForceSave(boolean b)` ignores its argument and always sets `true`. The `forceSave == false` branch in `save()` is dead. | `AgeGroup` L540 |
| D4 | `ChampionshipRepository.materializeIfRequired(ageGroup)` runs **before** the confirmation, so Cancel can leave a newly materialized `Championship` row behind. | `AgeGroupEditingFormFactory.update` L361 |
| D5 | `requiresAthleteReassignment()` compares `reassignmentHashCode()` which includes `code`; renames look like eligibility changes. It also cannot distinguish boundary-only from eligibility changes. | `AgeGroupRepository` L523-560, `Category.reassignmentHashCode` L545 |
| D6 | The existing check only looks at athletes **already** in the age group; athletes who become newly eligible (e.g. QT lowered, age range widened) are never considered. | `AgeGroupRepository.save` L490 |
| D7 | `CategoryRepository.doFindEligibleCategories(..., int ignoredEntryTotal)` ignores the qualifying total, so "Reset Athletes" does not enforce QT. Only `migrateEligibleCategories` applies `requiresEntryTotalConfirmation`. | `CategoryRepository` L119 |
| D8 | Active checkbox in the grid saves directly and swallows `AssignedAthletesException`; deactivating an age group with participants gets no confirmation. | `AgeGroupContent.createGrid` L329-336 |

Hibernate constraint that shapes the fix: `Category.participations` is
`@OneToMany(mappedBy="category", cascade=ALL, orphanRemoval=true)` and is initialized to an
empty list. **Merging an editor copy (empty participations) would orphan-delete the real
participations.** The save path must therefore copy scalar fields onto managed entities and
never `em.merge()` the editor graph.

---

## 2. Agreed rules

### 2.1 Change classification (per save)

Compute by comparing the editor's `AgeGroup` + draft categories against the managed `AgeGroup`:

| Class | Triggers | Confirmation | Effect on athletes |
|-------|----------|--------------|--------------------|
| **NONE** | code, `alreadyGendered`, championship association, medals, scoring, `wrSr/wrJr/wrYth` | none | none; refresh derived category codes only |
| **BOUNDARY** | `minimumWeight`/`maximumWeight` of any category changed; category **added with QT = 0**; category **deleted** (see DELETION) | 2-button | redistribute **existing participants of this age group** among its categories by body weight; may change inferred registration category; **do not** enforce QT |
| **ELIGIBILITY** | any category QT changed; category **added with QT > 0**; `minAge`/`maxAge`; `gender`; `active` toggled | 3-button | full eligibility check over **all athletes**, restricted to **this age group's categories**; QT enforced with entry total; other age groups' participations untouched |
| **DELETION** | one or more existing categories removed | 3-button, **"I will reassign later" disabled** | participants of removed categories must be re-evaluated (BOUNDARY or ELIGIBILITY reconciliation runs); athletes whose registration category was removed get a new inferred one or `null` |

Combined changes are **all-or-nothing**: the highest class wins (NONE < BOUNDARY < ELIGIBILITY);
DELETION forces the 3-button dialog with "later" disabled.

Deleting the whole age group: mandatory cleanup (existing `delete()`) plus registration
reassignment for athletes whose registration category was in that age group — confirm first.
Reprocess affected athletes automatically using the existing registration-preference rules.
Assign the preferred eligible replacement; clear registration if there is no eligible candidate.
This is application work during the transaction, not a request for manual reassignment.

### 2.2 Dialog texts (English; other languages via TSV, §7)

**BOUNDARY (2 buttons)**
- Title: `AgeGroup.BoundaryChange.Title` = "Body weight boundaries have changed"
- Text: `AgeGroup.BoundaryChange.Warning` = "Body weight boundaries have changed. The athletes currently in this age group will be moved to the matching categories if needed. This may change their inferred registration category."
- Buttons: `Confirm` (existing key) / `Cancel` (existing key)

**ELIGIBILITY / DELETION (3 buttons)**
- Title: `AgeGroup.EligibilityChange.Title` = "Athlete eligibility may change"
- Text: `AgeGroup.EligibilityChange.Warning` = "This change may make some athletes gain or lose eligibility to this age group. This may also affect their inferred registration category. You may need to adjust after reassignment."
- Extra line when DELETION: `AgeGroup.EligibilityChange.DeletionNote` = "Categories are being removed; athletes must be reassigned now."
- Buttons: `AgeGroup.ReassignNow` = "Reassign Athletes Now" / `AgeGroup.ReassignLater` = "I will reassign later" / `Cancel`

**Age group deletion**
- Title: `AgeGroup.Delete.Title` = "Delete age group"
- Text: `AgeGroup.Delete.Warning` = "Athletes will be removed from this age group. If their registration category belongs to this age group, a new eligible registration category will be assigned automatically. If none is available, their registration category will be cleared."
- Buttons: `Confirm` / `Cancel`

Successful updates normally close the editor. Run the success/close callback only after an
accepted save succeeds, not when a confirmation is opened. Cancel performs no save; whether
confirmation cancellation returns to the editor or closes it remains deferred in section 8.

The unlimited-weight category is a real category, never removed by weight-category editing.
Preserve its existing identity and maximum 999. Removing all finite boundaries leaves that same
category covering 0 to 999. Whole-age-group deletion is a separate confirmed operation.

"Participants of a deleted category" means participants BEFORE deletion, not dangling rows
afterward. For example, an athlete registered in a junior group may also participate in a
senior finite-weight category being removed. Querying senior participants after deleting that
participation misses the athlete, even though the unlimited category remains.
Raising minimum age from 12 to 13 is different: the category remains, but a 12-year-old loses
eligibility on reassignment. Both cases require pre-edit athlete information.

---

## 3. Design

### 3.1 Unambiguous identity: non-entity edit values

Supersedes the transient `originalCategoryId` proposal. A nullable field cannot distinguish a
loaded entity from a new draft; falling back to the source ID also mislabels a copied new draft.
Use a separate value with an explicit origin:

```java
sealed interface CategoryOrigin permits ExistingCategory, NewCategory {}

record ExistingCategory(long categoryId) implements CategoryOrigin {}
record NewCategory(UUID draftKey) implements CategoryOrigin {}

record CategoryValues(Double minimumWeight, Double maximumWeight, int qualifyingTotal,
        Integer wrYth, Integer wrJr, Integer wrSr) {}

record CategoryEdit(CategoryOrigin origin, CategoryValues values) {}
```

Validate non-null origin/values/draftKey. The repository snapshot constructs existing origins
from verified persisted rows belonging to the age group. The plus button creates NewCategory
with a UUID. Copies and field updates preserve origins unchanged, including a new draft's UUID.
Only a successful save followed by a fresh snapshot produces an existing origin for that row.

Never infer persistence from generated entity IDs, null, code, position or weight boundary.
Keep `Category(Category)`, entity equality and JSON export unchanged. No `originalCategoryId`
or `isNewDraft()` on the entity. The edit graph is not a JPA entity graph or an export DTO.

Adapt both category fields to edit values and bind the form to an age-group edit model, not
`AgeGroup.setCategories`. Audit their callers and provide explicit boundary adapters where
necessary. New-age-group creation must also preserve new origins across presentation refreshes.

Preserve the unlimited category's existing origin while recalculating its lower boundary.
Validate contiguous, non-overlapping ranges ending at 999 and reject deletion or replacement
of the existing unlimited row. New groups initialize their own unlimited category.
There is no maximum-999 exclusion in `AgeGroup.getCategories()`.

### 3.2 Change classification — new class `AgeGroupChange`

Load an immutable baseline before editing: age-group ID, persisted editable scalar values,
category IDs/values, and relevant definition dependencies. Enumerate exact fields from the binder
during implementation. Freeze proposed values before preview; use defensive collection copies.
Preview returns values and IDs only, never entities or lazy collections.

```java
enum ChangeKind { NONE, BOUNDARY, ELIGIBILITY }
enum SaveChoice { SAVE_ONLY, REASSIGN_NOW, REASSIGN_LATER }

record AgeGroupChange(ChangeKind kind, Set<Long> removedCategoryIds) {
    AgeGroupChange {
        removedCategoryIds = Set.copyOf(removedCategoryIds);
    }

    boolean deletesCategories() { return !removedCategoryIds.isEmpty(); }
    boolean requiresConfirmation() { return kind != ChangeKind.NONE; }
}
```

Classification follows section 2. Compare structured values, not hash codes or entity equality.
Deleted IDs are baseline IDs omitted by the edit; additions are explicit NewCategory origins.
Finite deletion is at least BOUNDARY; ELIGIBILITY wins for combined changes.

Reject unknown, foreign-owner, duplicate or disappeared existing IDs. Require each existing
origin in BOTH the baseline and current group's rows. Reject duplicate new draft keys and a
missing age group. Never treat a stale ID as a request to insert or resurrect a category.

At commit, reload and compare the baseline BEFORE any mutation. Reject changed definitions with
a translated stale-edit error and require reload/review/reconfirmation; do not silently upgrade
the classification and execute an action the user did not approve.
Protect comparison and writes using a database version/locking protocol, not a Java lock or
an unlocked second read. Inspect existing version fields and writers to choose the concrete
protocol. Every competing definition writer must participate, including category inserts/deletes.
Hold no database locks while the user considers a dialog. Re-read athlete state at commit.

Validate choice in the repository: SAVE_ONLY only for NONE; REASSIGN_LATER only for ELIGIBILITY
without deletions; BOUNDARY/deletion requires REASSIGN_NOW. The repository derives reconciliation
mode from the verified change. A disabled UI button is not enforcement.

### 3.3 Repository — replace `save()`'s reassignment branch

One transaction owns validation, materialization, definition changes, participation reconciliation
and registration repair. These are proposed helper contracts, not existing APIs:

```java
public static EditResult applyEdit(AgeGroupEdit edit, SaveChoice choice) {
    EditResult result = JPAService.runInTransaction(em -> {
        AgeGroup managed = loadAndProtectDefinition(em, edit.ageGroupId());
        validateBaselineAndOrigins(em, managed, edit);
        validateDuplicateCodeGender(em, managed, edit);
        AgeGroupChange change = classify(managed, edit);
        validateChoice(change, choice);
        AthleteSnapshot before = captureAffectedAthletes(em, managed, change);
        applyDefinitionScalars(em, managed, edit);
        materializeChampionshipInTransaction(em, managed);
        reconcileAndRemoveCategories(em, managed, edit, change, choice, before);
        em.flush();
        return resultSnapshot(managed, change, choice);
    });
    invalidateAfterCommit(result);
    return result;
}
```

Resolve category IDs to managed entities in THIS persistence context. Copy scalars without
replacing participation collections. Persist new entities only for NewCategory origins. Refresh
derived codes even on NONE edits. Never merge the editor graph or remove detached preview entities.

Materialization requires an EntityManager-accepting helper; the current wrapper starts its own
transaction and resets caches. Do not call it from this operation. The same applies to athlete
queries, cleanup and participant-count writes: helpers must share the transaction.

Repair registration references and both sides of participation relationships before deleting
categories or any intermediate flush. Existing cascade helpers flush internally and need review.
A failure must roll back championship creation, definitions, participations and registration
changes together. Whole-age-group deletion and Active changes have the same atomicity requirement.

Remove the unconditional global `removeBrokenParticipationsAndCategories()` from this path;
it could mutate unrelated groups. Persisted counts belong in the transaction; cached counts,
ranking/championship cache invalidation and UI refresh follow commit. Distinguish post-commit
refresh failure from save failure so a committed operation is not blindly retried.

Audit legacy `save(AgeGroup)` and import/loader callers before removing `forceSave`,
`AssignedAthletesException`, `cleanUp` or old classification methods. Preserve compatible
non-editor behavior without leaving a concurrent definition writer outside the locking protocol.

### 3.4 Reconciliation algorithms

Both execute inside the save transaction using a pre-mutation snapshot.
Capture athlete IDs, old participation IDs/category IDs, registration category ID/ownership,
actual/effective bodyweight, age inputs and BOTH team membership flags before changing definitions.
Presumed weight can derive from the old registration category; computing it after boundary
updates or registration clearing can change the answer. Preserve missing inputs for an explicit policy.

BOUNDARY scope: all pre-edit participants in the age group plus athletes registered there,
including participants linked only to a finite category being removed. Do not query this scope
after deletion. Redistribute within resulting weight intervals, including unlimited, without
QT enforcement. Do not recruit athletes from other groups.

ELIGIBILITY scope: all athletes; check resulting age limits, existing gender-inclusive matching
rules, active status, effective bodyweight and entry total. Reuse the interval convention
`minimumWeight < bodyWeight && bodyWeight <= maximumWeight` and enforce
`!category.requiresEntryTotalConfirmation(athlete.getEntryTotal())`. Check the actual Gender.I
predicate rather than inventing a new rule. Existing selection that ignores QT is insufficient.

Only mutate participation rows owned by the edited group. Preserve unchanged rows/metadata;
do not delete/recreate everything. Match numeric IDs, not entity equality or computed codes.
Other groups' participation rows remain unchanged.

#### Team membership: replicate the interactive registration logic

This applies to reassignment caused by age-group editing and its deferred per-group/global
actions. Do not change normal interactive athlete registration as part of this fix.
Reference `NAthleteRegistrationFormFactory.refreshTeamMembershipFields()` and
`refreshTeamMembershipField()`: retain selected teams that are still among the possible teams,
for both ordinary and mixed-team membership. The form binds these separately through
`Athlete.setAgeGroupTeams()` and `setMixedAgeGroupTeams()` after eligibility changes.

For each athlete, snapshot two sets of selected age-group IDs from the pre-mutation
participations. This is the headless equivalent of the form's selected values; there are no
unsaved checkbox values to prefer over the persisted snapshot. Use stable IDs, not the form's
display-name/code strings, so renaming an age group cannot lose its team selection.

```java
record TeamSelections(Set<Long> ordinary, Set<Long> mixed) {
        TeamSelections {
                ordinary = Set.copyOf(ordinary);
                mixed = Set.copyOf(mixed);
        }
}

record TeamFlags(boolean teamMember, boolean mixedTeamMember) {}

static TeamFlags membershipAfterReassignment(TeamSelections before,
                long ageGroupId, boolean mixedTeamsAvailable) {
        return new TeamFlags(before.ordinary().contains(ageGroupId),
                        mixedTeamsAvailable && before.mixed().contains(ageGroupId));
}
```

Populate `ordinary` with age-group IDs from participation rows whose teamMember is true;
populate `mixed` likewise from mixedTeamMember. Build a separate snapshot per athlete, never
shared selections across athletes. Multiple true selections in one age group collapse to one
set entry, just as the form's team selection does.

After deriving final participation targets, the possible ordinary teams are their age groups;
possible mixed teams are only those groups where `AgeGroup.isMixedTeams()` is true. For each
target participation in the operation's scope, apply the membership function above. This is
equivalent to intersecting old selections with the possible teams before applying the setters.
Do not invoke those whole-athlete setters in a scoped operation: apply flags only to in-scope rows.

- Same category/age group: keep the existing participation row; no membership write if unchanged.
- Different category, same age group: create the replacement with the retained selections,
    explicitly supplying BOTH flags to `addEligibleCategory(category, teamMember, mixedTeamMember)`.
- Newly eligible age group with no prior participation: neither set contains its ID, so both
    flags are false. Do not inherit the entity's teamMember=true constructor default.
- Age group no longer eligible: remove the participation; never transfer its selection to a
    different age group merely because registration moves there.
- Mixed teams unavailable: mixedTeamMember is false, matching the form's possible-team filter.
    Exact mixed-flag preservation applies when mixed-team availability is unchanged and enabled.

Unchanged valid rows retain their flags; the mixed-availability check is the explicit exception
to blindly preserving an obsolete selection. Registration preference is independent of team
selection. Do not use the participation copy constructor to copy computed ranks into a new row;
copy these membership flags and let the normal ranking invalidation/recalculation handle ranks.

This resolves membership transfer and new-participant defaults by reusing existing interactive
semantics, rather than introducing a new team-selection policy. Do not copy the form's
championship migration filters into this algorithm: edit scope and eligibility remain as above.

REASSIGN_LATER saves definitions while retaining all participation and registration references;
it is invalid for deletions. Section 8.B records the approved per-group/global follow-up actions.

Registration preference is already defined, not a new product decision. Reuse the ordering in
`CategoryRepository.doFindEligibleCategories()`: `RegistrationPreferenceComparator` (based on
`getMedalingSortCode()`), followed by the stable `Category.specificityComparator` sort. The latter
has precedence; the earlier order is retained for specificity ties. `Athlete.bestMatch()` and
the registration form select the first candidate, or null for an empty candidate list.
Reuse or extract that exact ordering/selection over the operation's eligible candidate list;
do not invent a new "most specific" heuristic or use only one comparator. Keep candidate
eligibility, including this plan's QT rules, separate from preference ordering. Reusing selection
must not invoke a global participation reset during a scoped operation.

For deletion, automatically apply this selection to affected registrations, excluding categories
being removed, and repair references before deletion. No candidate means null registration.
Add parity tests against established registration ordering, including specificity ties and an
empty candidate list. Team membership follows the interactive semantics above. When to reconsider
a still-valid registration and missing-input handling remain deferred in section 8; preference
order and membership defaults do not.

### 3.5 UI — `AgeGroupEditingFormFactory.update()`

```text
validated immutable edit -> read-only preview
  NONE                    -> submit SAVE_ONLY
  BOUNDARY, no deletion   -> Confirm / Cancel
  ELIGIBILITY or deletion -> Now / Later / Cancel (Later disabled for deletion)
accepted choice           -> atomic applyEdit -> success callback closes editor
cancel / failure          -> no success callback
```

Successful updates close the editor normally. Add an explicit pending/success/cancel/failure
completion path for age-group editing using a narrow form-factory hook or override; preserve
other CRUD forms' behavior. `OwlcmsCrudFormFactory.performOperationAndCallback()` currently
announces success and invokes the grid callback immediately after `update()` returns. The
callback hides the editor. Opening a confirmation and returning does NOT defer completion.

Invoke success exactly once AFTER commit; close and refresh at that point. Do not infer success
from returning the edited object. Exact Cancel presentation and asynchronous/busy/error behavior
are deferred in section 8, not assumed by the diagram.

Remove pre-confirmation AND form-open championship materialization. Loading the form/preview
must be read-only; combo-box preparation cannot justify implicit writes. Required creation
belongs in the accepted transaction, including new-age-group saving where applicable.
Do not mutate live grid entities merely by binding or previewing drafts.

After Later, support either per-group or deferred global reassignment. Do not direct users to
the UNCHANGED legacy Reset Athletes implementation: it ignores QT and resets membership.
Section 8.B specifies the approved replacement workflow and its remaining indicator decision.

### 3.6 UI — `ThreeWayConfirmationDialog` (new component)

Use `ConfirmationDialog` for boundary-only decisions. Add a focused three-choice component or
a compatible extension exposing explicit decisions rather than persistence callbacks that
blindly close immediately after starting background work:

```java
interface AgeGroupDecisionHandler {
    void reassignNow();
    void reassignLater();
    void cancel();
}
```

Use section 2.2's translated labels; display the deletion note and disable Later for deletions.
Keep text and buttons responsive; do not copy the fixed 550px paragraph/non-wrapping layout
without checking mobile widths and long translations. Focus, busy and error behavior follow
the final lifecycle decision. Load applicable Vaadin skills/API documentation before coding.

### 3.7 UI — Active checkbox in the grid (`AgeGroupContent.createGrid`)

Create a proposed Active edit without mutating the grid's persisted snapshot. Always classify
as ELIGIBILITY, including activation of an empty group: athletes can become eligible.
Use the same decision flow, baseline/choice validation and atomic reconciliation as the form.
Prevent programmatic restoration from recursively opening another confirmation. The checkbox's
appearance while pending and restoration after cancellation/failure remain deferred in section 8.

### 3.8 UI — Age group deletion (`AgeGroupEditingFormFactory.delete`)

`OwlcmsCrudFormFactory.buildConfirmDialog()` already confirms deletion. Customize that existing
confirmation for age groups instead of opening a second dialog inside `delete()`.
On acceptance, reload/validate managed state, capture affected athletes, repair registration
references and remove participations/categories in one transaction. Invoke success only after
commit. Do not commit deletion first and repair registrations in a later transaction.
Automatically select replacement registrations using the existing rules in section 3.4;
clear registration only when no eligible replacement exists.

---

## 4. File-by-file change list

| File | Change |
|------|--------|
| New non-entity edit/baseline/origin types | explicit existing/new identity, immutable proposed values and ID-only classification |
| `data/category/Category.java` | no edit-lineage fields or serialization changes required |
| `components/fields/CategoryGridField.java` | edit values, stable origins and preserved unlimited row |
| `components/fields/CategoryListField.java` | same; audit caller adapters |
| `data/agegroup/AgeGroupChange.java` (new) | classification (§3.2) |
| `data/agegroup/AgeGroupRepository.java` | preview, stale-edit protection, atomic edit/Active/deletion, scoped reconciliation and managed scalar copying |
| `data/agegroup/ChampionshipRepository.java` | transaction-local materialization helper without early cache resets |
| `data/agegroup/AgeGroup.java` | remove `forceSave` + `getForceSave/setForceSave` (or fix setter if kept for import) |
| `data/agegroup/AssignedAthletesException.java` | delete when no thrower remains |
| `components/ThreeWayConfirmationDialog.java` (new) | §3.6 |
| `nui/preparation/AgeGroupEditingFormFactory.java` | edit-model binder, deferred completion, existing delete confirmation, remove early materialization |
| `nui/crudui/OwlcmsCrudFormFactory.java` | narrow completion hook if needed; preserve unrelated CRUD behavior |
| `nui/preparation/AgeGroupContent.java` | Active checkbox §3.7 |
| `shared/src/main/resources/i18n/agegroup_confirmation_translations.tsv` (new, temporary) | §7 |
| `src/main/markdown/ReleaseNotes.md` | entry via `update-release-notes` skill |
| tests (§6) | new |

---

## 5. Implementation order

Resolve section 8 where needed before implementing its behavior. Keep partial work off active
save paths until the corresponding transactional and completion behavior is ready.

1. Add edit/baseline/origin types and pure classification/validation tests.
2. Establish stale-edit protection across relevant writers; implement transaction-local helpers,
    managed scalar copying, pre-edit snapshots and atomic cleanup.
3. Implement scoped reconciliation once remaining reassignment-trigger/input policies are settled; verify rollback
    and cross-group isolation before UI wiring.
4. Adapt both category fields and the age-group binder; preserve the unlimited origin and audit callers.
5. Wire completion, two/three-choice dialogs, successful closure, Active and existing delete confirmation.
6. Implement the selected Later workflow and final Cancel/background/error behavior.
7. Retire legacy APIs only after auditing import/loader and other callers.
8. Deliver translation TSV, regression coverage and release note using the relevant skills.

Use `verify-java-fix` and focused checks after Java edits; ask before Maven/build execution.
Continuing test consent covers focused reruns. Check actual structural changes/runtime before
asserting any DCEVM restart requirement; this plan no longer adds a transient entity field.

---

## 6. Tests (no Vaadin objects — see `add-test-case` skill)

- Classification: all section 2 changes, safe renames, finite deletions and mixed-change precedence.
- Existing origins and NewCategory UUIDs survive repeated copies and presentation round trips;
    no generated entity ID becomes a false existing origin. Edit state stays out of exports.
- Unknown/foreign/duplicate IDs, duplicate draft keys, changed baseline, removed category/group
    are rejected; competing-transaction tests verify no silent overwrite or resurrection.
- Removing all finite boundaries preserves the unlimited row ID with range 0 to 999.
- NONE and Later preserve participation IDs and registration references; QT edits retain existing
    category IDs. The server rejects Later with deletions, regardless of UI state.
- Reassignment includes athletes linked only to a removed finite category, including athletes
    registered elsewhere. Presumed weight uses pre-edit category values.
- QT uses entry total; other groups' participation rows and metadata remain unchanged. Verify
    replacement selection matches established registration preference, including specificity ties
    and no eligible candidate. Add remaining policy tests after section 8 is resolved.
- Team-selection parity tests use non-UI value fixtures: each of the four flag combinations
    survives a weight-category move when mixed teams are enabled; unchanged rows retain identity;
    renaming the age group does not lose selections; newly eligible groups receive false/false.
    Check mixed-team unavailability clears only the in-scope mixed selection, loss of eligibility
    removes its participation, and registration moving to another group does not transfer flags.
    Other athletes' and out-of-scope groups' selections remain unchanged. Global reconciliation
    applies the same per-athlete rules without replacing all participation rows.
- Inject failures after materialization, category changes and participation changes; all writes
    roll back, including whole-age-group deletion and Active edits.
- Preview/form loading writes nothing, including championship creation. Browser checks cover
    confirmation, successful closure, cancel/failure, no premature success, no double delete
    confirmation and no recursive Active dialog, using the final section 8 behavior.
- Later never invokes the unchanged legacy reset. Verify explicit per-group and global follow-up
    actions, including several age groups edited before one global reassignment.
- Global reassignment enforces QT, follows the resolved metadata policies, and repairs registrations
    from final participation state. Results are independent of age-group iteration order. Failure
    partway through rolls back the entire batch; global scope requires explicit confirmation.
- Check mobile/long-translation dialog layout and exactly-once completion.

---

## 7. Translations (TSV, never edit `translation4.csv`)

Header must be the first line of `translation4.csv` converted to tabs:

```
key	en	en_US	en_CA	es	es_419	es_ES	es_SV	es_EC	fr	fr_CA	fr_FR	pt	ru	de	sv	da	fi	no	fo	hu	ro	pl	hy	he	ar	el	ja	zh_HANT	et	ia
```

Keys to add (English given; fill fr/es/de/etc., fall back to English where unknown):

| key | en |
|-----|----|
| `AgeGroup.BoundaryChange.Title` | Body weight boundaries have changed |
| `AgeGroup.BoundaryChange.Warning` | Body weight boundaries have changed. The athletes currently in this age group will be moved to the matching categories if needed. This may change their inferred registration category. |
| `AgeGroup.EligibilityChange.Title` | Athlete eligibility may change |
| `AgeGroup.EligibilityChange.Warning` | This change may make some athletes gain or lose eligibility to this age group. This may also affect their inferred registration category. You may need to adjust after reassignment. |
| `AgeGroup.EligibilityChange.DeletionNote` | Categories are being removed; athletes must be reassigned now. |
| `AgeGroup.ReassignNow` | Reassign Athletes Now |
| `AgeGroup.ReassignLater` | I will reassign later |
| `AgeGroup.Delete.Title` | Delete age group |
| `AgeGroup.Delete.Warning` | Athletes will be removed from this age group. If their registration category belongs to this age group, a new eligible registration category will be assigned automatically. If none is available, their registration category will be cleared. |

Existing `CategoryAssignment.Title` / `CategoryAssignment.Warning` become unused after the UI
migration; leave them in `translation4.csv` (external tool owns removal).

Also supply a translated stale-edit message and any error/Later-action keys required by the
resolved workflow. Re-read the actual CSV header at implementation time; do not assume the
language list above is still current. Do not provide fallback UI strings for missing keys.

---

## 8. Unresolved decisions for the next discussion

### A. Participation metadata and remaining reassignment behavior (review item 8)

Settled: registration preference already exists. Reuse section 3.4's existing ordering and
first-candidate selection. Deleting an age group automatically repairs affected registrations;
no eligible candidate means cleared registration. Do not ask the maintainer to redesign preference.

Team membership now follows the interactive registration logic described in section 3.4:
retain ordinary/mixed selections by athlete and age-group ID while that team remains possible;
newly eligible age groups start with neither team selected. Preserve existing rows, and pass
both flags explicitly for category moves. Normal interactive editing remains unchanged.

Still deferred:
- When eligibility is gained, preserve a valid current registration or re-infer it?
- How should missing age/bodyweight and manual registration choices affect reassignment?

For the age-12-to-13 example, any required replacement uses the same existing preference rules;
there is no separate ordering decision. Never keep a reference to a deleted row.

### B. "I will reassign later" follow-up (review item 9, workflow approved)

Approved: support a selected-age-group "Reassign Athletes" action AND an explicit global
reassignment after several deferred edits. Global scope is intentional in the latter case,
not an isolation bug. Confirm the scope clearly: one age group versus all age groups.

Both actions use the same QT-aware reconciliation rules against the CURRENT saved definitions.
They must work with unchanged definitions: classification NONE does not mean no reassignment
is needed. Standalone reassignment needs its own validated repository contract, not a bypass
of edit-choice validation. Global reassignment examines all athletes and all age groups; it is
not limited to a UI-session list of edited groups.

Replace the user-facing legacy reset path with this explicit reconciliation, rather than
calling `resetParticipations(false, true)` unchanged. That implementation ignores QT and
resets team membership. Reuse the final metadata/registration policies from section 8.A for
both scopes. Audit other reset callers before changing shared legacy behavior.

For a global operation, snapshot athlete inputs before mutation, reconcile participation
against all final definitions, then select registration categories once from the final result.
Do not execute independently committed per-group saves or recompute registration after each
group: results must not depend on iteration order. Use one transaction and invalidate caches
after commit, with the same rollback and concurrency guarantees as scoped reconciliation.

After Later, the UI may offer the corrected global action as well as the per-group action.
It must never promote the unchanged legacy reset. Reassignment remains explicit, not triggered
automatically when leaving the editor or ending a session. Category deletion still cannot be
deferred because references to removed rows must be repaired immediately.

Remaining UI decision: whether to persist/show pending-reassignment indicators, or simply keep
both actions available without tracking deferred work. Manual athlete adjustments remain possible.

### C. Cancel, asynchronous execution and failure UX (review items 4 and 10)

Settled: successful updates close normally; Cancel saves nothing; success follows commit;
form opening/preview must not write. Still to settle:
- Does confirmation Cancel return to the populated editor or discard/close the editor?
- During save, which controls/dialogs remain visible and disabled? How are failure/retry presented?
- For Active, display the proposed or original checkbox value while awaiting a decision?

Existing repository requirements still apply: blocking I/O runs outside the Vaadin UI thread;
capture UI/locale before background work; pass immutable data; keep ui.access short and non-nested.
Prevent duplicate submission; handle detached UIs and post-commit refresh failures. Concrete
integration is deferred with the UX discussion, not permission to block the UI thread.

### Implementation checks, not additional product decisions

- Inspect definition writers/versioning and implement the shared stale-edit protection protocol.
- Reuse established Gender.I and effective-bodyweight semantics rather than inventing substitutes.
- Audit import/loader and both category-field callers before removing legacy APIs.
- Use the existing delete-confirmation hook rather than adding a second confirmation.
