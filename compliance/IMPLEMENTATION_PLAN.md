# Decision Normalization — Concrete Implementation Plan

Date: 2026-03-26 (revised)
Specification: `DECISION_NORMALIZATION_MATRIX.md`

## Design Principle: Classify Once, Read Everywhere

The current code re-infers semantics at every stage: `isSingleReferee()`, `isRefereeForcedDecision()`, combinations of both. This creates multiple places where the same classification decision is made independently, risking inconsistency.

The normalized approach: **classify the input into `InputKind` once, as early as possible, and store it on the FOP**. All downstream methods read `this.currentInputKind` instead of re-deriving from flags.

`isSingleReferee()` is preserved on the FOP as a configuration getter — it controls the solo-vs-three routing in `doPossiblySoloRefereeUpdate` and the `singleRefereeLight` display. After classification, `singleRef` for UIEvent purposes is derived from `currentInputKind`. `isRefereeForcedDecision()` and `setRefereeForcedDecision()` are **removed** — all callers that previously read them now use `currentInputKind` or the UIEvent's `isSingleReferee()` field.

## Prerequisites

Read the specification fully before starting any step. Each step describes: the file(s), the current code, the exact change, and what NOT to touch. Steps must be executed in order — later steps depend on earlier ones.

---

## Step 1 — Add `showDecisionsImmediately` and `currentInputKind` fields to `FieldOfPlay`

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Design

`showDecisionsImmediately` follows the existing `Config.getCurrent().featureSwitch("name")` pattern. The value is read once when the session is loaded — not checked on every decision. No `volatile` needed because FieldOfPlay methods are `synchronized` and the value is only read on the FOP thread after initialization.

`currentInputKind` is per-decision state: it is set once at the decision entry point and read by all downstream methods. It is cleared in `resetDecisions()`.

### What to add

Near the existing field `announcerDecisionImmediate` (line ~233), add:

```java
private boolean showDecisionsImmediately = false;
private InputKind currentInputKind = null;
```

### Initialize `showDecisionsImmediately` from Config feature switch

In the `init()` method (line ~1107), near the top after existing field initialization, add:

```java
this.showDecisionsImmediately = Config.getCurrent().featureSwitch("showDecisionsImmediately");
```

### Clear `currentInputKind` in `resetDecisions()`

In `resetDecisions()` (line ~2801), add after the existing `setRefereeForcedDecision(false)`:

```java
this.currentInputKind = null;
```

### Add getters

Near the existing `isAnnouncerDecisionImmediate()` (line ~1173):

```java
public boolean isShowDecisionsImmediately() {
    return showDecisionsImmediately;
}

public InputKind getCurrentInputKind() {
    return currentInputKind;
}
```

### What NOT to do
- Do not remove `announcerDecisionImmediate` or `singleReferee` fields — they are still used (`announcerDecisionImmediate` for `DecisionFullUpdate.isImmediate()`, `singleReferee` for configuration/UI display).
- Do not change any existing method besides `init()` and `resetDecisions()`.
- `refereeForcedDecision` field, getter, and setter will be removed in Step 9d after all callers are updated.

---

## Step 2 — Add `TimingPolicy` enum

**File (new):** `owlcms/src/main/java/app/owlcms/fieldofplay/TimingPolicy.java`

```java
package app.owlcms.fieldofplay;

public enum TimingPolicy {
    IMMEDIATE,
    DELAYED
}
```

### What NOT to do
- Do not add this as an inner class of FieldOfPlay — it will be referenced by UIEvent classes in a different package.

---

## Step 3 — Add `InputKind` enum

**File (new):** `owlcms/src/main/java/app/owlcms/fieldofplay/InputKind.java`

```java
package app.owlcms.fieldofplay;

public enum InputKind {
    ANNOUNCER_ENTRY,
    SOLO_INPUT,
    THREE_REFEREE_INPUT
}
```

---

## Step 4 — Add `timingPolicy` and `inputKind` to UIEvent.Decision and UIEvent.InitialDecision

**File:** `owlcms/src/main/java/app/owlcms/uievents/UIEvent.java`

### 4a — UIEvent.Decision (lines ~475–524)

Add two fields:

```java
private TimingPolicy timingPolicy;
private InputKind inputKind;
```

Add a **new constructor** alongside the existing one (do NOT remove the existing constructor):

```java
public Decision(Athlete a, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
                Object origin, FieldOfPlay fop, boolean singleReferee,
                TimingPolicy timingPolicy, InputKind inputKind) {
    this(a, decision, ref1, ref2, ref3, origin, fop, singleReferee);
    this.timingPolicy = timingPolicy;
    this.inputKind = inputKind;
}
```

Add getters:

```java
public TimingPolicy getTimingPolicy() { return timingPolicy; }
public InputKind getInputKind() { return inputKind; }
```

### 4b — UIEvent.InitialDecision (lines ~525–548)

Same pattern — add fields, new constructor, getters:

```java
private TimingPolicy timingPolicy;
private InputKind inputKind;

public InitialDecision(Athlete a, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
                       Object origin, FieldOfPlay fop, boolean singleReferee,
                       TimingPolicy timingPolicy, InputKind inputKind) {
    this(a, decision, ref1, ref2, ref3, origin, fop, singleReferee);
    this.timingPolicy = timingPolicy;
    this.inputKind = inputKind;
}

public TimingPolicy getTimingPolicy() { return timingPolicy; }
public InputKind getInputKind() { return inputKind; }
```

### What NOT to do
- Do NOT remove or modify the existing constructors. All existing callers continue to work unchanged. The new fields default to `null` when the old constructor is used.

---

## Step 5 — Classify `inputKind` at the entry points

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Design principle

`currentInputKind` is set **once** at the earliest possible point — the entry methods that first receive a decision event. All downstream methods read `this.currentInputKind` instead of re-deriving from `isSingleReferee()` or `isRefereeForcedDecision()`.

### 5a — `doPossiblySoloRefereeUpdate` (lines 1923–1933)

This is the entry point for all `DecisionUpdate` events (individual MQTT referee inputs).

#### Current code:

```java
private void doPossiblySoloRefereeUpdate(FOPEvent e) {
    if (isSingleReferee() || ((DecisionUpdate) e).getRefIndex() < 0) {
        boolean goodLift = ((DecisionUpdate) e).isDecision();
        simulateDecision(new ExplicitDecision(e.getAthlete(), e.getStackTrace(), isAnnouncerDecisionImmediate(),
                goodLift, goodLift, goodLift));
    } else {
        updateRefereeDecisions((DecisionUpdate) e);
        uiShowUpdateOnJuryScreen(e);
    }
}
```

#### Replace with:

```java
private void doPossiblySoloRefereeUpdate(FOPEvent e) {
    DecisionUpdate du = (DecisionUpdate) e;

    if (du.getRefIndex() < 0) {
        // ANNOUNCER_ENTRY: MQTT referee number 0, announcer-equivalent input.
        // Classify first, then route to existing announcer fast path.
        this.currentInputKind = InputKind.ANNOUNCER_ENTRY;
        boolean goodLift = du.isDecision();
        simulateDecision(new ExplicitDecision(e.getAthlete(), e.getStackTrace(),
                isAnnouncerDecisionImmediate(), goodLift, goodLift, goodLift));

    } else if (isSingleReferee()) {
        // SOLO_INPUT: solo referee input.
        // Classify first, then route through normal decision processing.
        this.currentInputKind = InputKind.SOLO_INPUT;
        boolean goodLift = du.isDecision();
        // Fill all three referee slots with the solo decision value
        // so processRefereeDecisions sees a full majority.
        getRefereeDecision()[0] = goodLift;
        getRefereeDecision()[1] = goodLift;
        getRefereeDecision()[2] = goodLift;
        long now = System.currentTimeMillis();
        getRefereeTime()[0] = now;
        getRefereeTime()[1] = now;
        getRefereeTime()[2] = now;
        notifyDecisionWithoutClock(e.getOrigin());
        setClockOwner(null);
        if (getAthleteTimer().isRunning()) {
            getAthleteTimer().stop();
        }
        setPreviousAthlete(e.getAthlete());
        processRefereeDecisions(e);
        uiShowUpdateOnJuryScreen(e);

    } else {
        // THREE_REFEREE_INPUT: normal three-referee path.
        // Classify on first referee input (don't overwrite if already set from earlier input in this decision).
        if (this.currentInputKind == null) {
            this.currentInputKind = InputKind.THREE_REFEREE_INPUT;
        }
        updateRefereeDecisions((DecisionUpdate) e);
        uiShowUpdateOnJuryScreen(e);
    }
}
```

### 5b — `simulateDecision` (lines 3216–3237)

This is the entry point for `ExplicitDecision` events (announcer UI buttons). All state machine handlers for `ExplicitDecision` call this method.

#### Current code:

```java
private void simulateDecision(ExplicitDecision ed) {
    long now = System.currentTimeMillis();
    if (getAthleteTimer().isRunning()) {
        getAthleteTimer().stop();
    }
    notifyDecisionWithoutClock(ed.getOrigin());
    this.setClockOwner(null);
    DecisionFullUpdate ne = new DecisionFullUpdate(ed.getOrigin(), ed.getAthlete(), ed.ref1, ed.ref2, ed.ref3, now,
            now, now, isAnnouncerDecisionImmediate());
    setRefereeForcedDecision(true);
    updateRefereeDecisions(ne);
    setRefereeForcedDecision(true);
    uiShowUpdateOnJuryScreen(ed);
    this.setPreviousAthlete(ed.getAthlete());
}
```

#### Add classification at the top, remove `setRefereeForcedDecision`:

```java
private void simulateDecision(ExplicitDecision ed) {
    // Classify: simulateDecision is only reached via announcer paths.
    // (Announcer UI ExplicitDecision, or MQTT refIndex < 0 routed through doPossiblySoloRefereeUpdate.)
    // If already classified by doPossiblySoloRefereeUpdate, don't overwrite.
    if (this.currentInputKind == null) {
        this.currentInputKind = InputKind.ANNOUNCER_ENTRY;
    }
    long now = System.currentTimeMillis();
    if (getAthleteTimer().isRunning()) {
        getAthleteTimer().stop();
    }
    notifyDecisionWithoutClock(ed.getOrigin());
    this.setClockOwner(null);
    DecisionFullUpdate ne = new DecisionFullUpdate(ed.getOrigin(), ed.getAthlete(), ed.ref1, ed.ref2, ed.ref3, now,
            now, now, isAnnouncerDecisionImmediate());
    // setRefereeForcedDecision(true) removed — Step 6 uses currentInputKind instead.
    updateRefereeDecisions(ne);
    uiShowUpdateOnJuryScreen(ed);
    this.setPreviousAthlete(ed.getAthlete());
}
```

This handles the case where `simulateDecision` is called directly from state machine handlers for `ExplicitDecision` (announcer UI), not via `doPossiblySoloRefereeUpdate`.

### Why `isSingleReferee()` is still read in step 5a

The `isSingleReferee()` check in `doPossiblySoloRefereeUpdate` reads the FOP configuration to determine the initial routing branch. This is the **only place** where `isSingleReferee()` influences control flow. After `currentInputKind` is classified, all downstream methods read `this.currentInputKind` — they never call `isSingleReferee()` or `isRefereeForcedDecision()`.

### `setRefereeForcedDecision(true)` calls are removed in step 5b

The two `setRefereeForcedDecision(true)` calls in `simulateDecision` are removed. Step 6 uses `currentInputKind == ANNOUNCER_ENTRY` instead, and all UI receivers will be updated (Steps 9b–9c) to read from the UIEvent or `currentInputKind`, not from this flag. The field itself is removed in Step 9d.

### What NOT to do
- Do not remove `isSingleReferee()` from `FieldOfPlay` — it is the configuration getter for solo-vs-three mode.
- Do not change state machine dispatch (the `handleFOPEvent` switch) — it routes correctly already.

---

## Step 6 — Update `processRefereeDecisions` to remove dead branches and use `currentInputKind`

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Current code (lines ~2313–2327):

```java
if (isRefereeForcedDecision()) {
    setGoodLift(nbWhite >= 1);
    showDecisionNow(e.getOrigin());
    return;
}
if (isSingleReferee()) {
    goodLift = nbWhite >= 1;
    if (!this.downEmitted) {
        emitDown(e);
        this.downEmitted = true;
    }
    setGoodLift(nbWhite >= 1);
    processDecisionDelay(e);
    return;
}
```

After step 5, solo referee input no longer enters the `isSingleReferee()` branch here with only 1 non-null decision. It enters with all 3 slots filled. And `isRefereeForcedDecision()` is no longer used for routing — `currentInputKind` is used instead.

### Required change

Replace both branches with a single `currentInputKind` check:

```java
if (this.currentInputKind == InputKind.ANNOUNCER_ENTRY) {
    // Announcer path: immediate decision, no delay.
    setGoodLift(nbWhite >= 1);
    showDecisionNow(e.getOrigin());
    return;
}
// The previous isSingleReferee() block is removed.
// Solo input now arrives with all three referee slots filled
// (set in doPossiblySoloRefereeUpdate) and flows through the
// normal three-decision path below.
if (nbWhite >= 2 || nbRed >= 2) {
    // ... rest unchanged
```

### Also:
In the `nbDecisions == 3` block, before `processDecisionDelay(e)`, add the down signal if not emitted (solo input skips the normal 2-of-3 majority down-signal logic because all 3 arrive at once):

Currently this block looks like:

```java
if (nbDecisions == 3) {
    if (this.wakeUpRef != null) {
        cancelWakeUpRef();
    }
    notifyDecisionWithoutClock(e.getOrigin());
    setGoodLift(nbWhite >= 2);
    processDecisionDelay(e);
}
```

Change to:

```java
if (nbDecisions == 3) {
    if (this.wakeUpRef != null) {
        cancelWakeUpRef();
    }
    if (!this.downEmitted) {
        emitDown(e);
        this.downEmitted = true;
    }
    notifyDecisionWithoutClock(e.getOrigin());
    setGoodLift(nbWhite >= 2);
    processDecisionDelay(e);
}
```

This ensures solo input (which arrives as 3 simultaneous decisions) still emits the down signal.

---

## Step 6b — Update `showDecisionNow` to use `currentInputKind`

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Current code (line ~3126):

```java
private void showDecisionNow(Object origin) {
    int nbWhite = 0;
    if (isSingleReferee()) {
        for (int i = 0; i < 3; i++) {
            nbWhite = nbWhite + (Boolean.TRUE.equals(getRefereeDecision()[i]) ? 1 : 0);
            if (getRefereeDecision()[i] != null) {
                nbWhite = nbWhite == 0 ? 0 : 3;
                break;
            }
        }
    } else {
        for (int i = 0; i < 3; i++) {
            nbWhite = nbWhite + (Boolean.TRUE.equals(getRefereeDecision()[i]) ? 1 : 0);
        }
    }
```

### Required change

The `isSingleReferee()` branch was needed when solo input had only 1 referee slot filled. After Step 5a, solo input fills all 3 slots with the same value, so the normal counting path produces the same result (0 or 3). Remove the special branch:

```java
private void showDecisionNow(Object origin) {
    int nbWhite = 0;
    for (int i = 0; i < 3; i++) {
        nbWhite = nbWhite + (Boolean.TRUE.equals(getRefereeDecision()[i]) ? 1 : 0);
    }
```

This works for all three input kinds — announcer (3 identical), solo (3 identical), three-referee (normal majority).

---

## Step 6c — Update `uiShowUpdateOnJuryScreen` to use `currentInputKind`

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Current code (line ~3547):

```java
private void uiShowUpdateOnJuryScreen(FOPEvent e) {
    logger.debug("### uiShowUpdateOnJuryScreen {}", isRefereeForcedDecision());
    pushOutUIEvent(new UIEvent.RefereeUpdate(getCurAthlete(),
            isRefereeForcedDecision() ? null : getRefereeDecision()[0],
            getRefereeDecision()[1],
            isRefereeForcedDecision() ? null : getRefereeDecision()[2],
            getRefereeTime()[0],
            getRefereeTime()[1],
            getRefereeTime()[2],
            e.getOrigin(),
            isRefereeForcedDecision() || isSingleReferee(),
            this));
}
```

### Required change

Replace `isRefereeForcedDecision()` with `currentInputKind` throughout:

```java
private void uiShowUpdateOnJuryScreen(FOPEvent e) {
    InputKind inputKind = this.currentInputKind;
    boolean singleRef = (inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT);
    logger.debug("### uiShowUpdateOnJuryScreen inputKind={}", inputKind);
    pushOutUIEvent(new UIEvent.RefereeUpdate(getCurAthlete(),
            singleRef ? null : getRefereeDecision()[0],
            getRefereeDecision()[1],
            singleRef ? null : getRefereeDecision()[2],
            getRefereeTime()[0],
            getRefereeTime()[1],
            getRefereeTime()[2],
            e.getOrigin(),
            singleRef,
            this));
}
```

---

## Step 7 — Update `processDecisionDelay` to respect `showDecisionsImmediately`

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Current code (lines 2374–2393):

```java
public void processDecisionDelay(FOPEvent e) {
    if (!isDecisionDisplayScheduled()) {
        if (e instanceof FOPEvent.DecisionFullUpdate) {
            if (((FOPEvent.DecisionFullUpdate) e).isImmediate()) {
                showDecisionNow(e.getOrigin());
            } else {
                emitInitialDecisionEvent(e.getOrigin());
                showDecisionAfterDelay(e.getOrigin(), REVERSAL_DELAY);
            }
        } else {
            emitInitialDecisionEvent(this);
            showDecisionAfterDelay(this, REVERSAL_DELAY);
        }
    }
}
```

### Required change

Add the `showDecisionsImmediately` check. When enabled, emit INITIAL_DECISION (so videos/consumers are triggered) then show immediately:

```java
public void processDecisionDelay(FOPEvent e) {
    if (!isDecisionDisplayScheduled()) {
        if (e instanceof FOPEvent.DecisionFullUpdate) {
            if (((FOPEvent.DecisionFullUpdate) e).isImmediate()) {
                showDecisionNow(e.getOrigin());
            } else if (isShowDecisionsImmediately()) {
                emitInitialDecisionEvent(e.getOrigin());
                showDecisionNow(e.getOrigin());
            } else {
                emitInitialDecisionEvent(e.getOrigin());
                showDecisionAfterDelay(e.getOrigin(), REVERSAL_DELAY);
            }
        } else {
            emitInitialDecisionEvent(this);
            if (isShowDecisionsImmediately()) {
                showDecisionNow(this);
            } else {
                showDecisionAfterDelay(this, REVERSAL_DELAY);
            }
        }
    }
}
```

### What this does
- `DecisionFullUpdate.isImmediate()` path → unchanged (announcer path).
- `showDecisionsImmediately` is `true` → INITIAL_DECISION is emitted, then `showDecisionNow` (no delay).
- Default → INITIAL_DECISION emitted, then `showDecisionAfterDelay` (3s reversal window).

---

## Step 8 — Update `emitInitialDecisionEvent` to read `currentInputKind` from FOP

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Current code (lines 2396–2408):

```java
private void emitInitialDecisionEvent(Object origin) {
    Boolean[] refereeDecisions = getRefereeDecision();
    int nbWhite = 0;
    for (int i = 0; i < 3; i++) {
        nbWhite = nbWhite + (Boolean.TRUE.equals(refereeDecisions[i]) ? 1 : 0);
    }
    Boolean pendingDecision = isSingleReferee() ? (nbWhite >= 1) : (nbWhite >= 2);
    pushOutUIEvent(new UIEvent.InitialDecision(getCurAthlete(), pendingDecision,
            refereeDecisions[0], refereeDecisions[1], refereeDecisions[2],
            origin, this, isRefereeForcedDecision() || isSingleReferee()));
}
```

### Required change

Read `currentInputKind` from the FOP (classified in step 5) instead of re-deriving:

```java
private void emitInitialDecisionEvent(Object origin) {
    Boolean[] refereeDecisions = getRefereeDecision();
    int nbWhite = 0;
    for (int i = 0; i < 3; i++) {
        nbWhite = nbWhite + (Boolean.TRUE.equals(refereeDecisions[i]) ? 1 : 0);
    }
    // Use currentInputKind (set at entry point) to determine display and timing.
    InputKind inputKind = this.currentInputKind;
    boolean singleRef = (inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT);
    Boolean pendingDecision = singleRef ? (nbWhite >= 1) : (nbWhite >= 2);
    TimingPolicy timingPolicy = isShowDecisionsImmediately() ? TimingPolicy.IMMEDIATE : TimingPolicy.DELAYED;

    pushOutUIEvent(new UIEvent.InitialDecision(getCurAthlete(), pendingDecision,
            refereeDecisions[0], refereeDecisions[1], refereeDecisions[2],
            origin, this, singleRef,
            timingPolicy, inputKind));
}
```

### Design notes
- `singleRef` is derived from `inputKind` — **not** from `isSingleReferee()` or `isRefereeForcedDecision()`. This is the key cleanup: the legacy flags are no longer consulted for this purpose.
- `emitInitialDecisionEvent` is never called for `ANNOUNCER_ENTRY` (that path bypasses it via `simulateDecision` → `showDecisionNow`). The `singleRef` guard for `ANNOUNCER_ENTRY` is defensive only.

---

## Step 9 — Update `uiShowRefereeDecisionOnSlaveDisplays` to read `currentInputKind` from FOP

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Current code in `uiShowRefereeDecisionOnSlaveDisplays` (lines 3525–3542):

```java
private void uiShowRefereeDecisionOnSlaveDisplays(Athlete athlete2, Boolean goodLift2, Boolean[] refereeDecision2,
        Long[] longs, Object origin2) {
    Boolean ref1 = null;
    Boolean ref2 = null;
    Boolean ref3 = null;
    if (isRefereeForcedDecision()) {
        ref1 = null;
        ref2 = refereeDecision2[1];
        ref3 = null;
    } else {
        ref1 = refereeDecision2[0];
        ref2 = refereeDecision2[1];
        ref3 = refereeDecision2[2];
    }
    pushOutUIEvent(new UIEvent.Decision(athlete2, goodLift2, ref1, ref2, ref3, origin2, this, isRefereeForcedDecision() || isSingleReferee()));
    setRefereeForcedDecision(false);
}
```

### Required change

Read `currentInputKind` from the FOP (classified in step 5) instead of re-deriving from legacy flags:

```java
private void uiShowRefereeDecisionOnSlaveDisplays(Athlete athlete2, Boolean goodLift2, Boolean[] refereeDecision2,
        Long[] longs, Object origin2) {
    // Use currentInputKind (set at entry point) for all semantic decisions.
    InputKind inputKind = this.currentInputKind;
    boolean singleRef = (inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT);

    Boolean ref1 = null;
    Boolean ref2 = null;
    Boolean ref3 = null;
    if (singleRef) {
        // Single-light display: center light only.
        ref1 = null;
        ref2 = refereeDecision2[1];
        ref3 = null;
    } else {
        ref1 = refereeDecision2[0];
        ref2 = refereeDecision2[1];
        ref3 = refereeDecision2[2];
    }

    TimingPolicy timingPolicy;
    if (inputKind == InputKind.ANNOUNCER_ENTRY) {
        timingPolicy = TimingPolicy.IMMEDIATE;
    } else {
        timingPolicy = isShowDecisionsImmediately() ? TimingPolicy.IMMEDIATE : TimingPolicy.DELAYED;
    }

    pushOutUIEvent(new UIEvent.Decision(athlete2, goodLift2, ref1, ref2, ref3, origin2, this,
            singleRef,
            timingPolicy, inputKind));
    // setRefereeForcedDecision(false) removed — field is deleted in Step 9d.
}
```

### What changed
- `isRefereeForcedDecision()` and `isSingleReferee()` are **no longer consulted**. The single-light vs three-light decision is derived from `inputKind`.
- `timingPolicy` is determined from `inputKind` + `showDecisionsImmediately`, not from the code path.
- The legacy `singleReferee` argument to UIEvent.Decision is set to `singleRef` (derived from `inputKind`), maintaining backward compatibility for existing UI receivers.
- `setRefereeForcedDecision(false)` is removed — the field is deleted in Step 9d.

---

## Step 9b — Fix `NCurrentAthlete` to read from UIEvent instead of FOP

**File:** `owlcms/src/main/java/app/owlcms/displays/scoreboard/NCurrentAthlete.java`

### Current code (line ~159):

```java
if (getFop().isSingleReferee() || getFop().isRefereeForcedDecision()) {
    decisions.set(0, e.ref2 != null ? e.ref2 : e.decision);
} else {
    decisions.set(0, e.ref1);
    decisions.set(1, e.ref2);
    decisions.set(2, e.ref3);
}
```

### Required change

Read from the UIEvent's `isSingleReferee()` flag (which is already correctly set for both announcer and solo paths in Steps 8–9):

```java
if (e.isSingleReferee()) {
    decisions.set(0, e.ref2 != null ? e.ref2 : e.decision);
} else {
    decisions.set(0, e.ref1);
    decisions.set(1, e.ref2);
    decisions.set(2, e.ref3);
}
```

### Why this is correct
- `UIEvent.Decision` already carries `singleReferee=true` for ANNOUNCER_ENTRY and SOLO_INPUT (set in Step 9).
- `UIEvent.InitialDecision` also carries `singleReferee=true` for the same cases (set in Step 8).
- Reading from the event is safe — no timing dependency on FOP flag being in the right state when the UI thread processes the event.

### Why reading FOP was a latent bug
`uiShowRefereeDecisionOnSlaveDisplays` used to call `setRefereeForcedDecision(false)` after emitting the UIEvent. If the UI thread processed the event after the flag was cleared, `NCurrentAthlete` would see the wrong value. Reading from the event avoids this race entirely.

---

## Step 9c — Fix `JuryContent` to read from `currentInputKind` instead of FOP flags

**File:** `owlcms/src/main/java/app/owlcms/nui/lifting/JuryContent.java`

### Current code (line ~400):

```java
if (fop.isRefereeForcedDecision()) {
    this.decisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(this.athleteUnderReview, null,
            curRefDecisions[1], null, null, curRefTimes[1], null, this, true, fop));
} else {
    this.decisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(this.athleteUnderReview,
            curRefDecisions[0],
            curRefDecisions[1], curRefDecisions[2], curRefTimes[0], curRefTimes[1], curRefTimes[2],
            this, false, fop));
}
```

### Required change

Replace `isRefereeForcedDecision()` with `currentInputKind`:

```java
InputKind inputKind = fop.getCurrentInputKind();
boolean singleRef = (inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT);
if (singleRef) {
    this.decisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(this.athleteUnderReview, null,
            curRefDecisions[1], null, null, curRefTimes[1], null, this, true, fop));
} else {
    this.decisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(this.athleteUnderReview,
            curRefDecisions[0],
            curRefDecisions[1], curRefDecisions[2], curRefTimes[0], curRefTimes[1], curRefTimes[2],
            this, false, fop));
}
```

---

## Step 9d — Remove `refereeForcedDecision` field from `FieldOfPlay`

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

After Steps 6, 6c, 9, 9b, and 9c, all callers of `isRefereeForcedDecision()` and `setRefereeForcedDecision()` have been updated. Now remove:

1. **Field:** Remove `private boolean refereeForcedDecision;` (near line ~237)
2. **Getter:** Remove `public boolean isRefereeForcedDecision()` (near line ~1195)
3. **Setter:** Remove `public void setRefereeForcedDecision(boolean)` (near line ~1509)
4. **In `resetDecisions()`:** Remove `setRefereeForcedDecision(false)` (near line ~2806)

### What NOT to do
- Do NOT remove `isSingleReferee()`, `setSingleReferee()`, or the `singleReferee` field — these remain as a FOP configuration setting (solo-vs-three mode).

---

## Step 10 — Serialize `timingPolicy` and `inputKind` in forwarders

### 10a — ForwarderPayloadBuilder.java

**File:** `owlcms/src/main/java/app/owlcms/monitors/websocket/ForwarderPayloadBuilder.java`

In `createDecision()` (around line 230), after the existing `singleReferee` serialization, add:

```java
if (event instanceof UIEvent.Decision) {
    UIEvent.Decision de = (UIEvent.Decision) event;
    if (de.getTimingPolicy() != null) {
        params.put("timingPolicy", de.getTimingPolicy().name());
    }
    if (de.getInputKind() != null) {
        params.put("inputKind", de.getInputKind().name());
    }
}
if (event instanceof UIEvent.InitialDecision) {
    UIEvent.InitialDecision ide = (UIEvent.InitialDecision) event;
    if (ide.getTimingPolicy() != null) {
        params.put("timingPolicy", ide.getTimingPolicy().name());
    }
    if (ide.getInputKind() != null) {
        params.put("inputKind", ide.getInputKind().name());
    }
}
```

The `null` guards ensure backward compatibility — if events are constructed via the old constructor, these fields are simply absent.

### 10b — EventForwarder.java private `createDecision()`

**File:** `owlcms/src/main/java/app/owlcms/monitors/EventForwarder.java`

In the private `createDecision()` (around lines 1030–1066), after existing `singleReferee` / `decision` serialization, add the same `timingPolicy` and `inputKind` fields.

Also fix the existing inconsistency: `singleReferee` is missing for `UIEvent.Decision` in this forwarder. Add it.

### 10c — WebSocketEventForwarder.java private `createDecision()`

**File:** `owlcms/src/main/java/app/owlcms/monitors/WebSocketEventForwarder.java`

Same pattern as 10b. The WebSocket forwarder already serializes `singleReferee` for both event types, so just add `timingPolicy` and `inputKind`.

### Serialization format

Fields are serialized as strings with enum names:

| Field | Example value |
|---|---|
| `timingPolicy` | `"IMMEDIATE"` or `"DELAYED"` |
| `inputKind` | `"ANNOUNCER_ENTRY"`, `"SOLO_INPUT"`, or `"THREE_REFEREE_INPUT"` |

These are additive — they do NOT replace `singleReferee`, `d1/d2/d3`, or `decisionEventType`.

---

## Step 11 — No dedicated UI toggle needed

The `showDecisionsImmediately` value is read from the existing `Config.featureSwitch` mechanism during `init()` (see Step 1). Operators enable it by adding `showDecisionsImmediately` to the feature switches string in **Prepare Competition → Language and System Settings**, then reloading the group. The value can also be set at startup via environment variable:

```
OWLCMS_FEATURESWITCHES=showDecisionsImmediately
```

No `AnnouncerContent.java` menu item or translation key is needed for this step.

---

## Step 12 — Verify: no regressions in announcer explicit decision path

**Verification only — no code changes in this step.**

Trace the announcer path after all changes:

1. Announcer clicks good/bad → `FOPEvent.ExplicitDecision` posted.
2. All states route to `simulateDecision(ed)` — **unchanged**.
3. `simulateDecision` → classifies `currentInputKind = ANNOUNCER_ENTRY` → `updateRefereeDecisions` → `processRefereeDecisions`.
4. `processRefereeDecisions` → `currentInputKind == ANNOUNCER_ENTRY` → `showDecisionNow`.
5. `showDecisionNow` → normal white count (all 3 slots identical) → `uiShowRefereeDecisionOnSlaveDisplays` → `singleRef=true`, `timingPolicy=IMMEDIATE`, `inputKind=ANNOUNCER_ENTRY`.
6. No `INITIAL_DECISION` emitted — **correct per spec**.

---

## Step 13 — Verify: solo referee through new path

**Verification only — no code changes in this step.**

Trace the solo referee path after all changes:

1. MQTT delivers `DecisionUpdate` with `refIndex >= 0`, `isSingleReferee()=true`.
2. `doPossiblySoloRefereeUpdate` → solo branch classifies `currentInputKind = SOLO_INPUT`, fills all 3 referee slots, stops timer, calls `processRefereeDecisions`.
3. `processRefereeDecisions` → `currentInputKind == SOLO_INPUT` (not `ANNOUNCER_ENTRY`) → skips announcer fast path.
4. `nbDecisions == 3` → emits down signal → `processDecisionDelay`.
5. `processDecisionDelay` → event is not `DecisionFullUpdate` → enters the `else` branch.
6. If `showDecisionsImmediately` is `false`: `emitInitialDecisionEvent` (with `timingPolicy=DELAYED`, `inputKind=SOLO_INPUT`) → `showDecisionAfterDelay` (3s delay) — **new behavior: solo now gets reversal delay**.
7. If `showDecisionsImmediately` is `true`: `emitInitialDecisionEvent` (with `timingPolicy=IMMEDIATE`, `inputKind=SOLO_INPUT`) → `showDecisionNow` — **equivalent to old behavior**.

---

## Step 14 — Verify: three-referee majority path

**Verification only — no code changes in this step.**

Trace the three-referee path:

1. Three individual `DecisionUpdate` events arrive → `updateRefereeDecisions` for each → `processRefereeDecisions`.
2. When 3rd arrives: `nbDecisions == 3` → `processDecisionDelay`.
3. If `showDecisionsImmediately` is `false`: `emitInitialDecisionEvent` (with `timingPolicy=DELAYED`, `inputKind=THREE_REFEREE_INPUT`) → `showDecisionAfterDelay` — **same as before**.
4. If `showDecisionsImmediately` is `true`: `emitInitialDecisionEvent` (with `timingPolicy=IMMEDIATE`, `inputKind=THREE_REFEREE_INPUT`) → `showDecisionNow` — **new: skips reversal delay when toggle enabled**.

---

## Step 15 — Verify: MQTT `refIndex < 0` (announcer MQTT device)

**Verification only — no code changes in this step.**

Trace:

1. MQTT delivers `DecisionUpdate` with `refIndex = -1`.
2. `doPossiblySoloRefereeUpdate` → `refIndex < 0` → enters announcer branch → `simulateDecision`.
3. Same as step 12 from point 3 onward — announcer fast path.

This works in BOTH `SOLO_REFEREE_MODE` and `THREE_REFEREE_MODE` because the `refIndex < 0` check is evaluated first, before `isSingleReferee()`.

---

## Summary of files changed

| File | Changes |
|---|---|
| `FieldOfPlay.java` | Add `showDecisionsImmediately` (from Config in `init()`), `currentInputKind` (set at entry points, cleared in `resetDecisions()`), getter for each. Classify `inputKind` in `doPossiblySoloRefereeUpdate` and `simulateDecision`. Remove `setRefereeForcedDecision()` calls from `simulateDecision` and `uiShowRefereeDecisionOnSlaveDisplays`. Replace `isRefereeForcedDecision()` with `currentInputKind == ANNOUNCER_ENTRY` and remove `isSingleReferee()` branch from `processRefereeDecisions`. Remove `isSingleReferee()` branch from `showDecisionNow`. Rewrite `uiShowUpdateOnJuryScreen` to use `currentInputKind`. Add `showDecisionsImmediately` check to `processDecisionDelay`. Rewrite `emitInitialDecisionEvent` and `uiShowRefereeDecisionOnSlaveDisplays` to read `currentInputKind`. Add down-signal emission to `nbDecisions==3` block. Remove `refereeForcedDecision` field, getter, and setter. |
| `TimingPolicy.java` | New enum file |
| `InputKind.java` | New enum file |
| `UIEvent.java` | Add `timingPolicy`/`inputKind` fields and new constructors to `Decision` and `InitialDecision` inner classes. Existing constructors untouched. |
| `NCurrentAthlete.java` | Replace `getFop().isSingleReferee() \|\| getFop().isRefereeForcedDecision()` with `e.isSingleReferee()` (read from UIEvent). |
| `JuryContent.java` | Replace `fop.isRefereeForcedDecision()` with `fop.getCurrentInputKind()` check. |
| `ForwarderPayloadBuilder.java` | Serialize `timingPolicy` and `inputKind` (additive, null-guarded) |
| `EventForwarder.java` | Serialize `timingPolicy` and `inputKind` (additive, null-guarded). Fix missing `singleReferee` on `Decision`. |
| `WebSocketEventForwarder.java` | Serialize `timingPolicy` and `inputKind` (additive, null-guarded) |

## Files NOT changed

| File | Why not |
|---|---|
| `DecisionElement.java` | Already reads `e.isSingleReferee()` from UIEvent — works unchanged. |
| `FOPEvent.java` | No changes to event classes needed |
| `FOPState.java` | No changes needed |
| `DecisionEventType.java` | Values already correct |

## Behavioral changes from before

1. **`inputKind` classified once at the entry point.** `currentInputKind` is set in `doPossiblySoloRefereeUpdate` or `simulateDecision` and read by all downstream methods. No downstream method re-derives input classification.
2. **`refereeForcedDecision` is removed.** The field, getter, and setter are deleted from `FieldOfPlay`. All callers now use `currentInputKind` or the UIEvent's `isSingleReferee()` flag.
3. **`isSingleReferee()` is a configuration getter only.** It is read once at the entry point in `doPossiblySoloRefereeUpdate` to determine solo-vs-three routing. After that, `currentInputKind` is the single source of truth. `isSingleReferee()` is never read downstream for timing, emission, or single-light display decisions.
4. **Solo referee input now gets a 3-second reversal delay by default.** Previously, solo input was immediate because it shared the announcer `refereeForcedDecision` bypass.
5. **`showDecisionsImmediately` feature switch (default off)** allows all referee-originated decisions to skip the reversal delay while still emitting `INITIAL_DECISION`. Uses the existing `Config.featureSwitch` mechanism — read once at session load in `init()`. To change: edit feature switches in system settings, then reload the group.
6. **`timingPolicy` and `inputKind` are carried through the entire event chain** and serialized to external consumers. Existing fields are untouched.
7. **MQTT `refIndex < 0` (announcer device)** is now cleanly separated from solo referee input in `doPossiblySoloRefereeUpdate`.

## Backward compatibility guarantees

- All existing UIEvent constructors preserved
- `singleReferee` boolean continues to be set on all UIEvents (derived from `currentInputKind`)
- `isSingleReferee()` remains on `FieldOfPlay` as a configuration getter (solo-vs-three mode)
- `refereeForcedDecision` is **removed** from `FieldOfPlay` — no callers remain after Steps 6, 6c, 9, 9b, 9c
- `d1/d2/d3` and `decisionEventType` output format unchanged
- New fields (`timingPolicy`, `inputKind`) are additive and null-guarded
- `DecisionElement` works unchanged (already reads from UIEvent)
- `NCurrentAthlete` updated to read from UIEvent (Step 9b) — fixes a latent race condition
- `JuryContent` updated to read `currentInputKind` (Step 9c)
- External consumers (publicresults, tracker-core) see same legacy fields plus optional new ones
