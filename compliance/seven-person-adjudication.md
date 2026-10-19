# Seven-Person Adjudication Panel

Status: analysis only, no code changes yet
Scope: field of play decision engine, Jury Control, referee phones, MQTT
devices, attempt boards and scoreboards, feature switch, translations
("Jury Chair"), documentation

## Rule Being Analysed

Source: IWF Technical and Competition Rules & Regulations 2026
([IWF-TCRR-2026.pdf](./IWF-TCRR-2026.pdf), published 19 October 2026): 7.5.3,
7.5.7, 7.5.8, 7.5.9, 7.5.10, 7.5.15, 7.5.17, 3.3.7.1, Regulation to 3.3.7
(items 4, 6, 8 and 9) and Regulation to 7.5 (loading errors).

When VPT is in use (7.5.7):

1. **Composition.** The Adjudication Panel has four Jury members and three
   Referees, seven voting officials. At IWF Events the Jury has four members
   (7.5.3).
2. **Initial decision.** The Referees' decision is the initial decision. If the
   combined majority agrees with it, no further action is taken.
3. **Split Jury.** If the Jury is split 2–2, the Referees' decision stands
   without review.
4. **Referral for review.** If the combined majority of the seven is opposite
   to the Referees' initial decision, "the system shall automatically indicate
   'Adjudication Panel Review', stop the competition and initiate the Video
   Playback Review (VPT) procedure. No yellow flag is required."
5. **Review.** Referees and Jury members review the lift on video together.
6. **Re-adjudication.** "Upon completion of the Video Playback Review, the
   Chair of the Jury shall direct the adjudication panel by pressing the
   Referee call buttons on the Referee Control Panel to render a new
   decision."
7. **Final determination.** The majority of the seven is final and is displayed
   on the official scoreboard.
8. **Rule identification.** For an invalid lift, the Technical Rule(s) relied
   upon are displayed on the scoreboard.
9. **Challenge.** Allowed only if the lift has not already been reviewed by the
   Adjudication Panel. A valid challenge triggers a VPT review, then the same
   combined procedure (7.5.8, Regulation to 3.3.7 item 9).

When VPT is not in use (7.5.8): the four-member Jury can overturn the Referees
only when unanimous, the same principle as the current 3- and 5-member juries
without VPT.

Jury members vote on each lift on the Jury Control Unit once the athlete has
replaced the barbell (7.5.8, TCRR 3.3.6).

The 2–2 rule needs no special code: with a 2–2 Jury, the combined majority is
always the referee majority. The panel can only be opposite to the Referees
when at least three jurors oppose a 2–1 decision, or all four oppose a 3–0
decision.

## Product Decisions (given)

- Enabled by a feature switch, id `7personAdjudication`.
- The Jury Console is dropped when the switch is on. Only Jury Control is
  fixed; its bottom indicator row must show the seven votes.
- The referee voting changes need only work with phones and MQTT devices.
  USB keypads (keyboard emulation) are not supported in the first version.

## Terminology

| Term | Code |
|---|---|
| Jury Console | [JuryContent.java](../owlcms/src/main/java/app/owlcms/nui/lifting/JuryContent.java), route `lifting/jury`, label `Jury_Console` |
| Jury Control | [JuryKeypadContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/JuryKeypadContent.java), route `jurykeypad`, label `Jury_Keypad` = "Jury Control" |
| Jury member phone | [JuryMobileContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/JuryMobileContent.java), route `jury` |
| Referee phone | [RefContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/RefContent.java), route `ref` |
| USB keypads | keyboard events in [DecisionElement.js](../owlcms/src/main/frontend/components/DecisionElement.js) `_readRef` (keys 1–6) |
| Initial decision | the 3 referee votes, committed by `commitCurrentDecision()` |
| Review vote | the second vote of the 7 officials during the review (new) |

## Current Behaviour

### Referee decisions

[FieldOfPlay.java](../owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java)

- `refereeDecision[3]` holds the votes. `processRefereeDecisions` emits the
  down signal at 2 matching votes, and on the third vote schedules the
  display and the commit (`processDecisionDelay`).
- `commitCurrentDecision` records the lift on the athlete, stores the three
  referee votes (`Athlete.setDecisions`), updates records and sets
  `athleteUnderReview`.
- The decision stays visible (`DECISION_VISIBLE`) for 3.5 s. When the
  `juryDecisionsOnAttemptBoard` switch is on, the reset waits up to 10 s for the
  jury votes (`scheduleDecisionReset`, `awaitingJuryCompletion`,
  `rescheduleDecisionResetForJury`).
- `DecisionUpdate` in `BREAK` is rejected (`unexpectedEventInState`). Referees
  cannot vote during a jury break.
- Referee phones disable the white button once the decision is shown
  (`RefContent.slaveDecision`) and re-arm only on `DecisionReset`,
  `StartLifting` or `StartTime`.

### Jury votes

- `JuryMemberDecisionUpdate` is accepted **in any state** (handled before the
  state switch). Votes are hidden until all jurors have voted, then revealed
  (`processJuryMemberDecisions`, `UIEvent.JuryUpdate`, MQTT `juryMemberDecision`
  `"n hidden"` then `"n good|bad"`).
- Jury votes are reset on a new clock (`resetDecisions`) and at the start of a
  JURY or CHALLENGE break (`resetJuryDecisions`), so the jury votes again
  during deliberation.
- Jury size is 0, 3 or 5 only: `Competition.setJurySize`/`getJurySize`,
  `Platform.setJurySize`, the Jury Control size selector
  (`createJurySizeSelect`: `0, 3, 5`). Arrays are sized 5
  (`resetJuryDecisions`).

### Jury verdict

- `effectiveJuryVerdict`: 3-person jury needs unanimity, otherwise the referee
  decision stands; 5-person jury uses majority with VPT, unanimity without.
- The verdict is only computed during a JURY/CHALLENGE break. After a 1.5 s
  grace period it posts `JuryDecision` automatically
  (`scheduleAutoJuryDecision`), unless `requireJuryPresidentDecision` is on.
- `doJuryDecision` applies the confirmation/reversal, sets
  `Athlete.setJuryDeliberation` or `setChallenge` for the attempt, fixes records,
  recomputes the order and resumes after 3.5 s.

### Technical rule (reason) for No Lift

Already partly present:

- [JuryRejectionReasons.java](../owlcms/src/main/java/app/owlcms/uievents/JuryRejectionReasons.java)
  lists 17 reasons (`Jury.incorrect.1..17`).
- Jury Control opens a reason dialog for No Lift
  (`openNoLiftReasonDialog`) and sends the code in `JuryDecision`.
- The code reaches the announcer notification
  ([AthleteGridContent.java](../owlcms/src/main/java/app/owlcms/nui/shared/AthleteGridContent.java)),
  `JuryDecisionDialog`, and the tracker (`juryReasonCode` in
  [WebSocketEventForwarder.java](../owlcms/src/main/java/app/owlcms/monitors/WebSocketEventForwarder.java)).
- It is **not** shown on the owlcms attempt board or scoreboards.
- The automatic jury decision (`scheduleAutoJuryDecision`) sends **no** reason.

### Jury Console vs Jury Control

| Function | Console | Control |
|---|---|---|
| Referee lights | yes | yes (top, `JuryDisplayDecisionElement`) |
| Jury vote indicators | yes | yes (row 7, 3 or 5 cells in a 5-column grid) |
| Deliberation, Challenge, Technical | yes | yes |
| Summon referees (all / each) | yes | yes |
| Call Technical Controller | yes (`JuryDialog`) | **no** |
| Good/No Lift with reason | dialog | buttons + reason dialog |
| Athlete grid / lift list | yes | no |
| Jury keyboard shortcuts (1–0, D, C, T, H–K) | yes | yes |
| Jury size selector | menu | select `0/3/5` |

## Gap Analysis

| # | Rule item | Today | Gap |
|---|---|---|---|
| G1 | Panel of 3 referees + 4 jurors | Jury size 0/3/5 only | Allow a 4-member jury when the switch is on |
| G2 | 4-of-7 majority | Jury verdict computed on jury votes only | New panel tally combining `refereeDecision` and jury votes |
| G3 | Automatic stop when panel ≠ referees | Jury must press Deliberation | Automatic review break after the initial decision |
| G4 | Review: majority of 7 is final | Only the jury votes again | Referees must vote again during the break (phones, MQTT) |
| G4b | Chair calls the new decision with the Referee call buttons after the video review | Summon only brings referees to the Jury table | OWLCMS issues the referee recall (`SummonRef`) twice: (1) implicitly when the 7-person vote triggers a review break or the Chair presses Challenge, and (2) explicitly when the Chair presses **Call for Votes** on Jury Control or **Summon Referees (all)** on the keypad/jury box during the break to open the vote |
| G4c | Jury deliberates for other reasons too | Deliberation button | Keep the manual Deliberation button; Call for Votes is available there when a lift is under review |
| G5 | Rule shown for No Lift | Announcer and tracker only; auto decision has no reason | Reason required for a panel No Lift; shown on attempt board/scoreboard |
| G6 | No challenge after panel review | Challenge always allowed | Refuse CHALLENGE when the attempt was reviewed |
| G7 | Jury Control shows 7 votes | Row 7 shows 3 or 5 jurors | 7-cell row: R1 R2 R3 \| J1 J2 J3 J4 |
| G8 | Jury Console dropped | Console in lifting menu | Hide/redirect when switch is on; move Call Technical Controller to Control |

## Proposed Design

### 1. Feature switch and configuration

- Add `SEVEN_PERSON_ADJUDICATION("7personAdjudication", …)` to
  [FeatureSwitch.java](../owlcms/src/main/java/app/owlcms/data/config/FeatureSwitch.java).
  The Java constant cannot start with a digit; the string id can. Section:
  `SPECIALTY_FEATURES` (or `USE_AT_YOUR_OWN_RISK` while experimental).
  Translation key `FeatureSwitch.7personAdjudication` goes in a TSV file.
- Derive the jury size instead of persisting 4: `FieldOfPlay.getJurySize()`
  returns 4 when the switch is on. This avoids changing the `0/3/5`
  validators in `Competition` and `Platform` and the database.
- Jury Control hides the jury size selector when the switch is on.
- `requireJuryPresidentDecision` is ignored when the switch is on (obsolete
  under the new rules, see section 4). The configuration page can note this
  next to that switch.
- The switch gives a four-member jury. The existing
  `Competition.videoPlaybackTechnology` checkbox selects the procedure, as it
  already does for 5-member juries:
  - VPT on: the seven-person panel procedure of this document;
  - VPT off: four-member jury, unanimity needed to overturn (7.5.8). This is
    the existing non-VPT path in `effectiveJuryVerdict`, extended to size 4.

### 2. Panel tally (pure logic)

A small non-UI class, unit-testable, for example `AdjudicationPanel`:

- inputs: `Boolean[3]` referee votes, `Boolean[4]` jury votes;
- outputs: whites, reds, `isComplete()`, `majority()` (`TRUE`/`FALSE` only
  when complete, else `null`), `differsFrom(refereeMajority)`.

**No early result.** The majority is only evaluated once the jury has voted:
all 4 jury votes in phase A, all 7 votes in phase B. Jury members must not
learn each other's votes. Any early outcome (an automatic stop, or a
"confirmed" status before the last juror votes) would reveal how the others
voted. Until then, only "voted / not voted" is shown.

### 3. Phase A: initial decision and automatic stop

1. The referee flow is unchanged: lights, down signal, commit, lifting order,
   normal decision reset. The 3 initial referee votes stay in
   `refereeDecision` and on the athlete.
2. Jury votes arrive as today, in any state, for `athleteUnderReview`. That
   athlete and the referee votes stay in the FOP until the next clock start
   (`restartTimer` → `resetDecisions`). The wait for the jury is therefore
   **indefinite**: no timeout.
   - **Display hold.** When `juryDecisionsOnAttemptBoard` is on, the decision
     stays visible (`DECISION_VISIBLE`) **until all 4 jury votes are in**,
     then the jury circles are revealed and the display is held for the usual
     `juryDecisionHoldMs` before the reset. This replaces the 10 s cap of
     `juryDecisionVisibleMaxMs` used by the legacy jury mode
     (`scheduleDecisionReset` arms no deadline while waiting; only
     `rescheduleDecisionResetForJury` arms one once the jury is complete).
     The announcer's clock-start request during this hold goes through the
     guard of step 4, which is the only way out other than the jury finishing.
   - When `juryDecisionsOnAttemptBoard` is off, the decision clears after the
     normal 3.5 s and the jury keeps voting on `athleteUnderReview` with the
     next athlete displayed; nothing is held.
3. When the 4th jury vote arrives, evaluate the panel (section 2):
   - Panel majority = referee decision: reveal the jury votes; nothing else
     happens.
   - Panel majority ≠ referee decision: post `BreakStarted(JURY, INDEFINITE)`
     automatically for `athleteUnderReview`. If the decision is still visible,
     the break is deferred (`deferredBreak`) until it is cleared. Otherwise it
     starts at once (`CURRENT_ATHLETE_DISPLAYED` for the next athlete). When
     the break starts, OWLCMS also issues the first (implicit) referee recall
     (`SummonRef(0)`) so all 3 referees are called to the video review.
4. **Next clock start while the jury has not finished voting.** A
   `TimeStarted` for the next attempt arrives while the jury votes for
   `athleteUnderReview` are incomplete (switch on, VPT on, at least one jury
   vote missing).
   - The clock is **not** started at first, and the request is **not** silently
     dropped. The dialog "Jury has not finished voting on the previous lift"
     with **Start clock anyway** (override) and **Cancel** is shown on the
     screen that owns the decision for the access mode in use:
     - **Accounts mode** (IWF meets, `Config.isAccountsMode()`): the
       **Competition Supervisor** page
       ([CompetitionDirectorContent.java](../owlcms/src/main/java/app/owlcms/nui/lifting/CompetitionDirectorContent.java),
       always enabled in that mode);
     - **PIN mode**: the **Speaker** (announcer) page
       ([AnnouncerContent.java](../owlcms/src/main/java/app/owlcms/nui/lifting/AnnouncerContent.java)).
     The console that sent the `TimeStarted` (timekeeper, or announcer when it
     is not the deciding screen) gets a notification that the start is pending
     a decision. The Jury Chair is notified on Jury Control.
   - The override posts `TimeStarted` with an override flag. It is the
     recovery path for a technical mishap (a jury device offline, a juror who
     cannot vote, a vote that never completes). The FOP then clears the whole
     pending state and starts the clock as today: if the decision display is
     still held for the jury (step 2), it is reset first (`DecisionReset`),
     then `restartTimer` → `resetDecisions` discards the incomplete jury votes
     and `athleteUnderReview`. There is no automatic stop for that lift; the
     jury can still deliberate by hand.
   - The existing `JuryDecision.MustAnnounceFirst` refusal in
     `CURRENT_ATHLETE_DISPLAYED` / `TimeStarted` is the pattern to follow,
     with an override added.
   - MQTT timekeeper boxes are treated like any other timekeeper console: the
     press is refused and the dialog appears on the Supervisor/Speaker screen.
     The timekeeper never needs to force the start; bypassing the jury is a
     Supervisor/Speaker decision. There is no override from the box (no
     "second press" mechanism).
   - The guard does not apply when no lift is under review, for example after
     a group switch or break.

A distinct notification type (new `JuryDeliberationEventType.PANEL_REVIEW`, or
a flag on `START_DELIBERATION`) lets boards and the announcer show
"Adjudication Panel Review" instead of "Jury deliberation".

A valid challenge (Challenge button) enters the same review (7.5.8) and also
issues the implicit referee recall (`SummonRef(0)`) when the break starts.
After that, the flow is the same as for an automatic review.

#### Manual deliberation stays

The **Deliberation** button remains on Jury Control, and it is still started by
hand. The jury also deliberates in situations that are not an automatic
review right after a lift. Examples:

- the clock was started with the override before the jury finished voting
  (step 4);
- something is noticed after the decision was cleared;
- misconduct (7.5.9), Technical Official issues (7.5.5, 7.5.6), or other
  matters that are not about the lift.

**What the TCRR lets the Jury decide outside the panel vote.** With VPT in use,
the validity of a lift can only change through the Adjudication Panel
procedure: the automatic review (7.5.7, 7.5.10) or a valid Challenge (7.5.7,
Regulation to 3.3.7 item 9), both ending in the 7-person vote. "There is no
appeal against the Referees' / Adjudication Panel's decision except for the
Challenge procedure" (7.5.9). The Chair therefore cannot, in a manual
deliberation, declare a lift Good or No Lift on their own authority. The
decisions the Jury *does* take by itself are of a different kind:

| Situation | Rule | Jury decision | Effect in OWLCMS |
|---|---|---|---|
| Barbell not equally loaded, platform damaged, barbell disarranged, incursion or external interference | 7.5.15 | grant an **additional attempt** | the attempt is voided and re-taken at the same weight |
| Loading error (wrong weight, or not a multiple of 1 kg), serious Speaker error | 7.5.17, Regulation to 7.5 | athlete **accepts** the lift (at the loaded weight, or the next lower multiple of 1 kg) or **refuses** it and gets an additional attempt at the requested weight; a failed lift at a heavier weight, or a 0.5 kg short load on a 2nd/3rd attempt, grants the additional attempt automatically | result recorded at the accepted weight, or attempt voided and re-taken |
| 20 kg rule not respected and noticed after lifts were taken | 6.6.5, Regulation to 6.6.5 item 7 | the non-compliant Clean & Jerk attempts are rendered **No Lift** | administrative correction of recorded attempts, no vote |
| Footwear touching the barbell after a successful attempt | 2.5.1.11 | warning; a repeat is No Lift | warning only on first occurrence |
| Misconduct, replacement of a Technical Official | 7.5.9, 7.5.5 | reprimand / sanction / replacement | no effect on the lift |

So a manual deliberation (switch on, VPT on) ends in one of:

- **Call for Votes** when the lift itself is in question: the normal path, the
  same 7-person majority as an automatic review;
- **Resume** with the lift unchanged;
- **Resume** after an **administrative correction** that is not an
  adjudication: additional attempt (loading error, interference) or 20 kg
  rule No Lift. **Decided:** no new Jury Control action for this. The Chair
  presses **Deliberation**, the Jury and the Technical Controller settle the
  matter verbally, the Competition Supervisor (or Speaker in PIN mode) edits
  the athlete card (void the attempt, change the recorded weight, or mark the
  attempt No Lift) and then resumes the competition from the same screen. No
  votes are called and the review vote machinery is bypassed. Both screens
  already have this: the jury-break notification on `AnnouncerContent`
  (inherited by `CompetitionDirectorContent`) has an **End jury break** button
  that posts `StartLifting`. This must keep working with the switch on, even
  though no vote was called and the Chair never pressed **Resume** on Jury
  Control.

The Chair override on the Waiting for Votes screen (section 6) is **only** the
recovery path for a vote that cannot complete; it is not a way to decide a
lift without the panel.

The three kinds of break share the same review machinery:

| Break | Started by | Lift under review | Call for Votes |
|---|---|---|---|
| Adjudication Panel Review | system (step 3) | yes | prompted (highlighted) |
| Challenge | Chair (Challenge button) | yes | prompted (highlighted) |
| Jury deliberation | Chair (Deliberation button) | if `athleteUnderReview` is set | available, not prompted |

- In a manual deliberation, the Chair decides whether a vote is needed. If no
  vote is called, **Resume** ends the break and the lift is unchanged.
- If a vote is called during a manual deliberation about a lift, the same
  7-person majority applies (switch on, VPT on).
- With the switch on, the jury votes are no longer cleared when the break
  starts (`resetJuryDecisions` in `doTONotifications`). They are cleared when
  the vote is called (Phase B, step B2).

### 4. Phase B: review vote of the seven officials

The review has two steps. The voting is **not** opened automatically. It is
opened by the Chair of the Jury after the video review (7.5.7
"Re-adjudication Following Review").

Step B1, video review in progress (first, implicit referee recall):

- When the automatic review break or Challenge break starts, the scoreboard
  shows the deliberation/challenge and OWLCMS automatically issues the first
  referee recall (`UIEvent.SummonRef(0)`) to summon all 3 referees to the video
  review.
- The break is running. No votes are being collected yet: referee
  `DecisionUpdate`s are refused, as today in `BREAK`.
- Jury Control shows a prompt to the Chair (section 6): review in progress,
  press **Call for Votes** (or Summon Referees on the keypad) when the video
  review is finished.

Step B2, the Chair calls for the vote (second, explicit referee recall):

- The Chair triggers the vote Either by pressing **Call for Votes** on the Jury
  Control screen (section 6) or by pressing **Summon Referees (all)**
  (`FOPEvent.SummonReferee(0)`) on the physical jury box/keypad (or row 4 on
  Jury Control), since physical keypads have no dedicated Call for Votes button
  and the TCRR 7.5.7 wording uses the Referee call buttons to request the new
  decision. In a `JURY` or `CHALLENGE` break with `athleteUnderReview != null`,
  OWLCMS interprets `SummonReferee(0)` as `FOPEvent.PanelVoteCalled`.
- The FOP clears `reviewRefereeDecision[3]` and the jury votes
  (`resetJuryDecisions()`), marks the vote as open, and pushes:
  - the second `UIEvent.SummonRef(0)` (all referees) so every referee device
    **buzzes a second time** to request the vote;
  - a review-vote event so devices re-arm and show "enter decision" rather
    than "see the jury" (section 5).
- Keep the initial referee votes. `reviewRefereeDecision[3]` is separate from
  `refereeDecision[3]`.
- While the vote is open, route `DecisionUpdate` in state `BREAK` to a new
  `doReviewRefereeVote` (switch on, break JURY or CHALLENGE,
  `athleteUnderReview != null`). No down signal, no commit, no lifting-order
  change. Ignore `DownSignal` / `downEmitted` from refboxes (today it would log
  `unexpectedEventInState`).
- Pressing **Call for Votes** again (or Restart Vote) restarts the vote (all
  votes cleared, all referees buzzed). The per-referee Re-call buttons on the
  Waiting for Votes screen re-buzz one referee who has not voted, without
  clearing anything.
- Votes are hidden until all 7 are in, then revealed together. This follows
  the existing jury hidden/reveal pattern. A new `UIEvent` (or `JuryUpdate`
  with a referee part) carries the 7 votes.
- Final decision: the panel majority, computed only when all 7 have voted
  (no early result, section 2). Reuse `scheduleAutoJuryDecision` with the
  7-vote tally (1.5 s grace, revalidated) instead of `effectiveJuryVerdict`.
- `requireJuryPresidentDecision` is **obsolete under the new rules**. The TCRR
  makes the majority of the seven final, so there is no Chair confirmation
  step. With `7personAdjudication` on, the switch is ignored and the vote
  result is applied automatically. The Chair's only route to decide is the
  override on the Waiting for Votes screen (section 6).
- **Jury Chair override.** When the vote goes wrong (a device offline, a
  referee on a USB keypad, a mistaken vote, a vote that never completes), the
  Chair decides on the "Waiting for Votes" screen (section 6). The override
  posts `JuryDecision` directly, as the Good/No Lift buttons do today. The vote
  is then closed and later votes are ignored.
- **Throwing the vote out.** As in Phase A, the Competition Supervisor
  (accounts mode) or Speaker (PIN mode) can abandon whatever is pending: the
  **End jury break** button on that screen ends the break even if the review
  vote is incomplete. A confirmation ("Review vote not finished — end break
  anyway?") is shown in that case. The incomplete votes and
  `athleteUnderReview` are discarded, and the lift keeps the referees'
  decision. So in both phases the "throw it out" path is on the
  Supervisor/Speaker screen, and the "decide the lift" path is the Chair
  override on Jury Control.
- **Override is marked.** A decision entered through the Chair override is
  flagged on the attempt and shown as **"Jury Chair Entered Decision"** on the
  announcer / Competition Supervisor notification (and in the jury
  deliberation record), so it is never mistaken for a panel vote. The
  Technical Controller normally states the reason verbally; OWLCMS does not
  ask for one beyond the No Lift rule.
- No Lift needs a technical rule (G5). A Good Lift is posted automatically.
  For a No Lift, either from the vote or from the override, the reason dialog
  opens on Jury Control (the 17 reasons already exist). The Chair's choice
  posts the `JuryDecision` with the code.
- `doJuryDecision` is reused as is: reversal/confirmation, records, order,
  `setJuryDeliberation` on the attempt, resume after 3.5 s.

### 5. Referee devices

OWLCMS issues the referee recall signal (`SummonRef`) **twice** during a review:

1. **Implicit recall at break start (Step B1):** When the 7-person Phase A tally
   triggers the deliberation break or the Chair presses Challenge, OWLCMS
   automatically sends `SummonRef(0)`. Referee phones beep and show the red
   "Please see jury" banner; MQTT refboxes receive `owlcms/fop/summon/<fop>` and
   buzz/blink yellow. Voting remains closed while the panel reviews the video.
2. **Explicit recall on Call for Votes (Step B2):** When the Chair presses
   **Call for Votes** on the screen (or **Summon Referees (all)** on the keypad
   / jury box, which OWLCMS interprets as Call for Votes while in a review
   break), OWLCMS re-arms the referee devices and sends the recall signal a
   second time to request their votes.

Phones ([RefContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/RefContent.java)):

- On the review-vote call: `resetRefVote()` (re-enables the white button,
  which `slaveDecision` disables), then the same beep as `slaveSummonRef`. Show
  the existing yellow "Please enter decision" banner
  (`JuryNotification.PleaseEnterDecision`, the `warningRow` used by
  `slaveWakeUpRef`), not the red "Please see jury" row. The banner stays until
  the referee votes (`doRedColor`/`doWhiteColor` already restore `topRow`).
- On the review end / `StartLifting`: reset as today.
- `doRed`/`doWhite` already post `DecisionUpdate`; FOP routing (section 4) does
  the rest.

MQTT ([MQTTMonitor.java](../owlcms/src/main/java/app/owlcms/monitors/MQTTMonitor.java)):

- Incoming: reuse `owlcms/refbox/decision/<fop>` (`"n good|bad"`), no change.
- Outgoing on the call, in this order:
  1. `owlcms/fop/resetDecisions/<fop>` so that boxes that lock after voting
     are re-armed (and the referee and jury LEDs on the jury box are cleared);
  2. `owlcms/fop/summon/<fop>` (`1`, `2`, `3` via `publishMqttSummonRef(0)`,
     or a single referee number on re-call), matching the TCRR 7.5.7 "Referee
     call" signal so the refbox buzzer sounds as for a summon (`decisionRequest`
     is not used; it is the wake-up reminder when a referee is slow to vote);
  3. new `owlcms/fop/panelReview/<fop>` `vote` (and `end` when the decision is
     made), for devices that want to tell "come to the jury" from "vote now".
- Document the sequence in
  [200-MQTTMessages.md](../docs/100-AdvancedTopics/200-MQTTMessages.md).
- Config: `mqttJurySize()` will publish 4. Blue Owl declares
  `type JurySize = 3 | 5` (`blue-owl-electron/src/common.ts`), but the value
  is not checked at runtime and the jury size is taken from the owlcms config
  (there is no Blue Owl setting). 4 falls into the 5-member wiring branch in
  `initializer/jury.ts`: stations 1–5 are created. owlcms drives the box with
  4-member behaviour, so **no Blue Owl change is needed**: the hidden (green)
  and reveal (white/red) `juryMemberDecision` messages are sent per juror for
  jurors 1 to `jurySize` only, so the station 5 LED stays dark.
  owlcms-firmata support for 4 is unverified.
- **Ignore jury votes beyond the jury size.** The station 5 buttons stay live
  on a 5-station box. Today a `5 good|bad` vote is stored in
  `juryMemberDecision[4]` (array of 5), is not counted by
  `processJuryMemberDecisions`, but still pushes a `JuryUpdate` for juror 5,
  so owlcms publishes `5 hidden` (station 5 turns green) and Jury Control
  receives an update for a juror it does not show. Fix in
  `FieldOfPlay.doJuryMemberDecisionUpdate`: drop the event (debug log, no
  state change, no UI event) when `refIndex < 0 || refIndex >= getJurySize()`.
  This applies to all sources (MQTT, phones, keypads) and to any jury size,
  not only when `7personAdjudication` is on.
- Referee voting without a running clock works by design: accepting referee
  decisions when the clock has not been started is an existing safety measure
  (in case the timekeeper forgets to start the clock), so refboxes do not gate
  button presses on clock state (verified in Blue Owl `referee/buttons.ts` and
  `referee/index.ts`), and `resetDecisions` re-arms any box that locks after
  voting.

USB keypads: `DecisionElement._readRef` only works while lifting and posts a
3-referee update. Not supported in review mode. Document that keyboard
referee input cannot take part in the review vote. In such setups the Jury
Chair uses the override on the Waiting for Votes screen.

Jury member devices: the jurors vote again after the same call. The call
clears the jury votes and pushes a `JuryUpdate` reset. Jury phones
([JuryMobileContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/JuryMobileContent.java))
and MQTT jury boxes re-arm, as they do today at the start of a deliberation.
Jury devices do not buzz; the jurors are at the table.

### 6. Jury Control (fix)

[JuryKeypadContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/JuryKeypadContent.java)

- Row 7 (the bottom indicator row, "Jury Decisions") becomes the panel row:
  seven cells `R1 R2 R3 | J1 J2 J3 J4`, with a visual separator and a label
  change ("Adjudication Panel"). The page grid has 5 columns; use a nested
  7-column grid spanning `1 / -1` instead of the current offset logic
  (`offset = nbJurors < 5 ? 1 : 0`, `totalEmpty = 5 - nbJurors`).
- Phase A: referee cells show the referee colours at once (they are public);
  jury cells keep the hidden/reveal behaviour (`juryVote`, `checkAllVoted`).
- Phase B: all 7 cells reset to "not voted" when the Chair calls the new
  decision, show "voted" in grey, reveal together.
- Show the panel status only once the votes are complete: "Panel confirms" or
  "Review required", with the tally. Before that, show only the count of
  votes received, never a partial tally.
- **Call for Votes button** (new on-screen button; during a review break,
  pressing **Summon Referees (all)** on row 4 or on a physical keypad/jury box
  does the same thing, since physical devices have no Call for Votes button and
  the Chair must use the screen anyway to pick a No Lift technical rule).
  - Placement: row 8 holds `Deliberation | Challenge | No Lift | Good Lift |
    Technical`. With the switch on, Call for Votes replaces No Lift / Good Lift
    (columns 3–4). The Jury Chair override moves to the "Waiting for Votes"
    screen below. The manual Deliberation button stays where it is.
  - Enabled during a JURY or CHALLENGE break with a lift under review.
    Disabled otherwise, for example in a technical break or a deliberation not
    about a lift.
  - **Prompt** (step B1): during an automatic review or a challenge, before the
    vote is open, the button is highlighted (blinking or accent colour). The
    header banner (`reviewHeader`) reads "Adjudication Panel Review — after
    the video review, press Call for Votes". During a manual deliberation the
    button is available but not highlighted.
  - Keyboard shortcut: `Key.KEY_K` (existing Summon All Referees shortcut) and
    `V` both trigger Call for Votes when in a review break.
- **"Waiting for Votes" screen** (new). It opens when Call for Votes is pressed
  and stays up until a decision is made. It can be a full-screen dark
  `Dialog`, like the No Lift reason dialog, or a swap of the lower half of the
  grid. It contains:
  - the 7-cell panel row (`R1 R2 R3 | J1 J2 J3 J4`): grey ring = not voted,
    grey dot = voted, colours revealed when all 7 are in;
  - a counter "n / 7 votes" and the athlete, attempt and weight under review
    (from `reviewHeader`);
  - **Re-call** buttons per referee, shown under the referees who have not
    voted. They buzz that referee again (`SummonReferee`) without clearing
    votes;
  - **Restart Vote**: clears all votes and buzzes again (same as Call for
    Votes);
  - **Jury Chair override**: **Good Lift** and **No Lift** (No Lift opens the
    reason dialog). Used when the votes go wrong. It closes the vote and posts
    the `JuryDecision`;
  - **Cancel**: closes the screen and discards the votes. The break continues
    and Call for Votes can be pressed again.

  When all 7 votes are in, the screen shows the revealed votes and the
  majority. A Good Lift is applied after the 1.5 s grace period; a No Lift
  opens the reason dialog. The screen then closes. Other Jury Control pages
  open on the same platform follow the vote state, as Jury Control already
  re-syncs from FOP state (`syncWithFopState`).
- Summon buttons (row 4): outside a lift review, they summon referees to the
  Jury table as today; during a `JURY` or `CHALLENGE` break with a lift under
  review, **Summon Referees (all)** (`SummonReferee(0)`) acts as **Call for
  Votes**, and individual referee buttons (`1..3`) re-call that referee while
  the vote is open.
- Deliberation, Challenge, Technical and Resume are unchanged.
- `getNbJurors()`, `registerShortcuts()`, `postJuryMemberDecision()` work with
  4 jurors as is (keys 1–8).
- Add **Call Technical Controller** (`SummonReferee` with ref number 4), which
  only the Console has today.
- Challenge button: refuse when the attempt has already been reviewed
  (section 8).

### 7. Jury Console (drop)

- [LiftingNavigationContent.java](../owlcms/src/main/java/app/owlcms/nui/lifting/LiftingNavigationContent.java):
  do not add the `Jury_Console` button when the switch is on.
- `lifting/jury` route: redirect to `jurykeypad` (keeping `fop`), or show a
  notice, when the switch is on. Bookmarked URLs keep working.
- `JuryContent`, `JuryDialog` and `JuryDecisionDialog` are left unchanged for
  the non-switch mode.
- Features lost with the Console: athlete grid, sound toggle. Call Technical
  Controller is moved to Control (section 6).
- Update [020-Jury.md](../docs/060-Refereeing/020-Jury.md).

### 8. Challenge after review

- In `BreakStarted` handling, when the switch is on and
  `requestedBreak == CHALLENGE`, refuse if the attempt under review already has
  a jury deliberation result (`Athlete` jury deliberation for the attempt is
  non-null), with an error notification (new translation key).

### 9. Displays and forwarders

- **Panel layout on the attempt board and scoreboards (decided).** The seven
  votes are shown as `R1 R2 R3 | J1 J2 J3 J4`: the three referees as the
  existing **rectangles**, then the four jury members as **circles** in
  numbered order. The same layout is used in both phases:
  - Phase A (the lift): the referee rectangles light up as today when the
    decision is shown; the jury circles follow the existing hidden ("voted")
    then revealed pattern once all four have voted.
  - Phase B (the review vote): all seven cells are cleared when the Chair
    calls for votes, show "voted" as votes arrive, and are revealed **all at
    once** when the seventh vote is in.
  - The Jury Chair is **jury member 1** (J1) for now, so the first circle is the
    Chair's. Add a CSS hook per juror index so J1 can be given a distinct
    colour (ring or fill) later if the seating practice makes it useful; no
    colour change in the first version.
- Rendering: jury circles come from the vote array (`DecisionElement.js` maps
  `juryDecisions`; `DecisionBlockState.renderJuryCircles(votes, jurySize)`);
  the referee rectangles already exist. Check the layout with 4 circles and
  with the referee rectangles re-cleared and re-revealed in Phase B (today the
  rectangles are only driven by the live referee decision).
- Show the technical rule on the attempt board for a panel No Lift
  ([JuryNotificationCard.java](../owlcms/src/main/java/app/owlcms/displays/attemptboard/JuryNotificationCard.java)
  does not show it today). The scoreboard decision area is a candidate too.
- Jury scoreboard page
  ([JuryDecisions.java](../owlcms/src/main/java/app/owlcms/displays/scoreboard/JuryDecisions.java)):
  check with 4 jurors.
- Tracker: `juryReasonCode` is already sent. Add the panel review event and the
  7 votes if the tracker should show them (`tracker-core/src/competition-hub.js`).

### 10. Data and reports

- `Athlete` keeps the 3 initial referee votes per attempt; jury and review
  votes are not stored (same as today). The attempt's jury deliberation flag
  records that a review happened. No database change is needed for a first
  version.
- If the protocol must list review outcomes or reasons, that is a separate
  change.

### 11. Terminology: "Jury President" becomes "Jury Chair"

The TCRR says "Chair of the Jury" (7.5.3, 7.5.7). This change applies to all
languages and does not depend on the switch.

- Translation key whose text changes: `JuryPresident` ("Jury President").
- `FeatureSwitch.requireJuryPresidentDecision` is not reworded. The switch is
  obsolete under the new rules (section 4) and only applies when
  `7personAdjudication` is off, so it can keep its legacy wording. It can be
  retired with the legacy jury mode.
- **Keep the key names.** `JuryPresident` is also the assignment id in
  [OfficialRole.java](../owlcms/src/main/java/app/owlcms/data/technicalofficial/OfficialRole.java)
  (`JURY_PRESIDENT("JuryPresident", …)`) and
  [TeamRole.java](../owlcms/src/main/java/app/owlcms/data/technicalofficial/TeamRole.java),
  used by session officials and timetables. Renaming it would break existing
  data. Change only the displayed text; `Java` identifiers can stay too.
- New keys from this analysis should use "Chair" (for example
  `JuryNotification.CallForVotes`, `JuryKeypad.WaitingForVotes`,
  `JuryKeypad.ChairOverride`).
- Not affected: `Books.OfficialPresident` ("President:"), which is a federation
  officer and not the jury.
- Per language, use the IWF term for "Chair":
  - many languages already say chair: sv *Juryordförande*, da *Juryformand*,
    fi *puheenjohtaja*, pl *Przewodniczący*, ja *審判長*, zh_HANT *主席*;
  - others say president (es *Presidente*, fr *Président*, pt *Presidente*,
    ru *Президент*, de *Präsident*, ro, hu, he, ar, el, et) and need a
    translator's call; for example ru *Председатель жюри*, de
    *Jury-Vorsitzender*, et *Žürii esimees*.
- Pre-existing data issues seen in the `JuryPresident` row: hy is "Delays
  Jury", ia is "364", fo is untranslated. Fix them in the same pass.
- Process: a TSV in `shared/src/main/resources/i18n/` (header copied from
  `translation4.csv`) for the external translation tool. `translation4.csv`
  is not edited.
- Docs: replace "jury president" in
  [020-Jury.md](../docs/060-Refereeing/020-Jury.md) and
  [180-Keypads.md](../docs/100-AdvancedTopics/180-Keypads.md).

## Effort and Phasing

| Phase | Content | Size |
|---|---|---|
| 1 | Switch, derived jury size 4, ignore jury votes beyond the jury size (station 5), panel tally class + unit tests | S |
| 2 | Phase A: panel evaluation on the 4th jury vote, automatic review break, clock-start guard with override, notifications | M |
| 3 | Phase B: Call for Votes (buzzer + re-arm), referee review votes in BREAK, phones, MQTT reset/summon/topic, final verdict | M–L |
| 4 | Jury Control 7-cell row, Call for Votes button and prompt, Waiting for Votes screen with Jury Chair override, Call TC, challenge refusal, reason flow | M |
| 5 | Drop Console (menu, route), displays with 4/7 circles, rule on attempt board | S–M |
| 6 | Docs (Jury, MQTT messages), TSV translations including "Jury Chair" and "Jury Chair Entered Decision", device tests | S |

Riskiest parts: the FOP state machine (clock-start guard and override,
automatic break from `CURRENT_ATHLETE_DISPLAYED` or deferred from
`DECISION_VISIBLE`, accepting referee input in `BREAK`) and real-device
behaviour after `resetDecisions`.

## Open Questions

Answered by the TCRR text:

- The switch with VPT gives the panel procedure; without VPT, a four-member
  unanimous jury (7.5.8). See section 1.
- A challenge uses the same combined procedure (7.5.8, Regulation to 3.3.7
  item 9). See section 3.
- The referee recall (`SummonRef`) is issued twice: first implicitly when the
  7-person vote triggers the review break or the Chair presses Challenge (to
  call the referees to the video review), and a second time explicitly when the
  Chair presses Call for Votes on the screen or Summon Referees (all) on the
  keypad/jury box (7.5.7) to open the new vote. See sections 3–6.
- Deliberation stays a manual button, separate from the automatic review.
  See section 3.

Decided:

- After Call for Votes, a "Waiting for Votes" screen holds the Jury Chair
  override (Good Lift / No Lift with rule), used when the votes go wrong. This
  also covers referees who cannot vote (for example on USB keypads). See
  sections 4 and 6.
- "Jury President" becomes "Jury Chair" in all translations; the keys are kept.
  See section 11.
- The Jury Chair picks the Technical Rule for a No Lift.
- No timeout for the jury votes. The lift under review and its votes are kept
  until the next clock start. When jury decisions are shown on the attempt
  board, the decision display is held until the jury has voted (no 10 s cap).
  A clock start with incomplete jury votes asks for an override ("Start clock
  anyway") instead of being blocked. See section 3, steps 2 and 4.
- The panel result is only evaluated once the jury has voted (all 4 in phase
  A, all 7 in phase B). Jury members must not learn each other's votes. See
  section 2.
- The clock-start override (step 4) is decided on the Competition Supervisor
  page in accounts mode (IWF meets) and on the Speaker page in PIN mode; the
  timekeeper only gets a notification. See section 3, step 4.
- On MQTT refboxes, the second-vote buzzer uses `summon` (`publishMqttSummonRef`),
  matching the TCRR 7.5.7 "Referee call" wording, rather than `decisionRequest`
  (the slow-referee wake-up reminder). Referee boxes also accept votes without
  a running clock as an existing safety measure (in case the timekeeper forgets
  to start the clock). See section 5.
- Boards show all seven votes, referees as rectangles then jury as circles in
  numbered order, in both phases; the review vote is revealed all at once. The
  Jury Chair is jury member 1; a distinct colour for that circle is a later
  decision. See section 9.

- Manual deliberation about a lift (switch on, VPT on): the lift can only be
  re-adjudicated by the 7-person vote (7.5.9: no appeal except the Challenge
  procedure). The Chair override is a recovery path for a vote that cannot
  complete, not a way to decide without the panel. Loading errors and external
  interference are handled by granting an additional attempt (7.5.15, 7.5.17,
  Regulation to 7.5), not by a vote. See section 3, "Manual deliberation
  stays".
- Loading errors and interference: no new Jury Control action. The Chair
  presses Deliberation, everything is settled verbally with the Technical
  Controller, the Competition Supervisor (Speaker in PIN mode) corrects the
  athlete card and resumes the competition from that screen, without calling
  for votes.
- A decision entered through the Jury Chair override is marked "Jury Chair
  Entered Decision" on the announcer / Competition Supervisor notification and
  in the deliberation record. See section 3, Phase B.
- MQTT timekeeper boxes have no clock-start override. A press while jury votes
  are missing is refused like any other console; the override is only on the
  Competition Supervisor / Speaker screen. See section 3, step 4.
- When jury votes do not come in, in Phase A or Phase B, the path to abandon
  what is pending is on the Competition Supervisor screen (accounts mode) or
  the Speaker screen (PIN mode): "Start clock anyway" in Phase A, "End jury
  break" with a confirmation in Phase B. Both discard the incomplete votes and
  the lift keeps the referees' decision. To decide the lift instead, the Chair
  uses the override on Jury Control (marked "Jury Chair Entered Decision").

No questions are open.
