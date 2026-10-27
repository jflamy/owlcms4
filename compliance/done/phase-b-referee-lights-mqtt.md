# Phase B review vote: timeline for the physical box, attempt board and scoreboard

Status: done and archived 2026-10-27. The timeline below is retained as the
implementation record. Its dated “today”, “planned” and “not implemented”
phrasing describes the state when the plan was written and is not an active
backlog; current behavior is documented by
[SEVEN_PERSON_ADJUDICATION.md](SEVEN_PERSON_ADJUDICATION.md) and
[TRACKER_JURY_VOTES.md](TRACKER_JURY_VOTES.md).

Reference for the seven-person Adjudication Panel review (VPT on). Each row
says what the **physical jury box / referee boxes**, the **Jury Control page**,
the **attempt board** and the **scoreboard decision block** show, and whether
that behaviour is **implemented** (owlcms, uncommitted) or **planned** (needs
the firmata / blue-owl-core changes below). See
[SEVEN_PERSON_ADJUDICATION.md](SEVEN_PERSON_ADJUDICATION.md) for the
procedure itself.

No new MQTT topic is introduced. Referee lights use the existing
`owlcms/fop/decision` topic with the same `hidden` convention already used by
`owlcms/fop/juryMemberDecision`. The clear at Call for Votes is the existing
`owlcms/fop/resetDecisions`.

## Timeline (VPT on)

"Current box" = a blue-owl-core 1.4.0 or firmata jury box receiving the
owlcms messages of the working tree, with the switch
`panelVoteDeviceIndicators` **off** (the only safe setting for a blue-owl-core
1.4.0 box; a firmata box ignores `hidden` on `fop/decision` and is safe either
way). "Planned box" = after the updated blue-owl-core or the updated firmata jury
workbooks are deployed and the switch is **on**. Where the two
columns are identical the behaviour does not depend on the device update.

| Step | owlcms MQTT messages | Physical jury box: current | Physical jury box: planned | Referee boxes | Jury Control page | Attempt board | Scoreboard decision block |
|---|---|---|---|---|---|---|---|
| Phase A: referee decision | `fop/decision N good/bad` live | referee LEDs white/red | same | normal confirmation | referee boxes white/red | referee lights, 4 grey jury circles | same as attempt board |
| Phase A: each jury vote | `juryMemberDecision N hidden` | juror LED green | same | — | juror circle grey "voted" | juror circle grey "voted" | same |
| Phase A: 4th jury vote | `juryMemberDecision N good/bad` ×4 | 4 juror LEDs white/red | same | — | 4 circles revealed | 4 circles revealed | same |
| 7-person majority opposes referees → automatic "Adjudication Panel Review" break | `summon 1,2,3` only (**no** `juryDeliberation`: the Phase A votes are kept) | all 7 LEDs keep the Phase A pattern until Call for Votes | same | summon beep | Phase A pattern kept; Call for Votes highlighted | "Adjudication Panel Review" + Phase A pattern | same |
| Chair presses Call for Votes (or Summon all) | `resetDecisions`, `summon 1,2,3` | all 7 LEDs off | same | re-armed, buzz, "enter decision" | 7 cells empty; Call for Votes blue (restart) | 7 cells empty | same |
| Each referee review vote | switch off: nothing. Switch on: `fop/decision N hidden` | **stays off** (firmata: ignored; blue-owl-core 1.4.0: wrong, see Compatibility) | referee LED **green** | current: no confirmation. Planned: same feedback as the initial vote | referee cell grey "voted" | referee cell grey "voted" | same |
| Each jury review vote | switch off: nothing. Switch on: `juryMemberDecision N hidden` | **stays off** | juror LED **green** | — | juror cell grey "voted" | juror cell grey "voted" | same |
| 7th vote cast (result known) | `fop/decision N good/bad` ×3, `juryMemberDecision N good/bad` ×4 | **all 7 LEDs white/red** (jury-private reveal, works on current devices) | same | own vote shown; no second confirmation | all 7 revealed, votes locked; result screen opens (Confirm Good Lift, or the rule dialog for a No Lift; Cancel vote result) | 7 grey "voted" until the Chair confirms (decided 2026-10-22; today all 7 are revealed here) | same as attempt board |
| Good Lift: Chair confirms (Confirm Good Lift, or Good Lift on the jury box), decision applied (decided 2026-10-22; today automatic after 1.5 s) | `refereesDecision good` | unchanged | same | lights off (decision displayed) | result shown | 7 white/red + Good Lift card (5 s) | 7 white/red + "Good Lift" |
| No Lift: Chair selects the rule, decision applied | `refereesDecision bad` | unchanged | same | lights off | result shown | 7 white/red + No Lift card with rule (5 s) | 7 white/red + "No Lift – rule" |
| Chair presses Cancel vote result (decided 2026-10-22, not implemented) | `resetDecisions` | all 7 LEDs off | same | reset | 7 cells empty; Call for Votes orange (state A) | 7 cells empty | same |
| 3.5 s later: lifting resumes automatically | `startLifting` | **unchanged**, 7 LEDs stay white/red | same | — | votes still shown | next athlete | decision block returns to ready |
| Resume (Jury Control or jury box) while the jury cycle is active, or next clock start | `resetDecisions` | all off | same | reset | cleared | — | — |
| Resume after lifting has resumed (clock not started) | `resetDecisions` | all off | same | reset | cleared | cleared | lingering verdict cleared |
| Resume in an ordinary break | `startLifting` only (same as Break Management) | unchanged | same | — | — | — | — |
| Resume during a lift before referee decisions | nothing | nothing | same | nothing | nothing | nothing | nothing |

The only rows where current and planned boxes differ are the review votes
(dark today, green after the update). Everything else, including keeping the
Phase A pattern through the review break, the private reveal at the 7th vote
and the clear on Resume / clock start, already works on the current devices
with the owlcms code of the working tree.

Notes:

- At the 7th vote the jury box and Jury Control reveal the votes and the votes
  are locked. The Chair confirms the result (Confirm Good Lift, or the rule
  for a No Lift) or throws it out with Cancel vote result. The attempt board
  and scoreboards reveal the votes with the decision; for a No Lift the
  scoreboards show the rule (TCRR 7.5.7 "Rule Identification"). Decided
  2026-10-22, not implemented: today the boards reveal at the 7th vote and a
  Good Lift is applied after 1.5 s.
- The revealed votes stay on the jury box and Jury Control until an explicit
  Resume or the next clock start; the automatic resume after 3.5 s does not
  clear them. This is the same lifecycle as the 3/5-member jury.
- Without VPT the same timeline applies except: the break is a "Jury
  Deliberation" started by the Chair (automatically on a unanimous jury
  opposite to the referees only with `automaticBreakNoVPT` on); the referees
  are not summoned automatically in either case (Summon Referees turns orange
  on Jury Control in Phase A as the cue; the Chair decides); Call for Votes
  does not re-arm the referees; only the 4 jurors vote again; at the reveal
  the referees' initial decision is republished with the jury result.

## Jury behaviour toggle switches (owlcms)

All in `FeatureSwitch`, grouped in their own **Jury options** section of the
feature page (`FeatureSwitchSection.JURY_OPTIONS`, between Scoreboard options
and General options). The first is the master switch, the others refine it.
Translation rows for the new switches and the section are in
`shared/src/main/resources/i18n/jury_behaviour_switches_translations.tsv`.

| Switch | Default | Effect | Status |
|---|---|---|---|
| Jury size 4 | 0 | Four-member jury. With VPT: the seven-person Adjudication Panel (7-person majority stops the competition, 7-person review vote). Without VPT: 4-member jury, unanimity rules (7.5.8), Call for Votes reopens the 4 jury votes only. Decision display held for the jury, Speaker never waits, rule shown with a No Lift. | implemented |
| `automaticBreakNoVPT` | **off** | Without VPT only. When on, a unanimous jury opposite to the referees stops the competition automatically for a "Jury Deliberation" (same mechanics as the VPT automatic break, without summoning the referees). When off (TCRR 7.5.8: the deliberation is manual), the jury votes on the lift are only recorded; the Chair presses Jury Deliberation and Call for Votes by hand. Changes nothing else: the second vote, the rule, the reveal and the Resume behaviour are the same. | implemented |
| `panelVoteDeviceIndicators` | **off** | Temporary. Gates only the physical green "vote received" LEDs during a Phase B vote: referee `fop/decision N hidden` and jury `juryMemberDecision N hidden`. Off: physical LEDs stay dark from Call for Votes to the 7th vote; Jury Control is the progress display. The red/white reveal at the 7th vote is not gated. Enable only after the updated blue-owl-core is deployed (blue-owl-core 1.4.0 misreads `hidden`; firmata ignores it harmlessly). | implemented |
| `juryDecisionsOnAttemptBoard` | off | Existing. Shows the jury circles on the attempt board and holds the decision display until the jury has voted (no time limit for a four-member jury). | existing |
| `requireJuryPresidentDecision` | off | Former 3/5-member jury option, now removed: every jury vote result waits for the Jury Chair. | removed |
| `noJuryAlphabeticalShortcuts` | off | Jury Control: when on, the letter shortcuts are not registered (D deliberation, C challenge, T technical, H/I/J/K summon, V Call for Votes), so a stray key on the Jury Control computer cannot start a break or summon the referees. The digit shortcuts used by USB jury keypads remain. Takes effect when the Jury Control page is (re)loaded. | implemented |

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
| 3 | Reveal at the 7th vote on all displays: `DecisionElementState` (attempt boards) and `Results` (scoreboard decision block) use `getVotes()` as soon as it is non-null. `doJuryDecision` sets `panelVotePubliclyRevealed` (`isPublicReveal()`), used by MQTT to avoid sending the referee lights twice | `FieldOfPlay`, `DecisionElementState`, `Results` | implemented |
| 4 | MQTT reveal of the 3 referee lights at the last vote: `fop/decision N good/bad` via `publishMqttRefereeLight` (individual lights, not the collective `fop/refereesDecision`) | `MQTTMonitor.slavePanelVoteUpdate` | implemented |
| 5 | MQTT green indicators, gated by `panelVoteDeviceIndicators`: `fop/decision N hidden` per referee review vote (VPT only); `juryMemberDecision N hidden` suppressed during a Phase B vote when the switch is off | `MQTTMonitor.slavePanelVoteUpdate`, `slaveJuryUpdate` | implemented, switch default off |
| 6 | Call for Votes publishes `resetDecisions` in both 4-member modes (clears and re-arms every device); `summon` only with VPT. No new topic | `MQTTMonitor.slavePanelVoteCalled` | implemented |
| 6b | `juryDeliberation` / `challenge` are not published when the Phase A votes are kept for the review (`getPanelReviewInitialVotes() != null`): the devices clear their jury lights on those topics. Published as before for a manual deliberation or a challenge without VPT, where owlcms clears the votes too | `MQTTMonitor.slaveJuryNotification` | implemented |
| 7 | Automatic `StartLifting` after the 3.5 s display does not clear the jury box; explicit Resume or the next clock start does (`JuryResume` → `JuryDisplayReset` → `resetDecisions`) | `FieldOfPlay.doJuryResume`, `MQTTMonitor` | implemented (committed in `530297387`) |
| 8 | Feature switches `panelVoteDeviceIndicators` and `automaticBreakNoVPT` (USE_AT_YOUR_OWN_RISK) and their translation rows; see "Jury behaviour toggle switches" | `FeatureSwitch`, `shared/.../jury_behaviour_switches_translations.tsv` | implemented; TSV awaits import |
| 8b | Without VPT, the automatic stop on a unanimous jury is gated by `automaticBreakNoVPT` (default off); otherwise the Chair starts the deliberation by hand | `FieldOfPlay.evaluatePanel` | implemented |
| 9 | Tests: jury-private reveal at the 7th vote, public reveal only with the decision (Good Lift after grace, No Lift after the rule), Resume states | `SevenPersonAdjudicationTest` | implemented |
| 10 | Tests for the MQTT messages themselves (`hidden` only with the switch, individual `good/bad` at the last vote, nothing for referees without VPT) | `MQTTMonitor` test with a stub publisher | **planned** |
| 11 | Document the `hidden` value of `fop/decision` in the MQTT topic documentation | docs | **planned** |

## firmata changes

Spreadsheet rows only, no Java change. firmata matches topic and message
exactly (`OutputEventHandler.handle`), so a message with no row is ignored.

Done in the two jury workbooks, `BlueOwl-Jury.xlsx` (legacy Blue Owl jury box
driven by firmata) and `JuryFull.xlsx` (sources in `diagrams/juryFull/`,
synced to `src/main/resources/devices/`): for each referee, three
`Ref N Hidden (R/G/B)` rows inserted after `Ref N Good (B)`, copied from the
"Jury N Hidden" pattern (green channel on, red and blue off). `hidden` is never
sent on `fop/decision` to a 3/5-member jury, so the rows are inert there.

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

Nothing else to change in the workbooks:

- the Phase A pattern is kept through the review break because owlcms does
  not publish `juryDeliberation` / `challenge` in that case, so the existing
  "Jury N Off" rows triggered by those topics are not an issue;
- the existing `fop/resetDecisions` rows clear all referee and jury LEDs at
  Call for Votes;
- jury position 5 stays dark because owlcms never addresses it with a
  four-member jury, and ignores a vote coming from it.

A firmata jury box with the previous workbooks ignores the referee `hidden`
message (LED stays dark until the reveal), so the switch is safe with firmata
either way. The firmata referee spreadsheets (`Referees.xlsx`,
`RefereesDown.xlsx`, `RefereeSolo.xlsx`) have no `fop/decision` rows, so
referee boxes are not affected.

## blue-owl-core changes

Repository: `blue-owl-core` (1.4.0, maintained by Scott González) — proposed as
a pull request, then picked up by `blue-owl-electron`
(`"blue-owl-core": "^1.4.0"`).

The jury box and the referee box are driven by owlcms messages; the only thing
blue-owl-core 1.4.0 cannot do is show a `hidden` referee decision: `fop/decision`
is parsed without checking the value, and every consumer treats anything other
than `good` as `bad`. The change makes `hidden` a valid value and handles it
everywhere a referee decision is consumed. Nothing else changes: jury-member
reset timing, `resetDecisions`, summon and reveal behave as today.

1. Types (`src/lib/model/referee/index.ts`): keep `Decision = 'bad' | 'good'`
   for what a referee publishes, and add a received type
   `RefereeDecisionState = Decision | 'hidden'`, used by the `decision` event
   in `src/lib/owlcms.ts` (`OwlcmsDecisionEvent.decision`) and by
   `JuryRefereeDecisionEvent.decision`. Exported from the package root
   (`src/index.ts`). `OwlcmsConfigEvent.jurySize` is widened to `3 | 4 | 5`
   (type only; the value is not used by the core).
2. `src/lib/owlcms.ts`: accept `good`, `bad` and `hidden` for `decision`;
   ignore any other value (defensive: an unexpected word must never light a
   red LED or throw).
3. Jury box, RGB referee LEDs (`src/lib/model/jury/referee-rgb-leds.ts`,
   used by blue-owl-electron): `hidden` → green, `good` → white, `bad` → red.
   The reveal then switches the LED to white or red, `resetRefereeDecisions`
   turns it off.
4. Jury box, separate good/bad LEDs (`src/lib/model/jury/referee-leds.ts`):
   no green LED; on `hidden` both LEDs are turned off (today it would look up
   `referee1HiddenLiftLed` and throw).
5. Jury model (`src/lib/model/jury/index.ts`): passes `hidden` through in the
   `refereeDecision` event (type widened as in 1).
6. Referee box (`src/lib/model/referee/index.ts`): pressing the button in the
   Phase B vote gives **the same feedback as the initial vote** (LED flash
   and vibration for good / bad). The box knows what its referee pressed:
   - `publishDecision(decision)` stores the decision it published
     (`publishedDecision`, it already clears `confirmed`);
   - on `decision` "N hidden" for its own number, it calls
     `decisionConfirmed(publishedDecision)`: the existing `rgb-led.ts`,
     `confirmation-leds.ts` and `vibration.ts` handlers then run exactly as
     for the initial vote. If nothing was published (unexpected), the message
     is ignored (no LED, no throw);
   - the `confirmed` flag already limits this to one confirmation per press:
     a changed vote clears it in `publishDecision` and gets its own
     confirmation, and the `good/bad` sent at the reveal does not re-trigger
     the feedback;
   - `resetDecision()` clears `publishedDecision`, so a late `hidden` message
     cannot confirm a vote from the previous phase.
7. Release and integration:
   - publish a new blue-owl-core version;
   - update blue-owl-electron's dependency and `package-lock.json` (the
     current lockfile pins the 1.4.0 tarball);
   - `npm run lint` in blue-owl-core (type check + eslint) and the normal
     blue-owl-electron build / type check.

## blue-owl-electron changes

- `src/common.ts`: `JurySize = 3 | 4 | 5`.
- `src/main/initializer/jury.ts`: explicit four-member branch creating jury
  members 1–4 on buttons/LEDs 1–4; station 5 is not instantiated (owlcms
  ignores it anyway).
- `src/renderer/src/components/JurySettings.svelte`: "Four Member Jury" label
  and cabling note (ports 1 to 4, port 5 unused; no fixed seat for the Chair).

## Compatibility

`hidden` on `fop/decision` is only sent during a Phase B vote with VPT and
`panelVoteDeviceIndicators` on. Keep the temporary switch off until physical
devices are updated:

| Switch | Device | Result during Phase B |
|---|---|---|
| off | any | physical LEDs remain off while voting; Jury Control shows progress; physical red/white reveal occurs at the last vote |
| on | firmata with the updated jury workbooks | green per referee/juror during the vote; red/white reveal at the last vote |
| on | firmata with the previous jury workbooks | referee `hidden` ignored (LED dark), juror LEDs green; red/white reveal at the last vote |
| on | blue-owl-core with the change | jury positions 1–4; green referee/jury LEDs; referee box feedback identical to the initial vote; red/white reveal at the last vote |
| on | blue-owl-core 1.4.0 | **wrong**: jury box RGB referee LED red, separate-LED jury box throws, refbox shows a red / "bad" confirmation or throws (`confirmation-leds.ts`) |

Release notes for owlcms and blue-owl-electron must state that Blue Owl
devices need the updated blue-owl-core before
`panelVoteDeviceIndicators` is enabled.
