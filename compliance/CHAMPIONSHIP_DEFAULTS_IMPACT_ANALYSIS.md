# Championship Defaults — Refactoring Specification

Date: 2026-04-07

## Goal

Ensure that every runtime code path resolves scoring, medal-mode, and team settings through `Championship`, never through `Competition` directly.

A `DefaultChampionship` sentinel backed by `Competition.getCurrent()` eliminates null checks: every code path always has a non-null Championship to ask.

## Design Decisions

1. **DefaultChampionship sentinel** — a subclass of `Championship` (`DefaultChampionship extends Championship`) whose overlapping getters delegate to `Competition.getCurrent()`. Not an `@Entity`. Not persisted. Singleton. Fits all `Championship`-typed parameters, collections, and return types without interface extraction.
2. **`Championship.of(name)`** returns the stored championship if `name` is non-null and found, otherwise returns `DefaultChampionship.getInstance()`.
3. **FieldOfPlay resolves championships from its age groups.** `FieldOfPlay.getActiveChampionships()` iterates `ageGroupMap`, calls `AgeGroup.getChampionship()` on each, collects a `Set<Championship>`. Falls back to `{DefaultChampionship}` when the set is empty.
4. **Aggregate rule for display.** A static helper `Championship.anyMultiMedal(Set<Championship>)` returns true if any championship in the set has `isSnatchCJTotalMedals() == true`. This drives scoreboard column visibility and FieldOfPlay sort-order during live lifting.
5. **Ceremony-time uses the specific championship.** `ResultsMedals.doCeremony()` receives a `Category` whose `AgeGroup.getChampionship()` gives the exact championship. That championship's `isSnatchCJTotalMedals()` decides which medals are highlighted.
6. **`Competition` keeps all public getters.** No getters are removed. The `"competition"` JXLS bean stays. User spreadsheet templates continue to work. The refactoring only changes which paths *Java source code* takes.
7. **`isSinclair()` is derived from the scoring system, not stored — and renamed.** `Competition.isSinclair()` is a legacy manual flag (UI checkbox is commented out; only active via `SinclairMeet` feature switch). Conceptually it means "this championship awards medals by global score, not by body-weight category total." That information already exists in the age group data: `AgeGroup.getScoringSystem().isMedalScore()` returns true for all global score systems (Sinclair, GAMX, QPoints, Robi, etc.). For stored championships, derive it: `getScoringSystem() != null && getScoringSystem().isMedalScore()`. For `DefaultChampionship`, delegate to `Competition.getCurrent().isScoreMedalChampionship()`. **Rename throughout Java code:** `isSinclair()` → `isScoreMedalChampionship()`. On `Competition`, keep a deprecated alias for JXLS backward compatibility:
   ```java
   // Competition.java — new name
   public boolean isScoreMedalChampionship() {
       return this.sinclairMeet || Config.getCurrent().featureSwitch("SinclairMeet");
   }
   // Deprecated alias — preserves ${competition.sinclair} bean path for user templates
   @Deprecated
   public boolean isSinclair() { return isScoreMedalChampionship(); }
   ```
   The `sinclairMeet` HTTP parameter sent to public results and the `SinclairMeet` feature-switch key are wire/config strings — rename them separately or leave as-is.
8. **Required UI toggle.** Add a checkbox labeled `Use default competition settings` to `ChampionshipDetailsDialog` and back it with a `useCompetitionDefaults` boolean on `Championship`.
    - **Default for newly created championships:** `true`
    - **Default for pre-existing championships during migration:** `false`, to preserve current snapshot behavior and avoid silently changing existing competitions
    - **When true:** championship getters delegate to `Competition.getCurrent()` for overlapping settings; championship-specific input fields are disabled or read-only and display the effective competition-backed values
    - **When false:** stored championship values are used and the fields become editable
    - **Transition from true to false:** copy the current effective competition values into the championship fields once, then allow editing
    Runtime consumers never check this flag directly — it is internal to Championship getter resolution and the details-dialog UI state.

## Current State

`Championship` stores its own copies of medal, scoring, and team settings. `populateScoringDefaults()` copies values once from `Competition` at creation time (snapshot model). Several runtime paths bypass Championship and read `Competition` directly.

`FieldOfPlay` has **zero** references to `Championship`. It does have `ageGroupMap` (`LinkedHashMap<String, Participation>`) populated from `AgeGroupRepository.findAgeGroups(getGroup())`. This map contains the age groups for the current session but is never resolved to championships.

## Competition Methods That Overlap With Championship

These `Competition` getters must no longer be called directly by championship-aware Java code. They remain public for JXLS backward compatibility.

### Stored settings

- `getScoringSystem()`
- `isSnatchCJTotalMedals()`
- `getTeamPoints1st()`, `getTeamPoints2nd()`, `getTeamPoints3rd()`
- `getMensBestN()`, `getWomensBestN()`, `getMixedBestN()`, `getMixedBestNElseDefault()`
- `getMaxTeamSize()`, `getMaxPerCategory()`

### Derived

- `isSinclair()` — legacy name, renamed to `isScoreMedalChampionship()`. Semantic shortcut for scoring mode; derived through Championship. Deprecated alias kept on `Competition` for JXLS bean compatibility.

## Championship-Only Settings (No Change Needed)

These already live exclusively on Championship and are not provided by Competition in business logic:

- `getBestAthleteScoringSystem()`, `getBestSnatchScoringSystem()`, `getBestCJScoringSystem()`
- `getMixedMensBestN()`, `getMixedWomensBestN()`
- `getTeamScoringSystem()`, `getMixedTeamScoringSystem()`
- `isExplicitMixedTeamMembers()`, `isMixedTeamEnabled()`

## Two Usage Patterns

Every bypass site uses one of two patterns. An implementing agent must apply the correct one.

### Pattern A — Aggregate (live display / live sort)

Used when the UI must make a single decision across all active championships on a FOP.

**Rule:** `Championship.anyMultiMedal(fop.getActiveChampionships())`

**Where it applies:**

| File | Methods affected |
|------|-----------------|
| `BaseResults.java` | `setDisplay()` — sets `showLiftRanks`, `showTotalRank` properties |
| `BaseResults.java` | `computedScore()`, `computedScoreRank()` — read `isScoreMedalChampionship()`, `getScoringSystem()`, `isDisplayScores()`, `isDisplayScoreRanks()` |
| `EventForwarder.java` | Column visibility block — sets `showLiftRanks`, `showTotalRank`, `showSinclair`, `showSinclairRank` |
| `WebSocketEventForwarder.java` | Same column visibility block as EventForwarder |
| `FieldOfPlay.java` | `computeResultOrderRanking()` — returns single `Ranking` enum for FOP sort order |
| `FieldOfPlay.java` | `medalistLeaders()` — filters leader list by snatch/CJ rank when multi-medal |
| `FieldOfPlay.java` | `previousGroupLeaders()` — same filtering for previous groups |

**Constraint:** `computeResultOrderRanking()` is inherently single-mode — the FOP can only sort one way at a time. The aggregate decision is the correct input. Per-championship sort is only possible in `ResultsMultiRanks` (which uses per-age-group rank columns and is excluded from this refactoring).

### Pattern B — Specific championship (ceremony / document / team)

Used when the code operates on a single known category, age group, or championship.

**Resolution path:** `Category.getAgeGroup().getChampionship()` → specific Championship instance.

**Where it applies:**

| File | Methods affected | How to resolve |
|------|-----------------|----------------|
| `ResultsMedals.java` | `doCeremony()`, `doMedals()`, `doMedalsDisplay()` — set `showLiftRanks`, `noLiftRanks` | Ceremony event carries `Category` → `AgeGroup.getChampionship()` |
| `ResultsMedals.java` | `getAthletesJson()`, `isMedalist()` — filter by snatch/CJ/total rank | Same category-resolved championship |
| `JXLSMedalsSheet.java` | `computeSortedAthletes()` — medal export | Already scoped to a category; resolve championship from it |
| `JXLSWinningSheet.java` | Falls back to `Competition.getScoringSystem()` | Resolve from championship parameter (already passed in call chain) |
| `JXLSCompetitionBook.java` | Reads `Competition.getScoringSystem()` | Resolve from championship parameter |
| `SessionResultsContent.java` | Falls back to `Competition.getScoringSystem()` | Resolve from selected championship |
| `PackageContent.java` | Falls back to `Competition.getScoringSystem()` in several paths | Resolve from selected championship |

### Pattern C — Already has championship (team logic)

These files already receive a Championship parameter but fall back to `Competition` when it is null. Replace the null fallback with `DefaultChampionship`.

| File | What to change |
|------|---------------|
| `TeamResultsTreeData.java` | Remove `Competition` fallback; use passed championship (never null after sentinel) |
| `TeamSelectionTreeData.java` | Remove fallback to `Competition.getMaxPerCategory()` |
| `TeamSelectionContent.java` | Remove fallback to `Competition.getMaxTeamSize()` |
| `TeamResultsContent.java` | Remove `Competition.getScoringSystem()` null-fallback |
| `TeamTreeItem.java` | Remove `Competition.getScoringSystem()` and `Competition.isSnatchCJTotalMedals()` reads |
| `Team.java` | Remove `Competition.getScoringSystem()` read |

### Leave as-is (backward compatibility)

| File | Why |
|------|-----|
| `Athlete.getCombinedPoints()` | Calls `Competition.getCurrent().isSnatchCJTotalMedals()`. Exposed to JXLS templates as `${athlete.combinedPoints}`. Leave unchanged for backward compat. |
| `JXLSWorkbookStreamSource.setReportingInfo()` | Puts `Competition.getCurrent()` into the JXLS bean map as `"competition"`. Must stay — user templates depend on it. |
| `ResultsMultiRanks.java` | Uses per-age-group rank columns and filters by `AgeGroup.getMedals()`. Does not read `Competition.isSnatchCJTotalMedals()`. Immune to this refactoring. Do not modify. |

### Athlete / ranking / exporter (evaluate individually)

| File | Current bypass | Resolution |
|------|---------------|------------|
| `Athlete.java` | Falls back to `Competition.getScoringSystem()` in some ranking methods | Evaluate each call site: if reachable from a championship-aware context, pass championship. If reachable only from JXLS bean paths, leave for backward compat. |
| `AthleteSorter.java` | Reads `Competition.getScoringSystem()`, `getTeamPoints1st/2nd/3rd()` | Callers should pass championship; sorter should accept it as a parameter. |
| `AthleteExporter.java` | Reads `Competition.getScoringSystem()` | Resolve from FOP's active championships (aggregate). |

## FieldOfPlay Wiring (New Code)

FieldOfPlay currently has no Championship awareness. Add:

```java
// New method on FieldOfPlay
public Set<Championship> getActiveChampionships() {
    if (this.ageGroupMap == null || this.ageGroupMap.isEmpty()) {
        return Set.of(DefaultChampionship.getInstance());
    }
    Set<Championship> result = new LinkedHashSet<>();
    for (String agCode : this.ageGroupMap.keySet()) {
        AgeGroup ag = AgeGroupRepository.findByName(agCode);
        if (ag != null) {
            result.add(ag.getChampionship());
        }
    }
    return result.isEmpty() ? Set.of(DefaultChampionship.getInstance()) : result;
}
```

The `AgeGroup.getChampionship()` method already exists (returns `Championship.of(this.computeChampionshipName())`). The `Championship.of(name)` method must be updated to return `DefaultChampionship.getInstance()` when name is null or not found.

## Aggregate Helper (New Code)

```java
// New static method on Championship
public static boolean anyMultiMedal(Set<Championship> championships) {
    return championships.stream().anyMatch(Championship::isSnatchCJTotalMedals);
}
```

## DefaultChampionship Sentinel (New Class)

`DefaultChampionship extends Championship`. No `@Entity` annotation. Not JPA-managed. Singleton.

```java
public class DefaultChampionship extends Championship {
    private static final DefaultChampionship INSTANCE = new DefaultChampionship();

    public static DefaultChampionship getInstance() { return INSTANCE; }

    private DefaultChampionship() { }

    @Override public Ranking getScoringSystem() {
        return Competition.getCurrent().getScoringSystem();
    }
    @Override public boolean isSnatchCJTotalMedals() {
        return Competition.getCurrent().isSnatchCJTotalMedals();
    }
    @Override public Integer getTeamPoints1st() {
        return Competition.getCurrent().getTeamPoints1st();
    }
    @Override public Integer getTeamPoints2nd() {
        return Competition.getCurrent().getTeamPoints2nd();
    }
    @Override public Integer getTeamPoints3rd() {
        return Competition.getCurrent().getTeamPoints3rd();
    }
    @Override public Integer getMensBestN() {
        return Competition.getCurrent().getMensBestN();
    }
    @Override public Integer getWomensBestN() {
        return Competition.getCurrent().getWomensBestN();
    }
    @Override public Integer getMixedBestN() {
        return Competition.getCurrent().getMixedBestN();
    }
    @Override public Integer getMaxTeamSize() {
        return Competition.getCurrent().getMaxTeamSize();
    }
    @Override public Integer getMaxPerCategory() {
        return Competition.getCurrent().getMaxPerCategory();
    }
    @Override public boolean isScoreMedalChampionship() {
        // Preserve legacy feature-switch path for competitions without stored championships
        return Competition.getCurrent().isScoreMedalChampionship();
    }
}
```

For stored championships, add to `Championship` base class:

```java
// Derived — no stored boolean needed
public boolean isScoreMedalChampionship() {
    return this.getScoringSystem() != null && this.getScoringSystem().isMedalScore();
}
```

`DefaultChampionship.isScoreMedalChampionship()` overrides this to delegate to `Competition.getCurrent().isScoreMedalChampionship()`, preserving the `SinclairMeet` feature-switch path for competitions that don't use stored championship data.

Not persisted. Not JPA-managed. No `@Entity` on `DefaultChampionship`. Place in the same package as `Championship`. JPA will not scan it because it lacks `@Entity` and is never added to a persistence context.

## Scoreboard Column Visibility

### Live scoreboards (BaseResults, EventForwarder, WebSocketEventForwarder)

Replace:
```java
// BEFORE
setShowLiftRanks(Competition.getCurrent().isSnatchCJTotalMedals()
    && !Competition.getCurrent().isSinclair());
```
With:
```java
// AFTER
Set<Championship> active = fop.getActiveChampionships();
boolean multiMedal = Championship.anyMultiMedal(active);
boolean scoreMedal = active.stream().anyMatch(Championship::isScoreMedalChampionship);
setShowLiftRanks(multiMedal && !scoreMedal);
```

All four display-init sites (`BaseResults.setDisplay()`, `EventForwarder`, `WebSocketEventForwarder`, and `BaseResults.computedScore()`/`computedScoreRank()`) must use the same aggregate computation.

### Medal scoreboard (ResultsMedals)

During ceremony: use the specific championship from the ceremony category.

```java
// BEFORE
this.getElement().setProperty("showLiftRanks",
    Competition.getCurrent().isSnatchCJTotalMedals());

// AFTER
Championship ch = ceremonyCategory.getAgeGroup().getChampionship();
this.getElement().setProperty("showLiftRanks", ch.isSnatchCJTotalMedals());
```

During non-ceremony medals display: use the aggregate from `fop.getActiveChampionships()`.

### Medal highlight constraint (preserve existing behavior)

`ResultsMedals` already forces athlete-row highlight and attempt-request highlight to empty during medal display. This prevents live-scoreboard highlighting from leaking into the medals view when lift-rank columns are visible. Preserve this.

### ResultsMultiRanks (do not modify)

`ResultsMultiRanks` renders per-age-group rank columns. It reads `AgeGroup.getMedals()` to filter which columns appear. It does NOT read `Competition.isSnatchCJTotalMedals()`. It is immune to this refactoring and must not be changed.

## FieldOfPlay Sort Order

`computeResultOrderRanking()` returns a single `Ranking` enum that drives the display sort for the entire FOP. This is inherently single-mode — the FOP cannot sort differently for different championships at the same time.

Replace:
```java
// BEFORE
boolean _3medals = Competition.getCurrent().isSnatchCJTotalMedals();

// AFTER
boolean _3medals = Championship.anyMultiMedal(getActiveChampionships());
```

Per-championship sort is only available through `ResultsMultiRanks`, which computes ranks per age group independently.

## JXLS Backward Compatibility

### Bean context — no change

`JXLSWorkbookStreamSource.setReportingInfo()` (line 651) puts `Competition.getCurrent()` into the JXLS context as `"competition"`. User templates reference bean paths like `${competition.scoringSystem}`, `${competition.snatchCJTotalMedals}`, etc. This must not change.

Optionally, a `"championship"` bean can be added alongside for new templates:
```java
getReportingBeans().put("competition", competition);       // kept
getReportingBeans().put("championship", resolvedChampionship); // new, optional
```

### Hidden bean-path dependency

`Athlete.getCombinedPoints()` (line 1760) calls `Competition.getCurrent().isSnatchCJTotalMedals()` internally. It is exposed to templates via `${athlete.combinedPoints}`. Leave this unchanged — it returns the competition-level default, which is the correct behavior for templates that don't distinguish championships.

## Implementation Order

### Phase 1 — Foundation (no behavioral change)

1. Create `DefaultChampionship` class. Override all overlapping getters to delegate to `Competition.getCurrent()`.
2. Update `Championship.of(name)` to return `DefaultChampionship.getInstance()` when name is null or not found.
3. Add `Championship.anyMultiMedal(Set<Championship>)` static helper.
4. **Add `Championship.isScoreMedalChampionship()` derived method** (compute from `getScoringSystem().isMedalScore()`).
5. Add `FieldOfPlay.getActiveChampionships()` — resolve from `ageGroupMap` via `AgeGroup.getChampionship()`.

**Test:** All existing tests pass. No behavior changes yet.

### Phase 2 — Aggregate sites (Pattern A)

6. Update `BaseResults.setDisplay()` — replace `Competition.getCurrent()` reads with `fop.getActiveChampionships()` aggregate.
7. Update `BaseResults.computedScore()` and `computedScoreRank()` — same aggregate.
8. Update `EventForwarder` column visibility block.
9. Update `WebSocketEventForwarder` column visibility block.
10. Update `FieldOfPlay.computeResultOrderRanking()`.
11. Update `FieldOfPlay.medalistLeaders()`.
12. Update `FieldOfPlay.previousGroupLeaders()`.

**Test:** Run with multi-championship sessions. Column visibility and leader selection should reflect the aggregate.

### Phase 3 — Specific-championship sites (Pattern B)

13. Update `ResultsMedals.doCeremony()`, `doMedals()`, `doMedalsDisplay()` — resolve championship from ceremony category.
14. Update `ResultsMedals.getAthletesJson()` and `isMedalist()`.
15. Update `JXLSMedalsSheet.computeSortedAthletes()`.
16. Update JXLS result sheets (`JXLSWinningSheet`, `JXLSCompetitionBook`).
17. Update result content classes (`SessionResultsContent`, `PackageContent`).

### Phase 4 — Team logic (Pattern C)

18. Update `TeamResultsTreeData` — remove Competition fallback.
19. Update `TeamSelectionTreeData` — remove fallback to `Competition.getMaxPerCategory()`.
20. Update `TeamSelectionContent` — remove fallback to `Competition.getMaxTeamSize()`.
21. Update `TeamResultsContent`, `TeamTreeItem`, `Team` — remove Competition reads.

### Phase 5 — Remaining and UI

22. Evaluate `Athlete.java` Competition reads: leave bean-path-exposed methods, update championship-aware internal methods.
23. Update `AthleteSorter.java` — accept Championship parameter from callers.
24. Update `AthleteExporter.java` — resolve from FOP aggregate.
25. Add `useCompetitionDefaults` boolean to `Championship` entity.
26. Migration rule: existing championships default `useCompetitionDefaults = false`; newly created championships default `useCompetitionDefaults = true`.
27. Wire `useCompetitionDefaults` into `ChampionshipDetailsDialog` with checkbox label `Use default competition settings`.
28. When the checkbox is checked, show effective competition-backed values and disable editing of overlapping settings.
29. When the checkbox is unchecked, copy the current effective values into the championship fields once, then enable editing.