# Phase 3: Team Computation Pipeline — Detailed Implementation Plan

## Prerequisites

- **Phase 0** ✅ Championship is a stored JPA entity with `id`, `name`, `type`.
- **Phase 1** ✅ `AgeGroup.isMixedTeams()`, `Competition.mixedBestN`, championship type picker in UI.
- **Phase 1b** ✅ Legacy `ageDivision` fallback removed.
- **Phase 2** ✅ `Participation.mixedTeamMember` boolean added, `Athlete`/`PAthlete` mixed helpers, `ParticipationDTO` extended.

## Goal

Update the team rollup pipeline so:
1. Championship carries its own scoring configuration (not derived from Competition at runtime).
2. Championships with `_MIXED` types use explicit `mixedTeamMember` membership for the MF (mixed) team.
3. Championships without `_MIXED` continue to compute the mixed team as the union of M and F teams.
4. Scoring configuration (team points, bestN, scoring system) comes from the Championship, not from `Competition.getCurrent()`.

## Reference Documents

| Document | Location | What to read |
|----------|----------|-------------|
| Master plan | `compliance/MIXED_CHAMPIONSHIP_PLAN.md` | Steps 17–19 (Phase 3 goals), step 27 (dependencies) |
| Impact analysis | `compliance/CHAMPIONSHIP_SCORING_IMPACT_ANALYSIS.md` | Sections 1–10 (all technical impacts), Summary table, Future Consideration |
| Phase 2 plan | `compliance/PHASE2_PARTICIPATION_MODEL_EXTENSION.md` | Lines 270–286 (deferred Phase 3 items) |

## Source Files Modified

| File | Path | Changes |
|------|------|---------|
| Championship.java | `owlcms/src/main/java/app/owlcms/data/agegroup/Championship.java` | Add ~12 scoring/team fields, bootstrap logic, remove majority-vote method |
| ChampionshipRepository.java | `owlcms/src/main/java/app/owlcms/data/agegroup/ChampionshipRepository.java` | Extend `reconcileFromAgeGroups()` to populate scoring fields |
| Competition.java | `owlcms/src/main/java/app/owlcms/data/competition/Competition.java` | Pass Championship to `doTeamRankings`, mixed-aware MF bean, championship scoring in TeamBest |
| TeamResultsTreeData.java | `owlcms/src/main/java/app/owlcms/data/team/TeamResultsTreeData.java` | Championship-aware gating in `doTeamGender`, championship-aware `getTopNTeamSize` |
| AthleteSorter.java | `owlcms/src/main/java/app/owlcms/data/athleteSort/AthleteSorter.java` | Add `pointsFormula` overload with championship team points |
| Participation.java | `owlcms/src/main/java/app/owlcms/data/category/Participation.java` | Add raw (ungated) points methods |
| TeamTreeItem.java | `owlcms/src/main/java/app/owlcms/data/team/TeamTreeItem.java` | Add `isMixedTeamMember()` / `setMixedTeamMember()` methods |
| TeamSelectionTreeData.java | `owlcms/src/main/java/app/owlcms/data/team/TeamSelectionTreeData.java` | Iterate Gender.MF for `_MIXED` championships; mixed-aware sorting |
| TeamSelectionContent.java | `owlcms/src/main/java/app/owlcms/nui/preparation/TeamSelectionContent.java` | Second checkbox column for `mixedTeamMember`; MF gender filter; `toggleMixedTeamMember()` |

---

## Step 1: Add scoring fields to Championship.java

**File:** `owlcms/src/main/java/app/owlcms/data/agegroup/Championship.java`

### 1a. Add JPA field declarations

After the existing `type` field (line 226), add 12 new fields. All fields use `@Enumerated(EnumType.STRING)` for Ranking enums and `@Column` for primitives. All are non-null after bootstrap.

```java
@Enumerated(EnumType.STRING)
private Ranking scoringSystem;

@Enumerated(EnumType.STRING)
private Ranking bestAthleteScoringSystem;

@Enumerated(EnumType.STRING)
private Ranking bestSnatchScoringSystem;

@Enumerated(EnumType.STRING)
private Ranking bestCJScoringSystem;

@Column(columnDefinition = "boolean default false")
private boolean snatchCJTotalMedals = false;

private Integer teamPoints1st;
private Integer teamPoints2nd;
private Integer teamPoints3rd;

private Integer mensBestN;
private Integer womensBestN;
private Integer mixedBestN;

@Enumerated(EnumType.STRING)
private Ranking teamScoringSystem;
```

### 1b. Add standard getters and setters

Add a getter and setter for each new field. These are plain getters — no fallback to Competition:

```java
public Ranking getScoringSystem() { return this.scoringSystem; }
public void setScoringSystem(Ranking scoringSystem) { this.scoringSystem = scoringSystem; }

public Ranking getBestAthleteScoringSystem() { return this.bestAthleteScoringSystem; }
public void setBestAthleteScoringSystem(Ranking s) { this.bestAthleteScoringSystem = s; }

public Ranking getBestSnatchScoringSystem() { return this.bestSnatchScoringSystem; }
public void setBestSnatchScoringSystem(Ranking s) { this.bestSnatchScoringSystem = s; }

public Ranking getBestCJScoringSystem() { return this.bestCJScoringSystem; }
public void setBestCJScoringSystem(Ranking s) { this.bestCJScoringSystem = s; }

public boolean isSnatchCJTotalMedals() { return this.snatchCJTotalMedals; }
public void setSnatchCJTotalMedals(boolean b) { this.snatchCJTotalMedals = b; }

public Integer getTeamPoints1st() { return this.teamPoints1st; }
public void setTeamPoints1st(Integer v) { this.teamPoints1st = v; }

public Integer getTeamPoints2nd() { return this.teamPoints2nd; }
public void setTeamPoints2nd(Integer v) { this.teamPoints2nd = v; }

public Integer getTeamPoints3rd() { return this.teamPoints3rd; }
public void setTeamPoints3rd(Integer v) { this.teamPoints3rd = v; }

public Integer getMensBestN() { return this.mensBestN; }
public void setMensBestN(Integer v) { this.mensBestN = v; }

public Integer getWomensBestN() { return this.womensBestN; }
public void setWomensBestN(Integer v) { this.womensBestN = v; }

public Integer getMixedBestN() { return this.mixedBestN; }
public void setMixedBestN(Integer v) { this.mixedBestN = v; }

public Ranking getTeamScoringSystem() { return this.teamScoringSystem; }
public void setTeamScoringSystem(Ranking r) { this.teamScoringSystem = r; }
```

### 1c. Rename or replace the existing majority-vote `getBestAthleteScoringSystem(String)`

The existing method at lines 328–356 uses majority vote across age groups. It must be **removed** (or renamed to a private helper for one-time bootstrap only). The new `getBestAthleteScoringSystem()` is the plain getter from 1b.

If the old method signature `getBestAthleteScoringSystem(String ageGroupPrefix)` is called from anywhere, those callers must be updated to use the plain getter. Search for all call sites first.

### 1d. Add a `populateScoringDefaults()` instance method

This method populates scoring fields from Competition defaults + age group overrides. Called during bootstrap and when creating new championships.

```java
/**
 * Populate scoring fields from Competition defaults, then override with
 * age group values using "last one seen wins".
 */
public void populateScoringDefaults() {
    Competition comp = Competition.getCurrent();
    // Start with Competition defaults
    this.scoringSystem = comp.getScoringSystem();
    this.bestAthleteScoringSystem = comp.getScoringSystem();
    this.snatchCJTotalMedals = comp.isSnatchCJTotalMedals();
    this.teamPoints1st = comp.getTeamPoints1st();
    this.teamPoints2nd = comp.getTeamPoints2nd();
    this.teamPoints3rd = comp.getTeamPoints3rd();
    this.mensBestN = comp.getMensBestN();
    this.womensBestN = comp.getWomensBestN();
    this.mixedBestN = comp.getMixedBestNElseDefault();
    this.teamScoringSystem = Ranking.GAMX; // default

    // Override from age groups: last one seen wins
    List<AgeGroup> ageGroups = AgeGroupRepository.findFiltered(null, null, this, null, true, -1, -1);
    for (AgeGroup ag : ageGroups) {
        if (ag.getScoringSystem() != null) {
            this.scoringSystem = ag.getScoringSystem();
        }
        if (ag.getBestAthleteScoringSystem() != null) {
            this.bestAthleteScoringSystem = ag.getBestAthleteScoringSystem();
        }
    }

    // Snatch/CJ scoring defaults to best athlete for now
    this.bestSnatchScoringSystem = this.bestAthleteScoringSystem;
    this.bestCJScoringSystem = this.bestAthleteScoringSystem;
}
```

### 1e. Add a `isMixed()` convenience method if not already present

Check if `Championship` already has `isMixed()`. If not, add:

```java
public boolean isMixed() {
    return this.type != null && this.type.isMixed();
}
```

**Verification:** After Step 1, the Championship entity compiles with 12 new columns. Hibernate auto-DDL will add them to the table.

---

## Step 2: Extend bootstrap to populate scoring fields

**File:** `owlcms/src/main/java/app/owlcms/data/agegroup/ChampionshipRepository.java`

### 2a. Update `reconcileFromAgeGroups()` to populate scoring defaults

In the `reconcileFromAgeGroups()` method (lines 93–157), after creating a new Championship (line 141), call `populateScoringDefaults()` before persisting:

**Current code (line 141):**
```java
Championship c = new Championship(name, type);
em.persist(c);
```

**New code:**
```java
Championship c = new Championship(name, type);
c.populateScoringDefaults();
em.persist(c);
```

### 2b. Handle migration for existing databases

For existing championships that already have `id`, `name`, `type` but no scoring fields (all null), add a migration check. In `bootstrapFromAgeGroups()` (lines 76–88), after the `if (count > 0)` early return, add a migration path:

```java
public static void bootstrapFromAgeGroups() {
    long count = JPAService.runInTransaction(em -> {
        return (Long) em.createQuery("select count(c) from Championship c").getSingleResult();
    });
    if (count > 0) {
        // Check if scoring fields need migration (scoringSystem is null = pre-Phase-3 data)
        migrateScoringFieldsIfNeeded();
        return;
    }
    logger.info("Bootstrapping Championship table from persisted age groups");
    reconcileFromAgeGroups();
}

private static void migrateScoringFieldsIfNeeded() {
    JPAService.runInTransaction(em -> {
        TypedQuery<Championship> q = em.createQuery(
            "select c from Championship c where c.scoringSystem is null", Championship.class);
        List<Championship> needsMigration = q.getResultList();
        for (Championship c : needsMigration) {
            c.populateScoringDefaults();
            em.merge(c);
            logger.info("Migrated scoring fields for championship '{}'", c.getName());
        }
        if (!needsMigration.isEmpty()) {
            em.flush();
        }
        return null;
    });
}
```

### 2c. Update `addChampionship()` in Championship.java

When `addChampionship()` creates a new Championship (line 79–82 of Championship.java), call `populateScoringDefaults()` before saving:

**Current:**
```java
Championship newChampionship = new Championship(canonicalName, canonicalType);
newChampionship = ChampionshipRepository.save(newChampionship);
```

**New:**
```java
Championship newChampionship = new Championship(canonicalName, canonicalType);
newChampionship.populateScoringDefaults();
newChampionship = ChampionshipRepository.save(newChampionship);
```

**Verification:** After Step 2, all Championships (new and migrated) have non-null scoring fields.

---

## Step 3: Add raw (ungated) points methods to Participation.java

**File:** `owlcms/src/main/java/app/owlcms/data/category/Participation.java`

### 3a. Add four raw points methods

Add after the existing `getCombinedPoints()` method (line 165). These methods always compute from rank, regardless of `teamMember` status:

```java
/**
 * Raw total points from rank, not gated by teamMember.
 * Used for mixed team accumulation where mixedTeamMember may be true
 * but teamMember may be false.
 */
public int getRawTotalPoints() {
    return AthleteSorter.pointsFormula(this.totalRank);
}

public int getRawSnatchPoints() {
    return AthleteSorter.pointsFormula(this.snatchRank);
}

public int getRawCleanJerkPoints() {
    return AthleteSorter.pointsFormula(this.cleanJerkRank);
}

public int getRawCombinedPoints() {
    return getRawSnatchPoints() + getRawCleanJerkPoints() + getRawTotalPoints();
}
```

### 3b. Expose raw methods on PAthlete proxy

**File:** `owlcms/src/main/java/app/owlcms/data/jpa/PAthlete.java`

Add proxy methods that delegate to the participation:

```java
public int getRawTotalPoints() {
    return this._getOriginalParticipation().getRawTotalPoints();
}

public int getRawSnatchPoints() {
    return this._getOriginalParticipation().getRawSnatchPoints();
}

public int getRawCleanJerkPoints() {
    return this._getOriginalParticipation().getRawCleanJerkPoints();
}

public int getRawCombinedPoints() {
    return this._getOriginalParticipation().getRawCombinedPoints();
}
```

**Verification:** Compilation succeeds. Raw methods return points for any ranked athlete regardless of team membership.

---

## Step 4: Add `pointsFormula` overload to AthleteSorter.java

**File:** `owlcms/src/main/java/app/owlcms/data/athleteSort/AthleteSorter.java`

### 4a. Add overload with explicit team-point values

After the existing `pointsFormula(Integer rank)` method (line 457), add:

```java
/**
 * Points formula using championship-specific team point values.
 */
public static int pointsFormula(Integer rank, int firstPlacePoints, int secondPlacePoints, int thirdPlacePoints) {
    if (rank == null || rank <= 0) {
        return 0;
    }
    switch (rank) {
        case 1:
            return firstPlacePoints;
        case 2:
            return secondPlacePoints;
        case 3:
            return thirdPlacePoints;
        default:
            return Math.max(0, thirdPlacePoints - (rank - 3));
    }
}
```

**Note:** The existing `pointsFormula(Integer rank)` reads from `Competition.getCurrent()`. It is kept for backward compatibility. The new overload accepts explicit values that callers resolve from the Championship.

**Verification:** Compilation succeeds. Existing callers are unaffected.

---

## Step 5: Pass Championship through the team-ranking pipeline in Competition.java

**File:** `owlcms/src/main/java/app/owlcms/data/competition/Competition.java`

### 5a. Change `doTeamRankings` signature to accept Championship

**Current signature (line 1992):**
```java
private void doTeamRankings(List<Athlete> athletes, String suffix, boolean singleAgeGroup)
```

**New signature:**
```java
private void doTeamRankings(List<Athlete> athletes, String suffix, boolean singleAgeGroup, Championship championship)
```

### 5b. Update `doTeamRankings` body for mixed-aware MF bean

**Current MF bean construction (lines 2000–2006):**
```java
List<Athlete> sortedMen = new ArrayList<>();
List<Athlete> sortedWomen = new ArrayList<>();
splitPTeamMembersByGender(athletes, sortedMen, sortedWomen);
athletes = new ArrayList<>();
athletes.addAll(sortedMen);
athletes.addAll(sortedWomen);
```

**New MF bean construction:**
```java
List<Athlete> sortedMen = new ArrayList<>();
List<Athlete> sortedWomen = new ArrayList<>();
splitPTeamMembersByGender(athletes, sortedMen, sortedWomen);

List<Athlete> sortedMixed;
if (championship != null && championship.isMixed()) {
    // _MIXED championship: explicit mixed membership
    sortedMixed = athletes.stream()
        .filter(a -> a.isMixedTeamMember())
        .collect(Collectors.toList());
} else {
    // Non-mixed: union of men and women
    sortedMixed = new ArrayList<>();
    sortedMixed.addAll(sortedMen);
    sortedMixed.addAll(sortedWomen);
}
```

Then replace all uses of the previous `athletes` variable (which was the concatenation) with `sortedMixed` for the `mw*` beans. The `athletes` variable was reused — now use `sortedMixed` consistently for MF:

**Old lines 2009–2014:**
```java
sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.TOTAL);
...
addToReportingBean("mwTeam" + suffix, sortedAthletes);
```

**New:**
```java
List<Athlete> sortedMixedForTotal = AthleteSorter.teamPointsOrderCopy(sortedMixed, Ranking.TOTAL);
...
addToReportingBean("mwTeam" + suffix, sortedMixedForTotal);
```

Apply the same pattern for all four bean groups:
- `mwTeam` (Ranking.TOTAL)
- `mwCombined` (Ranking.SNATCH_CJ_TOTAL)
- `mwCustom` (Ranking.CUSTOM)
- `mwTeamBest` (championship scoring system)

### 5c. Use championship scoring system for TeamBest beans

**Current (line 2040):**
```java
sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Competition.getCurrent().getScoringSystem());
sortedMen = AthleteSorter.teamPointsOrderCopy(sortedMen, Competition.getCurrent().getScoringSystem());
sortedWomen = AthleteSorter.teamPointsOrderCopy(sortedWomen, Competition.getCurrent().getScoringSystem());
```

**New:**
```java
Ranking bestScoring = (championship != null && championship.getScoringSystem() != null)
    ? championship.getScoringSystem()
    : Competition.getCurrent().getScoringSystem();
sortedMixedForBest = AthleteSorter.teamPointsOrderCopy(sortedMixed, bestScoring);
sortedMen = AthleteSorter.teamPointsOrderCopy(sortedMen, bestScoring);
sortedWomen = AthleteSorter.teamPointsOrderCopy(sortedWomen, bestScoring);
```

### 5d. Update callers of `doTeamRankings`

There are exactly **2 call sites**:

**Call site 1 — `teamRankingsForAgeDivision()` line 2264:**

Current: `doTeamRankings(athletes, adName, false);`
New: `doTeamRankings(athletes, adName, false, ad);`

(`ad` is the `Championship` parameter already available in `teamRankingsForAgeDivision(Championship ad)`)

**Call site 2 — `teamRankings()` line 2245:**

Current: `doTeamRankings(athletes, ageGroupPrefix, true);`
New: `doTeamRankings(athletes, ageGroupPrefix, true, null);`

This is the non-championship path (single age group prefix). Passing `null` for championship preserves existing behavior.

### 5e. Update `teamRankingsForAgeDivision` TeamBest beans

**Current (lines 2326–2331):**
```java
sortedMen = getOrCreateBean("mTeamBest" + adName);
sortedWomen = getOrCreateBean("wTeamBest" + adName);
AthleteSorter.teamPointsOrder(sortedMen, Competition.getCurrent().getScoringSystem());
AthleteSorter.teamPointsOrder(sortedWomen, Competition.getCurrent().getScoringSystem());
```

**New:**
```java
Ranking bestScoring = (ad != null && ad.getScoringSystem() != null)
    ? ad.getScoringSystem()
    : Competition.getCurrent().getScoringSystem();
sortedMen = getOrCreateBean("mTeamBest" + adName);
sortedWomen = getOrCreateBean("wTeamBest" + adName);
AthleteSorter.teamPointsOrder(sortedMen, bestScoring);
AthleteSorter.teamPointsOrder(sortedWomen, bestScoring);
```

**Verification:** After Step 5, the Championship object flows through the entire pipeline. MF beans use explicit mixed membership for `_MIXED` championships.

---

## Step 6: Championship-aware gating in TeamResultsTreeData.java

**File:** `owlcms/src/main/java/app/owlcms/data/team/TeamResultsTreeData.java`

### 6a. Update `doTeamGender()` accumulation gating

The `ageDivision` parameter is already a `Championship` object (line 89 signature). At line 153, replace the `isTeamMember()` check:

**Current (line ~153 inside doTeamGender):**
```java
if (a.isTeamMember()) {
```

**New:**
```java
boolean contributes;
if (gender == Gender.MF && ageDivision != null && ageDivision.isMixed()) {
    contributes = a.isMixedTeamMember();
} else {
    contributes = a.isTeamMember();
}
if (contributes) {
```

### 6b. Use raw points for mixed-only athletes

Inside the accumulation block, when computing `curPoints`, use raw points for MF in `_MIXED` championships (because the athlete may not be a gendered team member):

**Current (line ~122):**
```java
Integer curPoints = combinedTotal ? a.getCombinedPoints() : a.getTotalPoints();
```

**New:**
```java
Integer curPoints;
if (gender == Gender.MF && ageDivision != null && ageDivision.isMixed()) {
    // Use raw points — athlete may be mixed-only (teamMember=false, mixedTeamMember=true)
    curPoints = combinedTotal ? a.getRawCombinedPoints() : a.getRawTotalPoints();
} else {
    curPoints = combinedTotal ? a.getCombinedPoints() : a.getTotalPoints();
}
```

**Note on `getRawCombinedPoints` / `getRawTotalPoints`:** These are called on the `Athlete` interface. Since the athletes in the beans are `PAthlete` instances, the proxy methods added in Step 3b will be invoked.

**Important:** The `Athlete` class (or its interface) must also have these raw methods. Check if `Athlete` is an interface or if `PAthlete extends Athlete`. If `Athlete` is a concrete class:
- Add abstract/default `getRawTotalPoints()` etc. returning 0, OR
- Cast to `PAthlete` inside the `_MIXED` branch, OR
- Add the raw methods to the `Athlete` class delegating to `getMainRankings().getRawTotalPoints()`.

The recommended approach: Add the methods to `Athlete.java` delegating to `getMainRankings()`:

```java
public int getRawTotalPoints() {
    Participation mr = getMainRankings();
    return mr != null ? mr.getRawTotalPoints() : 0;
}
// ... same for getRawSnatchPoints, getRawCleanJerkPoints, getRawCombinedPoints
```

### 6c. Use championship `snatchCJTotalMedals` 

**Current (line ~111):**
```java
boolean combinedTotal = Competition.getCurrent().isSnatchCJTotalMedals();
```

**New:**
```java
boolean combinedTotal;
if (ageDivision != null) {
    combinedTotal = ageDivision.isSnatchCJTotalMedals();
} else {
    combinedTotal = Competition.getCurrent().isSnatchCJTotalMedals();
}
```

### 6d. Update `getTopNTeamSize()` to read from championship

**Current (lines 228–248):**
```java
private Integer getTopNTeamSize(Gender gender) {
    Integer maxCount = null;
    Competition comp = Competition.getCurrent();
    switch (gender) {
        case M:
            maxCount = comp.getMensBestN() != null ? comp.getMensBestN() : Integer.MAX_VALUE;
            break;
        case F:
            maxCount = comp.getWomensBestN() != null ? comp.getWomensBestN() : Integer.MAX_VALUE;
            break;
        case MF:
            maxCount = comp.getMixedBestNElseDefault() != null ? comp.getMixedBestNElseDefault() : Integer.MAX_VALUE;
            break;
        case I:
            return 0;
        default:
            break;
    }
    return maxCount;
}
```

The method needs access to the Championship. Since the championship is available as the `ageDivision` field passed to `doTeamGender()`, either:
- Store `ageDivision` as a class field (set in `buildTeamItemTree`), or
- Pass it as a parameter.

**Recommended:** Store as a class field since it doesn't change during the lifetime of a `TeamResultsTreeData` instance.

Add a field:
```java
private Championship championship;
```

Set it in `init()`:
```java
this.championship = ageDivision;
```

Then update `getTopNTeamSize()`:

```java
private Integer getTopNTeamSize(Gender gender) {
    Integer maxCount = null;
    if (this.championship != null) {
        switch (gender) {
            case M:
                maxCount = this.championship.getMensBestN();
                break;
            case F:
                maxCount = this.championship.getWomensBestN();
                break;
            case MF:
                maxCount = this.championship.getMixedBestN();
                break;
            case I:
                return 0;
            default:
                break;
        }
    } else {
        // Fallback for no-championship path
        Competition comp = Competition.getCurrent();
        switch (gender) {
            case M:
                maxCount = comp.getMensBestN();
                break;
            case F:
                maxCount = comp.getWomensBestN();
                break;
            case MF:
                maxCount = comp.getMixedBestNElseDefault();
                break;
            case I:
                return 0;
            default:
                break;
        }
    }
    return maxCount != null ? maxCount : Integer.MAX_VALUE;
}
```

**Verification:** After Step 6, team accumulation correctly distinguishes _MIXED vs non-mixed memberships, uses championship-level bestN, and uses raw points for mixed-only athletes.

---

## Step 7: Verify `isMixedTeamMember()` visibility

**File:** `owlcms/src/main/java/app/owlcms/data/category/Participation.java`

The existing `isMixedTeamMember()` (line 302) is **private**. It needs to be at least package-private or public for `PAthlete` to delegate to it and for the accumulation loop to call it via `Athlete`/`PAthlete`.

Check current visibility:
```java
private boolean isMixedTeamMember() {
    return this.mixedTeamMember;
}
```

**Change to public:**
```java
public boolean isMixedTeamMember() {
    return this.mixedTeamMember;
}
```

Similarly check `isTeamMember()` — it's also private (line 298). It's called indirectly through PAthlete. If any new code needs direct access, make it public too.

Also verify that `PAthlete.isMixedTeamMember()` exists (added in Phase 2). If it delegates to the participation, the participation method must be accessible.

---

## Step 8: Search for remaining `Competition.getCurrent()` references in team paths

After Steps 5 and 6, search the codebase for remaining places where `Competition.getCurrent()` is called in team scoring context and should use championship instead:

```bash
grep -rn "Competition.getCurrent()" --include="*.java" \
  owlcms/src/main/java/app/owlcms/data/team/ \
  owlcms/src/main/java/app/owlcms/data/competition/Competition.java | \
  grep -i "team\|scoring\|points\|bestN\|best.*n"
```

Key references to check and update if they are in the team scoring path:
1. `doTeamGender()` line 111: `Competition.getCurrent().isSnatchCJTotalMedals()` → done in 6c
2. `getTopNTeamSize()` line 230: `Competition.getCurrent()` → done in 6d
3. `doTeamRankings()` line 2040: `Competition.getCurrent().getScoringSystem()` → done in 5c
4. `teamRankingsForAgeDivision()` lines 2328–2329: `Competition.getCurrent().getScoringSystem()` → done in 5e
5. `init()` line 263: `Competition.getCurrent().computeReportingInfo(...)` → this is correct (calling the Competition method that internally routes to the championship pipeline)

---

## Step 9: Add mixed team methods to TeamTreeItem.java

**File:** `owlcms/src/main/java/app/owlcms/data/team/TeamTreeItem.java`

TeamTreeItem wraps either a Team node or an Athlete node in the Vaadin tree. Currently it only has `isTeamMember()` / `setTeamMember()` (lines 228–246). For `_MIXED` championships, the team selection UI needs to read and write `mixedTeamMember` on the underlying `PAthlete`.

### 9a. Add `isMixedTeamMember()` accessor

After the existing `isTeamMember()` method (line ~228):

```java
public Boolean isMixedTeamMember() {
    return (this.athlete != null ? this.athlete.isMixedTeamMember() : null);
}
```

### 9b. Add `setMixedTeamMember()` mutator

After the existing `setTeamMember()` method (line ~244):

```java
public void setMixedTeamMember(boolean b) {
    if (this.athlete != null) {
        this.athlete.setMixedTeamMember(b);
    }
}
```

### 9c. Add `mixedMembershipLabel` field for the mixed column count

The existing `membershipLabel` field (line ~64) is used by TeamSelectionContent to update the team member count when a checkbox is toggled. A second label is needed for the mixed column:

```java
private NativeLabel mixedMembershipLabel;

public NativeLabel getMixedMembershipLabel() {
    return this.mixedMembershipLabel;
}

public void setMixedMembershipLabel(NativeLabel label) {
    this.mixedMembershipLabel = label;
}
```

**Verification:** Compilation succeeds. No runtime impact until callers use these methods.

---

## Step 10: Mixed-team awareness in TeamSelectionTreeData.java

**File:** `owlcms/src/main/java/app/owlcms/data/team/TeamSelectionTreeData.java`

Currently `buildTeamItemTree()` iterates `Gender.mfValues()` (only M, F — line 164). For `_MIXED` championships, a third pass for Gender.MF is needed to show athletes that are `mixedTeamMember`.

### 10a. Add Gender.MF iteration for `_MIXED` championships

After the existing `for (Gender gender : Gender.mfValues())` loop (lines 164–210), add a conditional third pass:

```java
// After the M/F loop closes...

// For _MIXED championships, add a third pass for the mixed team (Gender.MF)
if (ageDivision != null && ageDivision.isMixed()) {
    Gender mixedGender = Gender.MF;
    Collection<Participation> mixedParticipations;

    if (participations == null) {
        return;
    }

    mixedParticipations = participations.stream()
            .filter(a -> a.isMixedTeamMember())
            .sorted((p1, p2) -> {
                int compare = 0;
                compare = ObjectUtils.compare(p1.getAthlete().getTeam(), p2.getAthlete().getTeam());
                if (compare != 0) {
                    return compare;
                }
                compare = ObjectUtils.compare(p1.getMixedTeamMember(), p2.getMixedTeamMember());
                if (compare != 0) {
                    return -compare;
                }
                compare = p1.getCategory().compareTo(p2.getCategory());
                return compare;
            })
            .collect(Collectors.toList());

    for (Participation p : mixedParticipations) {
        String curTeamName = p.getAthlete().getTeam();
        TeamTreeItem curTeamItem = findCurTeamItem(
                mixedGender,
                curTeamName != null ? curTeamName : "-");
        if (!this.teams.contains(curTeamItem)) {
            this.addRootItems(curTeamItem);
            this.teams.add(curTeamItem);
        }

        Group group = p.getAthlete().getGroup();
        curTeamItem.addTreeItemChild(this, new PAthlete(p), group != null ? group.isDone() : false);
    }
}
```

**Note:** `Participation.isMixedTeamMember()` must be public (verified in Step 7). Also `Participation.getMixedTeamMember()` (the Boolean getter) is needed for the sort comparator.

### 10b. Add Gender.MF to findAll filter

In `findAll()` (line ~78), the gender filter currently uses `genderFilterValue != athleteGender` which works for M and F. When the gender filter is set to MF, it should match athletes of **either** gender who are `mixedTeamMember`. Update the filter:

**Current (line ~89):**
```java
Gender athleteGender = p.getAthlete().getGender();
if (genderFilterValue != null && genderFilterValue != athleteGender) {
    return false;
}
```

**New:**
```java
Gender athleteGender = p.getAthlete().getGender();
if (genderFilterValue != null) {
    if (genderFilterValue == Gender.MF) {
        // MF filter: show athletes who are mixedTeamMember regardless of their gender
        if (!Boolean.TRUE.equals(p.getMixedTeamMember())) {
            return false;
        }
    } else if (genderFilterValue != athleteGender) {
        return false;
    }
}
```

### 10c. Extend findAll sorting for mixed membership

In `findAll()` (lines 116–145), the sorting uses `a.getTeamMember()` to group team members first. When the championship `isMixed()` and the user filters by MF, sort by `mixedTeamMember` instead:

This is a secondary concern — the MF pass in `buildTeamItemTree` handles tree construction. The `findAll` sorting can remain as-is for initial implementation, since the MF gender filter will only show mixed members anyway (from 10b).

**Verification:** After Step 10, the team selection tree correctly shows M, F, and MF team nodes for `_MIXED` championships.

---

## Step 11: Mixed-team UI in TeamSelectionContent.java

**File:** `owlcms/src/main/java/app/owlcms/nui/preparation/TeamSelectionContent.java`

### 11a. Add a second checkbox column for `mixedTeamMember`

After the existing `membershipRenderer` column (lines 367–393), add a second column that is visible only when the current championship `isMixed()`:

```java
ComponentRenderer<Component, TeamTreeItem> mixedMembershipRenderer = new ComponentRenderer<>(p -> {
    Championship ad = getAgeDivision();
    // Only show for _MIXED championships
    if (ad == null || !ad.isMixed()) {
        return new NativeLabel();
    }

    if (p.getAthlete() == null) {
        // Team node: show count of mixed members
        long nb = p.getTeamMembers().stream().filter(pa -> Boolean.TRUE.equals(pa.isMixedTeamMember())).count();
        NativeLabel label = new NativeLabel(nb + "");
        p.setMixedMembershipLabel(label);
        return label;
    } else {
        // Athlete node: checkbox for mixedTeamMember
        Checkbox mixedBox = new Checkbox("Name");
        mixedBox.setLabel(null);
        mixedBox.getElement().getThemeList().set("secondary", true);
        mixedBox.setValue(p.isMixedTeamMember() != null ? p.isMixedTeamMember() : false);
        mixedBox.addValueChangeListener(click -> {
            Boolean value = click.getValue();
            mixedBox.setValue(value);
            JPAService.runInTransaction(em -> toggleMixedTeamMember(p, value, em));
        });
        // prevent grid row selection from triggering
        mixedBox.getElement().addEventListener("click", ignore -> {
        }).addEventData("event.stopPropagation()");
        return mixedBox;
    }
});
grid.addColumn(mixedMembershipRenderer).setHeader(Translator.translate("TeamMembership.MixedTeamMember"))
        .setSortable(true).setTextAlign(ColumnTextAlign.CENTER);
```

**Note:** The `TeamMembership.MixedTeamMember` translation key must be added (see translation process in copilot-instructions). Fallback English: "Mixed Team".

### 11b. Add `toggleMixedTeamMember()` method

After the existing `toggleTeamMember()` method (lines 610–623), add:

```java
private Object toggleMixedTeamMember(TeamTreeItem tti, Boolean value, EntityManager em) {
    logger.info("{} {} as mixed team member for category {}", value ? "setting" : "removing",
            tti.getAthlete().getShortName(), tti.getAthlete().getCategory().getNameWithAgeGroup());
    Participation _getOriginalParticipation = ((PAthlete) tti.getAthlete())._getOriginalParticipation();
    boolean member = Boolean.TRUE.equals(value);
    _getOriginalParticipation.setMixedTeamMember(member);
    tti.setMixedTeamMember(member);
    em.merge(_getOriginalParticipation);
    TeamTreeItem parent = tti.getParent();
    List<TeamTreeItem> teamMembers = tti.getParent().getTeamMembers();
    long count = teamMembers != null
            ? teamMembers.stream().filter(m -> Boolean.TRUE.equals(m.isMixedTeamMember())).count()
            : 0;
    if (parent.getMixedMembershipLabel() != null) {
        parent.getMixedMembershipLabel().setText("" + count);
    }
    return null;
}
```

### 11c. Add Gender.MF to the gender filter for `_MIXED` championships

In `defineFilters()` (lines 465–475), the gender filter only offers M and F. Update it to include MF when the current championship is `_MIXED`:

**Current (line ~468):**
```java
this.genderFilter.setItems(Gender.M, Gender.F);
```

The items need to be updated dynamically when the championship changes. In the `setAgeDivisionSelectionListener()` method, after `setAgeDivision(ageDivisionValue)`, add:

```java
// Update gender filter options based on championship type
if (ageDivisionValue != null && ageDivisionValue.isMixed()) {
    TeamSelectionContent.this.genderFilter.setItems(Gender.M, Gender.F, Gender.MF);
} else {
    TeamSelectionContent.this.genderFilter.setItems(Gender.M, Gender.F);
}
TeamSelectionContent.this.genderFilter.setValue(null); // Reset selection
```

### 11d. Translation key

Create a TSV file for the new translation key following the project's i18n process. The key `TeamMembership.MixedTeamMember` with English value "Mixed Team" should be added.

**Verification:** After Step 11, the team selection page shows both a gendered team member checkbox and a mixed team member checkbox when viewing a `_MIXED` championship. The gender filter includes an MF option. Toggling the mixed checkbox persists `mixedTeamMember` on the Participation.

---

## Implementation Order

Execute steps in this order to minimize compilation errors:

1. **Step 1** (Championship fields) — no other file depends on these yet
2. **Step 2** (Bootstrap/migration) — needs Step 1 fields
3. **Step 3** (Raw points on Participation/PAthlete) — independent of Steps 1–2
4. **Step 4** (AthleteSorter overload) — independent of Steps 1–3
5. **Step 7** (Visibility fix) — independent, quick
6. **Step 9** (TeamTreeItem mixed methods) — independent, quick
7. **Step 5** (Competition pipeline) — needs Championship fields from Step 1
8. **Step 6** (TeamResultsTreeData) — needs Steps 1, 3, 5
9. **Step 10** (TeamSelectionTreeData mixed awareness) — needs Steps 7, 9
10. **Step 11** (TeamSelectionContent mixed UI) — needs Steps 9, 10
11. **Step 8** (Verification grep) — after all edits

Steps 1, 3, 4, 7, 9 can be done in parallel (no dependencies between them).
Step 2 depends on Step 1.
Steps 5 and 6 depend on Step 1.
Step 6 also depends on Steps 3 and 7.
Step 10 depends on Steps 7 and 9.
Step 11 depends on Steps 9 and 10.

---

## Testing Strategy

### Compile check
After all steps, compile: `mvn -DskipTests compile`

### Runtime check
1. Start with `OWLCMS_MEMORYMODE=true OWLCMS_INITIALDATA=LARGEGROUP_DEMO`
2. Verify Championships have non-null scoring fields (check database or add debug logging)
3. Navigate to Team Results page — verify M, F, MF teams display correctly
4. Verify scores/points accumulate as before (regression test)

### Mixed-specific test (when _MIXED championships exist)
1. Create a championship with type `U_MIXED`
2. Assign athletes with `mixedTeamMember = true` (some with `teamMember = false`)
3. Verify MF team uses `isMixedTeamMember()` gating
4. Verify mixed-only athletes contribute raw points correctly

### Team Selection test (Steps 9–11)
1. Navigate to Preparation → Teams page
2. Select a `_MIXED` championship in the championship dropdown
3. Verify the "Mixed Team" checkbox column appears
4. Verify checking the mixed checkbox persists `mixedTeamMember` on the Participation
5. Verify the mixed member count updates on the team node
6. Verify the Gender filter includes MF option and filters correctly
7. Select a non-mixed championship — verify the mixed column shows as empty/hidden

---

## What This Phase Does NOT Do

- **UI for championship scoring fields** — The EditChampionshipsDialog won't expose the 12 new fields yet. That's a Phase 5 concern.
- **SBDE import/export** — That's Phase 6.
- **Separate snatch/CJ scoring** — The fields exist but are set equal to `bestAthleteScoringSystem`. Future phase.
