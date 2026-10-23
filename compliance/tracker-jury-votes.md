# Tracker: jury votes and review votes in the decision block

Plan for giving the tracker ("video data" consumer, `tracker-core` + `tracker`) the
same decision block as the owlcms scoreboard for all jury sizes (0/3/4/5), in both
phases of the seven-person Adjudication Panel procedure. Procedure:
[SEVEN_PERSON_ADJUDICATION.md](./SEVEN_PERSON_ADJUDICATION.md). Timeline of the
owlcms displays: [phase-b-referee-lights-mqtt.md](./phase-b-referee-lights-mqtt.md)
(MQTT / physical devices are out of scope here).

Status: **proposal, no code written**. Two options are on the table:

- **Option A — raw events, rules re-implemented in tracker-core** (sections 1–9,
  written 2026-10-20 10:44, *before* commit `2c6fd4d57`; see the staleness notes
  in §1 and §3).
- **Option B — ship the output of the owlcms display state machines**
  (section 0, written after the 15:00 analysis). **Preferred**: it is the only
  way to get the exact same timeouts as the owlcms boards (e.g. the scoreboard
  keeping the decision visible longer than the attempt board).

## 0. Option B — forward the rendered state of `DecisionBlockState` / `DecisionElementState`

### 0.0 Minimum deliverable (independent of A vs B): reversal and reversal reason

Whatever option is chosen, the tracker must at least show **that the jury
reversed the referees' decision and why** (TCRR 7.5.7: a No Lift is announced with
the technical rule). Today:

| | `juryDecision` | `juryReversal` | `juryReasonCode` |
|---|---|---|---|
| `WebSocketEventForwarder.createJuryEvent()` | yes | yes | yes (No Lift only) |
| `EventForwarder.createJuryEvent()` (HTTP / publicresults) | yes | yes | **no** |
| tracker-core `decision` broadcast (~l.748) | yes | yes | yes |
| tracker-core after the next `update` message | **lost** (not in the `decisionFields` preserve list, ~l.628) | lost | lost |
| tracker UI | OBS scene only | **unused** | **unused** |

Minimum steps:

1. owlcms HTTP forwarder: add `juryReasonCode` (one `mapPut`, same as the
   WebSocket forwarder). With Option B both values also travel inside
   `scoreboardDecision.verdict` (§0.3) so they survive by construction.
2. tracker-core: add `juryDecision`, `juryReversal`, `juryReasonCode`,
   `waitForAnnouncer` to the `update` preserve list and clear them with the other
   decision fields on `StartTime` / `CURRENT_ATHLETE` (Option A §5.2 already lists
   this).
3. tracker UI: verdict line under the lights — `JuryDialog.GoodLiftLabel` /
   `JuryDialog.BadLiftLabel`, with `Jury.incorrect.<juryReasonCode>` appended for a
   No Lift (same wording as `Results.renderJuryVerdict()`), and a "reversal" style
   hook (`data-reversal="true"`) so a reversed decision can be highlighted. Only
   the `waitForAnnouncer=false` notification is painted (as OBS and the scoreboard
   already do).

### 0.1 Why raw events cannot give identical timing

Two owlcms timings are **not FOP events**, so a tracker working from raw events
can only approximate them:

- **Scoreboard review timeout.** `REVIEW_TIMEOUT_MS = 20_000` lives in
  [Results.java](../owlcms/src/main/java/app/owlcms/displays/scoreboard/Results.java)
  (`scheduleReviewTimeout()`, per-UI `ScheduledExecutorService`). The scoreboard
  ignores `DecisionReset` (`DecisionBlockState.onDecisionReset()`), and the
  four-member-jury verdict *lingers* after the automatic `StartLifting` until that
  timeout (`onStartLifting(keepVerdict)`). Option A's open question 5 ("clear at
  `RESET`") would make the tracker scoreboard differ from the owlcms scoreboard by
  up to 20 s.
- **Attempt board** follows `DecisionReset` / `JuryDisplayReset` (FOP-driven). The
  two boards are *meant* to differ; the tracker has to reproduce both, not one
  blended rule.

Running the two existing plain-Java state machines inside the forwarder and
shipping their **rendered output** gives identical behaviour by construction and
removes the rule re-implementation from tracker-core.

### 0.2 Reveal timing changed on 2026-10-20 (`2c6fd4d57`)

Both `DecisionElementState.slavePanelVoteUpdate()` and
`Results.slavePanelVoteUpdate()` now use `e.getVotes()` regardless of
`publicReveal`: **public boards reveal the seven votes at the 7th vote**;
`PanelVoteUpdate.isPublicReveal()` only matters to MQTT (referee lights not sent
twice). Option A §1 step 4, §3 `PANEL_VOTE` rule, §4.2 and §8 test 2 ("no
`true`/`false` cell between the 7th vote and `doJuryDecision`") are therefore
wrong. Under Option B the question does not arise: the forwarder shows whatever
the owlcms boards show.

### 0.3 Design

Per FOP, the forwarder owns one instance of each state machine:

| Board | State machine | Renderer implemented in the forwarder | Timing source |
|---|---|---|---|
| scoreboards / attempt bar | [DecisionBlockState](../owlcms/src/main/java/app/owlcms/displays/scoreboard/DecisionBlockState.java) | `DecisionSectionRenderer` that accumulates a snapshot and pushes it on every `render()`; `scheduleReviewTimeout()` / `cancelReviewTimeout()` use a scheduler whose callback calls `onReviewTimeout()` under the forwarder lock | the 20 s constant moves to a shared place (e.g. `DecisionDisplayRules`) so Results and the forwarder use one value |
| attempt board | [DecisionElementState](../owlcms/src/main/java/app/owlcms/components/elements/DecisionElementState.java) | `IDecisionRenderer` that accumulates a snapshot (`resetDecisionDisplay`, `showDecisionLights`, `showDownSignal`, `showJuryLights`, `showPanelVote`) | already FOP-driven; the class self-subscribes (`@Subscribe`), so the forwarder registers it on the FOP UI event bus and calls `setFop()` — no glue |

Each snapshot is a **complete** JSON object sent as **one string field** on
`decision` messages (same mechanism as `records`, `leaders`):

```json
"scoreboardDecision": {
  "state": "DELIBERATION",
  "athlete": "LAMY J.",
  "refs": ["white", "red", "voted"],
  "jury": ["voted", "voted", "empty", "empty"],
  "singleLight": false,
  "messageKey": "PublicMsg.AdjudicationPanelReview",
  "verdict": null
}
"attemptBoardDecision": {
  "mode": "DECISION",
  "refs": ["white", "red", "voted"],
  "jury": ["voted", "voted", "empty", "empty"],
  "singleLight": false,
  "down": false
}
```

Cell values: `empty | voted | white | red` (the owlcms vocabulary). `verdict` =
`{ "good": false, "reversal": true, "ruleCode": 5 }` or `null`; `reversal` and
`ruleCode` come from `JuryNotification.getReversal()` / `getReasonCode()`
(`DecisionBlockState.onJuryVerdict()` only receives `good` and the rule text, so
either the driver passes the reversal through to the renderer alongside the
verdict or `onJuryVerdict` gains a `reversal` parameter — the latter also lets the
owlcms scoreboard style a reversal). Send **keys and codes, not text**:
the tracker has `translation4.csv` and `hub.translate()` (`PublicMsg.*`,
`JuryDialog.GoodLiftLabel/BadLiftLabel`, `Jury.incorrect.<n>`). `state: READY` /
`mode: RESET` with empty cells means "nothing to show" — no absent-field semantics
to define.

### 0.4 Impact — owlcms

1. **Glue extraction (main cost).** `DecisionBlockState` has no subscriptions; the
   event→state mapping is ~12 handlers in Results (`afterSlaveDecision`,
   `afterSlaveJuryNotification`, `slaveJuryUpdate`, `slavePanelVoteUpdate`,
   `slaveJuryDisplayReset`, `slaveJuryStartTime`, `slaveBreakStart`,
   `slaveStartLifting`, `slaveDecisionReset`, `slaveJuryResetOnNewClock`), the
   resync block in `configureDecisionBlock()` (SwitchGroup / StartLifting / page
   load) and the helpers `computeRefLights()`, `initialJuryVotes()`,
   `panelVoted()`. Copying this into the forwarder creates two sources of truth
   that will drift: extract it into a plain-Java `DecisionBlockDriver` used by
   both Results (`uiAccess(() -> driver.on(e))`) and the forwarder.
   `SevenPersonAdjudicationTest` can then assert forwarder snapshots directly.
2. **The forwarder becomes stateful with a timer.** Today its handlers are
   `synchronized` and stateless beyond `lastTimerMap`. The timeout callback must
   take the same lock; make the scheduler injectable for `isTestingMode()` (as
   `DelayTimer` does).
3. **Push without a UIEvent.** `pushDecision(DecisionEventType, UIEvent)` needs a
   variant for the timeout tick, e.g. `decisionEventType = DECISION_BLOCK`.
   `createDecision()` fills `d1..d3` / `decisionsVisible` / `down` from the
   current forwarder state, so legacy consumers see a consistent payload.
4. **Connect / late join.** The connection snapshot builder in
   `WebSocketEventForwarder` (~l.1950–2020, athlete/break timer state) must include
   both blocks; the driver must resync on `SwitchGroup` / `StartLifting` exactly as
   `configureDecisionBlock()` does.
5. **Attempt-board options are per URL** (`showJuryDecisions`, `liveReferee`,
   `resetOnClockStart`, `showsDownSignal`, `dontReset`). The forwarder freezes one
   canonical configuration: `showJuryDecisions` =
   `FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD`, `liveReferee=false`,
   `resetOnClockStart=false`, `showsDownSignal=true`. Tracker attempt boards lose
   per-URL variation — document it.
6. **Protocol**: `WebSocketEventSender.PROTOCOL_VERSION` → `64.1.0` (new fields =
   MINOR). No new translations. The HTTP forwarder (publicresults) can carry the
   same two fields for free through the shared driver; unknown keys are ignored
   there.
7. Sender debounce (identical hash within 1 s) is safe: every push differs in the
   block field.

### 0.5 Impact — tracker-core (small)

- [competition-hub.js](../../tracker-core/src/competition-hub.js): add
  `scoreboardDecision` and `attemptBoardDecision` to `jsonFields` in
  `_sanitizeInboundPayload()` (~l.1962), to the `update` preserve list (~l.628) and
  to the `decision` broadcast payload (~l.748). **Do not** add them to
  `clearStaleDecisionForExplicitAthleteTimer()` /
  `clearStaleDecisionForCurrentAthleteUpdate()`: the snapshot is authoritative and
  owlcms already pushes `READY` on `StartTime`. The partial-merge problem of
  Option A §5.2 disappears because a single field is always complete.
- [timer-decision-helpers.js](../../tracker-core/src/utils/timer-decision-helpers.js):
  `extractDecisionState()` unchanged for the legacy shape; add
  `scoreboardDecision.state !== 'READY'` (resp. `attemptBoardDecision.mode !==
  'RESET'`) to the "decision visible" test so `computeDisplayMode()` prefers the
  block over `STOP` during a jury break. Translate `messageKey`,
  `Jury.incorrect.<ruleCode>`, `JuryDialog.*Label` in the hub.
- Option A §5.3 (`isJuryReview`, `verdict`, `messageKey` derivation) and §5.4 are
  dropped — exactly the parts that would drift.
- `MINIMUM_PROTOCOL_VERSION` stays `64.0.0`.

### 0.6 Impact — tracker (UI)

Same as Option A §6: a `DecisionLights.svelte` fed by `scoreboardDecision` in
`CurrentAttemptBar.svelte` and by `attemptBoardDecision` in
`attempt-board/page.svelte`, falling back to legacy `ref1..3` / `down` when the
block is absent (old owlcms). OBS (`helpers-core.js` decision listener),
celebrations and lower-third key on `decision.type ∈ {INITIAL_DECISION,
FULL_DECISION, DECISION_VISIBLE, JURY_DECISION}` and ignore other types, so the
extra `DECISION_BLOCK` messages are harmless — add one regression case in
`helpers-core.test.js`.

### 0.7 Trade-offs vs Option A

- ✔ Timing, reveal rules, labels, lingering verdicts identical by construction;
  any future change to the owlcms boards reaches the tracker with zero
  tracker-core work.
- ✔ Far less tracker-core logic; no change to hub merge semantics.
- ✖ Larger owlcms refactor (driver extraction, stateful forwarder with a timer).
- ✖ Old owlcms + new tracker: no improvement (Option A would have derived a review
  label from `breakType`). Acceptable: the pairing is owlcms-led.
- ✖ Attempt-board option variation frozen for the tracker.

### 0.8 Tests (Option B)

owlcms (JUnit 4, with maintainer consent):

1. `DecisionBlockDriverTest`: Results-equivalence — the driver fed by the existing
   `DecisionBlockStateTest` / `SevenPersonAdjudicationTest` scenarios produces the
   same renderer calls as before the extraction.
2. `SevenPersonAdjudicationTest` extension: a recording renderer on the forwarder
   asserts the snapshot sequence for panel review Good Lift / No Lift with rule,
   no-VPT review, Chair-entered decision, abandoned vote, and the 20 s lingering
   verdict (injected scheduler).
3. Connect snapshot contains both blocks in every FOP state.

tracker-core: JSON parsing of the two fields, preservation across `update`,
broadcast shape, `computeDisplayMode()` preference, translations of
`messageKey` / `ruleCode`; existing suites still pass.

tracker (vitest): `DecisionLights` for 0/3/4/5 circles and the four cell values;
attempt-board options test with the extended `decision` shape.

### 0.9 Open questions (Option B)

1. Keep Option A's `JURY_UPDATE` / `PANEL_VOTE` event types at all? Proposed: no —
   the blocks carry everything; one `DECISION_BLOCK` type for timer-driven pushes.
2. Should `scoreboardDecision` also carry the athlete-under-review *key* so the
   tracker can show flag/team from its own data? Cheap to add.
3. Canonical attempt-board configuration (0.4 item 5): confirm the defaults.

---

## 1. What exists today (Option A)

### owlcms forwarders

Two forwarders build near-identical `Map<String,String>` payloads:

- [WebSocketEventForwarder.java](../owlcms/src/main/java/app/owlcms/monitors/WebSocketEventForwarder.java)
  (tracker, `decision` message type): `createDecision()` ~l.983 and
  `createJuryEvent()` ~l.1032. Already sends `juryReasonCode` on `JURY_DECISION`.
- [EventForwarder.java](../owlcms/src/main/java/app/owlcms/monitors/EventForwarder.java)
  (HTTP, publicresults): `createDecision()` ~l.1007 and `createJuryEvent()` ~l.1057.
  Does **not** send `juryReasonCode` nor `panelReview`.
- [ForwarderPayloadBuilder.java](../owlcms/src/main/java/app/owlcms/monitors/websocket/ForwarderPayloadBuilder.java)
  has static `createDecision()` / `createJuryEvent()` copies (sends `panelReview`)
  that **nobody calls**; only `exportCompetitionData` / `computeParametersHash` are used.

Subscribed UI events that produce a `decision` message: `Decision`
(`FULL_DECISION`), `InitialDecision` (`initialDecision`, canonicalised to
`INITIAL_DECISION` by the hub), `DecisionReset` (`RESET`), `DownSignal`
(`DOWN_SIGNAL`), `JuryNotification` (`JURY_DECISION`, `START_DELIBERATION`,
`END_DELIBERATION`, `CHALLENGE`, `END_CHALLENGE`). **Not** subscribed:
`UIEvent.JuryUpdate`, `UIEvent.PanelVoteUpdate`.

`mapPut()` skips null values: a cell with no value is **absent** from the payload.

### FOP event flow the forwarder will see

Phase A (the lift), [FieldOfPlay.java](../owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java):

- each juror vote → `processJuryMemberDecisions()` → `JuryUpdate(juror i, full
  array with the real votes, jurySize)`; when all have voted →
  `JuryUpdate(collective, array, jurySize)`. The array always holds the real
  votes; **hiding undisclosed votes is a display rule** (`Results.pushJuryCircles()`:
  grey "voted" until every juror has voted, then white/red).
- the FOP holds `DecisionReset` while jury votes are awaited
  (`rescheduleDecisionResetForJury()`), then for `juryDecisionHoldMs`.

Review break (panel review, jury review without VPT, or challenge):

- `doTONotifications()` pushes `JuryNotification START_DELIBERATION`
  (`panelReview=true` for the automatic 7-person break) / `CHALLENGE` **before**
  the `BreakStarted` UI event; `capturePanelReviewInitialVotes()` snapshots the
  Phase A pattern (`fop.getPanelReviewInitialVotes()`, 7 cells R1 R2 R3 J1..J4).
- the attempt board shows that pattern until Call for Votes
  (`DecisionElementState.showPanelReviewInitialVotes()`): referees revealed; jury
  revealed only if all four voted, otherwise "voted"/"empty".

Phase B (Chair calls for votes, `doPanelVoteCalled()`):

1. jury votes cleared, `PanelVoteCalled(0)`, `PanelVoteUpdate(open=true, voted=all
   false, votes=null, publicReveal=false)`.
2. each referee review vote → `PanelVoteUpdate(open, voted[i]=true, votes=null,
   refereeUpdated=i)`.
3. each jury review vote → **`JuryUpdate` with the real votes** (from
   `processJuryMemberDecisions`) **then** `PanelVoteUpdate(open, voted, null)`.
4. 7th vote → `panelVoteRevealed=true` → **`JuryUpdate(collective, real votes)`**
   and `PanelVoteUpdate(open=true, votes=real, majority, publicReveal=false)`
   — ~~both are the **jury-private reveal** and must not reach the tracker~~
   **stale since `2c6fd4d57`**: the attempt board and the scoreboards reveal at
   this point (see §0.2); only MQTT uses `publicReveal`.
5. decision applied (`doJuryDecision()`): Good Lift after a 1.5 s grace, No Lift
   when the Chair has given the rule →
   `PanelVoteUpdate(open=true, votes, majority, publicReveal=true)` then
   `PanelVoteUpdate(open=false, votes, majority, publicReveal=true)` then
   `JuryNotification GOOD_LIFT/BAD_LIFT` (`waitForAnnouncer=false` for a four-member
   jury, `reasonCode` on a No Lift) → `DecisionReset` ~3 s → `StartLifting` 3.5 s.
6. abandon (Resume, end of break) → `closePanelVote()` →
   `PanelVoteUpdate(open=false, votes=null)` → displays reset.

Without VPT only the four jurors vote in Phase B; the referee review cells are
copied from the initial decision at the reveal (`checkPanelVoteComplete()`).

### tracker-core

- [protocol-config.js](../../tracker-core/src/protocol/protocol-config.js):
  `PROTOCOL_VERSION = MINIMUM_PROTOCOL_VERSION = '64.0.0'`; the hub only enforces
  the **minimum**, so a newer owlcms is always accepted. MINOR = new fields.
- [competition-hub.js](../../tracker-core/src/competition-hub.js):
  - `_sanitizeInboundPayload()` (~l.1946) only canonicalises `initialDecision` and
    parses JSON list fields; other strings pass through.
  - `decision` / `timer` messages are merged `{...existingState, ...normalizedParams}`
    (~l.610): **a field absent from a decision message keeps its previous value**.
    Today this is masked because `RESET` sets `decisionsVisible=false`.
  - `update` messages start from the incoming payload and only preserve the fields
    listed in `decisionFields` (~l.629: `decisionEventType d1 d2 d3 decisionsVisible
    down`); jury fields (`juryDecision`, `juryReasonCode`, `panelReview`…) are
    **lost on the first update after a decision message** (e.g. the `BreakStarted`
    update that follows `START_DELIBERATION`).
  - `clearStaleDecisionForExplicitAthleteTimer()` (~l.116) and
    `clearStaleDecisionForCurrentAthleteUpdate()` (~l.132) delete the same six
    fields on `StartTime` and on a `CURRENT_ATHLETE` update.
  - the `decision` broadcast payload (~l.748) exposes `ref1..3`, `juryDecision`,
    `juryReversal`, `juryReasonCode`, `waitForAnnouncer`, …; `getBroadcastSignature`
    (~l.240) is only used for debounce and decision messages are never debounced.
- [timer-decision-helpers.js](../../tracker-core/src/utils/timer-decision-helpers.js)
  `extractDecisionState()`: returns `visible:false` whenever `fopState==='BREAK'` or
  `break==='true'` — **the decision slot is blank during a jury break**;
  `mapDecision()` maps `'true'→'good'`, `'false'→'bad'`, anything else → `null`.
  `computeDisplayMode()` already gives `decision` priority over `break`.
- [presentation-helpers.js](../../tracker-core/src/utils/presentation-helpers.js)
  `inferBreakMessage()` maps `breakType JURY → PublicMsg.JuryDeliberation`,
  `CHALLENGE → PublicMsg.CHALLENGE`; a jury break has `mode=INTERRUPTION`, so the
  attempt bar currently shows the break message as the name and `STOP` in the
  timer slot.
- The tracker receives the full `translation4.csv` (translations zip), so
  `Jury.incorrect.<n>`, `PublicMsg.AdjudicationPanelReview`,
  `JuryDialog.GoodLiftLabel/BadLiftLabel` are translatable locally with
  `hub.translate()`.

### tracker (UI)

Decision lights are rendered from `decision.ref1..3` / `isSingleReferee` / `down` in
`tracker/src/lib/components/CurrentAttemptBar.svelte` (shared by the standard
scoreboards, attempt-bar and team scoreboard) and in
`tracker/src/plugins/attemptboards/attempt-board/page.svelte`; the lower-third
overlay and celebrations also read `decision`. The OBS plugin
(`plugins/OBS/shared/helpers-core.js`) switches scenes on `decision.type ∈
{INITIAL_DECISION, FULL_DECISION, DECISION_VISIBLE, JURY_DECISION}` and ignores
other types — **re-sending `FULL_DECISION` for every jury vote would re-trigger
the decision scene**, so new event types are required.

## 2. Reference behaviour (what the tracker must reproduce)

Cell model, identical to `DecisionElementState` and `DecisionBlockState`: seven
positions `R1 R2 R3 | J1..Jn` (n = jury size, 0 → no circles), each
`empty | voted | white | red`.

| Moment | R1..R3 | J1..Jn | Label |
|---|---|---|---|
| Lift, decision shown (3/4/5 jury) | white/red (single light: R2 only) | each juror: `voted` as votes arrive; all → white/red together | — |
| Review break, before Call for Votes | Phase A pattern | Phase A pattern (jury revealed only if all had voted) | Jury Deliberation / Adjudication Panel Review / Challenge |
| Call for Votes | all `empty` | all `empty` | same |
| Review votes arriving | `voted` per referee (VPT only) | `voted` per juror | same |
| 7th vote, jury-private reveal | unchanged (`voted`) | unchanged (`voted`) | same |
| Decision applied (public reveal) | white/red | white/red | Good Lift / No Lift – `<code> - <rule>` |
| `StartLifting` / `RESET` / vote abandoned | `empty` | `empty` | cleared |

Deliberation without a panel pattern (3/5 jury, manual deliberation): referee
lights of the reviewed decision, no circles (matches the scoreboard with
`DECISION_SECTION_SHOW_BOTH_JURY_VOTES` off), label as above.

## 3. Wire format (additive, protocol 64.1.0)

All values are strings (the forwarder payload is `Map<String,String>`).

| Field | Values | Meaning |
|---|---|---|
| `d1` `d2` `d3` | `true` / `false` / **`hidden`** / absent | referee cells. `hidden` only during a Phase B vote (vote received, not revealed). Unchanged in Phase A. |
| `j1`..`j5` | `true` / `false` / `hidden` / absent | jury cells, only `j1..j<jurySize>` are ever present. `hidden` = voted, not yet disclosed. |
| `jurySize` | `0`..`5` | number of circles to draw (empty circles for jurors who have not voted). Always sent on decision-type messages. |
| `decisionEventType` | + `JURY_UPDATE`, + `PANEL_VOTE` | new event types (see below). |
| `panelVoteOpen` | `true` / `false` | on `PANEL_VOTE` only. |
| `panelReview` | `true` | already on `START_DELIBERATION`; also added to the **update** payload while `fop.isPanelReview()` so it travels with `breakType`. |
| `juryReasonCode` | `1`..`17` | already sent by the WebSocket forwarder on `BAD_LIFT`; add to the HTTP forwarder. Text = `Jury.incorrect.<code>` translation, resolved by the tracker. |
| `singleReferee` / `singleLight` | `false` | explicitly sent on `PANEL_VOTE` and on review `START_DELIBERATION`/`CHALLENGE` so a single-light initial decision does not leave the tracker drawing one cell during a 3-referee review. |
| `fullName` | athlete under review | add to the jury payloads (`createJuryEvent`) so the tracker can name the athlete under review (today only `athleteFull` on `JURY_DECISION`). |

**Semantics: every decision-type message carries the complete cell state; a cell
absent from a decision message means `empty`.** This is only true once the hub
treats decision messages as authoritative for the cell fields (§5.2); on the wire
it costs nothing and keeps `RESET` / `END_DELIBERATION` / abandoned-vote payloads
small.

Event types and when they are sent:

| `decisionEventType` | Trigger (UIEvent) | Cells | Other fields |
|---|---|---|---|
| `INITIAL_DECISION`, `FULL_DECISION`, `DOWN_SIGNAL` | as today | `d*` from the forwarder lights (as today); `j*` Phase A rule from `fop.getJuryMemberDecision()` / `fop.getJurySize()` | as today + `jurySize` |
| `JURY_UPDATE` (new) | `UIEvent.JuryUpdate`, **Phase A only** (see guard) | `d*` from the forwarder lights; `j*` Phase A rule from the event arrays | `decisionsVisible`, `down`, `singleReferee` as current, `jurySize` |
| `RESET` | `DecisionReset` | all absent | `jurySize` |
| `START_DELIBERATION`, `CHALLENGE` | `JuryNotification` | if `fop.getPanelReviewInitialVotes() != null`: 7 cells with the attempt-board rule (referees revealed; jury revealed only if all 4 voted, else `hidden`/absent); else `d*` from `fop.getRefereeDecision()` with the `computeRefLights` single-light rule, no `j*` | `panelReview`, `singleReferee`, `fullName`, `jurySize` |
| `PANEL_VOTE` (new) | `UIEvent.PanelVoteUpdate` | `!open && !complete` → all absent (cleared). Otherwise per cell *i*: `votes != null && publicReveal` → `true`/`false`; else `voted[i]` → `hidden`; else absent | `panelVoteOpen`, `singleReferee=false`, `jurySize=4`, `fullName` |
| `JURY_DECISION` | `JuryNotification GOOD_LIFT/BAD_LIFT` | the **last sent** cell set (after the public reveal these are the 7 revealed votes; for a Chair-entered decision they are whatever was shown) | as today (`juryDecision`, `juryReversal`, `juryReasonCode`, `waitForAnnouncer`, …) |
| `END_DELIBERATION`, `END_CHALLENGE` | `JuryNotification` | all absent | — |

Phase A rule for `j*` (from `Results.pushJuryCircles`): if every juror `1..jurySize`
has voted → `true`/`false`; else each voted juror → `hidden`, others absent.

Message sequences (tracker view):

- 4-member jury, lift: `DOWN_SIGNAL` → `INITIAL_DECISION` → `FULL_DECISION
  (d=W W R, j1=hidden)` → `JURY_UPDATE (j1,j3=hidden)` → … → `JURY_UPDATE (j1..j4 =
  true/false)` → `RESET`.
- Panel review (VPT): … `FULL_DECISION`, `JURY_UPDATE`×4 → `START_DELIBERATION
  (panelReview, 7 revealed cells)` → *update BreakStarted (INTERRUPTION, JURY)* →
  `PANEL_VOTE (open, all absent)` → `PANEL_VOTE (d2=hidden)` → … → `PANEL_VOTE
  (7×hidden)` [7th vote: unchanged, private] → `PANEL_VOTE (open, 7 revealed)` →
  `PANEL_VOTE (closed, 7 revealed)` → `JURY_DECISION (BAD_LIFT, juryReasonCode=5,
  7 revealed)` → `RESET` → *update StartLifting (CURRENT_ATHLETE)*.
- Vote abandoned: `PANEL_VOTE (closed, all absent)`; the deliberation label stays
  until `END_DELIBERATION` / `StartLifting`.

## 4. owlcms forwarder changes

4.1 **One payload helper, used by both forwarders.** Add a small plain-Java class
(e.g. `monitors/websocket/DecisionCellsPayload`, or revive
`ForwarderPayloadBuilder`) with:

- `static void putPhaseACells(Map<String,String> sb, Boolean d1, Boolean d2, Boolean d3, Boolean[] jury, int jurySize)`
- `static void putInitialPatternCells(Map<String,String> sb, Boolean[] sevenVotes)` (attempt-board rule)
- `static void putPanelVoteCells(Map<String,String> sb, boolean[] voted, Boolean[] votes, boolean publicReveal)`
- `static void putReviewedRefereeCells(Map<String,String> sb, FieldOfPlay fop)` (the `Results.computeRefLights` rule)

Pure functions over arrays → unit-testable without Vaadin or a FOP.

4.2 **New subscriptions** in both forwarders (`WebSocketEventForwarder` ~l.859 next
to `slaveJuryNotification`, `EventForwarder` ~l.879):

- `@Subscribe slaveJuryUpdate(UIEvent.JuryUpdate e)`: **guard** — ignore when the
  FOP is in a `JURY`/`CHALLENGE` break (Phase B is driven by `PanelVoteUpdate`;
  this also drops the jury-private `JuryUpdate` at the 7th vote and any stray juror
  press after the reveal). Optional: honour
  `FeatureSwitch.DECISION_SECTION_SHOW_BOTH_JURY_VOTES` by forwarding during a
  break when no panel vote is open, as the scoreboard does. Otherwise
  `pushDecision(DecisionEventType.JURY_UPDATE, e)`.
- `@Subscribe slavePanelVoteUpdate(UIEvent.PanelVoteUpdate e)`: build the
  `PANEL_VOTE` payload; honour `publicReveal` strictly — `e.getVotes()` is used
  **only** when `e.isPublicReveal()`; otherwise only `e.getVoted()` is used. Store
  the resulting cells as `lastCells`.

4.3 **Existing payloads**: `createDecision()` adds `j*`/`jurySize` (Phase A rule;
`RESET` clears); `createJuryEvent()` adds cells per the table, `fullName`,
`singleReferee`, `panelReview` (HTTP forwarder), `juryReasonCode` (HTTP forwarder);
`createUpdate()` adds `panelReview` while `fop.isPanelReview()`. Keep a
`lastCells` map in the forwarder so `JURY_DECISION` repeats the revealed cells.

4.4 Add `JURY_UPDATE` and `PANEL_VOTE` to `DecisionEventType`.

4.5 Bump `WebSocketEventSender.PROTOCOL_VERSION` to `64.1.0` (new fields = MINOR).

4.6 Sender-side debounce (`sendWebSocket()`, identical hash within 1 s suppressed):
consecutive `PANEL_VOTE` messages differ by at least one cell, and the two
`PANEL_VOTE` at the reveal differ by `panelVoteOpen`; nothing is lost. No change.

4.7 Logging: `uiLog(e)` as for the other handlers; no new `warn`.

## 5. tracker-core changes

5.1 [protocol-config.js](../../tracker-core/src/protocol/protocol-config.js):
`PROTOCOL_VERSION = '64.1.0'`, **keep** `MINIMUM_PROTOCOL_VERSION = '64.0.0'` so
older owlcms still connect (they simply never send the new fields).

5.2 [competition-hub.js](../../tracker-core/src/competition-hub.js):

- Define one shared constant `DECISION_CELL_FIELDS = ['d1','d2','d3','j1'..'j5']` and
  `DECISION_STATE_FIELDS = ['decisionEventType', ...DECISION_CELL_FIELDS, 'decision',
  'decisionsVisible','down','jurySize','singleReferee','singleLight','panelVoteOpen',
  'panelReview','juryDecision','juryReversal','juryReasonCode','waitForAnnouncer',
  'actualLift','athleteFull','athleteAbbreviated']`.
- Use `DECISION_STATE_FIELDS` in the two stale-clear helpers (~l.116, ~l.132) and in
  the `update` preserve list (~l.629). This also fixes the existing loss of
  `juryDecision`/`juryReasonCode`/`panelReview` on the `BreakStarted` update.
- In the `decision` merge branch (~l.610): **delete every `DECISION_CELL_FIELDS`
  entry absent from the incoming decision message** (decision messages are
  authoritative for cells). Safe with older owlcms: `RESET` / `DOWN_SIGNAL` /
  `JURY_DECISION` payloads without `d*` already have the lights masked.
- Broadcast `decision` payload (~l.748): add `jury` (array of `'good'|'bad'|'voted'|null`,
  length `jurySize`), `jurySize`, `panelVoteOpen`, `panelReview`, `verdict`,
  `message` (see 5.3). Add `jury` and `verdict` to the debounce signature for
  completeness.
- `_sanitizeInboundPayload()`: no change needed (string pass-through); optionally
  coerce `jurySize` to a number.

5.3 [timer-decision-helpers.js](../../tracker-core/src/utils/timer-decision-helpers.js)
`extractDecisionState()`:

- `mapDecision`: `'true'→'good'`, `'false'→'bad'`, `'hidden'→'voted'`, else `null`
  (unknown values still degrade to no light).
- `jury`: `Array.from({length: jurySize}, (_, i) => mapDecision(fopUpdate['j'+(i+1)]))`;
  `jurySize = parseInt(fopUpdate.jurySize) || 0`.
- `isJuryReview = breakType ∈ {JURY, CHALLENGE} || decisionEventType ∈
  {START_DELIBERATION, CHALLENGE, PANEL_VOTE, JURY_DECISION}`; when `isJuryReview`
  the early `visible:false` return for break state is **skipped**, and
  `visible = hasAnyCell || isJuryReview` (so the label shows even with cleared cells).
  `StartTime` and `SESSION_DONE` still win.
- `verdict`: `{ good, reasonCode, reasonKey: 'Jury.incorrect.'+code }` when
  `decisionEventType === 'JURY_DECISION'` and `waitForAnnouncer !== 'true'`
  (same rule as `DecisionBlockState.onJuryVerdict`), else `null`.
- `messageKey`: `'PublicMsg.CHALLENGE'` for a challenge,
  `'PublicMsg.AdjudicationPanelReview'` when `panelReview==='true'`, else
  `'PublicMsg.JuryDeliberation'`; `null` outside a review. Translation is done by the
  caller (`hub.translate(key, locale)`), with `JuryDialog.GoodLiftLabel` /
  `JuryDialog.BadLiftLabel` for the verdict.
- `computeDisplayMode()`: unchanged — a visible decision already beats the break
  timer, which removes the `STOP` text during the review.

5.4 [presentation-helpers.js](../../tracker-core/src/utils/presentation-helpers.js)
`inferBreakMessage()`: `JURY` + `panelReview` → `PublicMsg.AdjudicationPanelReview`
(pass `fopUpdate.panelReview` through `extractCurrentAttempt`).

5.5 [WEBSOCKET_MESSAGE_SPEC.md](../../tracker-core/docs/WEBSOCKET_MESSAGE_SPEC.md)
§3 DECISION: document `hidden`, `j1..j5`, `jurySize`, `JURY_UPDATE`, `PANEL_VOTE`,
`panelVoteOpen`, `fullName` on jury events; `ReleaseNotes.md`.

## 6. tracker (UI) changes

- New shared `DecisionLights.svelte` (`tracker/src/lib/components/`): props
  `decision`, `translations`; renders `R1 R2 R3` rectangles (`good|bad|voted|pending`),
  a separator, `J1..Jn` circles (`good|bad|voted|pending`, CSS hook `data-juror={i}`
  so J1 can be styled later), and the verdict / review label below. Used by
  `CurrentAttemptBar.svelte` (both compact and full layouts) and
  `attempt-board/page.svelte`; lower-third keeps referee lights only unless asked.
- Down signal condition stays `down && no referee cell set`.
- OBS plugin: no change; verify `JURY_UPDATE` / `PANEL_VOTE` fall through its
  `decision.type` switch (they do today: unknown types are ignored).

## 7. Compatibility

| owlcms | tracker-core | Result |
|---|---|---|
| 64.0.x (old) | new | No `j*`/`jurySize` → `jury=[]`, no circles; behaviour identical to today except the review-break label/lights now show (driven by `breakType`/`START_DELIBERATION`, which old owlcms already sends). |
| new (64.1.0) | 1.17.x (old) | `JURY_UPDATE`/`PANEL_VOTE` are unknown types: `visible` is computed from `decisionsVisible`/`down`, lights unchanged (`hidden` → `null`); `j*`, `jurySize`, `panelVoteOpen` ignored; `MINIMUM_PROTOCOL_VERSION` 64.0.0 accepts 64.1.0. OBS plugin ignores the new types. The private reveal never reaches any tracker version because it is filtered in owlcms. |
| new | publicresults (HTTP) | unknown keys ignored (reads named parameters) — to verify in the publicresults repository, not present in this workspace. |

Blue-owl / MQTT devices are unaffected (different transport, see
phase-b-referee-lights-mqtt.md).

## 8. Tests

owlcms (JUnit 4, `mvn -Dtest=... test` with maintainer consent):

1. `DecisionCellsPayloadTest` (new, pure): Phase A rule for jury sizes 3/4/5
   (partial → `hidden`, complete → `true/false`); initial-pattern rule (jury
   incomplete → `hidden`, referees always revealed); panel-vote rule
   (`publicReveal=false` with `votes != null` → all `hidden`, never values;
   `publicReveal=true` → values; `!open && !complete` → no cells); single-light
   reviewed referee rule.
2. `SevenPersonAdjudicationTest` extension (FOP-driven, records UI events): register
   a forwarder-like recorder and assert the **sequence** of forwarder payloads for
   the panel review scenario (Good Lift and No Lift with rule), the no-VPT review,
   a Chair-entered decision, and an abandoned vote — in particular that no payload
   between the 7th vote and `doJuryDecision` contains a `true`/`false` cell.
3. Guard test: `JuryUpdate` during a `JURY` break produces no payload.

tracker-core (`node tests/...`, add to `npm test`):

4. `jury-cells-test.js`: `extractDecisionState` for `hidden`, `j1..j5`, `jurySize`,
   unknown values → `null`; `verdict` ignores `waitForAnnouncer=true`; `messageKey`
   per break type / `panelReview`; `visible` during a `JURY` break; `StartTime`
   still clears.
5. `decision-merge-test.js`: hub merge — cell absent from a decision message is
   removed; `update` after `START_DELIBERATION` preserves `panelReview`,
   `juryReasonCode`, `j*`; `CURRENT_ATHLETE` update and `StartTime` clear all
   `DECISION_STATE_FIELDS`.
6. Replay test: feed the recorded sequence of §3 (as fixture JSON) through
   `CompetitionHub.handleUpdate` and assert the broadcast `decision.jury` /
   `decision.ref*` at each step.
7. Existing suites (`broadcast-debounce`, `break-resync`, `athlete-timer-lifecycle`)
   still pass.

tracker (vitest):

8. `DecisionLights` component: 0/3/4/5 circles, `voted` vs revealed classes,
   verdict label with rule, single-referee layout.
9. Attempt-board options test still passes with the extended `decision` shape.

Manual (owlcms + tracker running):

10. 3-jury lift, 4-jury lift, panel review Good Lift / No Lift (VPT), jury review
    without VPT, challenge, Resume mid-vote; compare with the owlcms scoreboard side
    by side; confirm the 7th-vote private reveal is not visible on the tracker.

## 9. Open questions

1. **Deliberation without a panel pattern**: show no jury circles (scoreboard
   default), or honour `DECISION_SECTION_SHOW_BOTH_JURY_VOTES` in the forwarder?
   Proposed: honour the switch (cheap, one guard).
2. **Athlete under review on the tracker**: use `fullName` from the jury payloads to
   replace the break message in the name slot (scoreboard behaviour), or keep the
   break message as the name and show lights + verdict only? Proposed: keep the
   break message, add the athlete name in the decision label line
   ("Adjudication Panel Review – Lamy J.").
3. **HTTP forwarder / publicresults**: include it in this change (same helper, no
   publicresults rendering), or leave the HTTP payload untouched until publicresults
   gets a decision block? Proposed: include, since the helper is shared and unknown
   keys are ignored.
4. **`lastCells` on `JURY_DECISION`** vs. recomputing from the FOP: the FOP clears
   `panelVoteMajority` and the reveal flags before pushing the notification, so a
   stored snapshot is the only reliable source. Any objection to the forwarder
   keeping that state?
5. Should `RESET` after a jury verdict keep the revealed cells until `StartLifting`
   (scoreboard ignores `DecisionReset`) or clear at `RESET` (attempt board)? The
   0.5 s difference is invisible in automatic mode; proposed: clear at `RESET`.
