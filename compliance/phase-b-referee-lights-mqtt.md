# Phase B review vote: timeline for the physical box, attempt board and scoreboard

Reference for the seven-person Adjudication Panel review (VPT on). Each row
says what the **physical jury box / referee boxes**, the **Jury Control page**,
the **attempt board** and the **scoreboard decision block** show, and whether
that behaviour is **implemented** (owlcms, uncommitted) or **planned** (needs
the firmata / blue-owl-core changes below). See
[seven-person-adjudication.md](./seven-person-adjudication.md) for the
procedure itself.

No new MQTT topic is introduced. Referee lights use the existing
`owlcms/fop/decision` topic with the same `hidden` convention already used by
`owlcms/fop/juryMemberDecision`.

## Timeline (VPT on)

"Current box" = a blue-owl-core 1.4.0 or legacy-spreadsheet firmata jury box
receiving the owlcms messages of the working tree, with the switch
`panelVoteDeviceIndicators` **off** (the only safe setting for such a box).
"Planned box" = after the firmata `-4` workbooks and the updated blue-owl-core
are deployed and the switch is **on**. Where the two columns are identical the
behaviour does not depend on the device update.

| Step | owlcms MQTT messages | Physical jury box: current | Physical jury box: planned | Referee boxes | Jury Control page | Attempt board | Scoreboard decision block |
|---|---|---|---|---|---|---|---|
| Phase A: referee decision | `fop/decision N good/bad` live | referee LEDs white/red | same | normal confirmation | referee boxes white/red | referee lights, 4 grey jury circles | same as attempt board |
| Phase A: each jury vote | `juryMemberDecision N hidden` | juror LED green | same | — | juror circle grey "voted" | juror circle grey "voted" | same |
| Phase A: 4th jury vote | `juryMemberDecision N good/bad` ×4 | 4 juror LEDs white/red | same | — | 4 circles revealed | 4 circles revealed | same |
| 7-person majority opposes referees → automatic "Adjudication Panel Review" break | `juryDeliberation`, `summon 1,2,3` | **juror LEDs go off** (devices clear on `juryDeliberation`); referee LEDs stay white/red | **all 7 LEDs keep the Phase A pattern** until Call for Votes | summon beep | Phase A pattern kept; Call for Votes highlighted | "Adjudication Panel Review" + Phase A pattern | same |
| Chair presses Call for Votes (or Summon all) | `resetDecisions`, `summon 1,2,3`, `panelReview vote` | all 7 LEDs off | same | re-armed, buzz, "enter decision" | 7 cells empty; Waiting for Votes | 7 cells empty | same |
| Each referee review vote | switch off: nothing. Switch on: `fop/decision N hidden` | **stays off** | referee LED **green** | current: no confirmation. Planned: same feedback as the initial vote | referee cell grey "voted" | referee cell grey "voted" | same |
| Each jury review vote | switch off: nothing. Switch on: `juryMemberDecision N hidden` | **stays off** | juror LED **green** | — | juror cell grey "voted" | juror cell grey "voted" | same |
| 7th vote cast (result known) | `fop/decision N good/bad` ×3, `juryMemberDecision N good/bad` ×4 | **all 7 LEDs white/red** (jury-private reveal, works on current devices) | same | own vote shown; no second confirmation | all 7 revealed; No Lift: rule dialog opens | **still hidden** (7 grey) | **still hidden** (7 grey) |
| Good Lift: 1.5 s grace, decision applied | `refereesDecision good` | unchanged | same | lights off (decision displayed) | result shown | **public reveal**: 7 white/red + Good Lift card (5 s) | 7 white/red + "Good Lift" |
| No Lift: Chair selects the rule, decision applied | `refereesDecision bad` | unchanged | same | lights off | result shown | **public reveal**: 7 white/red + No Lift card with rule (5 s) | 7 white/red + "No Lift – rule" |
| 3.5 s later: lifting resumes automatically | `startLifting` | **unchanged**, 7 LEDs stay white/red | same | — | votes still shown | next athlete | decision block returns to ready |
| Resume (Jury Control or jury box) while the jury cycle is active, or next clock start | `resetDecisions` | all off | same | reset | cleared | — | — |
| Resume in an ordinary break | `startLifting` only (same as Break Management) | unchanged | same | — | — | — | — |
| Resume during a lift before referee decisions | nothing | nothing | same | nothing | nothing | nothing | nothing |

The two rows where current and planned boxes differ are the review-break start
(juror LEDs cleared today, kept after the update) and the review votes (dark
today, green after the update). Everything else, including the private reveal
at the 7th vote and the clear on Resume / clock start, already works on the
current devices with the owlcms code of the working tree.

Notes:

- The jury-private reveal at the 7th vote lets the jury record the votes. The
  public reveal waits for the decision so that a No Lift is shown with its
  rule (TCRR 7.5.7 "Rule Identification").
- The revealed votes stay on the jury box and Jury Control until an explicit
  Resume or the next clock start; the automatic resume after 3.5 s does not
  clear them. This is the same lifecycle as the 3/5-member jury.
- Without VPT the same timeline applies except: the break is a "Jury
  Deliberation" started by the Chair (automatically on a unanimous jury
  opposite to the referees only with `automaticBreakNoVPT` on); Call for Votes
  does not summon or re-arm the referees; only the 4 jurors vote again; at the
  reveal the referees' initial decision is republished with the jury result.

## Jury behaviour toggle switches (owlcms)

All in `FeatureSwitch`; the first is the master switch, the others refine it.
Translation rows for the new ones are in
`shared/src/main/resources/i18n/jury_behaviour_switches_translations.tsv`.

| Switch | Default | Effect | Status |
|---|---|---|---|
| `7personAdjudication` | off | Four-member jury. With VPT: the seven-person Adjudication Panel (7-person majority stops the competition, 7-person review vote). Without VPT: 4-member jury, unanimity rules (7.5.8), Call for Votes reopens the 4 jury votes only. Decision display held for the jury, Speaker never waits, rule shown with a No Lift. | implemented |
| `automaticBreakNoVPT` | **off** | Without VPT only. When on, a unanimous jury opposite to the referees stops the competition automatically for a "Jury Deliberation" (same mechanics as the VPT automatic break, without summoning the referees). When off (TCRR 7.5.8: the deliberation is manual), the jury votes on the lift are only recorded; the Chair presses Jury Deliberation and Call for Votes by hand. Changes nothing else: the second vote, the rule, the reveal and the Resume behaviour are the same. | implemented |
| `panelVoteDeviceIndicators` | **off** | Temporary. Gates only the physical green "vote received" LEDs during a Phase B vote: referee `fop/decision N hidden` and jury `juryMemberDecision N hidden`. Off: physical LEDs stay dark from Call for Votes to the 7th vote; Jury Control is the progress display. The red/white reveal at the 7th vote is not gated. Enable only after the firmata `-4` workbooks and the updated blue-owl-core are deployed (blue-owl-core 1.4.0 misreads `hidden`). | implemented |
| `juryDecisionsOnAttemptBoard` | off | Existing. Shows the jury circles on the attempt board and holds the decision display until the jury has voted (no time limit for a four-member jury). | existing |
| `requireJuryPresidentDecision` | off | Existing, 3/5-member juries. Ignored with `7personAdjudication`: the vote result is applied automatically. | existing |

Competition setting (not a switch): **Video Playback Technology** selects between the two four-member procedures.

## Resume command (same on Jury Control and the physical jury box)

Both post `FOPEvent.JuryResume`; owlcms publishes `UIEvent.JuryDisplayReset`
for the clear, consumed by Jury Control, the jury phone page, the jury
scoreboard and `MQTTMonitor` (`fop/resetDecisions`).

| FOP situation | Resume action |
|---|---|
| Ordinary (non-jury) break | exactly Break Management: `StartLifting`, no jury reset |
| Active lift before referee decisions | no-op |
| Referee decisions shown / jury cycle active | abandon pending votes, clear everywhere, `StartLifting` |

## owlcms changes

Status of the owlcms side (all in the working tree, not yet committed):

| # | Change | Where | Status |
|---|---|---|---|
| 1 | `PanelVoteUpdate` carries `refereeUpdated` (which referee just voted) and `publicReveal` (public boards may show the votes), in addition to `votes` (non-null once the jury-private reveal has happened) | `UIEvent.PanelVoteUpdate`, `FieldOfPlay.pushPanelVoteUpdate` | implemented |
| 2 | Jury-private reveal at the last vote: `FieldOfPlay.checkPanelVoteComplete` sets `panelVoteRevealed`, pushes the 7 votes, and `showJuryMemberDecisionsNow` reveals the jury circles on Jury Control. Without VPT the referees' initial decision is copied into the review votes at that point | `FieldOfPlay` | implemented |
| 3 | Public reveal with the decision: `doJuryDecision` sets `panelVotePubliclyRevealed`; `DecisionElementState` (attempt boards) and `Results` (scoreboard decision block) only use `getVotes()` when `isPublicReveal()` | `FieldOfPlay`, `DecisionElementState`, `Results` | implemented |
| 4 | MQTT reveal of the 3 referee lights at the last vote: `fop/decision N good/bad` via `publishMqttRefereeLight` (individual lights, not the collective `fop/refereesDecision`) | `MQTTMonitor.slavePanelVoteUpdate` | implemented |
| 5 | MQTT green indicators, gated by `panelVoteDeviceIndicators`: `fop/decision N hidden` per referee review vote (VPT only); `juryMemberDecision N hidden` suppressed during a Phase B vote when the switch is off | `MQTTMonitor.slavePanelVoteUpdate`, `slaveJuryUpdate` | implemented, switch default off |
| 6 | Call for Votes publishes `resetDecisions` + `panelReview vote` in both 4-member modes; `summon` only with VPT | `MQTTMonitor.slavePanelVoteCalled` | implemented |
| 7 | Automatic `StartLifting` after the 3.5 s display does not clear the jury box; explicit Resume or the next clock start does (`JuryResume` → `JuryDisplayReset` → `resetDecisions`) | `FieldOfPlay.doJuryResume`, `MQTTMonitor` | implemented (committed in `530297387`) |
| 8 | Feature switches `panelVoteDeviceIndicators` and `automaticBreakNoVPT` (USE_AT_YOUR_OWN_RISK) and their translation rows; see "Jury behaviour toggle switches" | `FeatureSwitch`, `shared/.../jury_behaviour_switches_translations.tsv` | implemented; TSV awaits import |
| 8b | Without VPT, the automatic stop on a unanimous jury is gated by `automaticBreakNoVPT` (default off); otherwise the Chair starts the deliberation by hand | `FieldOfPlay.evaluatePanel` | implemented |
| 9 | Tests: jury-private reveal at the 7th vote, public reveal only with the decision (Good Lift after grace, No Lift after the rule), Resume states | `SevenPersonAdjudicationTest` | implemented |
| 10 | Tests for the MQTT messages themselves (`hidden` only with the switch, individual `good/bad` at the last vote, nothing for referees without VPT) | `MQTTMonitor` test with a stub publisher | **planned** |
| 11 | Document the `hidden` value of `fop/decision` in the MQTT topic documentation | docs | **planned** |

## firmata changes

Create two new four-member profiles:

- `config/BlueOwl-Jury-4.xlsx`, copied from `BlueOwl-Jury.xlsx` (legacy Blue
  Owl jury box used for testing);
- `config/JuryFull-4.xlsx`, copied from `JuryFull.xlsx`.

Keep the existing `BlueOwl-Jury.xlsx` and `JuryFull.xlsx` unchanged as the
legacy 3/5-member profiles. A firmata spreadsheet cannot condition its output
rows on the jury size.

Spreadsheet only, no Java change: firmata matches topic and message exactly,
so an unknown message is ignored.

In both new `-4` workbooks:

1. remove the `Jury N Off (R/G/B)` rows triggered by
   `juryDeliberation` / `challenge`. The Phase A decisions must remain visible
   while the officials review the lift;
2. keep the existing `fop/resetDecisions` rows. Call for Votes sends that
   message and clears all referee and jury LEDs for the second vote;
3. jury position 5 remains inactive. OWLCMS sends jury-member messages only
   for positions 1–4 in four-member mode, so LED 5 is never lit;
4. add the referee `hidden` rows below.

Add, for each referee, three output rows copied from the "Jury N Hidden" rows
(green channel on, red and blue off):

| description | pin (BlueOwl-Jury) | pin (JuryFull) | topic trigger | message | pin action |
|---|---|---|---|---|---|
| Ref 1 Hidden (R) | 10 | 13 | fop/decision | 1 hidden | OFF |
| Ref 1 Hidden (G) | 9 | 12 | fop/decision | 1 hidden | ON |
| Ref 1 Hidden (B) | 8 | 11 | fop/decision | 1 hidden | OFF |
| Ref 2 Hidden (R) | 7 | 9 | fop/decision | 2 hidden | OFF |
| Ref 2 Hidden (G) | 6 | 8 | fop/decision | 2 hidden | ON |
| Ref 2 Hidden (B) | 5 | 7 | fop/decision | 2 hidden | OFF |
| Ref 3 Hidden (R) | 4 | 5 | fop/decision | 3 hidden | OFF |
| Ref 3 Hidden (G) | 3 | 4 | fop/decision | 3 hidden | ON |
| Ref 3 Hidden (B) | 2 | 3 | fop/decision | 3 hidden | OFF |

The existing "Ref N Reset" (`fop/resetDecisions`) and "Ref N Good/Bad"
(`fop/decision`) rows in the new copies handle the clear at Call for Votes and
the reveal. The firmata referee spreadsheets (`Referees.xlsx`,
`RefereesDown.xlsx`, `RefereeSolo.xlsx`) have no `fop/decision` rows, so
referee boxes are not affected.

## blue-owl-core changes

Repository: `blue-owl-core` (1.4.0, maintained by Scott González) — proposed as
a pull request, then picked up by `blue-owl-electron`
(`"blue-owl-core": "^1.4.0"`).

Today `fop/decision` is parsed without checking the value, and every consumer
treats anything other than `good` as `bad`. The change makes `hidden` a valid
value and handles it everywhere a referee decision is consumed.

1. Four-member configuration:
   - widen `OwlcmsConfigEvent.jurySize` in blue-owl-core and `JurySize` in
     blue-owl-electron from `3 | 5` to `3 | 4 | 5`;
   - add an explicit four-member branch in the blue-owl-electron jury
     initializer;
   - use jury button / LED positions **1, 2, 3 and 4**. Do not instantiate
     jury member 5. OWLCMS addresses jury LEDs individually, so LED 5 remains
     off unless a member-5 message is explicitly sent.
2. Jury decision reset timing:
   - existing 3- and 5-member operation is unchanged: `juryDeliberation` and
     `challenge` immediately clear the jury-member LEDs;
   - in four-member mode, keep the Phase A referee and jury decisions visible
     when deliberation / challenge starts;
   - clear them on the `resetDecisions` sent by OWLCMS when the Chair presses
     Call for Votes. This requires a four-member option on `JuryMember` (for
     example `resetOnDeliberation: false`, default `true`) so that 3- and
     5-member devices retain their current behaviour;
   - `resetDecisions` still clears all referee and jury LEDs and resets each
     refbox. Without VPT the referees are not summoned and do not vote again.
3. Types (`src/lib/model/referee/index.ts`): keep `Decision = 'bad' | 'good'`
   for what a referee publishes, and add a received type, e.g.
   `RefereeDecisionState = Decision | 'hidden'`, used by the `decision` event
   in `src/lib/owlcms.ts` (`OwlcmsDecisionEvent.decision`). Export the new
   type from the package root (`src/index.ts`) and widen
   `JuryRefereeDecisionEvent.decision` to the received type.
4. `src/lib/owlcms.ts`: accept `good`, `bad` and `hidden` for `decision`;
   ignore any other value (defensive: an unexpected word must never light a
   red LED or throw).
5. Jury box, RGB referee LEDs (`src/lib/model/jury/referee-rgb-leds.ts`,
   used by blue-owl-electron): on `refereeDecision` with `hidden` →
   `color('green').on()`; `good` → white, `bad` → red as today. The reveal
   then switches the LED to white or red, `resetRefereeDecisions` turns it off.
6. Jury box, separate good/bad LEDs (`src/lib/model/jury/referee-leds.ts`):
   no green LED; on `hidden` leave both LEDs off (today it would look up
   `referee1HiddenLiftLed` and throw).
7. Jury model (`src/lib/model/jury/index.ts`): pass `hidden` through in the
   `refereeDecision` event (type widened as in 3).
8. Referee box (`src/lib/model/referee/index.ts`): pressing the button in the
   Phase B vote gives **the same feedback as the initial vote** (LED flash
   and vibration for good / bad). The box knows what its referee pressed:
   - `publishDecision(decision)` stores the decision it published
     (`this.publishedDecision = decision`, it already clears `confirmed`);
   - on `decision` "N hidden" for its own number, call
     `decisionConfirmed(this.publishedDecision)`: the existing `rgb-led.ts`,
     `confirmation-leds.ts` and `vibration.ts` handlers then run exactly as
     for the initial vote. If nothing was published (unexpected), ignore the
     message (no LED, no throw);
   - the `confirmed` flag already limits this to one confirmation per press:
     a changed vote clears it in `publishDecision` and gets its own
     confirmation, and the `good/bad` sent at the reveal does not re-trigger
     the feedback.
   - `resetDecision()` must clear both `confirmed` and `publishedDecision`, so
     a late `hidden` message cannot confirm a vote from the previous phase.
9. Jury members (`src/lib/model/jury-member/*`): their feedback behaviour is
   unchanged. The jury buttons
   have no LED or vibration motor; the only feedback is the per-member RGB LED
   on the jury box (`rgb-led.ts`: green for `hidden`, white / red at the
   reveal), and it already behaves the same in Phase A and Phase B.
10. Release and integration:
    - publish a new blue-owl-core version;
    - update blue-owl-electron's dependency and `package-lock.json` (the
      current lockfile pins the 1.4.0 tarball);
    - run `npm run build` and `npm test` in blue-owl-core and the normal
      blue-owl-electron build / type check.

## Compatibility

`hidden` on `fop/decision` is only sent during a Phase B vote with VPT and
`panelVoteDeviceIndicators` on. Keep the temporary switch off until physical
devices are updated:

| Switch | Device | Result during Phase B |
|---|---|---|
| off | any | physical LEDs remain off while voting; Jury Control shows progress; physical red/white reveal occurs at the last vote |
| on | firmata with new `-4` spreadsheet | green per referee/juror during the vote; red/white reveal at the last vote |
| on | firmata with legacy spreadsheet | `hidden` ignored; red/white reveal at the last vote |
| on | blue-owl-core with the change | jury positions 1–4; green referee/jury LEDs; referee box feedback identical to the initial vote; red/white reveal at the last vote |
| on | blue-owl-core 1.4.0 | **wrong**: jury box RGB referee LED red, separate-LED jury box throws, refbox shows a red / "bad" confirmation or throws (`confirmation-leds.ts`) |

Release notes for owlcms and blue-owl-electron must state that Blue Owl
devices need the updated blue-owl-core before
`panelVoteDeviceIndicators` is enabled.
