# Medal Ranks and Best-Athlete Ranks

Status: Parts 1a and 1b implemented and tested; Parts 2 and 3 remain planned
Scope: best-athlete rankings, scoreboards, websocket/tracker payloads, Excelok. 
templates, championship scoring settings

The work is split in three parts, with Part 1 implemented in two stages:

1. [Part 1: Best-Athlete Rank Storage and Accessors](#part-1-best-athlete-rank-storage-and-accessors)
   - [1a: Prepare storage and accessors](#part-1a-prepare-storage-and-accessors)
   - [1b: Switch readers and writers, including live ranking](#part-1b-switch-readers-and-writers-including-live-ranking)
2. [Part 2: Scoreboards](#part-2-scoreboards)
3. [Part 3: Tracker Fixes](#part-3-tracker-fixes)

Shared foundations (goal, principles, terminology) come first; open items and
deferred work are at the end.

## Goal

Separate the two independent uses of a scoring system:

- **Medal system**: medals are awarded within each category, either by weight
  lifted (Total, or Snatch/Clean and Jerk/Total) or by a score (GAMX, Sinclair,
  Q-points, …).
- **Best-athlete system**: one ranking across all the categories of a
  championship, per gender. It has nothing to do with medals.

An athlete therefore has, independently, a category medal rank and a
best-athlete rank within the championship of each participation.

## Principles

1. A best athlete belongs to a championship. The system never computes, stores
   or displays a competition-wide (meet-wide) rank on its own: every best-athlete
   rank it maintains (scoreboards, publicresults, tracker) is computed within one
   championship, per gender.
2. The competition template is a settings template only. Its settings are copied
   to, or inherited by, real championships
   (`Championship.copyCompetitionSettingsFrom`, `resolveBestAthleteScoringSystem`).
   Every championship therefore always has a medal system and a best-athlete
   system. The template is never used as a ranking context: no ranking is ever
   computed over "the template" population. Reading the template's best-athlete
   system as the initial value of a selector remains allowed.
3. The category score rank (`Participation.categoryScoreRank`) is always the
   correct category medal rank, whatever the medal system. `Competition.computeMedals`
   assigns it for every category: for weight medals it is computed "same as
   TOTAL", for score medals it is computed on the score. Consumers must not branch
   on the medal system to choose a rank.
4. The historical names `sinclair` / `sinclairRank` mean the best-athlete score
   and rank. They are never used for medals.
5. Operator override. On the results and final-package pages (and the top
   best-athletes display), the operator may choose any scoring system for the
   athletes shown, and may clear the championship filter to show all athletes.
   Scores and ranks are then computed at report time, with the chosen system,
   over the athletes shown, on reporting `PAthlete`s; nothing is stored on the athlete.
   A package mixing championships produced this way is the operator's explicit
   choice and is acceptable.

## Ranks and Scores

Terminology: category kg ranks (Snatch, Clean and Jerk, Total) and the
category's *medal rank* are distinct when medals are awarded by score. A
*best-athlete rank* is computed across categories, within a championship
(or over the athletes shown, under principle 5). The term
"overall rank" used in existing code (`OverallRankSetter`,
`assignOverallRanksAndPoints`, the `overall` flag of `Competition.doReporting`)
means best-athlete rank and is retired.

A *reporting `PAthlete`* is a `PAthlete` created for one report: one athlete in
one category, as used by the results and final package workbooks and the top
best-athletes display (one JXLS row each). Reporting `PAthlete`s are created
fresh for every report from a database query and discarded with it; they are
never persisted or cached. A `PAthlete` delegates lift data and scores to the
wrapped `Athlete`, and holds a *copy* of the participation
(`PAthlete(Participation)` does `new Participation(p, athlete, category)`; the
copy constructor copies the ranks). Ranks written on a reporting `PAthlete`
therefore never reach the stored `Participation` entity. An athlete with three
participations yields three reporting `PAthlete`s.

| Concept | Source | Scope |
| --- | --- | --- |
| Kg result ranks | `Participation.snatchRank`, `cleanJerkRank`, `totalRank` | Category, irrespective of medal scheme |
| Medal rank | `Participation.categoryScoreRank` | Category |
| Medal score | `Participation.categoryScore` | Category |
| Total (kg) | `Athlete.getTotal()` | Athlete |
| Best-athlete score | `Ranking.getRankingValue(athlete, championship.getBestAthleteScoringSystem())` | Athlete (does not depend on the participation) |
| Best-athlete rank | Ranking of the championship's participants, by gender, with the championship's best-athlete system | Championship |

Snatch and Clean and Jerk kg ranks are computed and displayed in every medal
scheme. Medal policy controls their medal highlights, not their visibility.

Scores and ranks behave differently everywhere (templates, scoreboards,
payloads):

- Any score (`sinclair`, `smm`, `qAge`, `gamx`, `catSinclair`, …) can be shown
  for any athlete: scores are computed live from the athlete.
- A rank only has meaning within the list it was computed for.

## Competition Object and UI

Meet-wide notions on `Competition` and in the user interface, and what becomes
of them:

- Competition-level scoring accessors (`scoringSystem`, `getScoringSystem()`,
  `getLegacyCompetitionScoringSystem()`, `getBestAthleteScoringSystem()`,
  `isScoreMedalChampionship()`, `isSinclair()`, `sinclairMeet` and the
  `SINCLAIR_MEET` feature switch) become settings delegations to the competition
  template only, used for migration and defaults. After Parts 1 and 2 no caller uses
  them to rank or to choose a displayed value (`scoringSystemRankings`,
  `getGlobalScoreRanking`, `BaseResults.computedScore`, `TopSinclair.doUpdate`
  are the current ranking callers). `isSinclair()` is already deprecated; the
  `sinclairMeet` fallback is kept for non-migrated databases only.
- `displayScores` / `displayScoreRanks`: see Part 2 Controls.
- "Recompute Ranks" buttons (final package, team results and session results
  pages): recompute the category medal ranks of every category and the
  best-athlete ranks of every championship (Part 1b), replacing
  `Competition.recomputeAllAthleteRanks`.
- Competition editor "Scoring systems" checkboxes (`Competition.enabledRankings`,
  `RankingConfig.userEnabled`): kept, redefined as *scoring systems offered in
  the best-athlete dropdown* (and enabling the Sinclair-year radio buttons).
  Today they also gate computation: `RankingConfig.shouldCompute` makes
  `Ranking.getRankingValue` return 0 and `doGlobalRankings` skip the ranking for
  an unchecked system. That gating is removed: scores are always computed
  (`getRankingValue` no longer tests `shouldCompute`), and best-athlete ranks are
  computed only per championship (Part 1b) or at report time with the selected
  system (Part 1). Championship systems remain required (`mustCompute`,
  read-only checked), so a championship's own system is always offered. The
  GAMX placeholder variants (GAMX-MS, GAMX-MC, GAMX-S, GAMX-C) are left unchecked
  by default and therefore not offered.
- Displays navigation (`DisplayNavigationContent`): the top best-athletes button
  is titled with the template's best-athlete system; this is a default-settings
  use (principle 2) and is unchanged.

---

## Part 1: Best-Athlete Rank Storage and Accessors

Refactor of how best-athlete ranks are stored, written and read. It changes no
scoreboard columns; it fixes the final package and category results workbooks,
establishes live championship ranks, and prepares Part 2.

### Part 1a: Prepare storage and accessors

Implemented, without activating the new runtime path:

- `Participation.bestAthleteRank` is persisted and copied by the participation
  copy constructor. `Athlete.getBestAthleteRank()` / `setBestAthleteRank(int)`
  access the main participation; inherited by `PAthlete`, they access its own
  participation instead. The new getters are `@JsonIgnore` for now.
- `PAthlete.copyForReporting` always creates a private participation copy,
  including when the input is a plain athlete or a legacy `PAthlete(Athlete)`
  wrapper. The legacy constructor is unchanged.
- `AthleteSorter.assignBestAthleteRanks` sorts and ranks supplied `PAthlete`s
  for one population and scoring system. It reuses existing scoring and
  tie-breaking rules, adds an athlete-ID tie-break, resets ranks by gender and
  assigns duplicate participations the same rank.
- `AthleteSorter.bestAthleteOrderCopy` creates private reporting `PAthlete`s,
  ranks them, and returns one per athlete. Callers supply the already-filtered
  population. Neither helper has a production caller yet.
- Tests in `AthleteSorterTest` cover storage isolation, unchanged legacy
  accessors, duplicate handling, independent report populations and scoring
  selections, gender separation, zero scores and invited athletes.
  `ChampionshipTest.testBestAthleteRanksPersistSeparatelyForEachChampionship`
  verifies persistence and independent ranks on two championships'
  participations.

Preparatory scope:

- Add `Participation.bestAthleteRank`, its copy handling, and the new
  `getBestAthleteRank()` / `setBestAthleteRank(int)` accessors.
- Prepare the rank assignment and reporting helpers against the new storage,
  including one rank per athlete and private participation copies for reporting
  `PAthlete`s.
- Keep the existing runtime readers, writers and per-system accessors connected
  to their existing storage until Part 1b. Do not delete their setters or redirect
  the live meet-wide writer into `Participation.bestAthleteRank`.
- Test the new path independently before activating it.

At the end of 1a, the old fields, setters, rank lookup switches, computation
settings, live refresh, exports, UI and templates were unchanged. Their
subsequent cutover is covered by 1b below.

The sections below describe the completed Part 1 design. Switching callers,
activating the compatibility aliases, removing old setters and lists, and
changing export/import all belong to Part 1b, not to the preparatory stage.

### Problem

Today best-athlete ranks are written through a per-system `switch`
(`OverallRankSetter.increment`, and a near-identical, apparently unused one in
`MultiCategoryRankSetter`) calling 13 system-specific setters, and read through
two more switches (`Ranking.getRanking`, `AthleteSorter.getRank`) calling the
matching getters. Nothing keeps each setter/getter pair consistent (the cause of
the missing best-athlete ranks fixed in `PAthlete`), and every new scoring system
requires edits in all switches plus new `Athlete` fields and `PAthlete`
overrides. The stored values on `Athlete` are competition-wide ranks, which have
no meaning once there is more than one championship (principle 1).

### Storage

- A single `bestAthleteRank` field on `Participation`, stored like the category
  ranks. A participation belongs to one championship, which has one best-athlete
  system, so one value suffices; two championships using the same system (for
  example Masters and Military Masters with GAMX-M) never collide because each
  participation holds the rank of its own championship.
- A reporting `PAthlete` wraps its own copy of the participation (the copy
  constructor copies the ranks, and must copy `bestAthleteRank`). It
  therefore starts with the stored rank; when a report re-ranks
  (operator-selected system, unfinished categories excluded, winners only, no
  championship), it writes the copy, never the original.
- Live re-ranking (Part 1b) writes the original participation.

### Accessors

- One write and one read: `setBestAthleteRank(int)` / `getBestAthleteRank()`.
  `Athlete.getBestAthleteRank()` returns the main participation's rank
  (the main participation is the registration-category participation,
  `Athlete.computeMainRankings`); `PAthlete` reads and writes its own
  participation copy.
- The rank setter and `Ranking.getRanking` / `AthleteSorter.getRank` use the
  single pair for every scoring system, with no per-system switch; medal ranks
  (Snatch, Clean and Jerk, Total, Custom, category score) keep reading the
  participation.
- Per-system rank accessors (`sinclairRank`, `catSinclairRank`, `robiRank`,
  `gamxRank`, `gamxMRank`, `smhfRank`, `qPointsRank`, `qMastersRank`,
  `qYouthRank`, …) become deprecated aliases that all return
  `getBestAthleteRank()`:
  - On a reporting `PAthlete`, its best-athlete rank, computed for
    that report with the system selected in the dropdown. For a meet director
    this is the only meaningful value; `robiRank` and `sinclairRank` (documented
    in `docs/100-AdvancedTopics/110-TemplateVariables.md`) and `catSinclairRank`
    keep working in custom templates.
  - On a plain athlete, the best-athlete rank of the main participation, i.e.
    the live rank within the registration category's championship (Part 1b). The
    earlier idea of forwarding them to the championship of type DEFAULT is
    abandoned.
- Their Java callers disappear: the two switches above, `AthleteDTO.fromAthlete`
  (export removed, see below), `Athlete.copy`, and the pass-through delegations
  in `XAthlete`. The per-system setters are deleted (the deprecated columns are
  mapped by field).
- The `MultiCategoryRankSetter` best-athlete branches are deleted (its callers
  only pass medal rankings; to be confirmed while implementing).
- Renames: `OverallRankSetter` → `BestAthleteRankSetter`,
  `assignOverallRanksAndPoints` → `assignBestAthleteRanks`.

### One rank per athlete per championship

An athlete with two participations in the same championship (for example
overlapping U11 and U13 age groups in a children's championship) must occupy a
single place in that championship's best-athlete ranking. Today
`Competition.globalRankingKey` keys by athlete + category, so such an athlete
appears twice (with equal scores, since the best-athlete score does not depend
on the participation) and takes two places.

- The best-athlete ranking comparators (`WinningOrderComparator`, for example
  `compareGamxMResultOrder`) only compare gender, score and body weight; a final
  tie-break on the underlying athlete identity is added so that the same
  athlete's reporting `PAthlete`s are always contiguous.
- `assignBestAthleteRanks` gives a reporting `PAthlete` whose underlying athlete
  equals the previous one's the same rank, without incrementing. Reporting
  writes only the private participation copies; live recomputation (Part 1b)
  writes that rank to every original participation of the athlete in the
  championship.
- `mBest` / `wBest` keep a single reporting `PAthlete` per athlete: the
  duplicates are dropped after ranking, so the best-athlete pages list each
  athlete once.

### Deprecated `Athlete` fields

The persisted `Athlete` rank fields (`sinclairRank`, `catSinclairRank`,
`catQPointsRank`, `catGAMXRank`, `robiRank`, `smhfRank`, `qPointsRank`,
`qAgeRank`, `ageAdjustedTotalRank`, `gamxRank`, `gamxMRank`, `gamxURank`,
`gamxARank`) are marked `@Deprecated` (with a Javadoc pointer to
`Participation.bestAthleteRank`). Since they are no longer computed, they are
neither exported nor imported:

- V2 export/import and the tracker `DATABASE` payload go through `AthleteDTO`.
  Its best-athlete rank fields (`sinclairRank`, `qPointsRank`, `qMastersRank`,
  `smhfRank`, `catSinclairRank`, `catQPointsRank`, `gamxRank`, `robiRank`,
  `ageAdjustedTotalRank`) are removed from `fromAthlete` / `toAthlete` and the
  DTO. Older files that still contain them load unchanged because the V2 mapper
  disables `FAIL_ON_UNKNOWN_PROPERTIES`. No tracker or tracker-core code reads
  these athlete-level fields.
- The legacy V1 path (`CompetitionData`) serializes `Athlete` directly. The
  remaining rank fields that are not yet `@JsonIgnore` (`ageAdjustedTotalRank`,
  `catSinclairRank`, `catQPointsRank`, `catGAMXRank`, `gamxRank`, `gamxMRank`,
  `gamxURank`, `gamxARank`) receive `@JsonIgnore`, like `sinclairRank`,
  `qPointsRank`, `qAgeRank`, `robiRank` and `smhfRank` already have.
- Team ranks (`teamSinclairRank`, …) are out of scope.

### Template lists

Decision: the only best-athlete lists are `mBest` / `wBest`. They contain the
athletes shown (selected championship, or all athletes when none is selected),
ranked once with the system selected in the best-athlete dropdown, on
reporting `PAthlete`s of their own. Every rank name on these (`bestLifterRank`, `sinclairRank`,
`smmRank`, `qAgeRank`, `catSinclairRank`, …) returns that rank. `bestRankingTitle`
gives the system name. These are the lists to use in new templates.

The per-system best-athlete lists (`m`/`w`/`mw` + reporting name of a scoring
system: `mSmm`, `mQMasters`, `mCatSinclair`, `mGamx…`, `mRobi`, …) are no longer
produced. Medal and team lists (`mTot`, `mSn`, `mCJ`, `mCustom`, `mCombined`,
`mTeam`, `mClubs`, …) are unaffected.

A scan of 476 template files (owlcms 68/69 resources, `usaw-owlcms`, `usaw`,
`meets`) found per-system list loops in only two templates:

- Quebec `qc/Qc_JduQ_fr_CA.xlsx` and `qc/Qc_Juvenile_fr_CA.xlsx`
  (`mCatSinclair` / `wCatSinclair`): fixed to use `mBest` / `wBest`,
  `bestAthleteRank`, `bestLifterScore` and `bestRankingTitle`. The dropdown
  normally selects CAT_SINCLAIR, but these templates follow an operator's
  alternative selection too. Their independent Sinclair comparison score
  and existing layout are preserved. A JXLS regression test renders both
  templates with CAT_SINCLAIR and BW_SINCLAIR, checks both genders' ranks,
  scores and headings, and covers empty best-athlete lists.
- USAW `local/templates/competitionBook/Masters.xlsx` (JXLS1 format): deferred,
  see Deferred Work.

Code consequences:

- `Competition.doReporting` / `doMixedReporting` (via `doGlobalRankings` and
  `categoryRankings` for ROBI) stop building per-system best-athlete lists, and
  stop setting `mBest` / `wBest` sorted by the template's medal system. Ranking
  the same reporting `PAthlete`s successively under every enabled system is no
  longer done.
- `mBest` / `wBest` are built by the workbook from the participations shown, for
  the selected system: `JXLSCompetitionBook` (no longer looking up
  `reportingBeans.get(system.getMReportingName())`) and `JXLSWinningSheet`, which
  today only sorts plain athletes and does not assign ranks; both use the same
  builder and produce ranked reporting `PAthlete`s.
- With no championship selected (principle 5), the same report-time ranking is
  applied to all the athletes shown, one reporting `PAthlete` per athlete, with the selected
  system. Today `Competition.doGlobalRankings` (non-participation-scoped path)
  feeds plain `Athlete` objects, so `JXLSCompetitionBook.copyReportRow` passes
  them through and the ranks are written into the in-memory athlete's rank
  fields. This path produces reporting `PAthlete`s instead, so that it no
  longer depends on the deprecated athlete rank fields.
- The top best-athletes display (`TopSinclairPage` with no championship,
  `TopSinclair.doUpdate`) no longer reads `Competition.getGlobalRanking` /
  `getGlobalScoreRanking`; it builds its list on demand for the selected system
  over the athletes shown, as `TopSinclairPage.getChampionshipRanking` already
  does for a selected championship.
- `Championship.of(null).getBestAthleteScoringSystem()` and
  `Competition.getBestAthleteScoringSystem()` are no longer used as a stand-in
  for a missing championship when ranking. Callers resolve the athlete's or the
  selected championship, or use the operator's explicit choice (principle 5).
- `docs/100-AdvancedTopics/110-TemplateVariables.md` documents `mBest`, `wBest`,
  `bestLifterRank`, `bestLifterScore` and `bestRankingTitle`, and marks the
  per-system rank names as deprecated aliases.

Template documentation is updated: it also documents the preferred
`bestAthleteRank` accessor, distinguishes live-computed scores from list ranks,
and explicitly lists all 20 deprecated per-system rank getters. The getters
remain in place pending the maintainer's decision on removal; neither
`bestLifterRank` nor `bestLifterScore` is deprecated.

### Best-athlete scoring selector (results and final package pages)

The "Score for best athlete" dropdown on the final package page
(`PackageContent`) always has a value; the empty value is removed (no clear
button, no `null` assignment).

- Selecting a championship sets the dropdown to that championship's best-athlete
  system (`PackageContent.onChampionshipChanged`, already the case).
- Clearing the championship filter keeps the current selection.
- When the page opens with no championship selected, the dropdown is set to the
  competition template's best-athlete system (the competition default). This is
  only an initial value for the selector, not a ranking computed over the
  template (principle 2).
- The operator can then change it freely (principle 5).

Consequences:

- The grid score (`SessionResultsContent.computeScore`) and the workbook
  (`JXLSCompetitionBook`, `JXLSWinningSheet` through `setBestLifterScoringSystem`)
  always receive the same explicit system, so the grid and the generated files
  always agree.
- The per-athlete fallback for an empty selection (`PackageContent.getScoringSystem`
  returning `null` when several championships are in use, and
  `computeScore` resolving each athlete's own championship) is no longer reached
  from this page.
- `JXLSCompetitionBook.resolveBestAthleteScoringSystem` and
  `JXLSWinningSheet` always receive an explicit system from this page, so their
  `Championship.of(null)` fallback is not reached from it.
- The session results page (`SessionResultsContent`) has the same dropdown
  without a clear button; its initial value already comes from the competition
  template, consistent with this rule.

### Part 1 acceptance criteria

These criteria apply after the Part 1b cutover.

- The final package and category results best-athlete pages show the rank for
  the selected system (regression covered by
  `AthleteSorterTest.pAthleteBestLifterRankUsesReportRowRankForAllScoringSystems`).
- `mBest` / `wBest` are the only best-athlete lists; their reporting
  `PAthlete`s carry ranks for the selected system in both the final package and
  the category results workbooks, and every per-system rank name on them
  returns that rank.
- Two championships using the same system (for example Masters and Military
  Masters with GAMX-M) produce independent ranks for the same athlete.
- No ranking is computed over the competition template.
- On the final package page, the best-athlete dropdown is never empty; selecting
  a championship sets it to that championship's best-athlete system, clearing the
  championship keeps it, and the grid score and the generated package use the
  same system.
- The Quebec templates produce correct CAT_SINCLAIR best-athlete pages from
  `mBest` / `wBest`.
- An athlete with two participations in the same championship appears once in
  `mBest` / `wBest`, with one rank, and the following athlete's rank is not
  shifted.
- No code outside the deprecated `Athlete` fields themselves reads or writes
  those fields; V2 export omits them and import ignores them.

### Part 1b: Switch readers and writers, including live ranking

Implemented and verified. `ChampionshipTest` now exercises the actual
`AthleteRepository.save` path, not just manually assigned participation ranks:

- A saved lift changes ranks across sessions and weight categories in the
  same championship.
- One athlete has independent ranks in two championships using the same
  system (first in Junior and second in Senior).
- Unaffected championships and the opposite gender are not reranked; test
  sentinel ranks detect even an unnecessary recomputation with unchanged order.
- Saving a detached athlete with an older rank preserves the newer stored
  ranks when editing a non-ranking field.

The tests restore the fixture's athletes, scoring settings and ranks.
Validation: all 48 `ChampionshipTest` tests and all 348 `AllTests` tests pass,
with no failures, errors or skipped tests. The Quebec-template regression
also runs in `AllTests`. No production changes were needed to make the new
save-and-recompute tests pass.

Depends on Part 1a. Switch the ranking writers and their readers together:

- Activate per-championship live ranking and populate the new participation
  ranks for existing competition data before exposing them to live readers.
- Switch the rank helpers, reporting builders and deprecated accessor aliases
  to the new participation storage.
- Remove the old meet-wide writer and per-system setters in the same cutover.
  Apply the template, selector and export/import changes described above.
- Do not leave an intermediate runtime in which the new accessors read
  unpopulated ranks or the old meet-wide writer writes championship ranks.

This supplies live best-athlete ranks before Part 2 changes the scoreboard
columns. The former separate live-ranking part is included here.

#### Live ranking

- The championship comes from the registration category
  (`Athlete.category` → age group → championship) for scoreboards.
- The system is that championship's best-athlete system.
- The field is that championship's participants of the same gender.
- The rank is written to `Participation.bestAthleteRank` of each participant;
  the scoreboard reads `athlete.getMainRankings().getBestAthleteRank()`, which
  is the rank in the registration category's championship with no further
  lookup, published as `sinclairRank`.
- Live recomputation does only the necessary work, in two steps:
  1. Re-ranking. A lift changes only the lifter's score, so after a lift only
     the championships in which the lifter has a participation are re-ranked,
     for the lifter's gender only. The ranking input for each such championship
     is all its participants across all sessions and platforms; the existing
     `AthleteRepository.findAthletesForGlobalRanking(group)` query returns only
     the categories present in the session, which is enough for category ranks
     but not for a championship, so a per-championship query is used for this
     step. Ranks in other championships cannot change and are not recomputed.
     Weigh-in, body weight, category and eligibility changes trigger the same
     targeted re-ranking for the athlete's championships.
  2. Display. The scoreboard keeps querying its own session as today and reads
     the stored rank from each athlete's main participation.
- Example: in one bodyweight category with one P15 and one P17 athlete in
  different championships, each has their own GAMX score and each is ranked 1.
- Example: two platforms, platform A running Open (GAMX) and platform B running
  Masters (GAMX-M). A lift on A re-ranks only Open for the lifter's gender and
  writes Open participations; a lift on B does the same for Masters. The two
  re-rankings never touch the same participations and can run concurrently.
  Each scoreboard shows the ranks of its own championship with its own system
  name as header; a Masters session on a third platform is included in B's
  ranks. Today both platforms run the same competition-wide refresh with the
  competition medal system, so one of the two championships never gets a rank.

Removals:

- `Competition.scoringSystemRankings` (called from
  `FieldOfPlay.updateScoringSystemRanking`), which ranks the whole meet with the
  medal system, and `Competition.recomputeAllAthleteRanks`, which does the same
  in bulk.
- The competition-wide lists built by `Competition.computeReportingInfo()` with
  no championship, as a ranking maintained by the system (`getGlobalRanking` /
  `getGlobalScoreRanking`). They remain available only as the report-time "all
  athletes shown" view of principle 5, built on reporting `PAthlete`s.
- The gating of the live refresh on `isDisplayScoreRanks()` / `isDisplayScores()`
  in `FieldOfPlay.updateScoringSystemRanking`: the per-championship re-ranking
  runs whenever a best-athlete system is in use for the lifter's championships.

#### Live-ranking acceptance criteria

- `sinclairRank` for an athlete equals their rank within the championship of
  their registration category, using that championship's best-athlete system,
  and is refreshed after each lift, including when other participants of that
  championship lift in another session or on another platform.
- After a lift, only the lifter's championships (and gender) are re-ranked.
- Two platforms running different championships with different best-athlete
  systems each show correct ranks for their own championship.
- No competition-wide rank is computed or stored.

---

## Part 2: Scoreboards

Depends on completed Part 1 (1a and 1b). Live championship ranks are already
available; this part separates the medal and best-athlete display columns.

### Layout

```text
… | total | medal score | medal rank | sinclair | sinclairRank
```

- `total`: always shown.
- Medal score (new column): `categoryScore` of the registration-category
  participation. Shown when at least one displayed registration category medals
  by score; blank on rows whose category medals by weight. The header is the
  medal system name when all such rows use the same system, otherwise a generic
  "Score".
- Medal rank (new `medalRank` field, replacing the `totalRank` column):
  `categoryScoreRank` of the registration-category participation, for every row.
  Always shown. The kg total rank (`totalRank`) is still sent but is not a
  scoreboard column.
- Medal highlight (`medalHighlight`, name proposed): the highlight for the
  category medal, whether awarded by weight or by score, computed without
  branching on the medal system:

  ```text
  medalHighlight = medalPolicy.includesTotal() && isMedalist(athlete, CATEGORY_SCORE)
                   ? "medal" + categoryScoreRank : ""
  ```

  A score medal system implies a Total-only medal policy
  (`Championship.resolveMedalScoringSystem` forces TOTAL when Snatch and Clean
  and Jerk medals are enabled), so `includesTotal()` only excludes the
  lifts-only policy. `snatchMedal` and `cleanJerkMedal` are unchanged.
  Medal withholding rules must apply to `CATEGORY_SCORE` exactly as they do to
  `TOTAL`: `AthleteSorter.isMedalist` currently applies the IMWA qualifying-total
  withholding only for `TOTAL`, and the "QT not met" policy of
  [Championship Qualification and Scoring Policies](championship-qualification-policies.md)
  (`RANKS_WITHOUT_MEDALS`) must withhold the highlight while keeping the rank.
- `sinclair` / `sinclairRank`: best-athlete score and rank for the championship
  of the registration category (`Athlete.getBestAthleteRank()`, Part 1). Hidden
  by default. Never medal-highlighted.

A single shared medal-rank column is used deliberately: sessions that mix
weight-medal and score-medal categories must not show two rank columns.

### Scenarios

Snatch and Clean and Jerk kg rank columns follow the **medal policy** of the
championships on display, not the medal system: they are shown when at least
one displayed championship awards lift medals (`Lifts and Total` or
`Lifts only`). Displayed championships may have conflicting policies; the board
shows the union of what they need, so one three-medal championship is enough to
show the lift rank columns for every row. A score medal system
(`Championship.resolveMedalScoringSystem`) forces a Total-only policy, so a
session of only Q-Masters or GAMX-U championships has no lift rank columns.

Where lift ranks are shown, an athlete can be fifth by kg lifted but first by
the category's medal score; these ranks measure different things. The medal
policy alone determines whether the kg lift-rank cells receive medal highlights.

Mixed medal systems coexist in a session, never within one registration
category. The validation fixtures use separate athletes and categories:
`F 64` for Total medals and `W35Q 64` for Q-Masters medals (the W35Q category
has a 0-999 kg bodyweight range), and a separate youth category for GAMX-U.
When there is one athlete in each category, both athletes have medal rank 1;
they are not ranked against one another. Category-grouped boards must show
separate category sections.

| Session | Sn/CJ ranks | Total | Medal score | Medal rank | Best athlete |
| --- | --- | --- | --- | --- | --- |
| Only Q-Masters medals | Hidden (Total-only policy) | Shown | Shown, header "Q-Masters" | Q-Masters rank within the category | Hidden by default |
| Only Total medals | Per medal policy: shown for three-medal, hidden for Total-only | Shown | Hidden | Total rank within the category | Hidden by default |
| Q-Masters + GAMX-U medals | Hidden (both Total-only) | Shown | Shown, header "Score"; each row shows its own system's value | Each row's score rank within its category | Hidden by default |
| Total + Q-Masters medals | Per the Total championship's policy; when shown, kg ranks appear on both category types | Shown | Shown, header "Q-Masters"; blank on Total rows | Total rows: total rank. Q-Masters rows: Q-Masters rank | Hidden by default |

Compared with today: in score-medal sessions the total-rank column is no longer
hidden, the medal rank is the category rank (not the stored competition-wide
rank in the medal system), and the score moves out of the `sinclair` columns
into the medal-score column.

### Controls

All controls are global feature switches shown in the "Scoreboard options"
section of the Language and System Settings page; there is no per-display or
URL option.

| Switch | Effect |
| --- | --- |
| `displayBestScore` | Shows the `sinclair` (best-athlete score) column |
| `displayBestScoreRank` | Shows the `sinclairRank` column |
| `noBestScoreRank` / `noSinclairRank` | Force the `sinclairRank` column off; takes precedence over `displayBestScoreRank`. Kept as an override. |
| `displayBodyWeight` | Unrelated to ranks: shows the body weight (2 decimals) in its own `bodyWeight` column, placed after Birth, with a "B.W." header. It no longer repurposes `custom1`; the `custom1`/`custom2` columns are again controlled only by the stylesheet variables (`--custom1Width`, `--custom1Visibility`). |

Changes:

- The best-athlete columns no longer turn on automatically when a displayed
  category medals by score (that was their medal use, now moved to the
  medal-score column).
- `Competition.displayScores` / `displayScoreRanks` (editor checkboxes commented
  out, values only reachable from old databases or imports) are dropped from the
  visibility logic; only the feature switches apply.

### Field names

Every field has one meaning everywhere (scoreboard JSON, websocket payloads,
`DATABASE` participations, templates). No alias is kept: the tracker is fixed
in Part 3 rather than kept compatible.

| Field | Meaning | Status |
| --- | --- | --- |
| `totalRank` | kg total rank within the category | Unchanged; no longer a scoreboard column |
| `medalRank` | Category medal rank (`categoryScoreRank`); equals `totalRank` when medals are by weight | New |
| `medalScore` | Category medal score (`categoryScore`) | New |
| `medalHighlight` | Medal highlight class for `medalRank` (name proposed) | New, replaces `totalMedal` and `sinclairMedal` |
| `totalMedal` | Previously the highlight of the kg total rank column | No longer set |
| `sinclairMedal` | Previously the score-medal highlight on `ResultsMedals` / `ResultsRankings` | No longer set |
| `sinclair` | Best-athlete score | Meaning restricted to best athlete |
| `sinclairRank` | Best-athlete rank within the championship | Meaning restricted to best athlete |

Templates: `l.totalRank` stays the kg total rank; `l.medalRank` / `l.medalScore`
are added as the names of `l.categoryScoreRank` / `l.categoryScore`. The shipped
category results templates print `l.medalRank` next to the total so that
score-medal categories show the rank that matches the medals.

### Code changes

- `BaseResults.getAthleteJson`, `ResultsMultiRanks`, `ResultsMedals`,
  `ResultsRankings.applyMedalClasses`, `CurrentAthlete`: emit `totalRank`
  (kg), `medalRank`, `medalScore`, `medalHighlight` as above; emit
  `sinclair` / `sinclairRank` from the best-athlete score and
  `Athlete.getBestAthleteRank()`; never set `sinclairMedal`.
- Remove the medal-system branching in `BaseResults.computedScore` /
  `computedScoreRank` and the reading of competition-wide `Athlete` ranks.
- Column visibility (`BaseResults`, `showSinclair`, `showSinclairRank`, new
  `showMedalScore`) per the Controls section; the owlcms medal-rank column is
  always shown and no longer uses `showTotalRank`.
- Front-end (`Results.js`, `renderResultsAthleteRow.js`, `ResultsMulti.js`,
  `ResultsMedals.js`, `ResultsRankingsByCategory.js`, `ResultsStartList.js`):
  the rank column after the total renders `medalRank` with `medalHighlight`;
  add the medal-score column; `sinclairRank` cell loses the `sinclairMedal`
  class.
- Forwarders (`EventForwarder`, `WebSocketEventForwarder`,
  `websocket/AthleteExporter`): emit the same fields and flags as the
  scoreboards (`totalRank`, `medalRank`, `medalScore`, `medalHighlight`,
  `sinclair`, `sinclairRank`, `showMedalScore`, `showSinclair`,
  `showSinclairRank`, `showLiftRanks`); remove their copies of the medal-system
  branching in `computedScore` / `computedScoreRank`. `showTotalRank` keeps
  being sent with today's meaning (`false` when every displayed category medals
  by score) because the tracker regular scoreboards still key their kg rank
  column on it (Part 3).
- `ParticipationDTO` (`DATABASE` payload) gains `categoryScoreRank`,
  `categoryScore` and `bestAthleteRank` (Part 1), alongside the unchanged
  `snatchRank`, `cleanJerkRank`, `totalRank`.
- `AthleteSorter.isMedalist`: apply the IMWA qualifying-total withholding to
  `CATEGORY_SCORE` as to `TOTAL`.

### Team points

Team points follow the medal rank, never a mix. `Participation.getTotalPoints`
(and `AthleteSorter.imwaPointsFormula`) feed `pointsFormula` with
`categoryScoreRank` instead of `totalRank`. For weight-medal championships the
two are equal, so IWF-style meets are unchanged; for score-medal championships
the points now follow the medals. Snatch and Clean and Jerk points are
unchanged.

### Part 2 acceptance criteria

- `totalRank` is the kg total rank in every category and every payload.
- Snatch and Clean and Jerk kg ranks are always visible, regardless of the
  medal scheme. Category-boundary spacers are retained.
- In a weight-medal category, `medalRank` equals `totalRank`.
- In a score-medal category, `medalRank` equals `categoryScoreRank`, and the
  medal-score column shows `categoryScore`.
- In a session mixing both, one medal-rank column is shown, and the medal-score
  column is blank on weight-medal rows.
- `medalHighlight` highlights the top three `categoryScoreRank` values for both
  weight and score medals, is empty under a lifts-only medal policy, and honours
  the IMWA qualifying-total withholding and the `RANKS_WITHOUT_MEDALS` policy for
  both.
- `sinclairMedal` and `totalMedal` are not emitted.
- The `sinclair` / `sinclairRank` columns appear only when their feature switches
  are on, never because a category medals by score.
- In a score-medal championship, team points are computed from the medal rank
  (`categoryScoreRank`); in a weight-medal championship they are unchanged.

---

## Part 3: Tracker Fixes

Depends on Part 2 (owlcms emits the new fields). Goal: the tracker scoreboards
work like the owlcms scoreboards, and the tracker team scoreboards and books use
the medal rank.

Current state: no tracker scoreboard handles score-medal championships today.

- The standard scoreboards (session results, lifting order, rankings, all
  through `AthletesGrid.svelte`) render only `snatchRank`, `cleanJerkRank` and
  `totalRank` from the live payload; `sinclair` / `sinclairRank` are dropped by
  `stripToDisplayFields`; there is no notion of a medal system. In a score-medal
  session owlcms sends `showTotalRank=false`, so they show no rank at all and
  sort within a category by kg total rank.
- The team scoreboard plugin has two modes. The "team score" mode sums the
  athletes' scores (Sinclair, SMHF, Q-points, GAMX, … chosen by the
  `scoringSystem` option); it uses the score formulas and is unaffected by
  medals. The "TeamPoints" mode is rank-based and takes `totalRank` from owlcms
  (live athlete on one path, `DATABASE` participation on another), so it only
  knows weight medals.
- The books (`iwf-results`, `iwf-startbook`) rank and colour medals from the
  `DATABASE` participation `totalRank`.
- The `DATABASE` payload carries no category score rank, so none of the above
  can use the medal rank today. The score formulas exist in the tracker but have
  never been used to rank; score-medal support is new work, not an adjustment.

tracker-core:

- `docs/WEBSOCKET_MESSAGE_SPEC.md` and `docs/API_REFERENCE.md`: `totalRank` is
  the kg total rank; document `medalRank`, `medalScore`, `medalHighlight`,
  `showMedalScore`; `totalMedal` is no longer sent; `showTotalRank` keeps its
  current meaning;
  `sinclair` / `sinclairRank` are the best-athlete score and rank within the
  championship; `DATABASE` participations carry `categoryScoreRank`,
  `categoryScore`, `bestAthleteRank`.
- `src/protocol/parser-v2.js`, `src/competition-hub.js`: pass the new fields and
  flags through.

tracker:

- Regular scoreboards (`src/lib/server/standard-scoreboard-helpers.js`,
  `src/lib/components/AthletesGrid.svelte`, `src/plugins/scoreboards/rankings`):
  **out of scope for now; they do not do score-based medals.** They keep
  showing the kg `totalRank`, hidden when `showTotalRank` is `false`, i.e. in a
  score-medal session they show no rank, as today. When they are taken up, the
  target is the owlcms layout: the rank column after the total renders
  `medalRank` with `medalHighlight` (always shown), a medal-score column driven
  by `showMedalScore`, within-category sort by `medalRank`, and `sinclair` /
  `sinclairRank` columns driven by `showSinclair` / `showSinclairRank`.
- Team scoreboards (`src/plugins/teams/team-scoreboard`, `team-rankings`),
  "TeamPoints" mode (the "team score" mode is unchanged):
  they must not use owlcms-precomputed ranks for team points. A new lift value
  (live, or a predicted total "if the next attempt succeeds") invalidates the
  ranks owlcms sent, and the `DATABASE` copies of athletes in other sessions can
  be stale between pushes. They therefore recompute the medal ranks locally:
  group participations by category; compute each athlete's medal value with the
  championship's medal system (`ChampionshipDTO.scoringSystem`: the total for
  weight medals, otherwise the score formula the tracker already has:
  Sinclair, SMHF, Q-points, Q-Masters, GAMX variants) from the most recent lift
  values (live session, else database) or from the predicted total; sort and
  assign ranks within the category (ties broken by body weight, as owlcms does);
  derive team points with the championship's medal policy and team-points
  values. This is the same computation owlcms performs in `computeMedals`, so
  the "actual" team points match owlcms once lifts are settled, and the
  "predicted" points use the same rule.
- Owlcms-sent `medalRank` / `categoryScoreRank` are used by the tracker only for
  display of settled ranks (standard scoreboards, books), never as an input to
  team points on the team scoreboards.
- Books (`iwf-results`, `iwf-startbook`): medal colouring and medal ranking from
  `categoryScoreRank`; the kg `totalRank` column stays the kg rank.
- No fallback to `totalRank` as a medal rank: the tracker version that
  implements Part 3 requires the owlcms version that implements Part 2.

### Part 3 acceptance criteria

- Tracker regular scoreboards are unchanged: kg `totalRank` shown in
  weight-medal sessions, no rank in score-medal sessions.
- Tracker team points equal owlcms team points for a score-medal championship
  once lifts are settled, and predicted team points are computed with the same
  medal rule from predicted totals.
- Books colour medals from the medal rank in a score-medal championship.

---

## Open Items

None.

## Deferred Work

- USAW `local/templates/competitionBook/Masters.xlsx` (JXLS1 format): its
  Q-Masters (`MQAGE` / `WQAGE`) and SMHF (`MSMF` / `WSMF`) sheets all loop over
  `mSmm` / `wSmm`, so the Q-Masters sheets are in SMHF order with Q-Masters
  ranks. The file is probably not used by USAW. If it is kept, it will be fixed
  to a correct best-athlete page per gender looping over `mBest` / `wBest`
  (`bestLifterScore`, `bestLifterRank`, `bestRankingTitle`), the system coming
  from the dropdown. Handled separately from this plan.
