# dev67_1 Merge Checklist - 2026-06-26

## Current State

- Branch: `dev67_1`
- Base: `dev67` (`f9091ba3b`, `67.0.0-rc12`)
- Operation: merge `67_1_gamx` into `dev67_1`
- Merge status: conflicts resolved, merge not committed
- Safety references still available:
  - `safety/67_1_gamx-before-dev67-merge-recovery-20260626`
  - `stash@{0}: pre-dev67-merge-recovery-20260626`

## Decisions Already Made

- [x] Keep `dev67` for `AnnouncerContent.java`
- [x] Keep `dev67` for `DecisionElement.java`
- [x] Keep `dev67` for attempt-board CSS files
- [x] Keep `dev67` for release notes, `release.sh`, and `translation4.csv`
- [x] Keep `67_1_gamx` for GAMX migration/resource changes
- [x] Keep `67_1_gamx` for `TopSinclair` fixes
- [x] Keep `67_1_gamx` for `TopTeamsSinclair` fixes

## Verify Explicit `dev67` Winners

These should have no diff from `dev67`.

- [ ] `ReleaseNotes.md`
- [ ] `src/main/markdown/ReleaseNotes.md`
- [ ] `release.sh`
- [ ] `shared/src/main/resources/i18n/translation4.csv`
- [ ] `owlcms/src/main/java/app/owlcms/components/elements/DecisionElement.java`
- [ ] `owlcms/src/main/java/app/owlcms/nui/lifting/AnnouncerContent.java`
- [ ] `shared/src/main/resources/css/grid/attemptboard.css`
- [ ] `shared/src/main/resources/css/nogrid/attemptboard.css`
- [ ] `shared/src/main/resources/css/public/attemptboard.css`
- [ ] `shared/src/main/resources/css/transparent/attemptboard.css`

## Verify Explicit `67_1_gamx` Winners

These should have no diff from `67_1_gamx`.

### GAMX

- [ ] `docs/GAMX_NORMALIZATION_PLAN.md`
- [ ] `owlcms/scripts/gamx_generate_json.py`
- [ ] `owlcms/scripts/gamx-source/GAMX_CJ_Snatch.xlsx`
- [ ] `owlcms/scripts/gamx-source/GAMX_calculator_allages_current.xlsx`
- [ ] `owlcms/src/main/java/app/owlcms/data/scoring/GAMX2.java`
- [ ] `shared/src/main/java/app/owlcms/utils/GamxZipHelper.java`
- [ ] `owlcms/src/test/java/app/owlcms/data/scoring/GAMX2ComparisonTest.java`
- [ ] `owlcms/src/test/java/app/owlcms/data/scoring/GamxJsonParityTest.java`
- [ ] `owlcms/src/test/java/app/owlcms/data/scoring/GamxLiftValuesTest.java`
- [ ] `owlcms/src/main/resources/gamx/params-*.json`
- [ ] CSV params from `dev67` are removed: `owlcms/src/main/resources/gamx/params_*.csv`
- [ ] `owlcms/src/main/resources/gamx/gamx.xlsx` is removed

### Top Displays

- [ ] `owlcms/src/main/java/app/owlcms/displays/top/TopSinclair.java`
- [ ] `owlcms/src/main/java/app/owlcms/nui/displays/top/TopSinclairPage.java`
- [ ] `owlcms/src/main/java/app/owlcms/displays/top/TopTeamsSinclair.java`
- [ ] `owlcms/src/main/java/app/owlcms/nui/displays/top/TopTeamsSinclairPage.java`
- [ ] Confirm `TopTeamsSinclair.doUpdate(...)` does not return early when `fop == null`
- [ ] Confirm `TopTeamsSinclairPage.shouldRegisterPageOnUiEventBus()` returns `false`
- [ ] Confirm `TopTeamsSinclairPage` uses `Championship.resolveDisplayChampionship(ageDivisionName, true)`

## Review Remaining Staged Areas

These staged changes came from `67_1_gamx` and still need explicit review/decision.

### Timer Elements

- [x] `owlcms/src/main/java/app/owlcms/components/elements/AthleteTimerElement.java`
- [x] `owlcms/src/main/java/app/owlcms/components/elements/BreakTimerElement.java`
- [x] `owlcms/src/main/java/app/owlcms/components/elements/TimerElement.java`

### Team / Top Scoring Support

- [x] `owlcms/src/main/java/app/owlcms/data/team/TeamResultsDisplayRules.java`
- [x] `owlcms/src/main/java/app/owlcms/data/team/TeamResultsTreeData.java`
- [x] `owlcms/src/main/java/app/owlcms/data/team/TeamTreeItem.java`
- [x] `owlcms/src/main/java/app/owlcms/displays/top/TopTeams.java`
- [x] `owlcms/src/main/java/app/owlcms/nui/displays/top/TopTeamsPage.java`
- [x] `owlcms/src/main/java/app/owlcms/nui/results/TeamResultsContent.java`

### Scoring / Config Support

- [ ] `owlcms/src/main/java/app/owlcms/data/agegroup/Championship.java`
- [ ] `owlcms/src/main/java/app/owlcms/data/platform/Platform.java`
- [ ] `docs/FeatureToggles.md`
- [ ] `.vscode/launch.json`

### Other Behavior Changes

- [ ] `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`
- [ ] `owlcms/src/main/java/app/owlcms/monitors/WebSocketEventForwarder.java`
- [ ] `owlcms/src/main/java/app/owlcms/nui/displays/AbstractDisplayPage.java`
- [ ] `owlcms/src/main/java/app/owlcms/nui/lifting/TCContent.java`
- [ ] `owlcms/src/main/java/app/owlcms/nui/lifting/WodkeeperContent.java`
- [ ] `owlcms/src/main/java/app/owlcms/nui/shared/SafeEventBusRegistration.java`
- [ ] `owlcms/src/test/resources/testDatabases/NVF_TeamsDatabase_2026-01-07_08h13_en.json`
- [ ] `shared/src/main/resources/i18n/collar_threshold_translations.tsv`

### Attempt Board JavaScript

- [ ] `owlcms/src/main/frontend/components/AttemptBoard.js`

## 67_1_gamx Patch-ID Review

`git cherry -v dev67 67_1_gamx` reported these `+` patches as not patch-equivalent to `dev67`:

- [ ] `8c5fa6b85` migrate GAMX tables to JSON; enable pull from tracker
- [ ] `ac0b4d77` made unit tests pass for championships
- [ ] `1bc17032` records sanity check
- [ ] `0bd85b9c` perf: route championship reads through the Championship cache
- [ ] `4b454478` 67.1 release notes start - should not affect final result
- [ ] `546f1229` Fix birth date movement
- [ ] `97fc5a58` release: add skipTranslations option to release script - should not affect final result
- [ ] `e9c26e57` error messages formatting for records import
- [ ] `ad5ef563` release notes update - should not affect final result
- [ ] `39f3f88c` updated release notes - should not affect final result
- [ ] `e0dd5143` top points and top teams
- [ ] `38aa950f` for testing
- [ ] `7817bf81` prep beta
- [ ] `cce42d7a` collar threshold
- [ ] `6668e686` prep
- [ ] `f2a5ac9a` prepare future release
- [ ] `2ccba48b` sync release notes - should not affect final result
- [ ] `bfa57887` Batch decision display transitions through reactive payloads
- [ ] `ef1f5e91` translation for missing decision warning - `translation4.csv` should not affect final result

`-` entries in `git cherry` were already patch-equivalent to `dev67`; do not review them unless a conflict resolution reintroduced a difference.

## Validation Checklist

- [ ] `git diff --name-only --diff-filter=U` returns no files
- [ ] `git diff --check` acceptable, or known whitespace exceptions documented
- [ ] `git diff --cached --check` acceptable, or known whitespace exceptions documented
- [ ] No conflict markers in tracked files
- [ ] VS Code Java diagnostics clean for staged Java files
- [ ] Targeted Java/Maven tests run only after explicit consent
- [ ] Final merge commit reviewed before commit
