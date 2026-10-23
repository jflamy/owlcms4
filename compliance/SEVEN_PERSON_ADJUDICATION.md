# Seven-Person Adjudication Panel

Status: **done** (2026-10-23). Implemented and tested for the four-member
jury, with or without VPT, including the Chair confirmation, **Cancel vote
result**, the No Lift rule request and the Speaker dialogs, and for 3- and
5-member juries (section 12); Playwright scenarios 1-16 pass. The follow-ups
(documentation, MQTT tests, device tests, translation keys) are listed under
[Remaining Work](#remaining-work). The device timeline is in
[done/phase-b-referee-lights-mqtt.md](done/phase-b-referee-lights-mqtt.md).
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

When VPT is not in use (7.5.7, last paragraph; 7.5.8): the four-member Jury
can overturn the Referees only when unanimous. "Such decision and its reasons
must be communicated to the Speaker ... via the Technical Controller ... and
announced by the Speaker." OWLCMS uses the jury deliberation procedure of
section 12, shared with 3- and 5-member juries:

- **Trigger.** The TCRR leaves the deliberation to the Jury Chair, so by
  default the break is started by hand (Jury Deliberation button). With the
  `automaticBreakNoVPT` switch on, a unanimous four-member jury opposite to
  the referees stops the competition automatically.
- **Who votes.** The referees do not vote again; the four jurors may vote
  again. A vote is not required: the Chair can give the decision directly.
- **Decision.** The Chair gives it (Jury Control or jury box), or the Speaker
  enters what the Technical Controller relays. When the boards show it
  depends on how it was entered (section 13).
- **Summoning the referees.** Calling the referees is the Chair's decision
  (7.5.8): neither the manual nor the automatic (`automaticBreakNoVPT`) break
  summons them. Jury Control prompts the Chair in Phase A: **Summon Referees**
  turns orange when the four jury votes on the lift are unanimously opposite
  to the referees, and returns to normal once the deliberation has started.
- **Jury vote in the review (decided 2026-10-23).**
  - Review started by the Chair (Deliberation or Challenge): the jury lights
    are cleared when the break starts and the jurors may vote again at once;
    the Chair asks for the vote verbally. Summon Referees (all) summons the
    referees, as the TCRR has it ("the Jury may call the Referees in
    question").
  - Automatic stop (`automaticBreakNoVPT`, an OWLCMS option not in the TCRR):
    same as with VPT. The jury votes that caused the stop stay displayed, and
    Summon Referees (all) becomes Call for Votes, which clears them and opens
    the new vote. The referees are summoned with the individual buttons 1 / 2
    / 3.

The break is titled "Jury Deliberation" (not "Adjudication Panel Review"). The jury behaviour switches
are listed in
[phase-b-referee-lights-mqtt.md](done/phase-b-referee-lights-mqtt.md).

Jury members vote on each lift on the Jury Control Unit once the athlete has
replaced the barbell (7.5.8, TCRR 3.3.6).

The 2–2 rule needs no special code: with a 2–2 Jury, the combined majority is
always the referee majority. The panel can only be opposite to the Referees
when at least three jurors oppose a 2–1 decision, or all four oppose a 3–0
decision.

## Product Decisions (given)

- Enabled by a feature switch, id `7personAdjudication`.
- The Jury Console is removed entirely, with or without the switch (decided).
  Jury Control is the only jury page; its bottom indicator row must show the
  seven votes.
- The referee voting changes need only work with phones and MQTT devices.
  USB keypads (keyboard emulation) are not supported in the first version.

## Terminology

| Term | Code |
|---|---|
| Jury Console | removed (was `JuryContent`, route `lifting/jury`, label `Jury_Console`) |
| Jury Control | [JuryKeypadContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/JuryKeypadContent.java), route `jurykeypad`, label `Jury_Keypad` = "Jury Control" |
| Jury member phone | [JuryMobileContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/JuryMobileContent.java), route `jury` |
| Referee phone | [RefContent.java](../owlcms/src/main/java/app/owlcms/nui/referee/RefContent.java), route `ref` |
| USB keypads | keyboard events in [DecisionElement.js](../owlcms/src/main/frontend/components/DecisionElement.js) `_readRef` (keys 1–6) |
| Initial decision | the 3 referee votes, committed by `commitCurrentDecision()` |
| Adjudication Panel, panel | only the seven-person panel: 3 referees and 4 jurors, `7personAdjudication` on and VPT in use (`FieldOfPlay.isPanelProcedure`). "Panel" is used for nothing else |
| Review vote | the vote during a review: the panel vote (7 officials) or the jury vote |
| Jury vote | the review vote of the jury alone, for every jury that is not the panel (3 members with or without VPT, 4 members without VPT, 5 members). The referees do not vote again; their decision on the lift stays shown on every display while the jurors vote. Code names (`PanelVoteUpdate`, `panelVoteOpen`...) cover both kinds of review vote |

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
  `juryDecisionsOnAttemptBoard` switch is on, the reset waits indefinitely for
  all configured jurors to vote (`scheduleDecisionReset`,
  `awaitingJuryCompletion`, `rescheduleDecisionResetForJury`).
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
| G6 | No challenge after panel review | Challenge always allowed | None: the jury applies the rule (decided, section 8) |
| G7 | Jury Control shows 7 votes | Row 7 shows 3 or 5 jurors | 7-cell row: R1 R2 R3 \| J1 J2 J3 J4 |
| G8 | Jury Console dropped | Console in lifting menu | Hide/redirect when switch is on |

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
- `requireJuryPresidentDecision` is removed (decided 2026-10-22): the Chair
  confirmation of the vote result is always on, for every jury size (sections
  4 and 12).
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
     stays visible (`DECISION_VISIBLE`) **until all configured jury votes are in**,
     then the jury circles are revealed and the display is held for the usual
     `juryDecisionHoldMs` before the reset. There is no jury-vote timeout:
     `scheduleDecisionReset` arms no deadline while waiting; only
     `rescheduleDecisionResetForJury` arms one once the jury is complete).
     The announcer's clock-start request during this hold goes through the
     guard of step 4. A review started by the Chair (Jury Deliberation or
     Challenge) also ends the hold (decided 2026-10-23): a jury box that does
     not work would otherwise keep the decision displayed and the deliberation
     deferred for ever. The display ends after its usual minimum, then the
     break starts with the votes on the lift kept; the vote called in that
     review cannot complete either, so the Chair gives the decision with the
     Good Lift / No Lift buttons (marked "Jury Chair Entered Decision").
     The hold applies to every configured jury (3, 4 or 5 members), with or
     without VPT. A four-member jury without VPT triggers the stop only with
     `automaticBreakNoVPT` on (7.5.8 makes the deliberation manual); 3- and
     5-member juries never trigger a review automatically.
   - When `juryDecisionsOnAttemptBoard` is off, the decision clears after the
     normal 3.5 s and the jury keeps voting on `athleteUnderReview` with the
     next athlete displayed; nothing is held. The next clock is nevertheless
     refused until every juror has voted, the Chair/Speaker enters the jury
     decision, or the Speaker/Competition Director chooses **Start Clock
     Anyway** (which discards the incomplete jury votes and leaves the
     referees' decision in place).
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

A valid challenge (Challenge button) follows exactly the procedure of the
corresponding jury break; only the trigger (the Chair, after a Challenge
Card) and the label ("Challenge") differ:

- **With VPT:** same as the automatic Adjudication Panel Review. The lift's
  referee and jury votes are kept and shown until Call for Votes, all
  referees are recalled (`SummonRef(0)`), and the 7-person majority decides.
- **Without VPT:** same as a jury deliberation opened by the Chair. No
  automatic summon (Chair's decision), the four jurors vote again and must be
  unanimous to reverse. (The TCRR describes challenges only with VPT;
  OWLCMS allows it and applies the no-VPT jury procedure.)

Validity (once per athlete, timing, lift not already reviewed) is checked by
the Jury, not by OWLCMS (section 8).

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

The Chair override (the No Lift / Good Lift buttons during a review, section
6) is **only** the recovery path for a vote that cannot complete; it is not a
way to decide a lift without the panel.

The three kinds of break share the same review machinery:

| Break | Started by | Lift under review | Call for Votes |
|---|---|---|---|
| Adjudication Panel Review | system (step 3) | yes | prompted (highlighted) |
| Challenge | Chair (Challenge button) | yes | prompted (highlighted) |
| Jury deliberation | Chair (Deliberation button) | if `athleteUnderReview` is set | prompted (highlighted) |

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
- Pressing **Call for Votes** again restarts the vote (all votes cleared, all
  referees buzzed). While the vote is open, the individual referee buttons on
  row 4 of Jury Control re-buzz one referee who has not voted, without
  clearing anything.
- Votes are hidden until all 7 are in (displays show who has voted in gray).
  At the last vote, Jury Control, the physical jury box, the attempt board and
  the scoreboards reveal the referee and jury votes red/white (boards: where
  the jury votes are shown, section 9). The decision, with the rule for a No
  Lift, pops over them when the Chair confirms it (decided 2026-10-23). If the
  Chair cancels the vote result, the boards are cleared.
- **The votes are locked once all 7 are in.** A vote changed after that
  (referee device, jury device or keypad) is ignored. Only Cancel vote result
  or a new Call for Votes reopens the voting.
- **Chair confirmation (decided, four-member jury).** The result of the review
  vote is not applied by itself, whether it is a Good Lift or a No Lift. Jury
  Control opens the result screen (section 6) and the Chair either:
  - **confirms** the result: **Confirm Good Lift** for a Good Lift; choosing
    the technical rule for a No Lift (reason 17 "Other" is available). The
    jury box's Good Lift and No Lift are the same as Jury Control's (section
    13): Good Lift confirms a Good Lift result, No Lift opens the rule dialog
    on Jury Control;
  - or presses **Cancel vote result**: for example the jury says a vote was
    entered by mistake. All 7 votes are cleared on every display and device,
    the vote is closed and the break continues with Call for Votes prompted
    again. The new vote may give the opposite result.
  Confirming is not deciding: the Chair can apply the result of the vote or
  throw it out, not change it. A different decision needs a new vote or the
  marked override. Without VPT the Chair confirms the same way, but the
  decision then goes to the Speaker, who announces it (section 12).
- When the decision is applied, the scoreboards show "No Lift – rule" for a No
  Lift (TCRR 7.5.7). The decision is shown for 3.5 s and lifting resumes (the
  attempt board jury card lasts 5 s), but the revealed votes remain visible on
  Jury Control and the physical jury box until explicit Resume/clear or the
  next athlete clock starts. `PanelVoteUpdate.isPublicReveal()` marks the
  update sent when the decision is applied (used by MQTT so that the
  individual referee lights are not sent twice).
- Final decision: the panel majority, computed only when all 7 have voted
  (no early result, section 2), applied when the Chair confirms it. There is
  no automatic Good Lift after a grace period.
- The Chair confirmation replaces the `requireJuryPresidentDecision` switch,
  which is removed; it applies to every jury size (section 12). The TCRR makes
  the majority of the seven final, and the confirmation does not change that,
  since the Chair cannot apply a different result.
- **Jury Chair override.** When the vote goes wrong (a device offline, a
  referee on a USB keypad, a vote that never completes), the Chair decides
  with the **No Lift / Good Lift** buttons that replace Deliberation and
  Challenge during a review (section 6), before the vote is called or while
  it is open. After a completed vote, the Chair first presses Cancel vote
  result, then uses the buttons. The override posts `JuryDecision` directly;
  the vote is then closed and later votes are ignored.
- **Throwing the vote out.** The Chair override may not come in either (Jury
  Control device or network down, Chair unavailable). As in Phase A, the
  Competition Supervisor (accounts mode) or Speaker (PIN mode) is then the
  last resort: the **End jury break** button on that screen ends the break
  even if the review vote is incomplete. A confirmation ("Review vote not
  finished — end break anyway?") is shown in that case. The incomplete votes
  and `athleteUnderReview` are discarded, and the lift keeps the referees'
  decision. So the order in Phase B is: the 7-person vote, then the Chair
  override on Jury Control, then the Supervisor/Speaker override.
- **Override is marked.** A decision entered through the Chair override is
  flagged on the attempt and shown as **"Jury Chair Entered Decision"** (with
  VPT only; without VPT the Chair's decision is the normal path) on the
  announcer / Competition Supervisor notification (and in the jury
  deliberation record), so it is never mistaken for a panel vote. "Chair
  entered" means exactly that the Chair used the **Good Lift / No Lift**
  buttons (Jury Control or the physical jury box) instead of a vote; a No Lift
  that results from a vote, for which the Chair only supplies the rule, is not
  marked. A Chair-entered decision **clears the jury votes everywhere** (review
  vote cells, jury circles, Phase A pattern, physical jury box) before the
  decision card, so that nothing contradicting the Chair's decision stays on
  display. The
  Technical Controller normally states the reason verbally; OWLCMS does not
  ask for one beyond the No Lift rule.
- No Lift needs a technical rule (G5). For a No Lift, either from the vote or
  from the override, the reason dialog opens on Jury Control (the 17 reasons
  already exist). The Chair's choice posts the `JuryDecision` with the code. A
  Good Lift from the vote is posted when the Chair confirms it.
- `doJuryDecision` is reused: reversal/confirmation, records, order,
  `setJuryDeliberation` on the attempt, resume after 3.5 s.
- **Announcing and Speaker dialogs:** section 13, the same for every jury. The
  Chair's decision is applied at once (a No Lift once its rule is chosen); the
  Speaker announces it verbally, which the competition does not wait for. The
  rule is shown on the attempt board and in the scoreboard decision section
  ("No Lift – rule").
- **End of the break.** The break is over once the decision has been shown
  long enough on the boards (3.5 s), then lifting resumes by itself. Starting
  the clock while the jury decision is shown also ends the display and starts
  the clock (it is no longer refused).

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
     is not used; it is the wake-up reminder when a referee is slow to vote).
  No new topic: the existing messages already clear and re-arm the devices.
- When the review break starts (automatic stop, or a challenge with VPT), the
  Phase A votes are kept for display, so `owlcms/fop/juryDeliberation/<fop>` /
  `owlcms/fop/challenge/<fop>` are **not** published: the devices clear their
  jury lights on those topics. They are published as before when the jury
  votes are cleared (manual deliberation, challenge without VPT).
- Document the sequence in
  [200-MQTTMessages.md](../docs/100-AdvancedTopics/200-MQTTMessages.md).
- Config: `mqttJurySize()` publishes 4. Blue Owl takes the jury size from this
  message (there is no Blue Owl setting); `type JurySize` is widened to
  `3 | 4 | 5` in `blue-owl-electron/src/common.ts` and `initializer/jury.ts`
  creates stations 1–4 only (station 5 is not instantiated). owlcms also
  ignores a vote from a station beyond the jury size
  (`FieldOfPlay.doJuryMemberDecisionUpdate`).
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
Chair uses the No Lift / Good Lift override (section 6).

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
- **Buttons.** Jury Control has two rows of buttons.
  - Row 4, calls to the officials: **Summon Referees (all)**, **Left**,
    **Center**, **Right**, **Technical**. Summoning a referee starts a jury
    break if none is running, since the referee leaves the chair.

    The physical keypad and jury box have no Call for Votes button, so their
    Summon All key means **Vote** during a review where the vote is called
    that way (TCRR 7.5.7, "the Referee call buttons"): with the seven-person
    panel, and after the automatic stop of a four-member jury without VPT.
    Outside such a review it summons all the referees. The `K` shortcut on Jury Control is the
    keypad's Summon All and behaves the same way.

    On the Jury Control screen there is no ambiguity (decided 2026-10-23):
    the **Summon Referees (all)** button always means summon. It is
    **disabled** during a review where Summon All means Vote, and the orange
    **Call for Votes** in row 8 is used instead. In a jury vote started by the
    Chair it stays enabled and summons the referees (TCRR 7.5.7,
    "the Jury may call the Referees in question"). Left / Center / Right
    always summon one referee; while a seven-person vote is open they re-buzz
    a referee who has not voted, without clearing votes.
  - Row 8, the jury actions, four equal buttons. The two left buttons depend
    on the state; Call for Votes and Resume never move:

    | When | Row 8 |
    |---|---|
    | No review (lifting, ordinary break, jury voting on the lift) | **Deliberation** · **Challenge** (yellow) · Call for Votes (disabled) · **Resume** |
    | Review: `JURY` or `CHALLENGE` break with a lift under review | **No Lift** · **Good Lift** (the Chair override) · **Call for Votes** · **Resume** |

    Once a review has started, whether automatically or with Deliberation or
    Challenge, the yellow buttons are hidden: starting another review makes
    no sense. They come back when the break ends.
- **Call for Votes** (the keypad and jury box use their Summon All key, `K`,
  see row 4; there is no separate key, keypads are not reprogrammed). Enabled during a review, disabled otherwise.
  In a jury vote started by the Chair (no seven-person panel, with or
  without VPT) the jury lights are already cleared and the vote is open, so
  Call for Votes only clears the votes and restarts it. If Jury Control is not used, the Chair decides with the jury
  box's Good Lift / No Lift, or the Technical Controller relays the decision
  to the Speaker.
  - **Prompt (decided):** highlighted in orange from the start of every review
    (automatic Adjudication Panel Review, Challenge, manual Deliberation) until
    the vote is called, and again after **Cancel vote result**. It is blue
    while a vote is open; pressing it then restarts the vote.
- **States of a review on Jury Control:**

  | State | Panel row | Row 8 / screen |
  |---|---|---|
  | A. Review started, no vote called | Automatic review: the Phase A votes stay visible during the video review. Challenge: cleared. | No Lift · Good Lift · Call for Votes (orange) · Resume |
  | B. Vote open | all cleared, then grey "voted" as votes arrive, values hidden | same row, Call for Votes blue (restart) |
  | C. Vote complete, waiting for the Chair | the 7 votes revealed everywhere (boards: where jury votes are shown); votes locked | result screen (below) |
  | D. Decision applied | the decision (and rule) pops over the votes on the boards | the break ends 3.5 s later; the yellow buttons come back; the panel row is cleared by Resume or the next clock start |

- **Result screen (decided)**, state C. A dark modal dialog, like the reason
  dialog. It cannot be closed with Escape or by clicking outside; it closes
  only through its buttons, or when the decision is applied or the vote
  cancelled from elsewhere (another Jury Control page, the jury box, End jury
  break on the Competition Supervisor / Speaker screen). All Jury Control
  pages open on the platform show the same screen (`syncWithFopState`).
  - Good Lift result: **Confirm Good Lift** · **Cancel vote result**. The
    Chair's Good Lift button on the physical jury box is the same as Confirm
    Good Lift.
  - No Lift result: the reason dialog; choosing a rule confirms the No Lift.
    Its footer button is **Cancel vote result** (it replaces Cancel, which
    left the result on the screen).
  - **Cancel vote result** clears the 7 votes on every display and device,
    closes the vote and returns to state A, with Call for Votes orange. Typical
    use: the jury says a vote was entered by mistake; the new vote may give the
    other result. If the devices then fail, the Chair uses the No Lift / Good
    Lift override in state A or B (marked "Jury Chair Entered Decision").
  - The Chair cannot apply the opposite of the vote from this screen; that
    needs Cancel vote result, then a new vote or the override.
  - Without the panel (section 12) the screen is the same and the result is
    counted with the jury's rule; the confirmed decision goes to the Speaker,
    who announces it.
- Technical is unchanged. Jury Control and the physical jury box use the same
  Resume command:
  - in a normal non-jury break, Resume behaves exactly like Break Management:
    it posts `StartLifting`, with no additional jury reset;
  - during an active lift before referee decisions, Resume is a no-op;
  - once referee decisions are shown or a jury cycle is active, Resume
    abandons pending jury/review votes, clears all referee/jury decisions and
    resumes.
  Automatic `StartLifting` after the 3.5 s decision display does not clear the
  revealed votes; manual Resume/clear or the next clock start does.
- `getNbJurors()`, `registerShortcuts()`, `postJuryMemberDecision()` work with
  4 jurors as is (keys 1–8).
- No Call Technical Controller button (decided): there is no device to call
  the Technical Controller, so the Console's button is not carried over.

### 7. Jury Console (removed)

- **Decided: removed entirely**, with or without the switch. `JuryContent`
  (route `lifting/jury`) and `JuryDialog` are deleted; `JuryDecisionDialog`
  stays (it is the Speaker's jury decision dialog).
- [LiftingNavigationContent.java](../owlcms/src/main/java/app/owlcms/nui/lifting/LiftingNavigationContent.java):
  no `Jury_Console` button; the Jury Control button uses the `Jury_Keypad`
  label ("Jury Control", without "on mobile device").
- Accounts with the Jury role land on Jury Control (`WorkPage.JURY_CONTROL`).
- Features lost with the Console: athlete grid, sound toggle, and the Call
  Technical Controller button (no device to call the TC; section 6).
- Update [020-Jury.md](../docs/060-Refereeing/020-Jury.md).

### 8. Challenge after review

- **Decided: no refusal in OWLCMS.** The jury knows that a lift already
  reviewed by the Adjudication Panel cannot be challenged and simply does not
  press Challenge. OWLCMS does not check it.

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
  - The TCRR does not require the jury votes to be shown. Two switches, each
    covering the vote on the lift and the review vote, for every jury size
    (decided 2026-10-23): `juryDecisionsOnAttemptBoard` for attempt boards (it
    also holds the decision indefinitely for the jury votes on the lift; the per-board URL
    parameter `showJuryDecisions` still overrides it) and
    `juryDecisionsOnScoreboard` (new, off by default) for the scoreboard
    decision section. `decisionSectionShowBothJuryVotes` is removed. When a
    switch is off, that board shows nothing of the review vote, only its
    decision.
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
- `FeatureSwitch.requireJuryPresidentDecision`: the English text now says
  "Jury Chair" (and de/ru/et use their "chair" word); the key is unchanged.
  The switch is removed (section 12), so this text and its key become unused.
- **Done (in `adjudication_translations.tsv`):** `JuryPresident` en "Jury
  Chair", de *Jury-Vorsitzender*, ru *Председатель жюри*, et *Žürii
  esimees*, he *יו״ר חבר השופטים*; fo and hy fixed. The IWF says "Jury
  Chair". Languages that already use their word for chair (sv, da, fi, no,
  pl, zh_HANT), or whose word for chair is the same as for president (es,
  fr, pt, ro, el, hu, ar, hy), are unchanged; ja keeps 審判長 (hand translated). The `ia` column holds row ids, not a
  translation. User docs (020-Jury.md, 180-Keypads.md) updated; the RFP
  proposals under docs/RFP are left as submitted.
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
- Per language, use the word for "Chair" (the IWF no longer says "President"):
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

### 12. Juries without the seven-person panel (decided 2026-10-23)

Applies to 3- and 5-member juries (with or without VPT) and to the four-member
jury without VPT. The four-member jury with VPT uses the panel procedure of
sections 3 to 6. A three-member jury is a logistical fallback under the 2026
TCRR, and five-member juries are not expected; one procedure covers them all.

The TCRR has the jury decision "and its reasons ... communicated to the Speaker
... via the Technical Controller ... and announced by the Speaker" (7.5.7, no
VPT). The jury may not use the Jury Control screen at all, only the jury box
and the jury scoreboard, so the Speaker must be able to enter the decision.

Counting of a jury vote:

| Jury | Who votes in the review | Reverses the referees when |
|---|---|---|
| 3 members | the 3 jurors | unanimous |
| 4 members, no VPT (switch on) | the 4 jurors | unanimous |
| 5 members, VPT | the 5 jurors | majority |
| 5 members, no VPT | the 5 jurors | unanimous |

Otherwise the referees' decision stands. This is the counting of
`effectiveJuryVerdict` today.

- **Phase A.** The jurors vote on the lift as today. Automatic stop only for the
  four-member jury with `automaticBreakNoVPT` on; none for 3- and 5-member
  juries (for now). Summon Referees turns orange as the cue when the jury vote
  on the lift would reverse the referees. With jury votes shown on the attempt
  board, the decision display waits indefinitely for every configured juror.
  With them hidden, the display clears normally but the next clock is refused
  until all jurors vote, the jury decision is entered, or Start Clock Anyway
  discards the incomplete vote.
- **Review starts** (Deliberation, Challenge, or the automatic stop):
  - Jury Control: No Lift · Good Lift · Call for Votes · Resume, the yellow
    buttons hidden (section 6);
  - Speaker: the "Waiting for decision" dialog (section 13).
- **Jury vote (optional).** In a review started by the Chair, the jury lights
  are cleared when the break starts and the jurors may vote at once; the Chair
  asks for the vote verbally, and Summon Referees (all) summons the referees.
  The referees do not vote again: their decision on the lift stays shown on
  Jury Control, the jury box and the boards while the jurors vote.
  After the four-member jury's automatic stop, the votes that caused it stay
  displayed and Summon Referees (all) calls the vote, as with VPT; the
  individual buttons summon the referees. Keeping the votes on the lift and
  calling the vote are needed only after an automatic trigger. VPT makes no
  difference for 3- and 5-member juries (decided 2026-10-23): they have no
  automatic trigger, so every review is started by the Chair as above. For
  them the only change from dev68 is that the Chair gives the rule (a
  5-member jury with VPT still reverses by majority). The votes are
  hidden until all jurors have voted, then revealed on Jury Control, the jury
  box and the boards (where jury votes are shown), as with the seven-person
  panel. They are not sent to the Speaker. Once complete, the votes are locked
  and Jury Control shows the
  result screen (section 6): Confirm Good Lift, the rule for a No Lift, or
  Cancel vote result.
- **The decision** comes from one of:
  - the Chair confirming the vote result on Jury Control;
  - the Chair pressing **Good Lift / No Lift** on Jury Control or on the jury
    box without a vote (the same thing). No Lift asks for the rule on Jury
    Control; Good Lift does not;
  - the Speaker pressing Good Lift / No Lift in the dialog, as relayed by the
    Technical Controller (no rule), for example when Jury Control is not
    used.
  None of these is marked "Jury Chair Entered Decision" (VPT only).
- **Announcement:** section 13.
- **Removed:** the `requireJuryPresidentDecision` switch (a jury vote never
  decides by itself; the Chair decides), the automatic jury decision after
  1.5 s (`scheduleAutoJuryDecision`), and the "announcer controlled jury
  decision" setting (section 13 applies to every jury).
- Unchanged: jury size 0 (no jury) and the jury size selector (0 / 3 / 5) when
  the switch is off.

### 13. Speaker dialogs and announcing (decided 2026-10-23)

The same for every jury (3, 4 or 5 members, with or without VPT). It restores
the dev68 jury decision dialog (`JuryDecisionDialog`) for the Speaker and the
Competition Supervisor page, which inherits it, and extends it to the
four-member jury. The decision may always come through the Technical
Controller, since devices or Jury Control may fail.

| Event | Speaker / Competition Supervisor | Public boards |
|---|---|---|
| Review starts: automatic, Challenge or Deliberation | dialog **"Waiting for decision"**, athlete and attempt, **Good Lift / No Lift / End jury break** | "Adjudication Panel Review", "Challenge" or "Jury deliberation" |
| Votes coming in | dialog unchanged; the Speaker never sees the votes | "voted" cells where the switches are on |

How the decision is applied:

| Decision | Applied and shown on the boards | Speaker dialog |
|---|---|---|
| **Good Lift** from the Chair: Jury Control or jury box, directly or confirming a Good Lift vote result | **at once** | the decision (reversal or confirmed) with **Close** |
| **No Lift** from the Chair: Jury Control or jury box. Both post the same FOP event; every running Jury Control page opens the rule dialog, and the rule is chosen there | **at once**, when the rule is chosen | the decision and the rule, with **Close** |
| No Lift requested but no rule given (Jury Control not running, or the rule dialog cancelled) | nothing is applied | still "Waiting for decision"; the Technical Controller relays the decision and the Speaker enters it, or ends the break |
| the **Speaker** enters Good Lift / No Lift, as relayed by the Technical Controller | at once (no rule) | closes |
| End jury break, or Resume (Jury Control or jury box) | the break ends, the referees' decision stands | closes |

- **Who resumes the competition** depends on whether the decision reached the
  system complete:
  - **Complete from the Chair** (a Good Lift, or a No Lift with its rule): it
    is applied at once and the competition resumes 3.5 s after it is shown.
    The Speaker announces it verbally; the system does not wait for that. With
    VPT the TCRR does not require the announcement for the competition to
    resume; without VPT the TCRR has the Speaker announce the decision and its
    reasons (7.5.7), and since the rule is shown, the announcement does not
    gate the system either. The dialog shows the decision and the rule, asks
    the Speaker to announce it, and has a single **Close** button. The
    "Display Decision" step (`Announcer.DisplayDecision`, "Click the button to
    display the outcome ... This will resume the competition") goes.
  - **Not complete** (a No Lift without a rule because Jury Control is not
    running or the rule dialog was cancelled, or no decision from Jury Control
    or the jury box at all; typically without VPT): the Technical Controller
    relays the decision, and the **Speaker or Competition Supervisor** enters
    it in the "Waiting for decision" dialog (Good Lift / No Lift), which
    applies it and resumes the competition, or ends the break with End jury
    break.
- **The rule dialog is driven by FOP events**, never opened by the Jury
  Control page logic itself. The No Lift button of Jury Control and the jury
  box's No Lift post the same FOP event; the FOP asks every running Jury
  Control page for the rule (as `JuryRuleRequired` does today); the chosen
  rule posts the `JuryDecision`. Cancel withdraws the request on every page.
- The boards reveal the votes (or the jury votes) as soon as the last one is
  in, where `juryDecisionsOnAttemptBoard` / `juryDecisionsOnScoreboard` are
  on; the decision, Good Lift / No Lift with the rule, pops over them when it
  is applied. Lifting resumes 3.5 s later.
- **"Jury Chair Entered Decision" (with VPT).** With VPT the decision must
  come from the 7-person vote and a No Lift needs its rule (7.5.7). A Chair
  decision that is not the confirmation of the vote result (a jury box button,
  or Good Lift / No Lift on Jury Control) is an override: the Speaker dialog
  shows "Jury Chair Entered Decision" with the decision, and it is recorded.
  Without VPT the Chair's decision is the normal path and is not marked
  (decided 2026-10-23).
- Reason 17 "Other" covers rulings that have no TCRR number. It is a rule like
  the others and is unrelated to the "Jury Chair Entered Decision" marker.
- A Challenge also opens the waiting dialog (in dev68 it did not).
- **Known delay (accepted 2026-10-23).** No current-athlete update is sent while
  a decision is shown, so after a jury decision the Speaker's grid and top bar
  keep the state from before the decision until lifting resumes, about 3.5 s
  later (`doJuryDecision` recomputes the order; `uiDisplayCurrentAthleteAndTime`
  sends `LiftingOrderUpdated` on resume). The scoreboards are correct at once
  (they redraw on the jury notification). Same as dev68. To revisit only if the
  delay becomes longer.
- **Attempt board and jury size (accepted 2026-10-23).** The attempt board
  reads the number of jurors when it loads. After the jury size is changed on
  Jury Control (or the seven-person switch or VPT setting is changed), open
  attempt boards must be reloaded to show the right number of jury cells. The
  scoreboard follows the change at once.
- The yellow stoppage notice (Acknowledge / End jury break) is no longer used
  on the Speaker page for a review; it stays for referees called outside a
  review, Technical Controller called, technical break and marshal. The green
  / red verdict notification on the Speaker page goes.
- The other consoles (Timekeeper, Marshal, Technical Controller...) keep their
  notices: the review (yellow) and the decision (green / red) when it is
  shown. They have no way to resume the competition.
- The "announcer controlled jury decision" setting is removed: the rule above
  applies to every jury.

## Effort and Phasing

| Phase | Content | Size |
|---|---|---|
| 1 | Switch, derived jury size 4, ignore jury votes beyond the jury size (station 5), panel tally class + unit tests | S |
| 2 | Phase A: panel evaluation on the 4th jury vote, automatic review break, clock-start guard with override, notifications | M |
| 3 | Phase B: Call for Votes (buzzer + re-arm), referee review votes in BREAK, phones, MQTT reset/summon/topic, final verdict | M–L |
| 4 | Jury Control 7-cell row, Call for Votes button and prompt, Jury Chair override, reason flow, result screen with Cancel vote result | M |
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

- During a review the Deliberation and Challenge buttons become the Jury Chair
  override (No Lift with rule / Good Lift), used when the votes go wrong. This
  also covers referees who cannot vote (for example on USB keypads). The
  "Waiting for Votes" screen of the first design was not built. See sections 4
  and 6.
- The result of the review vote waits for the Chair: Confirm Good Lift, or a
  rule for a No Lift, or **Cancel vote result**, which clears the votes and
  lets the Chair call a new vote. Completed votes are locked; the boards show
  them as soon as they are in, and the decision pops over them. Same without
  VPT. See sections 4 and 6
  (2026-10-22).
- Call for Votes is highlighted in orange in every review until the vote is
  called, including a manual Deliberation, and again after Cancel vote result.
  See section 6 (2026-10-22).
- "Jury President" becomes "Jury Chair" in all translations; the keys are kept.
  See section 11.
- The Jury Chair picks the Technical Rule for a No Lift.
- No timeout for the jury votes. The lift under review and its votes are kept
  until the next clock start. When jury decisions are shown on the attempt
  board, the decision display is held until the jury has voted (no 10 s cap),
  for every configured jury (3, 4 or 5 members), with or without VPT.
  A clock start with incomplete jury votes is refused; the deciding console
  may choose "Start clock anyway" to discard the incomplete votes and leave
  the referees' decision in place. See section 3, steps 2 and 4.
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
- When jury votes do not come in, in Phase A or Phase B, the last-resort
  override is on the Competition Supervisor screen (accounts mode) or the
  Speaker screen (PIN mode): "Start clock anyway" in Phase A, "End jury break"
  with a confirmation in Phase B. Both discard the incomplete votes and the
  lift keeps the referees' decision. In Phase B the Chair override on Jury
  Control (marked "Jury Chair Entered Decision") comes first; the
  Supervisor/Speaker override covers the case where the Chair's decision does
  not come in either.

- Without the seven-person panel (3- and 5-member juries, four-member jury
  without VPT): the jury vote is optional and revealed like the panel's; the Chair
  decides (confirming a vote result, or Good Lift / No Lift directly, a rule
  required for a No Lift on Jury Control), or the Speaker enters it as relayed
  by the Technical Controller. `requireJuryPresidentDecision` and the
  "announcer controlled jury decision" setting are removed. "Jury Chair Entered Decision" is marked
  with VPT only. No automatic stop for 3/5 juries; the next clock waits for
  all jury votes or an explicit resolution. A 5-member jury keeps majority
  with VPT and unanimity without. See
  section 12 (2026-10-23).

- Speaker dialogs, for every jury: "Waiting for decision" when a review
  starts. The jury box's Good Lift / No Lift are the same as Jury Control's.
  The Chair's decision is applied at once (a No Lift once its rule is chosen
  on Jury Control); the Speaker dialog then shows it with a Close button and
  the Speaker announces it verbally, which the competition does not wait for.
  Without a rule (no Jury Control), the Technical Controller relays the
  decision and the Speaker enters it. Votes are shown on the boards only where
  the jury vote switches are on. With VPT, a Chair decision that is not the
  confirmation of the vote is marked "Jury Chair Entered Decision" in the
  Speaker dialog. See section 13 (2026-10-23).

- Summon All during a review, on the keypad and jury box (and `K`): it calls
  the vote with the seven-person panel and after the automatic stop of a
  four-member jury without VPT; the votes that caused the review stay
  displayed until then, and the individual buttons summon the referees. In a
  jury vote started by the Chair (with or without VPT) it summons the referees
  (TCRR 7.5.7); the jury lights are cleared at the start of the break, the
  referees' decision stays shown, and the Chair asks for the vote verbally.
  On the Jury Control screen the Summon Referees (all) button always means
  summon: it is disabled when Summon All means Vote, and the orange Call for
  Votes is used instead. Summoning starts a jury break, since the referee
  leaves the chair. 3- and 5-member juries with VPT behave as without VPT
  (no automatic trigger, so the review is started by the Chair); only the
  jurors vote, counted as before (3: unanimity, 5: majority with VPT). See
  sections 6 and 12 (2026-10-23).

Open:

None.

## Remaining Work

### 1. Chair confirmation and Cancel vote result (implemented 2026-10-23)

Done: no automatic Good Lift; votes locked once complete; the No Lift result
asks the Jury Control pages for the rule (`UIEvent.JuryRuleRequired`,
`forVote`); Confirm Good Lift / Cancel vote result on Jury Control, the same
dialog on every page (`syncJuryDialogs`), not closable by Escape or outside
click; Cancel vote result clears the displays and devices
(`JuryDisplayReset`) and, after a review started by the Chair without VPT,
reopens the vote; Call for Votes orange in every review; the boards (attempt
board, scoreboard decision section) reveal the review votes as soon as the
last one is in, and the decision pops over them; tests in
`SevenPersonAdjudicationTest`.

Also done: the jury updates of a review vote are flagged
(`JuryUpdate.isReviewVote`) and ignored by public boards, which show the
review vote only from `PanelVoteUpdate`: hidden while in progress, revealed as
soon as the last vote is in (no VPT and 3/5 juries included).

Also done: a decision entered by the Jury Chair during an open vote, or
Cancel vote result, clears the votes on the scoreboard decision section but
keeps the review label (`DecisionBlockState.onJuryDisplayReset`), so the
verdict that follows is shown; before, the section went back to the current
athlete and showed a stale break timer (scenario 16).

Left:

- Device timeline: [done/phase-b-referee-lights-mqtt.md](done/phase-b-referee-lights-mqtt.md)
  is updated for the confirmation.

### 2. Juries without the panel: one procedure (implemented)

Done (2026-10-23): the automatic jury decision (`scheduleAutoJuryDecision`),
`FeatureSwitch.REQUIRE_JURY_PRESIDENT_DECISION` and the "announcer controlled
jury decision" checkbox are removed (`Competition.announcerControlledJuryDecision`
is kept, deprecated, for the database column and existing exports); the jury
vote alone never decides; "Jury Chair Entered Decision" is marked only with
the seven-person panel; Summon All means Vote only with the panel or after the
automatic stop without VPT (`FieldOfPlay.isSummonAllVote`), and the Summon
Referees (all) button of Jury Control is disabled then; a review started by
the Chair without VPT clears the jury lights and opens the vote at once.

Also done (2026-10-23, stage B): the review vote for 3- and 5-member juries,
jury only, counted with `effectiveJuryVerdict` (`hasJuryReviewVote`,
`getReviewVoteSize`; vote arrays are 3 referees + the jury size); the vote
opens at the start of the review, with or without VPT (corrected 2026-10-23:
keeping the votes on the lift was first applied to 3/5 juries with VPT; it
belongs to the automatic trigger only), and the boards and jury box keep the
referees' decision shown during the jury vote; Jury Control uses the review layout
(`panelMode`) for every jury size, and its orange Summon Referees cue uses
`FieldOfPlay.isJuryVoteAgainstReferees`; MQTT reveals the referee lights with
the jury result for every jury size; tests for 3- and 5-member counting.

Left:

- Translations: `FeatureSwitch.requireJuryPresidentDecision`,
  `Competition.announcerControlledJury` and
  `FeatureSwitch.decisionSectionShowBothJuryVotes` are unused; remove them in
  the translation tool.

### 3. Speaker dialogs (implemented 2026-10-23)

Done: every review start opens the "Waiting for decision" dialog on the Speaker
and Competition Supervisor pages (Challenge and the automatic review included);
no jury decision waits for the Speaker (`toBeAnnouncedJuryDecision` removed);
the dialog then shows the decision and rule with a Close button and stays when
lifting resumes, until closed or the clock starts; a decision entered by the
Speaker is flagged (`JuryNotification.isSpeakerEntered`) and closes the
dialog; the green / red verdict notification of the Speaker page is gone; the
jury box's No Lift posts the same rule request as Jury Control's
(`FOPEvent.JuryRuleRequest`), and so does the `box=true` testing mode; the
rule is shown on the boards for every jury size. Translations in
`jury_speaker_dialog_translations.tsv` (`JuryKeypad.ConfirmGoodLift`,
`JuryKeypad.CancelVoteResult`, `Announcer.AnnounceJuryDecision`, and the new
"Waiting for decision" text of `Announcer.WaitingForJuryDecision`).

Also done: the jury votes on boards follow `juryDecisionsOnAttemptBoard` and
the new `juryDecisionsOnScoreboard` (off by default) for the lift and the
review; `decisionSectionShowBothJuryVotes` is removed (section 9).

Left:

- `Announcer.DisplayDecisionAndAnnounce` and `Announcer.JuryDecisionExplanation`
  are no longer used.

### 4. User documentation (done 2026-10-23)

[020-Jury.md](../docs/060-Refereeing/020-Jury.md) rewritten for Jury Control
with screenshots taken from the Playwright scenario reports: jury sizes and
settings, the seven-person panel (automatic review, Call for Votes, result
waiting for the Chair, Cancel vote result, decision without a vote), the
3/4/5-member jury review, the Speaker dialogs, Resume.
[180-Keypads.md](../docs/100-AdvancedTopics/180-Keypads.md): the Jury Chair
keypad keys are unchanged from the Jury Console (`d c t h i j k g b Escape`);
what `c`, `k`, `g`, `b` and `Escape` do depends on whether a review is in
progress (`k` is Call for Votes during a panel review). The `V` shortcut
added during development is removed: keypads are not reprogrammed.

### 5. MQTT documentation (done 2026-10-23)

[200-MQTTMessages.md](../docs/100-AdvancedTopics/200-MQTTMessages.md):
`hidden` decision state and the `panelVoteDeviceIndicators` switch;
`resetDecisions` on Call for Votes, Chair decision, Cancel vote result and
Resume, with the referees' decisions republished for a jury vote;
`juryDeliberation` / `challenge` not published when the votes on the lift are
kept; `jurybox/summon all` as Call for Votes during a panel review;
`jurybox/decision bad` as a rule request.

### 6. MQTT tests

No `MQTTMonitor` test exists. Add one with a stub publisher covering:

- `fop/decision N hidden` only when `panelVoteDeviceIndicators` is on;
- individual `fop/decision N good/bad` at the last vote, not the collective
  `fop/refereesDecision`, and not twice when the public reveal follows;
- nothing published for referees without VPT;
- `resetDecisions` on Call for Votes; `summon` only with VPT;
- `juryDeliberation` / `challenge` suppressed when
  `getPanelReviewInitialVotes() != null`.

### 7. Device tests and release coordination

- Test on firmata with the updated jury workbooks, and on Blue Owl devices with
  the updated blue-owl-core (positions 1–4, green referee / jury LEDs, red /
  white reveal at the last vote). The compatibility table in the archived
  phase-b document lists the expected result per combination.
- Then decide whether `panelVoteDeviceIndicators` can default to on.
- Release notes for owlcms and blue-owl-electron must state that Blue Owl
  devices need the updated blue-owl-core before `panelVoteDeviceIndicators`
  is enabled.

### 8. Unused translation keys

The design's "Waiting for Votes" screen was not built (the Chair override
reuses the Deliberation / Challenge buttons instead). Four keys from that
design are unused in Java but present in `translation4.csv`:
`JuryKeypad.WaitingForVotes`, `JuryKeypad.ChairOverride`, `JuryKeypad.Recall`,
`JuryKeypad.RestartVote`. Remove them in the translation tool and from
[adjudication_translations.tsv](../shared/src/main/resources/i18n/adjudication_translations.tsv)
if that file is still awaiting import.
