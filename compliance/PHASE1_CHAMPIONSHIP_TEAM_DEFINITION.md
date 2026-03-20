## Phase 1: Championship and Team Definition — Detailed Plan

### Context

Phase 0 (complete) established Championship as a persisted JPA entity with `name` and `type`.
ChampionshipType now has `_MIXED` variants (`U_MIXED`, `MASTERS_MIXED`, `IWF_MIXED`)
and predicates (`isMasters()`, `isU()`, `isIWF()`, `isMixed()`).
`Championship.isMixed()` delegates to `getType().isMixed()`.

Phase 1 wires the mixed-team concept into the data model, configuration surfaces,
and convenience accessors *before* touching athlete assignment (Phase 2) or results (Phase 3).


### Design Decisions

- **No companion/pairing concept.** Each championship is independent with its own type.
  A "Masters" championship (type `MASTERS`) and a "Masters Mixed" championship
  (type `MASTERS_MIXED`) are separate championships. They share no structural link.
  `isMasters()` returns true for both; `isMixed()` distinguishes them.

- **Type is freely assignable.** Any championship can be set to any `ChampionshipType`.
  The UI just needs a type picker. No toggling, no derived types.

- **`AgeGroup.isMixedTeams()`** is simply `getChampionshipType().isMixed()`.
  An age group belongs to one championship. If that championship's type is a
  `_MIXED` variant, the age group supports mixed team membership.


### Step 1: Add `AgeGroup.isMixedTeams()`

**File:** `AgeGroup.java`

```java
public boolean isMixedTeams() {
    ChampionshipType ct = getChampionshipType();
    return ct != null && ct.isMixed();
}
```

This is the query point that Phase 2 (Participation model) and Phase 4 (UI) will use
to decide whether `mixedTeamMember` is relevant for the age group's participations.


### Step 2: Add mixed team scoring configuration to `Competition.java`

**File:** `Competition.java`

Add a `mixedBestN` field parallel to `mensBestN` / `womensBestN`:

```java
@Column
@JsonProperty(value = "mixedTeamSize", index = 48)
private Integer mixedBestN;
```

Add getter/setter and `getMixedBestNElseDefault()` following the existing pattern
(falls back to `maxTeamSize` if null).

This stores how many mixed team members count toward team scoring.
Default behavior: if null, fall back to `maxTeamSize` (same pattern as mens/womens).


### Step 3: Add mixed team scoring configuration to the Competition editing UI

**File:** `CompetitionEditingFormFactory.java`

Find where `mensBestN` / `womensBestN` fields are bound to the form and add a
parallel `mixedBestN` field in the same section. Follow the existing form-binding pattern.


### Step 4: Update `EditChampionshipsDialog` to support type selection

**File:** `EditChampionshipsDialog.java`

Currently:
- New championships are always created as `ChampionshipType.U`
- No UI to set or change the type

Phase 1 changes:
- Add a `ComboBox<ChampionshipType>` to the "Add" row so the user can choose
  the type when creating a new championship.
- For each existing championship row, add a `ComboBox<ChampionshipType>` to
  view and change the type. Changing the type on an existing championship is
  just `championship.setType(newType)` + persist.
- No constraints on type selection — any championship can be any type.


### Step 5: Verify JSON round-trip for `mixedBestN`

The `@JsonProperty` annotation on `mixedBestN` (Step 2) handles export automatically.
On import, Jackson will set the field if present in JSON, otherwise null (backward-compatible).
No additional code needed beyond the annotation.


### Files Modified

| File | Change |
|------|--------|
| `AgeGroup.java` | Add `isMixedTeams()` |
| `Competition.java` | Add `mixedBestN` field, getter, setter, default method |
| `CompetitionEditingFormFactory.java` | Add `mixedBestN` UI field |
| `EditChampionshipsDialog.java` | Add type picker (ComboBox) for create and edit |


### What Phase 1 Does NOT Do

- **Does not add `mixedTeamMember` to Participation** — that's Phase 2.
- **Does not change the team membership UI** — that's Phase 4.
- **Does not change team scoring/rollup** — that's Phase 3.
- **Does not change results views** — that's Phase 5.
- **Does not change SBDE import/export** — that's Phase 6.

Phase 1 establishes the infrastructure so that later phases can ask:
"does this age group support mixed teams?" (`AgeGroup.isMixedTeams()`)
and "how many mixed team results count?" (`Competition.getMixedBestNElseDefault()`).


### Commit Batches (Proposed)

**Batch 1: Mixed teams query and scoring config**
- `AgeGroup.java`: `isMixedTeams()`
- `Competition.java`: `mixedBestN` field + accessors
- `CompetitionEditingFormFactory.java`: UI field binding

**Batch 2: Championship type editing**
- `EditChampionshipsDialog.java`: type ComboBox for create and edit
