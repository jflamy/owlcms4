<!-- markdownlint-disable -->
# Decision Block — Behavioral Spec

Status: **DRAFT for review**
Scope: the bottom "decision section" of the results scoreboard (`Results` / `BaseResults`),
feature switch `DECISION_SECTION`.

This document is the single source of truth for **what the decision block shows** in each of
its states. It is written so that the block can be implemented as one state machine
(`DecisionBlockState`) that `Results.java` renders from, and unit-tested against event
scenarios (no Vaadin objects in the tests).

**Timing is not specified here.** When the block enters and leaves a state, and how long a
decision, a vote or a jury outcome stays, is specified only in
[Jury Display Holds](../../../../../../../../compliance/done/010-JURY_DISPLAY_HOLDS.md). The hold
rules, the event transition table and the state diagram that were in this document were removed
on 2026-11-11 (git history).

---

## 1. Visual elements

The decision block is composed of four independent visual pieces:

| # | Element | Driven by | Notes |
|---|---------|-----------|-------|
| 1 | **Clock** (decision-section timer) | `decisionSectionDecisionActive == false` | Shown only when no decision/review is being displayed. |
| 2 | **Referee decision lights** | `dsRefereeDecisions` (`DecisionBlockDecisionElement`) | white / red per referee; in `READY_FOR_NEXT_ATHLETE` they render as **empty** (blank slots), not the previous athlete's colors, even though the element still holds the previous decision values. |
| 3 | **Jury vote circles** | `juryDecisions` property (rendered in `Results.js`) | `empty` → `voted` (gray, undisclosed) → `white`/`red` (disclosed). |
| 4 | **Athlete-under-review name** | `decisionSectionDecisionActive == true` + name | Replaces the clock while a decision/review is displayed. |

Key rule: **"holds a value" ≠ "is shown".** In `READY_FOR_NEXT_ATHLETE` the referee and jury
elements may still contain the previous athlete's values, but the block renders them as **empty**
(blank slots), never the previous colors. What is displayed is a property of the *state*, not of
the underlying element data.

---

## 2. States

### `READY_FOR_NEXT_ATHLETE` (idle / clock mode)
- Clock: **visible** (the athlete clock; during a break, the break countdown when one is running).
- Current athlete: **visible** next to the clock — start number, name, and age groups when
  `DECISION_SECTION_SHOW_AGE_GROUPS` is on. During a break the break description takes its place.
- Referee lights: **empty** — rendered as blank slots even if the element still holds the previous
  athlete's decision values.
- Jury circles: **empty / hidden** — rendered blank even if the element still holds previous votes.
- Athlete-under-review name: **hidden** (the name shown is the current athlete's, not the athlete who
  just lifted).
- The clock may be started, stopped, restarted, stopped… any number of times without leaving
  this state.

### `DECISION_VISIBLE` (referee + jury votes shown)
- Entered once **all 3 referees have voted** *and* the **down signal has been shown** according
  to the immediate-vs-delayed decision rules.
- Clock: hidden. Athlete name: **visible** (the athlete who just lifted).
- Referee lights: **visible** (the referee decision).
- Jury circles: **visible**:
  - jury members who have voted → `voted` (full gray, **not disclosed**);
  - jury members who have not voted → `empty`.
- When **all jury members have voted**, disclose the jury decisions (`white`/`red`).
- How long the block stays in this state, and what ends it: stages 1, 2 and 7 of
  [Jury Display Holds](../../../../../../../../compliance/done/010-JURY_DISPLAY_HOLDS.md).

### `JURY_DELIBERATION` (jury deliberation or challenge)
- Entered from any state on a jury-deliberation or challenge notification.
- Athlete name: **visible** — the **athlete under review** (referee decisions for that athlete are
  shown again).
- Referee lights show the original decision only until a review vote opens.
  For a seven-person review, they then show who has voted and reveal the three
  new referee votes with the four jury votes. For a jury-only deliberation or
  challenge (3, 4 or 5 jurors), they stay **empty**, including after the jury
  votes are revealed; the referees did not vote again.
- Jury circles: **cleared**, and:
  - shown (live, then disclosed) **only if** the "second jury vote is public" toggle is on
    (`DECISION_SECTION_SHOW_BOTH_JURY_VOTES`);
  - when the toggle is off there is no second vote to show, so **no circles are displayed at all**
    (not even empty placeholders).
- The label names the review stage (`ReviewLabels`): Adjudication Panel Review or Jury Review
  for an automatic review, Jury Deliberation or Challenge for a review started by the Chair, then
  Adjudication Panel Vote or Jury Vote once the Chair explicitly calls the vote. A vote that opens by
  itself at the start of a jury-only deliberation or challenge keeps the review name; Cancel vote
  result returns to it.
- The outcome label (Good Lift / No Lift, with the rule when one was given) replaces the
  review label.
- How long the review and the outcome stay, and what ends them: stages 3 to 7 of
  [Jury Display Holds](../../../../../../../../compliance/done/010-JURY_DISPLAY_HOLDS.md).

---

## 3. Transitions and timing

Removed on 2026-11-11. The stages, what starts and ends them, and their durations are specified
only in [Jury Display Holds](../../../../../../../../compliance/done/010-JURY_DISPLAY_HOLDS.md).

Rendering notes that do not depend on timing:
- The **down signal** alone never makes the block visible; visibility waits for the referee
  `Decision` event (which respects immediate-vs-delayed rules).
- Entering `READY_FOR_NEXT_ATHLETE` renders the referee and jury elements as **empty**. It does
  not need to wipe their underlying data — the render is empty regardless, and the next
  `DECISION_VISIBLE` entry repopulates them.

---

## 4. State diagram

Removed on 2026-11-11, with the transition table.

---

## 5. Reload / attach (derive state, no history)

When the page attaches or re-syncs, the state is derived from the current FOP state (there is no
event history to replay):

| FOP condition | Derived state |
|---------------|---------------|
| `state == DECISION_VISIBLE`, break type ∈ {JURY, CHALLENGE} | `JURY_DELIBERATION` |
| `state == BREAK`, break type ∈ {JURY, CHALLENGE} | `JURY_DELIBERATION` |
| `state == DECISION_VISIBLE` (normal) | `DECISION_VISIBLE` |
| anything else | `READY_FOR_NEXT_ATHLETE` |

---

## 6. Why a single state object

Today the block has **two authorities**: `dsRefereeDecisions` self-subscribes to the FOP bus
(`DecisionElementState` + its own `@Subscribe` methods, `resetOnClockStart=true`) **and**
`Results.java` also drives it from several handlers. The two race, which is why the lights are not
reliably hidden in `READY_FOR_NEXT_ATHLETE`.

Target design:
- One `DecisionBlockState` (plain Java, no Vaadin) owns the state + payload
  (reviewed athlete, referee decision, jury votes, toggle) and exposes `on(UIEvent…)` transitions.
- `dsRefereeDecisions` becomes a **pure renderer** (stop it self-subscribing for this display);
  it is driven only by the state machine.
- `Results.java` implements a thin **`DecisionSectionRenderer`** interface (`showClock`,
  `showRefLights`, `showJuryCircles`, `showAthleteUnderReview`, `hideAll`) whose scope is
  **only the decision section** — it does not govern the rest of `Results.java` (scoreboard grid,
  group info, ranks, etc.). `Results.java` implements this interface simply because it owns the
  `@Id` handles to the decision-section elements; the interface is a narrow rendering contract for
  the decision block alone. It just renders the current snapshot.
- Unit tests feed event scenarios and assert the resulting state + rendered snapshot; the
  expected timing comes from
  [Jury Display Holds](../../../../../../../../compliance/done/010-JURY_DISPLAY_HOLDS.md).

---

## 7. Open questions for review

Resolved (2026-07-09):

- **No second vote → no circles.** During `JURY_DELIBERATION` with the 2nd-vote-public toggle
  **off**, no jury circles are displayed at all (not even empty placeholders).

The questions about the review timer and the review hold that were answered here are superseded
by [Jury Display Holds](../../../../../../../../compliance/done/010-JURY_DISPLAY_HOLDS.md).
