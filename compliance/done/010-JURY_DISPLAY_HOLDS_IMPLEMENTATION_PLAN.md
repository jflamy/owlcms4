# Jury Display Holds: Implementation Plan

Spec: [010-JURY_DISPLAY_HOLDS.md](010-JURY_DISPLAY_HOLDS.md). Status: **done (2026-11-11), archived.** Sets A
and B are implemented; the scenario numbers in set B are the ones in use at the time (see the mapping there).

Two sets of work. Set A changes the product code so that it matches the spec. Set B changes the
Playwright scenarios so that they check the spec. Set A comes first: the checks of set B fail on
the code as it is.

## Execution

One supervising agent runs the phases one at a time. Each phase is given to an executor agent
(Sonnet) with a bounded prompt. After each phase the supervisor checks the result (diff, test
reports, scenario reports) and reports to the maintainer before starting the next phase.

## Out of scope

- **Scenarios for the automatic stop** (stages 3 and 4 of the spec for the 3-4-5 juries with
  `automaticBreakNoVPT` on). Its code is in set A (A5 to A7), with unit tests only.
- The tracker: it keeps the earlier timing rules.
- A scenario for a corrected outcome. The correction is covered by unit tests (phase A2).

## Rules for every phase

- Do not commit, stage or push. Do not edit `translation4.csv`.
- No fully-qualified class names in Java. No hard-coded UI strings. No test-only conditionals in
  production code.
- Change files with the editor tools, not with shell redirection or scripts.
- VS Code compiles Java on save. Do not run `javac`. Check the edited Java files for errors
  before reporting.
- Tests run only through Maven, focused:
  `mvn -pl owlcms -am -Dtest=<Classes> -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test`,
  then read `owlcms/target/surefire-reports/`. Never `JUnitCore`.
- Scenarios run only on the dedicated server:
  `playwright/scripts/run-jury-scenario.sh --server --scenario=<ids>` (ports 7070, 7071, 7072).
  Never use ports 8080 to 8099. No `sleep` polling: run the command and wait for it to end.
- Scenario timing rules: every clock starts from 1:00; never check 0:59; scheduled checks are at
  least 2 s apart, and at least 2 s away from the moment a display is expected to change.
- Report at the end of the phase: files changed, what was verified and how, what is left.

## Set A: product code

Working-tree state at the start: stage 2 on the decision block (20 s from the last jury vote) is
already in `DecisionBlockState.java` and its test, uncommitted. Keep it.

### A1. Decision block: the 20 s start at the outcome (stage 5)

- File: `owlcms/src/main/java/app/owlcms/displays/scoreboard/DecisionBlockState.java`.
- Today `onJuryVerdict` paints the verdict and `onStartLifting(true)` starts the timeout 3.5 s
  later (`verdictLingering`).
- Change: `onJuryVerdict` (not `waitForAnnouncer`) starts the review timeout when it paints the
  verdict. `onStartLifting(true)` keeps a shown verdict and does not start or restart the
  timeout. A verdict stays until the timeout, a clock start, an explicit Resume
  (`onJuryDisplayReset`), a break started by an official, or the next referee decision, as today.
- Tests: `DecisionBlockStateTest`. Adapt `juryVerdictStaysUntilStartLifting`,
  `verdictLingersAfterAutomaticResumeUntilTimeout`,
  `lingeringVerdictSurvivesTheResyncsThatFollowTheResume`,
  `lingeringVerdictEndsOnClockStartOrResumeOrNextDecision`, `liftsDoneKeepsJuryVerdictUntilReviewTimeout`.
  Add: the timeout is scheduled once, at the verdict, and expires to READY without a
  `StartLifting`.
- Verify: `-Dtest=DecisionBlockStateTest`.

### A2. Decision block: a corrected outcome (stage 5)

- Files: `DecisionBlockState.java`, `Results.java` (`afterSlaveJuryNotification`, cases
  `GOOD_LIFT` / `BAD_LIFT`).
- Change: a second verdict on the same lift replaces the label and restarts the 20 s. A verdict
  that arrives when the block is back on the current athlete (state READY) shows the athlete of
  the notification with the reviewed referee lights and the verdict, for 20 s. `Results` passes
  the athlete (`e.getAthlete()`) and `computeRefLights(fop, true)`.
- Tests: two new cases in `DecisionBlockStateTest` (second verdict during the 20 s; verdict
  after the return to READY). Remove or adapt `juryVerdictIgnoredOutsideDeliberation`.
- Verify: `-Dtest=DecisionBlockStateTest`.

### A3. Decision block: last snatch or last lift, jury votes missing (stage 7)

- File: `DecisionBlockState.java`, method `onLiftsDone`.
- Today a 20 s timer starts with the break or the end of the session when a jury has not
  finished voting.
- Change: with a jury whose first vote is not complete, `onLiftsDone` starts no timer; the block
  waits like the attempt board. The last jury vote starts the 20 s (already in `onJuryUpdate`).
  A session reload returns to ready (already so). No jury, a complete first vote and a shown
  verdict keep their running timer.
- Tests: replace `liftsDoneWithJuryKeepsDecisionUntilReviewTimeout` by: no timer at the break;
  the last vote starts it; expiry returns to READY. Keep the other `liftsDone*` cases.
- Update the class comment of `DecisionBlockState` to match A1 to A3.
- Verify: `-Dtest=DecisionBlockStateTest`.

### A4. Durations in `timing.properties`

- Keys, in milliseconds, added to `owlcms/src/main/resources/timing/timing.properties` with a
  comment block:

  | Key | Default | Replaces |
  | --- | --- | --- |
  | `decisionVisibleDuration` | 3500 | `FieldOfPlay.DECISION_VISIBLE_DURATION` |
  | `juryDecisionHold` | 10000 | `FieldOfPlay.JURY_DECISION_HOLD_MS` |
  | `juryOutcomeCardDuration` | 5000 | `FieldOfPlay.JURY_DECISION_CARD_MS` |
  | `decisionBlockHold` | 20000 | `Results.DECISION_BLOCK_HOLD_MS` |

- `Competition.loadTimingConfig` reads them into static fields, the same way as the athlete
  timer values (`parseTimingProp`, the "timing override" log line). The constants stay as the
  compiled-in defaults.
- Users of the constants read the loaded values: `FieldOfPlay` (instance fields
  `decisionVisibleDuration`, `juryDecisionHoldMs`, the card deadline), `OBSMonitor`,
  `StreamingEventMonitor`, `AbstractAttemptBoard` (`JURY_NOTIFICATION_DIALOG_DURATION_MS`),
  `Results` (`scheduleReviewTimeout`). `FieldOfPlay.setJuryDecisionTimingForTests` keeps working.
- The no-limit wait for missing votes (`JURY_DECISION_VISIBLE_MAX_MS = -1`) is a rule and gets
  no key.
- Tests: one case that loads a properties file with the four keys and reads the values back
  (next to the existing timing tests, if any; else in `ConfigTest`).
- Docs: one paragraph in `docs/100-AdvancedTopics/140-Configuration.md`, after the athlete timer
  parameters.
- Verify: the new test, `SevenPersonAdjudicationTest`, `DecisionBlockStateTest`.

### A5. Automatic stop for every 3-4-5 jury (stage 3)

- File: `FieldOfPlay.java`, `evaluatePanel`.
- Today only the four-member jury without VPT stops automatically, on a unanimous jury.
- Change: with `automaticBreakNoVPT` on, every jury that is not the seven-person panel (3, 4
  without VPT, 5) stops automatically when its complete first vote is against the referees:
  unanimous for 3 or 4 jurors and for 5 without VPT, majority for 5 with VPT. Use the existing
  jury rule (`effectiveJuryVerdict` / `computeCurrentJuryVerdict`) rather than a new one. With
  the switch off, nothing changes.
- Tests: `SevenPersonAdjudicationTest`: 3 jurors unanimous against (stops), 3 jurors split (no
  stop), 5 jurors without VPT 4 to 1 against (no stop), 5 jurors with VPT 3 to 2 against
  (stops), 5 jurors unanimous against (stops), switch off (no stop).
- Verify: `-Dtest=SevenPersonAdjudicationTest`.

### A6. Explicit call for votes in the automatic modes (stages 3 and 4)

- File: `FieldOfPlay.java`, `openJuryVoteAtReviewStart` and the review start paths.
- Today, without the seven-person panel, Deliberation or Challenge opens the jury vote at once.
- Change: with `automaticBreakNoVPT` on, a review started by Deliberation or Challenge keeps the
  first-vote decisions (referees and jury) visible, like an automatic stop, until the Chair
  calls for votes. With the switch off, the vote opens at once, as today. The seven-person panel
  is unchanged.
- Tests: `SevenPersonAdjudicationTest`: switch on, Deliberation keeps the first votes and opens
  no vote; Call for Votes opens it; switch off, Deliberation opens the vote at once.
- Verify: `-Dtest=SevenPersonAdjudicationTest`.

### A7. Summon All is Call for Votes in the automatic modes (stage 4)

- File: `FieldOfPlay.java`, `isSummonAllVote`; Jury Control (`JuryKeypadContent`) if it tests
  the panel directly.
- Change: Summon All (keypad, jury box, `K`) means Call for Votes during a review of the
  seven-person panel, and during a review of a 3-4-5 jury with `automaticBreakNoVPT` on, before
  the vote is called. Otherwise it summons the referees, as today. The three individual summon
  buttons always summon one referee.
- Tests: `SevenPersonAdjudicationTest`: switch on, Summon All in a review opens the vote; switch
  off, it summons the referees.
- Update the user documentation of Summon All in `docs/060-Refereeing/020-Jury.md` and
  `docs/100-AdvancedTopics/200-MQTTMessages.md` if they describe it.
- Verify: `-Dtest=SevenPersonAdjudicationTest`.

### A8. Verification of set A (supervisor)

- `-Dtest=AllTests,DecisionElementStateTest`.
- Update section 6 of the spec: remove the differences that are closed.

## Set B: Playwright scenarios

The scenario numbers below are those used when set B was done. The scenarios were renumbered by jury on
2026-11-11 (panel 1xx, four-member jury 2xx, three-member jury 3xx, no jury 5xx). Old → new: 010 → 100,
020 → 105, 030 → 110, 035 → 115, 040 → 120, 050 → 125, 060 → 130, 170 → 135, 180 → 140, 200 → 145, 210 → 150,
220 → 155, 250 → 160, 320 → 165, 360 → 170; 120 → 200, 130 → 205, 140 → 210, 150 → 215, 160 → 220, 205 → 225;
070 → 300, 080 → 305, 090 → 310, 100 → 315, 110 → 320, 115 → 325, 190 → 330, 230 → 335, 240 → 340, 260 → 345,
270 → 350, 280 → 355, 290 → 360, 300 → 365, 310 → 370, 350 → 375, 370 → 380, 380 → 385, 390 → 390,
400 → 395; 330 → 500, 340 → 505, 410 → 510, 420 → 515.

File: `playwright/src/main/java/playwright/JuryScenario.java`, unless stated otherwise.

### B1. The scenarios read the durations

- New option `--timing=<file>`; default `owlcms/src/main/resources/timing/timing.properties`
  from the repository root. `run-jury-scenario.sh` passes the default.
- A small record holds the four durations of A4. The waits and the step texts that depend on
  them use it: the 3.5 s display, the 10 s hold, the 5 s card, the 20 s block hold. Fixed pauses
  that only let the screens settle (1.5 s, 2 s) stay as they are.
- A check before a display changes is placed 2 s before the duration; a check after the change,
  2 s after it.
- Verify: scenarios 300 and 330 pass unchanged.

### B2. Checks that are wrong or loose under the spec

- **390, 400** (silent jury, last snatch / last lift): 30 s after the referees the decision
  block is still on the athlete under review with the referee decision and empty jury circles.
  It is cleared after the Speaker reloads the session.
- **370, 380**: the attempt board check at 10 s + 2 s and the decision block checks (still shown
  at 20 s - 2 s, cleared at 20 s + 2 s) are counted from the last jury vote.
- **300, 320**, last step: the text no longer says that the block stays because no clock has
  started.
- **180, 190**: the block still shows the outcome 20 s - 2 s after the outcome, and is back on
  the clock 20 s + 2 s after it.
- Verify: scenarios 180, 190, 300, 320, 370, 380, 390, 400.

### B3. Outcome card on the attempt board

- Card: the dialog overlay `vaadin-dialog-overlay[theme~="jury-notification-dialog"]` with
  `.jury-notification-card`; the decision is in `.jury-notification-decision-text`, the reason in
  `.jury-notification-rule` (absent when no reason was given).
- New checks: the card is shown with the expected decision, and with the reason when the Chair
  gave one, no reason otherwise; the card is gone.
- Every flow that ends with an outcome checks the card in its first step after the outcome
  (`runPanelReview`, `runJuryReview`, `runJuryBoxDecision`, `runSpeakerDecidesReview`,
  `runBrokenJurorReview`, the after-next-clock flow).
- "The card is gone" is checked 5 s + 2 s after the outcome, in the scenarios of B4 and in 180
  and 190. Behind the card the board is on the current athlete once the 3.5 s have passed.
- Verify: scenarios 010, 070, 180, 210, 230, 260.

### B4. The 20 s after the outcome, once per flow

- One scenario per flow waits out the decision block hold. Before: the block still shows the
  outcome 20 s - 2 s after it, and the card is gone. After: the block is back on the clock, on
  the athlete of the top line, 20 s + 2 s after the outcome.

  | Flow | Scenario | The 20 s start at |
  | --- | --- | --- |
  | Panel review, the Chair gives the rule | 010 | the outcome |
  | Three-member jury review | 070 | the outcome |
  | Jury box decision | 260 | the outcome |
  | Jury agrees, three-member jury | 300 | the last jury vote |
  | Jury agrees, seven-person panel | 320 | the last jury vote |
  | Speaker enters the decision | 180, 190 | the outcome (done in B2) |

- The other scenarios keep their length.
- Verify: scenarios 010, 070, 260, 300, 320.

### B5. Fixture: two more single-athlete sessions

- `playwright/scripts/make-jury-fixture.py`: add M6 (next lift: third snatch) and M7 (next lift:
  third clean and jerk), same construction as M2 to M5; update its header comment.
- Regenerate `playwright/fixtures/jury-scenarios.json` from
  `/Users/jflamy/Downloads/owlcmsDatabase_2026-11-10_12h16.json`.
- Update the header of `run-jury-scenario.sh`.
- Verify: the generator prints six sessions; M1 still has 14 athletes; scenario 370 passes.

### B6. No jury on the last snatch and on the last lift

- Scenarios **410** (no jury, session M6, last snatch) and **420** (no jury, session M7, last
  lift), `Setup.NONE`, in `runLastLift` (a branch for no jury).
- Checks: 2 s after the 3.5 s display, the attempt board shows the break before clean and jerk,
  or the end of the session. The decision block is still on the athlete with the referee
  decision 20 s - 2 s after the decision, and cleared 20 s + 2 s after it.
- Update the class comment and the header of `run-jury-scenario.sh`.
- Verify: scenarios 410, 420.

### B7. Verification of set B (supervisor)

- All scenarios on the dedicated server: `playwright/scripts/run-jury-scenario.sh --server`.
- Report the failures by scenario; fix or report each one.

## After the plan

- The development server on 8080 must be restarted after the Maven runs.
- Nothing is committed without the maintainer's authorization.
