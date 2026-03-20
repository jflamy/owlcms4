# Phase 2: Participation Model Extension

Add a second membership boolean (`mixedTeamMember`) to the `Participation` entity so athletes can belong to an independent mixed team when the championship type is a `_MIXED` variant. Extend the athlete-side helpers and serialization paths to expose both membership dimensions cleanly.

## Prerequisites

- Phase 0 complete: stored Championship entity.
- Phase 1 complete: `AgeGroup.isMixedTeams()`, `mixedBestN` on Competition, ChampionshipType picker in EditChampionshipsDialog.
- Phase 1b complete: no legacy `ageDivision` fallback; all championship identity comes from stored Championship.

## Design Decisions

1. The existing `teamMember` boolean keeps its current semantics: it controls gender-specific team membership (M or F team).
2. A new `mixedTeamMember` boolean controls independent mixed-team membership. It is only meaningful when the participation's championship type is a `_MIXED` variant.
3. When the championship type is **not** `_MIXED`, mixed results are the aggregate of M and F teams (current behavior). The `mixedTeamMember` field is ignored.
4. When the championship type **is** `_MIXED`, mixed results come from athletes explicitly flagged with `mixedTeamMember = true`. Gender-specific teams still come from `teamMember`.
5. An athlete can have `teamMember = true` and `mixedTeamMember = true` simultaneously — the two memberships are independent.
6. Default value for `mixedTeamMember` is `false` for backward compatibility. Existing databases keep current behavior.
7. Points calculation for mixed teams follows the same formula as gender-specific points (via `AthleteSorter.pointsFormula`), gated by `mixedTeamMember` instead of `teamMember`. Separate rank columns for mixed teams are **deferred** — Phase 2 adds only the membership boolean; Phase 3 adds rank columns if needed for team scoring.

## Current State (Before Phase 2)

### Participation.java
- Fields: `id` (composite), `athlete`, `category`, `teamMember`, plus rank columns (`snatchRank`, `cleanJerkRank`, `totalRank`, `customRank`, `combinedRank`, `categoryScoreRank`, `teamSnatchRank`, `teamCJRank`, `teamTotalRank`, `teamSinclairRank`, `teamRobiRank`, `teamCombinedRank`).
- `teamMember` column: `boolean default true`. Gates all `*Points()` methods.
- `getChampionshipType()`: navigates `category → ageGroup → championshipType`.
- Copy constructor copies `teamMember` from source.
- `long_dump()` logs `teamMember`.

### Athlete.java
- `getAgeGroupTeams()`: iterates participations, collects age-group display names where `p.getTeamMember() == true`.
- `setAgeGroupTeams(Set<String>)`: iterates participations, sets `p.setTeamMember(...)` based on whether the age-group display name is in the provided set.
- `getPossibleAgeGroupTeams()`: returns all eligible age-group display names (regardless of membership).
- `addEligibleCategory(category, teamMember)`: creates Participation and sets `teamMember`.
- `isTeamMember()`: checks main ranking's `teamMember`.
- `setTeamMember(boolean)`: throws `UnsupportedOperationException` — must go through PAthlete.

### PAthlete.java
- `isTeamMember()` → `this.p.getTeamMember()` (from the participation).
- `setTeamMember(boolean)` → `this.p.setTeamMember(member)`.

### ParticipationDTO.java (V2 export)
- Has `teamMember` field (Boolean).
- `fromParticipation()`: reads `participation.getTeamMember()`.
- `toParticipation()`: writes `participation.setTeamMember(teamMember != null ? teamMember : false)`.
- Also stores `championshipType` as String.

### NAthleteRegistrationFormFactory.java (UI)
- `CheckboxGroup<String> ageGroupTeamField` — items from `getPossibleAgeGroupTeams()`, value from `getAgeGroupTeams()`.
- On save, calls `setAgeGroupTeams(selectedSet)`.
- Translation key: `TeamMembership.Title`.

### Competition.java (team reporting)
- `splitPTeamMembersByGender()`: filters `a.isTeamMember()`, then splits by `a.getGender()` into M and F lists.
- `doTeamRankings()`: calls `splitPTeamMembersByGender()`, creates reporting beans with prefixes `m`, `w`, `mw`.
- Reporting bean keys: `mTeam{suffix}`, `wTeam{suffix}`, `mwTeam{suffix}`, plus Combined/Custom/TeamBest variants.

### TeamResultsTreeData.java
- `buildTeamItemTree()`: iterates `Gender.mfmfValues()` (F, M, MF).
- `doTeamGender()`: builds key like `{genderKey}Team{suffix}` where genderKey is `m`, `w`, or `mw`.
- `getTopNTeamSize(Gender)`: returns `mensBestN` for M, `womensBestN` for F. **Currently returns 0 for Gender.I and has no case for Gender.MF** — this is a key extension point for `mixedBestN`.
- Inside loop: only `a.isTeamMember()` athletes contribute scores.

### TeamSelectionTreeData.java
- Sorts team members above non-members using `getTeamMember()`.
- `checkCounts()`: counts athletes where `isTeamMember()`.

---

## Implementation Steps

### Step 1: Add `mixedTeamMember` to Participation.java

**File:** `owlcms/src/main/java/app/owlcms/data/category/Participation.java`

**Changes:**

1. Add field (next to `teamMember`):
   ```java
   @Column(columnDefinition = "boolean default false")
   private boolean mixedTeamMember = false;
   ```

2. Add public getter:
   ```java
   public boolean getMixedTeamMember() {
       return isMixedTeamMember();
   }
   ```

3. Add private `isMixedTeamMember()`:
   ```java
   private boolean isMixedTeamMember() {
       return this.mixedTeamMember;
   }
   ```

4. Add setter:
   ```java
   public void setMixedTeamMember(boolean mixedTeamMember) {
       this.mixedTeamMember = mixedTeamMember;
   }
   ```

5. Update copy constructor to copy `mixedTeamMember`:
   ```java
   this.setMixedTeamMember(p.isMixedTeamMember());
   ```

6. Update `long_dump()` to include `mixedTeamMember`:
   ```java
   + ", mixedTeamMember=" + getMixedTeamMember()
   ```

**Do NOT add mixed-specific points methods yet.** Points calculation for mixed teams will be added in Phase 3 when the team scoring pipeline is extended.

### Step 2: Add mixed-team helpers to Athlete.java

**File:** `owlcms/src/main/java/app/owlcms/data/athlete/Athlete.java`

**Changes:**

1. Add `getMixedAgeGroupTeams()` — returns age-group display names where `p.getMixedTeamMember() == true`:
   ```java
   public Set<String> getMixedAgeGroupTeams() {
       Set<String> s = new LinkedHashSet<>();
       for (Participation p : getParticipations()) {
           if (p.getMixedTeamMember()) {
               s.add(p.getCategory().getAgeGroup().getDisplayName());
           }
       }
       return s;
   }
   ```

2. Add `setMixedAgeGroupTeams(Set<String>)` — updates `mixedTeamMember` on each participation:
   ```java
   public void setMixedAgeGroupTeams(Set<String> s) {
       for (Participation p : getParticipations()) {
           p.setMixedTeamMember(s.contains(p.getCategory().getAgeGroup().getDisplayName()));
       }
   }
   ```

3. Add `getPossibleMixedAgeGroupTeams()` — returns eligible age groups filtered to those whose championship is `_MIXED`:
   ```java
   public Set<String> getPossibleMixedAgeGroupTeams() {
       Set<String> s = new LinkedHashSet<>();
       List<Category> pcats = getParticipations().stream()
               .map(p -> p.getCategory()).collect(Collectors.toList());
       pcats.sort(new RegistrationPreferenceComparator());
       for (Category c : pcats) {
           AgeGroup ag = c.getAgeGroup();
           if (ag != null && ag.isMixedTeams()) {
               s.add(ag.getDisplayName());
           }
       }
       return s;
   }
   ```
   This ensures the mixed-membership UI only appears for age groups whose championship is truly `_MIXED`. If no championships are `_MIXED`, the set is empty and the UI control is hidden (see Step 4).

4. Add `addEligibleCategory` overload or update existing one to accept `mixedTeamMember`:
   ```java
   public void addEligibleCategory(Category category, boolean teamMember, boolean mixedTeamMember) {
       // same as existing, plus:
       participation.setMixedTeamMember(mixedTeamMember);
   }
   ```
   The existing two-argument overload `addEligibleCategory(category, teamMember)` should continue to work, defaulting `mixedTeamMember = false`.

### Step 3: Add mixed-team proxy to PAthlete.java

**File:** `owlcms/src/main/java/app/owlcms/spreadsheet/PAthlete.java`

**Changes:**

1. Add `isMixedTeamMember()`:
   ```java
   public boolean isMixedTeamMember() {
       return this.p.getMixedTeamMember();
   }
   ```

2. Add `getMixedTeamMember()`:
   ```java
   public Boolean getMixedTeamMember() {
       return this.a.getMixedTeamMember();
   }
   ```
   Note: Decide whether this should delegate to `this.p.getMixedTeamMember()` (participation-specific) or `this.a` (athlete-level). Follow the same pattern as `isTeamMember()`/`getTeamMember()` — use the participation for `is` and the athlete for `get`.

3. Add `setMixedTeamMember(boolean)`:
   ```java
   public void setMixedTeamMember(boolean member) {
       this.p.setMixedTeamMember(member);
   }
   ```

### Step 4: Update ParticipationDTO.java (V2 export/import)

**File:** `owlcms/src/main/java/app/owlcms/data/export/v2/ParticipationDTO.java`

**Changes:**

1. Add field:
   ```java
   private Boolean mixedTeamMember;
   ```

2. Add getter/setter:
   ```java
   public Boolean getMixedTeamMember() { return mixedTeamMember; }
   public void setMixedTeamMember(Boolean mixedTeamMember) { this.mixedTeamMember = mixedTeamMember; }
   ```

3. In `fromParticipation()`:
   ```java
   dto.setMixedTeamMember(participation.getMixedTeamMember());
   ```

4. In `toParticipation()`:
   ```java
   participation.setMixedTeamMember(mixedTeamMember != null ? mixedTeamMember : false);
   ```

**Backward compatibility:** Older JSON exports will not have `mixedTeamMember` in the DTO. Jackson's `@JsonIgnoreProperties(ignoreUnknown = true)` on `Participation` (and likely on the DTO or the import path) means missing fields default to `null`, which maps to `false` via the null guard.

### Step 5: Expose `mixedBestN` in team-size lookup (preparation for Phase 3)

**File:** `owlcms/src/main/java/app/owlcms/data/team/TeamResultsTreeData.java`

**Changes:**

Update `getTopNTeamSize(Gender)` to handle `Gender.MF`:
```java
private Integer getTopNTeamSize(Gender gender) {
    Competition comp = Competition.getCurrent();
    switch (gender) {
        case M:
            return comp.getMensBestN() != null ? comp.getMensBestN() : Integer.MAX_VALUE;
        case F:
            return comp.getWomensBestN() != null ? comp.getWomensBestN() : Integer.MAX_VALUE;
        case MF:
            return comp.getMixedBestNElseDefault();
        case I:
            return 0;
        default:
            return Integer.MAX_VALUE;
    }
}
```

This is a minimal, safe change: the MF case currently falls through to `default` which leaves `maxCount` as `null`. Returning `mixedBestNElseDefault()` wires the configured mixed team size into the scoring pipeline. The actual mixed-team scoring logic change (checking `mixedTeamMember` instead of `teamMember` for MF gender) will be done in Phase 3.

---

## Files Modified Summary

| # | File | Change |
|---|------|--------|
| 1 | `Participation.java` | Add `mixedTeamMember` field, getter, setter, copy-constructor update, `long_dump()` update |
| 2 | `Athlete.java` | Add `getMixedAgeGroupTeams()`, `setMixedAgeGroupTeams()`, `getPossibleMixedAgeGroupTeams()`, update `addEligibleCategory()` |
| 3 | `PAthlete.java` | Add `isMixedTeamMember()`, `getMixedTeamMember()`, `setMixedTeamMember()` |
| 4 | `ParticipationDTO.java` | Add `mixedTeamMember` field, getter, setter, DTO conversion methods |
| 5 | `TeamResultsTreeData.java` | Wire `MF` case in `getTopNTeamSize()` to `mixedBestNElseDefault()` |

## What Phase 2 Does NOT Do

These items are deferred to later phases:

- **Phase 3:** Team computation pipeline changes (using `mixedTeamMember` for `_MIXED` championships, adding mixed-specific rank columns to Participation if needed, `splitPTeamMembersByGender` extension or new `splitMixedTeamMembers` method, reporting bean generation for independent mixed teams).
- **Phase 4:** UI changes in `NAthleteRegistrationFormFactory` — adding a second `CheckboxGroup` for mixed-team membership, shown only when `_MIXED` championships exist.
- **Phase 5:** Team results display changes in `TeamResultsContent`, `TeamSelectionTreeData`.
- **Phase 6:** SBDE import/export round-trip for `mixedTeamMember`.

## Verification Checklist

1. Existing databases: `mixedTeamMember` column appears as `false` for all existing participation rows (Hibernate auto-DDL adds the column with `default false`).
2. Existing behavior: all current team scoring, results, and UI is unchanged because nothing reads `mixedTeamMember` yet (except the `getTopNTeamSize` MF wiring, which already had no meaningful behavior for MF).
3. V2 JSON export: new `mixedTeamMember` field appears in exported JSON.
4. V2 JSON import: old exports without `mixedTeamMember` import cleanly (defaults to `false`).
5. `Athlete.getPossibleMixedAgeGroupTeams()` returns an empty set when no championships have a `_MIXED` type — this is the correct baseline for competitions that don't use independent mixed teams.
6. `Athlete.getPossibleMixedAgeGroupTeams()` returns the correct age groups when `_MIXED` championships exist.
7. Copy constructor preserves `mixedTeamMember` value.
8. `long_dump()` shows both `teamMember` and `mixedTeamMember` for debugging.

## Commit Strategy

Single commit for all Phase 2 changes:
```
Add mixedTeamMember to Participation model for independent mixed teams
```

All 5 files changed together since they form a coherent unit.
