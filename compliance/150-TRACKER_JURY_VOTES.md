# Tracker: jury votes and adjudication decisions over the WebSocket

Plan for giving the tracker (`tracker-core` + `tracker`) everything the OWLCMS boards show about referee decisions,
jury votes, review votes and jury decisions, for every jury size (0/3/4/5), with or without Video Playback
Technology (VPT). Procedure: [SEVEN_PERSON_ADJUDICATION.md](done/SEVEN_PERSON_ADJUDICATION.md).

Status: **decided 2026-10-26; OWLCMS forwarder and tracker-core 2.0 implemented, tracker UI in progress.** Replaces the earlier Option A / Option B proposal (git history).

## 1. Decisions

- **Full facts, display choices in the tracker.** OWLCMS always sends the complete decision information. Whether
  something is shown, and for how long, is decided by each tracker plugin.
- **WebSocket only.** Only `WebSocketEventForwarder` changes. The legacy HTTP forwarder (`EventForwarder`) is not
  touched.
- **Forced upgrade.** OWLCMS 69 requires tracker-core 2.0 and the matching tracker release. No technical enforcement:
  an older tracker keeps working and shows none of the new information (section 7).
- **The forwarder runs the OWLCMS attempt board state machine** (`DecisionElementState`) and sends its result, so
  the display rules (reveal at the last vote, referees' decision kept during a jury vote, Chair confirmation, Cancel
  vote result) are not reimplemented in tracker-core.
- **Celebrations stays referee-only.** A later jury review is shown by the scoreboard behind it.
- **No athlete-facing option in the tracker.** Referee order is a display choice, not OWLCMS data.
- **OWLCMS behaviour is unchanged.** With `juryDecisionsOnAttemptBoard` on, a slow jury keeps the decision on the
  boards (people want to see what was voted); a stuck jury is released by starting the clock with the override
  (section 2.3).
- Moving the duplicated payload code into `ForwarderPayloadBuilder` is a separate, later cleanup.

## 2. Reference behaviour (OWLCMS 69)

### 2.1 Attempt board

[DecisionElementState](../owlcms/src/main/java/app/owlcms/components/elements/DecisionElementState.java) (plain
Java, renderer interface `IDecisionRenderer`) drives the lights; the jury card comes from
[AbstractAttemptBoard.slaveJuryNotification()](../owlcms/src/main/java/app/owlcms/displays/attemptboard/AbstractAttemptBoard.java).

| Moment | Referee cells | Jury cells |
|---|---|---|
| Lift: down signal, then decision | down arrow, then white/red (single light: middle only) | `voted` as jurors vote; all `white`/`red` once every juror has voted |
| Review starts (`START_DELIBERATION` / `CHALLENGE`) | the votes that led to the review (panel, or automatic stop), if any | revealed only if every juror voted, else `voted`/`empty` |
| Vote called (`PanelVoteCalled(0)`) | all `empty` (seven-person panel), or the referees' decision kept (3/5 jury, 4 without VPT) | all `empty` |
| Votes arrive (`PanelVoteUpdate`) | `voted` | `voted` |
| Last vote in | all revealed together | revealed |
| Cancel vote result, Chair-entered decision, Resume (`JuryDisplayReset`); abandoned vote | cleared | cleared |
| Jury decision applied (`waitForAnnouncer=false`) | | jury card, 5 s (`FieldOfPlay.JURY_DECISION_CARD_MS`): Reversal/Confirmed, Good Lift/No Lift ("New Record!" instead of Good Lift when the decision makes a record), athlete, judged attempt, rule `"<code> - <Jury.incorrect.code>"` |
| `DecisionReset` | cleared | cleared |

During a jury or challenge break the decision area is shown when `fop.isPanelVoteOpen()` or
`fop.getPanelReviewInitialVotes() != null`.

### 2.2 Scoreboard decision section

[DecisionBlockState](../owlcms/src/main/java/app/owlcms/displays/scoreboard/DecisionBlockState.java), painted by
[Results](../owlcms/src/main/java/app/owlcms/displays/scoreboard/Results.java):

- athlete under review (start number, name; age groups with `decisionSectionShowAgeGroups`), referee lights, jury
  circles (switch `juryDecisionsOnScoreboard`), review-vote progress;
- label: `PublicMsg.JuryDeliberation`, `PublicMsg.AdjudicationPanelReview` or `PublicMsg.CHALLENGE`, replaced by
  the verdict `JuryDialog.GoodLiftLabel` / `JuryDialog.BadLiftLabel` `– <rule>`;
- stopwatch counting up from the start of the review, stopped at the verdict (switch `decisionSectionStopwatch`);
- after Cancel vote result or a Chair override: votes cleared, the referees' decision under review and the label
  stay;
- timing: `DecisionReset` is ignored. With a jury, the decision stays until the next clock start, a non-jury break,
  a new decision, a deliberation or StartLifting. With no jury, a 20 s timeout (`REVIEW_TIMEOUT_MS`) applies. After a
  four-member jury verdict, the verdict stays 20 s once lifting resumes;
- the section itself: shown on public and video scoreboards, on warm-up scoreboards with
  `decisionSectionOnWarmupScoreboard`; the per-display `decisionSection` option overrides it.

### 2.3 Holding the decision for the jury (FOP)

`FieldOfPlay.scheduleDecisionReset()`: with `juryDecisionsOnAttemptBoard` on, a jury, and a referee decision from the
devices, the decision stays visible until the last juror votes (then 3.5 s). An announcer-entered or single-input
decision waits at most 10 s (`JURY_DECISION_VISIBLE_MAX_MS`). With the switch off, the decision clears after 3.5 s.
In all cases the next clock is refused until every juror has voted (`isJuryVotesAwaited()`).

The hold ends when the last juror votes, the jury decision is applied, the Chair starts a deliberation or challenge,
Resume is pressed, the clock is started with the override (`refuseClockStartWhileJuryVotesPending`, incomplete votes
abandoned), or the session is switched. A plain clock start is refused (`JuryVotesPending`).

## 3. Wire format

All objects below are sent as JSON objects (not strings), like `records`.

### 3.1 New decision message types

| OWLCMS event | `decisionEventType` | Sent when |
|---|---|---|
| `JuryUpdate` | `JURY_UPDATE` | a juror votes on the lift (review votes are carried by `PANEL_VOTE`) |
| `PanelVoteCalled` (`ref == 0`) | `PANEL_VOTE_CALLED` | the Chair calls for votes |
| `PanelVoteUpdate` | `PANEL_VOTE` | a review vote arrives, the last vote reveals all, the vote closes or is abandoned |
| `JuryDisplayReset` | `JURY_DISPLAY_RESET` | Cancel vote result, Chair-entered decision, Resume |

The existing types (`initialDecision`, `FULL_DECISION`, `DOWN_SIGNAL`, `RESET`, `START_DELIBERATION`,
`END_DELIBERATION`, `CHALLENGE`, `END_CHALLENGE`, `JURY_DECISION`) and their flat fields are unchanged.

### 3.2 `decisionState`

In **every `decision` and every `update` message**. The decision cells as the OWLCMS attempt board state machine
computes them, with jury votes always included.

```json
"decisionState": {
  "sequence": 42,
  "athleteKey": -2005683348,
  "visible": true,
  "down": false,
  "singleRef": false,
  "refs": ["white", "white", "red"],
  "jury": ["voted", "empty", "voted", "empty"],
  "review": "deliberation",
  "panelReview": true,
  "reviewVoteOpen": false,
  "reviewStartMillis": 1788100000000,
  "originalRefereeDecisions": [true, true, false]
}
```

| Field | Meaning |
|---|---|
| `sequence` | increases with every change; a lower value than the last one received is stale |
| `athleteKey` | athlete the cells describe: `fop.getAthleteUnderReview()` when set, otherwise the current athlete |
| `visible` | OWLCMS currently shows the decision area (down arrow, lights or review cells); false after a reset |
| `down` | down arrow shown instead of the cells |
| `singleRef` | single-light setup: only `refs[1]` is meaningful |
| `refs` | 3 referee cells: `empty`, `voted`, `white`, `red` |
| `jury` | one cell per juror when there is a jury (`[]` otherwise), same vocabulary, **whatever the switches** |
| `review` | `none`, `deliberation` or `challenge` |
| `panelReview` | the review is the automatic seven-person Adjudication Panel Review (`fop.isPanelReview()`) |
| `reviewVoteOpen` | the Chair has called for votes and the vote is in progress (`fop.isPanelVoteOpen()`) |
| `reviewStartMillis` | during a review only: epoch ms when the deliberation or challenge started (scoreboard stopwatch) |
| `originalRefereeDecisions` | during a review only: the referees' lights on the lift under review, `true`/`false`/`null` |

A reset clears the cells (`visible=false`, all `empty`). A plugin that keeps a decision on screen longer (tracker
scoreboard) keeps the previous `decisionState` it received.

### 3.3 `adjudicationDecision`

On **every `JURY_DECISION` message**, including the Speaker-only ones (`waitForAnnouncer=true`, which boards skip).

```json
"adjudicationDecision": {
  "decision": false,
  "reversal": true,
  "ruleCode": 5,
  "athleteKey": -2005683348,
  "liftTypeKey": "Snatch",
  "attemptNumber": 2,
  "originalRefereeDecisions": [true, true, false],
  "refereeVotes": [false, false, true],
  "juryVotes": [false, false, false, true],
  "newRecord": false,
  "recordKind": "none",
  "waitForAnnouncer": false,
  "chairEntered": false,
  "speakerEntered": false
}
```

| Field | Meaning |
|---|---|
| `decision` | `true` = Good Lift, `false` = No Lift |
| `reversal` | the jury reversed the referees (`false` = confirmed); `null` when OWLCMS does not say |
| `ruleCode` | IWF rejection rule `1`..`17` for a No Lift when given, else `null`; text is `Jury.incorrect.<code>` |
| `athleteKey`, `liftTypeKey`, `attemptNumber` | the **judged** attempt: the athlete's last completed attempt (`liftNo = attemptsDone`), `liftTypeKey` `Snatch` / `Clean_and_Jerk`, `attemptNumber` `1`..`3` |
| `originalRefereeDecisions` | the referees' lights on that lift, from the athlete's stored per-attempt decisions; `[null, null, null]` when the lift was entered by the announcer or a single input |
| `refereeVotes` | the 3 referees' review votes, **seven-person panel only** (`fop.isPanelProcedure()`); `[]` for 3/5 juries and the four-member jury without VPT |
| `juryVotes` | the 3..5 jurors' review votes, `true`/`false`/`null`, **whatever was entered** (all `null` after a Chair override, which clears the votes) |
| `newRecord` | a record was set (the value used by the attempt board card) |
| `recordKind` | `none`, `new` or `denied` (as the flat field) |
| `waitForAnnouncer` | notification meant for the Speaker only |
| `chairEntered` | the Jury Chair's override (seven-person panel only) |
| `speakerEntered` | the decision was entered by the Speaker |

No text and no display duration: the tracker translates `JuryNotification.Reversal` / `Confirmed`,
`JuryDialog.GoodLiftLabel` / `BadLiftLabel` and `Jury.incorrect.<code>`, and chooses the card duration (5 s on
OWLCMS attempt boards).

### 3.4 Plugin-option defaults

In every `update` message, string values `"true"` / `"false"` like the other update flags:

| Field | Feature switch | Tracker use |
|---|---|---|
| `juryDecisionsOnAttemptBoard` | `JURY_DECISIONS_ON_ATTEMPT_BOARD` | default of the attempt board "show jury votes" option |
| `juryDecisionsOnScoreboard` | `JURY_DECISIONS_ON_SCOREBOARD` | default of the scoreboard "show jury votes" option |
| `decisionSectionOnWarmupScoreboard` | `DECISION_SECTION` | default of the scoreboard "decision section" option on warm-up scoreboards |
| `decisionSectionStopwatch` | `DECISION_SECTION_STOPWATCH` | default of the scoreboard "review stopwatch" option |
| `decisionSectionShowAgeGroups` | `DECISION_SECTION_SHOW_AGE_GROUPS` | default of the scoreboard "age groups" option |

### 3.5 Example: four-member jury with VPT, automatic panel review, No Lift

| # | Message | `decisionState` (refs \| jury) | Other |
|---|---|---|---|
| 1 | `DOWN_SIGNAL` | `down` | |
| 2 | `FULL_DECISION` | W W R \| empty ×4 | |
| 3 | `JURY_UPDATE` ×4 | … voted …, then W W R \| W R R R | |
| 4 | `START_DELIBERATION` | W W R \| W R R R, `review=deliberation`, `panelReview` | `reviewStartMillis`, `originalRefereeDecisions` |
| 5 | update `BreakStarted` (JURY) | same | |
| 6 | `PANEL_VOTE_CALLED` | empty ×3 \| empty ×4, `reviewVoteOpen` | |
| 7 | `PANEL_VOTE` ×6 | progressively `voted` | |
| 8 | `PANEL_VOTE` (7th vote) | R R W \| R R R W | |
| 9 | `JURY_DECISION` | unchanged | `adjudicationDecision` (No Lift, rule) |
| 10 | `RESET`, update `StartLifting` | cleared, `review=none` | |

## 4. OWLCMS changes (`WebSocketEventForwarder` only)

1. **State machine.** One `DecisionElementState` per forwarder, configured: `showJuryDecisions=true` (full facts),
   `liveReferee=false`, `resetOnClockStart=false`, `showsDownSignal=true`, `dontReset=false`, `setFop(fop)`.
   **Not registered on the event bus**: the forwarder's handlers call it explicitly, then build and send one
   message, so the order is fixed (Guava does not order subscribers).
2. **Delegation.** Existing handlers: `slaveDecision` → `slaveShowDecision`, `slaveDownSignal`,
   `slaveDecisionReset`, `slaveJuryNotification` (`START_DELIBERATION` / `CHALLENGE` → initial pattern),
   `slaveBreakStart`, `slaveStartTime`, `slaveStopTime`. New `@Subscribe` handlers: `JuryUpdate`, `PanelVoteCalled`,
   `PanelVoteUpdate`, `JuryDisplayReset`, each delegating and pushing a decision message (section 3.1).
3. **Renderer.** `ForwarderDecisionState` (package `monitors.websocket`) hosts the state machine, implements
   `IDecisionRenderer` and keeps the current `decisionState`:
   `resetDecisionDisplay` (cleared), `showDecisionLights` (refs from booleans; single light: middle cell = `ref2`, or
   the decision when `ref2` is null), `showDownSignal`, `showJuryLights` (jury cells), `showPanelVote` (refs and jury
   cells). `sequence` comes from one counter seeded from the clock, so it also increases across OWLCMS restarts.
4. **Review fields.** `review` from the FOP break type (JURY / CHALLENGE with an athlete under review),
   `panelReview`, `reviewVoteOpen`; `reviewStartMillis` recorded by the forwarder at `START_DELIBERATION` /
   `CHALLENGE` and cleared at StartLifting; `originalRefereeDecisions` from the athlete under review's stored
   decisions for the reviewed lift.
5. **`adjudicationDecision`** built in `createJuryEvent()` for `GOOD_LIFT` / `BAD_LIFT`, from the `JuryNotification`
   (`getReversal`, `getReasonCode`, `getNewRecord`, `isWaitForAnnouncer`, `isChairEntered`, `isSpeakerEntered`) and
   the athlete (`getKey`, `attemptsDone`, stored decisions). The votes are copied into the notification by
   `FieldOfPlay.doJuryDecision()` (`JuryNotification.setReviewVotes`) because the forwarder's bus is asynchronous and
   the FOP may have cleared them by the time it runs; the referee review votes are reported only when
   `isPanelProcedure()`, since without the panel the review array holds a copy of the original decision.
6. **Athlete accessor.** Add `Athlete.getDecisions(int liftNo)` (counterpart of `setDecisions`).
7. **Update messages.** `decisionState` and the five option defaults (section 3.4) in `createUpdate()`.
8. **Connect / session switch.** On `SwitchGroup` and when the connection opens, resync the state machine as
   `AbstractAttemptBoard.syncWithFOP()` does (`showPanelReviewInitialVotes(null)` in a break) so the next message
   carries the current state.
9. **Protocol.** `WebSocketEventSender.PROTOCOL_VERSION` raised to the OWLCMS 69 value.
10. **Debounce.** `sendWebSocket()` drops identical payloads within 1 s; `sequence` makes consecutive states
    distinct. No change.

## 5. tracker-core 2.0

1. **Merge.** Decision messages of the new types (`JURY_UPDATE`, `PANEL_VOTE_CALLED`, `PANEL_VOTE`,
   `JURY_DISPLAY_RESET`) must not overwrite `decisionEventType` in the platform state: store their type in a
   separate field (`juryEventType`). Celebrations, OBS and scoreboards keep reading `FULL_DECISION`. The `decision`
   broadcast still carries each message's own type.
2. **`decisionState`.** Keep the latest per platform, ignoring a lower `sequence`; preserved across `update`
   messages (they carry it anyway); passed to plugins in the platform state and the `decision` broadcast.
3. **`adjudicationDecision`.** Stored with the platform state and in the `decision` broadcast for `JURY_DECISION`;
   cleared like the other decision fields.
4. **Review break.** `extractDecisionState()` must not hide the decision during a jury or challenge break; plugins
   decide.
5. **Option defaults** from section 3.4 exposed to plugins.
6. **Docs.** `WEBSOCKET_MESSAGE_SPEC.md` (sections 3.1–3.4), `ReleaseNotes.md` (2.0.0).

## 6. tracker

- **Attempt board:** draws `decisionState` (3 referee cells, jury circles per its "show jury votes" option, down
  arrow); shows the decision area in a review break when a review vote is open or cells are shown; adjudication card
  from `adjudicationDecision` (`waitForAnnouncer=false`), duration option defaulting to 5 s; review label from
  `review` / `panelReview`.
- **Scoreboards:** decision section options (shown, jury votes, stopwatch, age groups) defaulting to section 3.4.
  OWLCMS-like timing computed from the messages: keep the last decision until the next clock start (timer
  `StartTime`), a non-jury break, a new `FULL_DECISION` or StartLifting; 20 s when there is no jury; 20 s after a
  four-member jury verdict once lifting resumes. Verdict label from `adjudicationDecision`. Stopwatch from
  `reviewStartMillis`.
- **Celebrations:** unchanged (referee decision only).

## 7. Compatibility

| OWLCMS | Tracker | Result |
|---|---|---|
| 69 | tracker-core 2.0 + tracker | supported |
| 69 | tracker-core 1.x | not supported. It keeps working as before and shows none of the new information; the new types can replace `FULL_DECISION` in the platform state, which may make Celebrations miss a video |
| 68 or older | tracker-core 2.0 | no `decisionState` / `adjudicationDecision`; behaviour as before |

## 8. Tests

OWLCMS (JUnit, with maintainer consent):

1. Delegation coverage: every `@Subscribe` method of `DecisionElementState` has a forwarder delegation.
2. `SevenPersonAdjudicationTest` extension with a recording forwarder: the `decisionState` sequence for panel review
   Good Lift / No Lift with rule, jury vote without VPT (referees' decision kept), 3- and 5-member juries, Cancel vote
   result, Chair override, abandoned vote.
3. `adjudicationDecision`: `refereeVotes` empty without the panel, `juryVotes` after a Chair override,
   `originalRefereeDecisions` from the athlete, judged attempt after the lifting order moved.
4. `ProtocolRecordingTest` runs the scenario against the peer dev tracker. Both headless and watch modes use:
   - 10 s before-introduction countdown;
   - 5 s athlete introduction, no timer;
   - one 20 s before-snatch countdown, started when athlete introduction ends;
   - after 5 s, officials are introduced for 5 s while that countdown continues;
   - the remaining approximately 10 s display "before snatch", then lifting starts;
   - the automatic clean & jerk break is shortened to 10 s for the simulation.
   The recording checks the durations and verifies that the officials' ceremony neither pauses nor restarts the
   snatch countdown. The database uses a four-member jury plus three referees with VPT enabled.
   The scenario also enables `juryDecisionsOnAttemptBoard`, so the tracker displays both rows by default.
   Each run starts a fresh tracker on port **51793**, so open `/attempt-board?fop=A` and `/lifting-order?fop=A` pages
   reconnect on the next run. At the end of the session, the test checks the translated scoreboard label and the
   jury-row option through the tracker API.

tracker-core: merge does not overwrite `decisionEventType`; stale `sequence` ignored; `decisionState` preserved and
broadcast; decision shown during a review break; replay of the section 3.5 sequence.

tracker (vitest): attempt board cells and card; scoreboard timing rules; Celebrations unchanged after `JURY_UPDATE`.

Manual: 3-jury lift, 4-jury lift, panel review Good Lift / No Lift (VPT), jury review without VPT, challenge, Cancel
vote result, Resume mid-vote, stuck jury released by the clock override; tracker side by side with the OWLCMS
attempt board and scoreboard.
