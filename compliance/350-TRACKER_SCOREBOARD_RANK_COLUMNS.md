# Tracker Scoreboards: Rank and Score Columns

This item spans OWLCMS (the forwarder) and the tracker (`owlcms-tracker`, repository
`tracker`). The tracker scoreboards must show the same rank and score columns as the
OWLCMS scoreboards, which follow the championships of the athletes' registration
categories.

## Context

Reported: tracker scoreboards show the Snatch and Clean & Jerk rank columns even when
every championship of the displayed registration categories awards medals for the
Total only.

### OWLCMS scoreboards

`BaseResults.setRankColumns` calls `ScoreboardRankData.columns(championships, ...)`
with the championships of the displayed athletes' registration categories (secondary
participations do not count):

- `liftRanks`: at least one championship awards Snatch and Clean & Jerk medals
  (`Championship.isSnatchCJTotalMedals()`, medal policy `ALL_THREE` or `LIFTS_ONLY`).
  A score medal system forces `TOTAL_ONLY`, so score championships never turn it on.
- `medalScore`: at least one championship awards medals by a score (scoring system
  other than `TOTAL`).
- `medalSystems`: the score systems in use; `ScoreboardRankData.title` turns them into
  the column title (the system name when there is one, "Score" otherwise).

The board then shows `Total | Score (when medalScore) | Rank`. Rank is always shown and
holds the **medal rank** (`Participation.getCategoryScoreRank()`): the Total rank for a
Total-medal championship, the score rank for a score-medal championship. Per athlete,
`ScoreboardRankData.fields` provides `medalScore` (empty for Total-medal athletes) and
`medalRank`.

### What the forwarder sends

`WebSocketEventForwarder` (V2, current tracker) and `EventForwarder` (legacy HTTP)
compute the same `ScoreboardRankData.columns` from
`FieldOfPlay.getScoreboardChampionships()` (registration-category based), but:

- `setShowLiftRanks(true)` is hard-coded; `columns.liftRanks()` is ignored. **This is
  the reported bug.**
- `showTotalRank` is `false` only when every displayed championship awards score
  medals (`scoreOnly`).
- `showMedalScore`, `medalScoringName`, `showSinclair`, `showSinclairRank` and
  `scoringName` are sent correctly.

Per athlete, `AthleteExporter.buildDisplayInfo` already adds `ScoreboardRankData.fields`
to `displayInfo`: `snatchRank`, `cleanJerkRank`, `totalRank`, `medalRank`,
`medalScore`, `medalHighlight` (always empty, highlighting off), `sinclair`,
`sinclairRank`. Leaders use the same exporter (`exportLeaderEntries`), so leader rows
carry the same fields.

### What the tracker shows

`src/lib/components/AthletesGrid.svelte` is shared by the start-order, lifting-order
and rankings scoreboards (`src/lib/server/standard-scoreboard-helpers.js`):

- Snatch and C&J rank columns follow `showLiftRanks`, for athlete and leader rows.
- A Total rank column follows `showTotalRank` and shows `totalRank`.
- No Score column. `stripToDisplayFields` drops `medalScore` and `medalRank`;
  `extractDisplaySettings` does not read `showMedalScore` or `medalScoringName`.
- The rankings scoreboard (`sortStrategy: 'byRank'`) sorts each category by
  `snatchRank` before C&J has started and by `totalRank` after.

So on score-medal championships the tracker shows the wrong rank (mixed sessions) or no
rank at all (score-only sessions), and never the score.

## Target behavior

Same columns as the OWLCMS scoreboard. Rows are the displayed session's registration
categories:

| Championships in the session | Lift rank columns | Score column | Rank column holds |
| --- | --- | --- | --- |
| All Total-only | hidden | hidden | Total rank |
| Total-only and three-medal | shown | hidden | Total rank |
| All three-medal or lifts-only | shown | hidden | Total rank |
| All score | hidden | shown | score rank |
| Score and Total-only | hidden | shown, blank for Total-medal athletes | each athlete's medal rank |
| Score and three-medal | shown | shown, blank for Total-medal athletes | each athlete's medal rank |

A registration category belongs to a single championship, so within a category every
rank uses the same criterion.

## Work

### Step 1 — OWLCMS: send the lift-rank decision

In `WebSocketEventForwarder` and `EventForwarder` (the block that computes
`scoreboardChampionships`):

- compute `columns` before setting the flags;
- replace `setShowLiftRanks(true)` with `setShowLiftRanks(columns.liftRanks())`;
- keep `setShowTotalRank(!scoreOnly)` unchanged (see Compatibility);
- remove the commented-out `sinclair`/`ranks` lines above the block.

No new keys and no change to `AthleteExporter`.

### Step 2 — Tracker: carry the medal fields and flags

In `src/lib/server/standard-scoreboard-helpers.js`:

- `stripToDisplayFields`: keep `medalRank` and `medalScore`.
- `extractLeaders`: leader entries are already flattened from `displayInfo`; check
  that `medalRank` and `medalScore` survive.
- `extractDisplaySettings`:
  - remove the early `return {}` (it depends on string truthiness of two unrelated
    flags);
  - add `showMedalScore: fopUpdate.showMedalScore === 'true'`;
  - add `medalScoringName: fopUpdate.medalScoringName || ''`;
  - add `hasMedalRank`: true when the session athletes carry `medalRank` (a new
    OWLCMS), used for the fallback below.
- `headers`: add `score: competitionHub.translate('Score', lang)` as the fallback title
  when `medalScoringName` is empty.

The scoreboard cache key already includes the FOP state, so the new settings follow
updates without a cache change.

### Step 3 — Tracker: grid columns

In `src/lib/components/AthletesGrid.svelte`:

- Replace the hard-coded column numbers (`snatchRankCol`, `totalRankCol`, ...) with
  positions computed from the optional columns, in order: Snatch rank, C&J rank,
  Score, Rank.
- Add the Score column after Total when `showMedalScore`:
  - header: `displaySettings.medalScoringName || headers.score`, spanning the two
    header rows like Total;
  - cell: `athlete.medalScore || ''` (blank, not `-`, for Total-medal athletes, like
    OWLCMS);
  - leader rows get the same cell;
  - new CSS width variable `--col-score` (three decimals, e.g. `123.456`);
  - portrait header variant like `col-total-portrait`.
- Rank column:
  - when `hasMedalRank`: always shown, value `medalRank`;
  - otherwise (older OWLCMS): unchanged, follows `showTotalRank` and shows `totalRank`.
- Check `calculateGridLayout`: it counts rows only, so no change expected.
- Check the narrow/portrait layout rules that hide columns: Score must be hidden or
  kept consistently with Total.

### Step 4 — Tracker: rankings sort

In `sortByRank`, after C&J has started, sort each category by `medalRank` when present,
falling back to `totalRank`. Before C&J has started, keep the `snatchRank` order (the
only partial order available); check it against the OWLCMS rankings scoreboard. Use the
same field for `displayRank`.

### Step 5 — Other tracker consumers

Check that these do not rely on `showLiftRanks` being always true:

- `src/plugins/teams/team-scoreboard/helpers.data.js` (reads display settings);
- `src/lib/server/parser-v1.js` (legacy path, reads the flags);
- `src/plugins/attemptboards/attempt-bar/helpers.data.js` (uses the shared helpers but
  not the grid).

The IWF books compute medals from the championship's medal policy themselves
(`src/lib/medal-policy.js`) and are not affected.

## Compatibility

| OWLCMS | Tracker | Result |
| --- | --- | --- |
| new | old | Lift ranks hidden when no displayed championship awards lift medals; otherwise unchanged. Total rank and missing Score unchanged. Never removes a column that should be shown |
| old | new | `medalRank` absent: Rank column falls back to `showTotalRank`/`totalRank`, lift ranks shown as today. If `showMedalScore` is sent, the Score column appears |
| new | new | Target behavior |

`showTotalRank` keeps its meaning so that old trackers still hide a misleading Total
rank in score-only sessions. The new tracker ignores it when `medalRank` is present.

## Tests

OWLCMS:

- `ScoreboardRankDataTest` already covers `columns().liftRanks()`. Add a forwarder-level
  check only if the flag computation is extracted from the forwarders into a testable
  method (e.g. a small `ScoreboardRankData` helper returning `showLiftRanks` and
  `showTotalRank`), so both forwarders share it.

Tracker (`vitest`, `tests/unit/`):

- `extractDisplaySettings` with each flag combination, including missing keys.
- `stripToDisplayFields` keeps `medalRank`/`medalScore`.
- `sortByRank`: mixed categories (one Total, one score) after C&J has started, sorted
  by `medalRank`; fallback to `totalRank` when `medalRank` is absent.
- Grid column computation: extract the column-position logic from the Svelte component
  into a plain function so it can be tested for the six session types of the target
  table.

Visual check (both new), one session per row of the target table, on the start-order,
lifting-order and rankings scoreboards, in landscape and portrait; compare with the
OWLCMS scoreboard for the same session.

## To decide when implementing

- Whether the tracker also shows the best-athlete score and its rank (`showSinclair`,
  `showSinclairRank`, `scoringName`, per-athlete `sinclair` and `sinclairRank`), which
  OWLCMS shows behind feature switches. Same mechanism as the Score column; not needed
  for the reported problem.
- Whether medal highlighting (`medalHighlight`) is enabled for the tracker. OWLCMS
  currently exports it empty.
