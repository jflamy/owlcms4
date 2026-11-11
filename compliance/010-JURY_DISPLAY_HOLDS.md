# Jury Display Holds

Status: **decided 2026-11-11; implemented in the working tree (not committed), unit-tested.** The
Playwright scenarios are being updated (set B of the [implementation plan](010-IMPLEMENTATION_PLAN.md)).

This is the only specification of **how long** the attempt boards and the scoreboard decision
block stay on a decision, a vote or a jury outcome, and of **what starts and ends** each stage.
The hold rules that were in
[DecisionBlock_SPEC.md](../owlcms/src/main/java/app/owlcms/displays/scoreboard/DecisionBlock_SPEC.md),
[SEVEN_PERSON_ADJUDICATION.md](done/SEVEN_PERSON_ADJUDICATION.md) and
[TRACKER_JURY_VOTES.md](done/TRACKER_JURY_VOTES.md) have been removed from those documents.
What each display draws (lights, circles, labels) stays specified there.

## 1. Principle

- A decision is shown long enough to be read, then the display returns to the current athlete.
- The **scoreboard decision block** shows the prior decision for **20 s**, unless there is a
  vote to show.
- The **attempt board** shows a completed first vote for **10 s**, and a referee decision
  without a jury for **3.5 s**.
- The competition flow does not wait: the Speaker's page and the scoreboard top line move to the
  next athlete 3.5 s after the referee decision, whatever the jury.
- No jury means no jury vote, no replay review, no deliberation, no challenge and no jury
  outcome.

## 2. Juries

| Name in this document | Configuration |
| --- | --- |
| No jury | jury size 0 |
| Seven-person panel | four-member jury with Video Playback Technology (VPT): 3 referees and 4 jurors |
| 3-4-5 jury, automatic stop OFF | 3 jurors, 4 jurors without VPT, or 5 jurors; `automaticBreakNoVPT` off (default) |
| 3-4-5 jury, automatic stop ON | same juries; `automaticBreakNoVPT` on |

The automatic stop makes the 3-4-5 juries follow the same stages as the seven-person panel,
except that the referees do not vote again.

## 3. Stages

| Stage | Display | No jury | Seven-person panel | 3-4-5 jury, automatic stop OFF | 3-4-5 jury, automatic stop ON |
| --- | --- | --- | --- | --- | --- |
| **1. Referee decision shown, first vote not complete** | Attempt board | 3.5 s, then current athlete | Stays on the athlete under review, no limit | Same as panel | Same as panel |
| | Decision block | 20 s from the decision, then current athlete | Stays on the athlete under review, no limit | Same as panel | Same as panel |
| **2. First vote complete, no review** | Attempt board | n/a | 10 s from the last jury vote, then current athlete | Same as panel | Same as panel |
| | Decision block | n/a | 20 s from the last jury vote, then current athlete | Same as panel | Same as panel |
| **3. Replay review: review started, vote not yet called** | Both | n/a | Starts automatically when the first vote does not confirm the referees, or when the Chair presses Deliberation or Challenge. No limit. Both show the first-vote decisions that led to the review | Does not exist: Deliberation or Challenge goes straight to stage 4 | Starts automatically when the jury's first vote is against the referees, or when the Chair presses Deliberation or Challenge. No limit. Both show the first-vote decisions. A Chair who does not want a vote presses Resume (stage 6) |
| **4. Review vote (the actual deliberation)** | Both | n/a | Starts on Call for Votes (Jury Control) or Summon All (keypad). No limit, also once the votes are revealed, until the outcome. 3 referee and 4 jury indicators | Starts at once with Deliberation or Challenge. No limit until the outcome. Jury circles only, referees hidden. Summon All summons the referees | Starts only on an explicit call: Call for Votes (Jury Control) or Summon All (keypad). No limit until the outcome. Jury circles only, referees hidden. The three individual buttons summon the referees |
| **5. Outcome** | Attempt board | n/a | Outcome card, with the reason when one was given: 5 s from the outcome. Behind it, the board returns to the current athlete after 3.5 s | Same as panel | Same as panel |
| | Decision block | n/a | Outcome, with the reason when one was given: 20 s from the outcome, then current athlete | Same as panel | Same as panel |
| **6. Review ended by Resume, no outcome** | Both | n/a | Return to the current athlete at once | Same as panel | Same as panel |
| **7. Last snatch or last lift of the session** | Both | Stages 1 to 6 apply unchanged; the displays then show the break before clean and jerk, or the end of the session | Same as no jury | Same as no jury | Same as no jury |

A separate call for votes exists only in the automatic modes: the seven-person panel, and the
3-4-5 juries with the automatic stop ON. Without it, nobody would see the first votes that led
to the review.

## 4. Definitions

- **Athlete under review:** the last athlete who received a referee decision.
- **First vote:** the referee decision plus the jurors' votes on the lift. It is complete when
  the last juror has voted.
- **Start of the 10 s and 20 s of stage 2:** the last jury vote, or the referee decision when the
  jurors had all voted before it was shown. The decision is always shown for at least 3.5 s.
- **Jury against the referees** (automatic stop ON): unanimous for 3 or 4 jurors, and for 5
  jurors without VPT; majority for 5 jurors with VPT.
- **Outcome:** the moment a jury decision is applied.
  - Normal case: after the review vote, the Chair confirms a Good Lift or gives the reason for a
    No Lift on Jury Control, or presses the matching Good Lift / No Lift button of the jury box
    (no reason is shown).
  - Also outcomes: a Chair decision against the result of the vote, a Chair decision without a
    complete vote, and a decision entered by the Speaker.
  - A second decision on the same lift (a correction) is a new outcome: the 5 s and the 20 s
    start again.
- **Stages 3 and 4 replace the holds:** a review that starts during the 10 s or the 20 s of
  stage 2 takes over at once.
- **Summon All:** the jury box button and the `K` shortcut follow the keypad.

## 5. What ends a hold early

- The next athlete's clock starts. While jury votes are missing, the Speaker (Competition
  Supervisor in accounts mode) must confirm "Start clock anyway".
- A new referee decision replaces it.
- An official starts a break, a ceremony starts, or the session is reloaded.
- The break before clean and jerk and the end of the session do **not** end a hold (stage 7).
  With a jury that never votes, the displays stay on the athlete under review until the session
  is reloaded.
- An attempt board with "Show jury decisions" unchecked behaves as with no jury (3.5 s).

## 6. Differences with the code

None (2026-11-11). The six differences listed when this spec was written were closed by phases
A1 to A7 of the [implementation plan](010-IMPLEMENTATION_PLAN.md): the decision block's 20 s
start at the outcome, a corrected outcome restarts them, the block waits for missing jury votes
at the end of the snatches or of the session, every 3-4-5 jury may stop automatically, and in
the automatic modes the vote is called explicitly (Summon All included). Unit tests cover them;
the Playwright scenarios are updated in set B of the plan.

## 7. Durations

| Duration | Value | `timing.properties` key (`Competition` field) |
| --- | --- | --- |
| Referee decision display | 3.5 s | `decisionVisibleDuration` |
| Attempt board, after a complete first vote | 10 s | `juryDecisionHold` |
| Attempt board, outcome card | 5 s | `juryOutcomeCardDuration` |
| Decision block, prior decision or outcome | 20 s | `decisionBlockHold` |

"No limit" is a rule, not a duration (`FieldOfPlay.JURY_DECISION_VISIBLE_MAX_MS = -1`).

The durations are read from
[timing.properties](../owlcms/src/main/resources/timing/timing.properties) (a copy in the `local`
directory overrides it), so that the Playwright scenarios can take their waits from the same
values.
