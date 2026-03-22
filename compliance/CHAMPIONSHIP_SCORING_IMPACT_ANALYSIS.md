# Impact Analysis: Championship-Level Scoring Configuration

## Clarification (from domain expert)

A championship controls the awards:
- The **best athlete** award is computed using a best-athlete scoring system set for the championship.
- **Medals** are awarded using a (possibly different) medaling scoring system, also set for the championship.
- **Points** are computed based on the ranks obtained from the medaling system.
- The championship determines whether there are total-only or snatch/CJ/total medals.
- Currently, championships derive their defaults for medaling and best athlete from the overall competition.

Team scores can be of two types:
- **Sum of points** — used by default for the gendered teams. Points come from the gendered contribution (ranks within the athlete's gendered category).
- **Sum of scores** — GAMX used by default as gendered team score, but any scoring system could be used.

When there is no `_MIXED` championship, the mixed team is the union of the M and F teams. When there is `_MIXED`, the mixed team is explicit (athletes with `mixedTeamMember = true`).

### Team naming and composition

The team name (e.g., "CAN") is the same display name for all three teams. The three beans `mTeam`, `wTeam`, `mwTeam` represent the three teams for the same club/country:

**`_MIXED` championship (explicit mixed membership):**
- Men's team (CAN): 8 men selected via `teamMember`
- Women's team (CAN): 8 women selected via `teamMember`
- Mixed team (CAN): 8 athletes of any gender combination selected via `mixedTeamMember` (could be 2M + 6F, 5M + 3F, etc.)
- An athlete CAN be on the mixed team without being on the gendered team, and vice versa. The two memberships are fully independent.

**Non-`_MIXED` championship (automatic mixed = union):**
- Men's team (CAN): separate gendered team
- Women's team (CAN): separate gendered team
- Mixed team (CAN): automatic union of men + women, up to 16 total, or with topN rules per gender (e.g., top 5 men + top 3 women). The existing `getTopNTeamSize(a.getGender())` inside the MF accumulation loop already handles per-gender limits within the union.

---

## Current Architecture: How Scoring Flows

### Hierarchy of scoring configuration

```
Competition (global defaults)
    scoringSystem                    → Ranking (e.g., BW_SINCLAIR)
    snatchCJTotalMedals              → boolean
    teamPoints1st/2nd/3rd           → Integer (28/25/23)
    mensBestN / womensBestN / mixedBestN → Integer (8)
        ↓ defaults to
AgeGroup (per age group)
    scoringSystem                    → Ranking (medaling system)
    bestAthleteScoringSystem         → Ranking
    medals                           → boolean
        ↓ used at bootstrap time ("last one seen wins")
Championship (persisted scoring config, always explicit)
    scoringSystem                    → Ranking
    bestAthleteScoringSystem         → Ranking
    snatchCJTotalMedals              → boolean
    teamPoints1st/2nd/3rd           → Integer
    mensBestN / womensBestN / mixedBestN → Integer
    teamScoringSystem               → Ranking
    (no runtime aggregation — values set at creation/bootstrap)
```

### Championship propagation through team scoring

```
TeamResultsContent / TopTeams (UI)
  → new TeamResultsTreeData(ageGroupPrefix, championship, gender, ranking, includeNotDone)
     ↓
  → Competition.computeReportingInfo(ageGroupPrefix, championship)
     ↓
  → doComputeReportingInfo(athletes, ageGroupPrefix, championship)
     ↓ if championship != null and no ageGroupPrefix
  → teamRankingsForAgeDivision(championship)           ← Championship object available
     ↓
     for each ageGroup in championship:
       athletes = AgeGroupRepository.allPAthletesForAgeGroup(ageGroup)
       doTeamRankings(athletes, championship.getName(), false)    ← Championship LOST (name only)
         ↓
         splitPTeamMembersByGender(athletes, men, women)         ← only isTeamMember()
         ↓
         Creates beans: mTeam{suffix}, wTeam{suffix}, mwTeam{suffix}
                        mCombined, wCombined, mwCombined
                        mCustom, wCustom, mwCustom
                        mTeamBest, wTeamBest, mwTeamBest
         ↓
         mwTeam = men + women concatenated (always union, never explicit mixed)
         ↓
         "TeamBest" uses Competition.getCurrent().getScoringSystem()   ← COMPETITION level
```

### How each PAthlete carries its scoring context

```
PAthlete(participation)
  → wraps specific Participation
     → Participation has: teamMember, mixedTeamMember, ranks, points
     → Participation.getChampionshipType() navigates: category → ageGroup → championshipType
     → Points gated by isTeamMember(): getSnatchPoints(), getTotalPoints(), etc.
```

### How doTeamGender accumulates scores

`TeamResultsTreeData.doTeamGender()` reads athletes from the `mwTeam{suffix}` bean and, in a single pass, accumulates ALL score types simultaneously:

```java
// For each athlete in the bean:
Integer curPoints = combinedTotal ? a.getCombinedPoints() : a.getTotalPoints();
double curSinclair = a.getSinclairForDelta();
double curGamx = a.getGamx();
double curQPoints = a.getQPoints();
double curRobi = a.getRobi();
// ... etc
// All accumulated into the Team object if a.isTeamMember() && counted < maxN
```

The `isTeamMember()` gate controls ALL accumulation. For MF gender, this currently means the union of men and women who are gender-specific team members.

---

## Impact Analysis

### 1. Championship needs persisted scoring configuration

**Current state:** Championship has only `id`, `name`, `type`. Scoring comes from AgeGroup fields or from Competition global defaults. The existing `getBestAthleteScoringSystem()` uses a majority-vote aggregation across age groups — this will be removed.

**Needed:** Championship needs persisted scoring fields that always hold explicit values. The UI shows the actual value, not an inherited/empty state.

| Field | Type | Purpose |
|-------|------|---------|
| `scoringSystem` | `Ranking` | Medaling scoring system |
| `bestAthleteScoringSystem` | `Ranking` | Best athlete award scoring |
| `bestSnatchScoringSystem` | `Ranking` | Best snatch award scoring (future — defaults to `bestAthleteScoringSystem` initially) |
| `bestCJScoringSystem` | `Ranking` | Best clean & jerk award scoring (future — defaults to `bestAthleteScoringSystem` initially) |
| `snatchCJTotalMedals` | `boolean` | Medal type (total only vs snatch/CJ/total) |
| `teamPoints1st` | `Integer` | Points for 1st place in medaling rank |
| `teamPoints2nd` | `Integer` | Points for 2nd place |
| `teamPoints3rd` | `Integer` | Points for 3rd place |
| `mensBestN` | `Integer` | Top N men for team scoring |
| `womensBestN` | `Integer` | Top N women for team scoring |
| `mixedBestN` | `Integer` | Top N for mixed team scoring |
| `teamScoringSystem` | `Ranking` | Scoring system for sum-of-scores team ranking |

**Impact:** Adds ~12 columns to the Championship table. All fields have explicit values — there is no null-means-inherited pattern.

**Key design decision: explicit values, no `getEffective*()` fallback.** Each Championship field holds a real value. The UI displays the actual setting and allows direct editing. There is no hidden inheritance from Competition at runtime.

**Bootstrap (initial transition):** When a Championship is first created (from age groups at startup, from the championship dialog, or from spreadsheet upload), its scoring fields are populated from the Competition defaults, then overridden using the "last one seen wins" rule applied to the age groups being read. After bootstrap, the Championship's values are authoritative and editable independently.

```java
// At bootstrap time, populate from Competition defaults:
championship.setScoringSystem(Competition.getCurrent().getScoringSystem());
championship.setBestAthleteScoringSystem(Competition.getCurrent().getScoringSystem());
championship.setSnatchCJTotalMedals(Competition.getCurrent().isSnatchCJTotalMedals());
championship.setTeamPoints1st(Competition.getCurrent().getTeamPoints1st());
// ... etc.
// Then override from age groups using last-one-seen-wins:
for (AgeGroup ag : ageGroupsInChampionship) {
    if (ag.getScoringSystem() != null) {
        championship.setScoringSystem(ag.getScoringSystem());
    }
    if (ag.getBestAthleteScoringSystem() != null) {
        championship.setBestAthleteScoringSystem(ag.getBestAthleteScoringSystem());
    }
}
```

After initial transition, AgeGroup-level scoring fields are NOT consulted. The Championship is the sole authority.

### 2. Two kinds of team score: points vs scores

**Current state:** `doTeamGender()` accumulates both:
- **Points** (rank-based): `curPoints = a.getCombinedPoints()` or `a.getTotalPoints()`. Points come from `Participation.getSnatchPoints()` / `getTotalPoints()` which gate on `isTeamMember()` and call `AthleteSorter.pointsFormula(rank)`.
- **Scores** (formula-based): `curSinclair`, `curGamx`, `curQPoints`, `curRobi`, etc. These come from Athlete methods like `getSinclairForDelta()`, `getGamx()` and are NOT gated by `isTeamMember()` — they always return the computed value. The gating happens in the accumulation loop's `if (a.isTeamMember())` check.

**Points come from gendered ranks.** A female athlete ranked 2nd in her category gets 25 points. Those 25 points contribute to the F team. In a non-`_MIXED` championship, those same 25 points also contribute to the MF (mixed) team. In a `_MIXED` championship, the athlete's points contribute to the MF team only if `mixedTeamMember = true`.

**Scores are intrinsic to the athlete.** A female athlete's GAMX score is computed from her bodyweight and total. It doesn't depend on rank or team membership. It contributes to team totals based on the same gating logic (gendered `isTeamMember()` or `isMixedTeamMember()`).

**Impact on team points formula:** Currently `AthleteSorter.pointsFormula()` reads team points from `Competition.getCurrent()`. With championship-level config, it needs access to the championship's team points. Options:
- (a) Pass team points as parameters to `pointsFormula()`
- (b) Store points formula on the Participation (denormalized from Championship at ranking time)
- (c) Thread Championship through the call chain

**Recommendation:** Option (a) — minimal change. `pointsFormula(rank, first, second, third)` overload. The existing `pointsFormula(rank)` continues to use Competition defaults for backward compatibility.

### 3. Championship propagation gap in doTeamRankings

**Current state:** `teamRankingsForAgeDivision(Championship ad)` receives the Championship object but only passes `ad.getName()` (a String) to `doTeamRankings()`. Inside `doTeamRankings`, the Championship is unknown.

**Impact:** `doTeamRankings` must receive the Championship object so it can:
- Call `championship.isMixed()` to decide whether MF uses union (men+women) or explicit (`mixedTeamMember`)
- Call `championship.getScoringSystem()` for the "TeamBest" beans instead of `Competition.getCurrent().getScoringSystem()`
- Call `championship.isSnatchCJTotalMedals()` to determine medal type
- Call `championship.getTeamPoints1st()` etc. for points formula

**Signature change:**
```java
// Before:
private void doTeamRankings(List<Athlete> athletes, String suffix, boolean singleAgeGroup)

// After:
private void doTeamRankings(List<Athlete> athletes, String suffix, boolean singleAgeGroup, Championship championship)
```

### 4. splitPTeamMembersByGender needs a mixed-aware variant

**Current state:** `splitPTeamMembersByGender()` always gates on `isTeamMember()` and produces men + women lists. The mwTeam bean is their concatenation.

**Impact for `_MIXED` championships:** When `championship.isMixed()`, the mw (mixed) team is NOT the union of men and women. Instead, it is the set of athletes where `isMixedTeamMember() == true`, regardless of gender. The M and F teams still come from `isTeamMember()`.

**Needed:**
- Keep `splitPTeamMembersByGender()` unchanged for M and F lists
- Add logic in `doTeamRankings` to build the `mwTeam*` beans differently when `championship.isMixed()`:
  - Non-mixed: `mwTeam = men + women` (current behavior)
  - Mixed: `mwTeam = athletes.stream().filter(a -> a.isMixedTeamMember())` (explicit mixed membership)

### 5. Points come from gendered contribution

**Current state:** Points are computed from the participation's rank in its gendered category. `Participation.getTotalPoints()` returns `isTeamMember() ? AthleteSorter.pointsFormula(this.totalRank) : 0`. The `totalRank` is the rank within the category (which is gendered — categories belong to gendered age groups).

**For mixed teams:** An athlete's points in the mixed team come from their gendered rank. A woman ranked 1st in her F category gets 28 points. Those 28 points contribute to the mixed team if she's a mixed team member. The mixed team does NOT re-rank athletes across genders — it uses the gendered ranks.

**Impact:** The existing points-from-rank mechanism works correctly for mixed teams. No change to how ranks are computed. The only change is the gating: for MF accumulation in a `_MIXED` championship, check `isMixedTeamMember()` instead of `isTeamMember()`.

**Critical: mixed and gendered membership are independent.** An athlete CAN be `mixedTeamMember = true` and `teamMember = false` (on the mixed team but not the gendered team). This means `Participation.getTotalPoints()` — which gates on `isTeamMember()` — returns 0 for a mixed-only member. The mixed team accumulation must NOT rely on the gated points methods.

**Solution: raw points methods.** Add ungated points methods to Participation that always compute from rank:

```java
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

The accumulation loop in `doTeamGender` then uses:
- For M/F teams: existing gated `a.getTotalPoints()` (gates on `teamMember`)
- For MF team in `_MIXED` championship: `a.getRawTotalPoints()` with external gating on `isMixedTeamMember()`
- For MF team in non-mixed championship: existing behavior (union of men+women, gated points)

### 6. doTeamGender accumulation needs championship-aware gating

**Current state:** `TeamResultsTreeData.doTeamGender()` at line 153:
```java
if (a.isTeamMember()) {
    // accumulate points and scores
}
```

**Impact:** For Gender.MF in a `_MIXED` championship, this must check `a.isMixedTeamMember()` instead.

The Championship object is already available in `doTeamGender()` (passed as `ageDivision` parameter). The check becomes:
```java
boolean contributes;
if (gender == Gender.MF && ageDivision != null && ageDivision.isMixed()) {
    contributes = a.isMixedTeamMember();
} else {
    contributes = a.isTeamMember();
}
if (contributes) {
    // accumulate
}
```

### 7. getTopNTeamSize needs championship-level override

**Current state:** `TeamResultsTreeData.getTopNTeamSize(Gender)` reads from Competition:
```java
case M: return comp.getMensBestN();
case F: return comp.getWomensBestN();
case MF: return comp.getMixedBestNElseDefault();
```

**Impact:** Should read from championship directly (championship always has explicit values):
```java
case M: return championship.getMensBestN();
case F: return championship.getWomensBestN();
case MF: return championship.getMixedBestN();
```

### 8. AthleteSorter.pointsFormula needs championship-level points config

**Current state:** `pointsFormula(rank)` reads `teamPoints1st/2nd/3rd` from `Competition.getCurrent()`.

**Impact:** For championship-level team points, either:
- Add overload `pointsFormula(rank, championship)` that reads from championship
- Or pre-resolve the points values and pass them as parameters

**Recommendation:** Add overload, keep existing method as backward-compatible default.

### 9. Reporting bean structure is unchanged

The bean key scheme (`mTeam{suffix}`, `wTeam{suffix}`, `mwTeam{suffix}`) does not need structural changes. The suffix is the championship name. What changes is HOW the beans are populated:
- `mTeam` / `wTeam`: populated from `isTeamMember()` athletes, split by gender (unchanged)
- `mwTeam` when non-mixed: populated from men + women union (unchanged)
- `mwTeam` when `_MIXED`: populated from `isMixedTeamMember()` athletes (new behavior)

### 10. Relationship to AgeGroup scoring fields

**Current state:** AgeGroup has `scoringSystem` and `bestAthleteScoringSystem`. The existing `Championship.getBestAthleteScoringSystem()` uses a majority-vote aggregation across age groups.

**Impact of championship-level scoring:** Championship becomes the sole authority for scoring configuration. The majority-vote method is removed. AgeGroup scoring fields are only used during initial transition (bootstrap) to populate the Championship, using the "last one seen wins" rule. After that, the Championship fields are authoritative and directly editable.

**Migration for existing databases:** At startup, if a Championship has no scoring fields yet (detected by a sentinel like `scoringSystem == null`), bootstrap them from Competition defaults + age group overrides using last-one-seen-wins. This is a one-time operation. After migration, the Championship always has explicit values.

---

## Summary: What Changes vs What Stays

### Changes Required

| Component | Change |
|-----------|--------|
| **Championship.java** | Add ~12 scoring/team fields with explicit values (no null fallback) |
| **Competition.doTeamRankings()** | Accept Championship parameter; use championship scoring config |
| **Competition.teamRankingsForAgeDivision()** | Pass Championship to doTeamRankings (already has it) |
| **Competition.splitPTeamMembersByGender()** | No change (gendered split stays the same) |
| **Competition.doTeamRankings() MF bean** | Build differently when `championship.isMixed()` |
| **TeamResultsTreeData.doTeamGender()** | Championship-aware gating for MF accumulation |
| **TeamResultsTreeData.getTopNTeamSize()** | Read from championship directly (explicit values) |
| **AthleteSorter.pointsFormula()** | Add overload accepting championship team-point values |
| **Participation** | Add `getRawTotalPoints()`, `getRawSnatchPoints()`, `getRawCleanJerkPoints()`, `getRawCombinedPoints()` (ungated by `teamMember`) |

### No Change Needed

| Component | Reason |
|-----------|--------|
| Participation rank fields | Ranks are gendered; mixed team uses gendered ranks |
| PAthlete proxy | Already has both `isTeamMember()` and `isMixedTeamMember()` |
| Athlete scoring methods (GAMX, Sinclair, etc.) | Intrinsic to athlete; not gated by team membership |
| Reporting bean key scheme | Same `m/w/mw` prefix structure |
| Gender.mfmfValues() | Already iterates F, M, MF |
| ParticipationDTO | Already has `mixedTeamMember` field |
| CategoryRankings | Category ranks are gendered, unchanged |

---

## Resolved: Mixed and Gendered Membership Are Independent

**An athlete CAN be `mixedTeamMember = true` and `teamMember = false`.** The two memberships are fully independent. A coach selects 8 men for the men's team, 8 women for the women's team, and 8 athletes (any gender mix) for the mixed team — these are three separate selections.

This means raw (ungated) points methods are needed on Participation so that mixed team accumulation can use the athlete's gendered rank-based points even when the athlete is not on the gendered team.

---

## Future Consideration: Separate Snatch and CJ Scoring Systems

In the future, championships will support separate scoring systems for best snatch and best clean & jerk awards:

- `bestSnatchScoringSystem` — scoring system for the best snatch award
- `bestCJScoringSystem` — scoring system for the best clean & jerk award

These will also require separate score computations for snatch and CJ (analogous to the existing total-based scores like GAMX, Sinclair, etc., but applied to the snatch result and CJ result independently).

**For Phase 3:** Add these two fields to Championship now. At bootstrap time, they are set to the same value as `bestAthleteScoringSystem`. The getters are plain getters (no fallback chain):

```java
public Ranking getBestSnatchScoringSystem() {
    return this.bestSnatchScoringSystem;
}

public Ranking getBestCJScoringSystem() {
    return this.bestCJScoringSystem;
}
```

At bootstrap:
```java
championship.setBestSnatchScoringSystem(championship.getBestAthleteScoringSystem());
championship.setBestCJScoringSystem(championship.getBestAthleteScoringSystem());
```

This means:
- The columns exist from day one, avoiding a future schema migration.
- No code reads them differently until a future phase implements separate snatch/CJ scoring.
- Existing behavior is unchanged (all three hold the same value after bootstrap).
- UI for setting these separately is deferred — the Championship editing dialog can expose them later when the feature is ready.
