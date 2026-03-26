# Decision Normalization — Concrete Implementation Plan

Date: 2026-03-26
Specification: `DECISION_NORMALIZATION_MATRIX.md`

## Prerequisites

Read the specification fully before starting any step. Each step describes: the file(s), the current code, the exact change, and what NOT to touch. Steps must be executed in order — later steps depend on earlier ones.

---

## Step 1 — Add `showDecisionsImmediately` field to `FieldOfPlay`

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Design

This follows the existing `Config.getCurrent().featureSwitch("name")` pattern. The value is read once when the session is loaded — not checked on every decision. No `volatile` needed because FieldOfPlay methods are `synchronized` and the value is only read on the FOP thread after initialization.

### What to add

Near the existing field `announcerDecisionImmediate` (line ~233), add:

```java
private boolean showDecisionsImmediately = false;
```

### Initialize from Config feature switch

In the `init()` method (line ~1107), near the top after existing field initialization, add:

```java
this.showDecisionsImmediately = Config.getCurrent().featureSwitch("showDecisionsImmediately");
```

This means:
- The value is read from the `featureSwitches` string once per `init()` call (which happens inside `loadGroup()`)
- To change it live, the operator edits the feature switches in **Prepare Competition → Language and System Settings**, then reloads the group
- Environment variable `OWLCMS_FEATURESWITCHES=showDecisionsImmediately` can set it at startup
- No per-FOP UI toggle needed — the feature switch mechanism handles it

### Add getter (no setter needed — initialized from Config)

Near the existing `isAnnouncerDecisionImmediate()` (line ~1173):

```java
public boolean isShowDecisionsImmediately() {
    return showDecisionsImmediately;
}
```

### What NOT to do
- Do not remove `announcerDecisionImmediate`, `refereeForcedDecision`, or `singleReferee` fields yet.
- Do not change any existing method besides `init()`.

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

## Step 5 — Reroute `doPossiblySoloRefereeUpdate` for solo referee input

**File:** `owlcms/src/main/java/app/owlcms/fieldofplay/FieldOfPlay.java`

### Current code (lines 1923–1933):

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

### Required change

Split the condition into three paths:

1. **`refIndex < 0`** → announcer entry. Keep routing to `simulateDecision` (unchanged behavior).
2. **`isSingleReferee()`** → solo referee input. Route through normal decision processing (NOT `simulateDecision`).
3. **else** → three-referee input. Keep existing behavior.

Replace with:

```java
private void doPossiblySoloRefereeUpdate(FOPEvent e) {
    DecisionUpdate du = (DecisionUpdate) e;
    if (du.getRefIndex() < 0) {
        // Announcer-equivalent MQTT input (referee number 0).
        // Always goes through simulateDecision — immediate, no INITIAL_DECISION.
        boolean goodLift = du.isDecision();
        simulateDecision(new ExplicitDecision(e.getAthlete(), e.getStackTrace(),
                isAnnouncerDecisionImmediate(), goodLift, goodLift, goodLift));
    } else if (isSingleReferee()) {
        // Solo referee input. Treat the first valid decision as the solo decision.
        // Route through normal decision processing so it gets INITIAL_DECISION
        // and reversal delay (unless showDecisionsImmediately is enabled).
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
        // Notify if decision came without clock
        notifyDecisionWithoutClock(e.getOrigin());
        setClockOwner(null);
        if (getAthleteTimer().isRunning()) {
            getAthleteTimer().stop();
        }
        setPreviousAthlete(e.getAthlete());
        // Now process as normal — processRefereeDecisions will see 3 decisions
        // and route to processDecisionDelay (not showDecisionNow).
        processRefereeDecisions(e);
        uiShowUpdateOnJuryScreen(e);
    } else {
        // Normal three-referee path
        updateRefereeDecisions((DecisionUpdate) e);
        uiShowUpdateOnJuryScreen(e);
    }
}
```

### Why this works
- `refIndex < 0` still uses `simulateDecision` which sets `refereeForcedDecision=true` → goes to `showDecisionNow` (announcer fast path preserved).
- `isSingleReferee()` now fills all 3 slots and calls `processRefereeDecisions`. Since `refereeForcedDecision` is still `false`, it will NOT take the forced-decision shortcut. Instead, it reaches the `nbDecisions == 3` branch → `processDecisionDelay`.
- Three-referee path is unchanged.

### What NOT to do
- Do not modify `simulateDecision` yet.
- Do not remove the `isSingleReferee()` check from `processRefereeDecisions` yet — step 6 handles that.

---

## Step 6 — Update `processRefereeDecisions` to remove the dead solo branch

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

After step 5, solo referee input no longer enters the `isSingleReferee()` branch here with only 1 non-null decision. It enters with all 3 slots filled. But keeping the `isSingleReferee()` branch with its `nbWhite >= 1` threshold is harmful — it changes majority logic.

### Required change

Remove the `isSingleReferee()` block entirely. Solo input now arrives with all 3 slots filled and goes through the normal `nbDecisions == 3` path at the bottom.

```java
if (isRefereeForcedDecision()) {
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

## Step 8 — Pass `timingPolicy` and `inputKind` through `emitInitialDecisionEvent`

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

Determine `inputKind` and `timingPolicy`, then use the new constructor:

```java
private void emitInitialDecisionEvent(Object origin) {
    Boolean[] refereeDecisions = getRefereeDecision();
    int nbWhite = 0;
    for (int i = 0; i < 3; i++) {
        nbWhite = nbWhite + (Boolean.TRUE.equals(refereeDecisions[i]) ? 1 : 0);
    }
    boolean singleRef = isRefereeForcedDecision() || isSingleReferee();
    Boolean pendingDecision = singleRef ? (nbWhite >= 1) : (nbWhite >= 2);

    InputKind inputKind = isSingleReferee() ? InputKind.SOLO_INPUT : InputKind.THREE_REFEREE_INPUT;
    TimingPolicy timingPolicy = isShowDecisionsImmediately() ? TimingPolicy.IMMEDIATE : TimingPolicy.DELAYED;

    pushOutUIEvent(new UIEvent.InitialDecision(getCurAthlete(), pendingDecision,
            refereeDecisions[0], refereeDecisions[1], refereeDecisions[2],
            origin, this, singleRef,
            timingPolicy, inputKind));
}
```

### Design note
`emitInitialDecisionEvent` is never called for announcer entry (that path goes through `simulateDecision` → `showDecisionNow` directly). So `inputKind == ANNOUNCER_ENTRY` is never produced here — which is correct per the spec.

---

## Step 9 — Pass `timingPolicy` and `inputKind` through `UIEvent.Decision`

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

Determine `inputKind` and `timingPolicy`, then use the new constructor:

```java
private void uiShowRefereeDecisionOnSlaveDisplays(Athlete athlete2, Boolean goodLift2, Boolean[] refereeDecision2,
        Long[] longs, Object origin2) {
    Boolean ref1 = null;
    Boolean ref2 = null;
    Boolean ref3 = null;
    boolean forced = isRefereeForcedDecision();
    if (forced) {
        ref1 = null;
        ref2 = refereeDecision2[1];
        ref3 = null;
    } else {
        ref1 = refereeDecision2[0];
        ref2 = refereeDecision2[1];
        ref3 = refereeDecision2[2];
    }

    InputKind inputKind;
    TimingPolicy timingPolicy;
    if (forced) {
        inputKind = InputKind.ANNOUNCER_ENTRY;
        timingPolicy = TimingPolicy.IMMEDIATE;
    } else if (isSingleReferee()) {
        inputKind = InputKind.SOLO_INPUT;
        timingPolicy = isShowDecisionsImmediately() ? TimingPolicy.IMMEDIATE : TimingPolicy.DELAYED;
    } else {
        inputKind = InputKind.THREE_REFEREE_INPUT;
        timingPolicy = isShowDecisionsImmediately() ? TimingPolicy.IMMEDIATE : TimingPolicy.DELAYED;
    }

    pushOutUIEvent(new UIEvent.Decision(athlete2, goodLift2, ref1, ref2, ref3, origin2, this,
            forced || isSingleReferee(),
            timingPolicy, inputKind));
    setRefereeForcedDecision(false);
}
```

### Design note
The `forced` variable captures `isRefereeForcedDecision()` BEFORE the `setRefereeForcedDecision(false)` reset at the bottom. This is the existing behavior — unchanged.

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
3. `simulateDecision` → `setRefereeForcedDecision(true)` → `updateRefereeDecisions` → `processRefereeDecisions`.
4. `processRefereeDecisions` → `isRefereeForcedDecision()` is `true` → `showDecisionNow` — **unchanged**.
5. `showDecisionNow` → `uiShowRefereeDecisionOnSlaveDisplays` → `forced=true` → `inputKind=ANNOUNCER_ENTRY`, `timingPolicy=IMMEDIATE` — **new fields, same behavior**.
6. `setRefereeForcedDecision(false)` at end — **unchanged**.
7. No `INITIAL_DECISION` emitted — **correct per spec**.

---

## Step 13 — Verify: solo referee through new path

**Verification only — no code changes in this step.**

Trace the solo referee path after all changes:

1. MQTT delivers `DecisionUpdate` with `refIndex >= 0`, `isSingleReferee()=true`.
2. `doPossiblySoloRefereeUpdate` → new solo branch fills all 3 referee slots, stops timer, calls `processRefereeDecisions`.
3. `processRefereeDecisions` → `isRefereeForcedDecision()` is `false` → skips forced-decision shortcut.
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
| `FieldOfPlay.java` | Add `showDecisionsImmediately` field initialized from `Config.featureSwitch` in `init()`, add getter. Rewrite `doPossiblySoloRefereeUpdate`. Remove `isSingleReferee()` branch from `processRefereeDecisions`. Add `showDecisionsImmediately` check to `processDecisionDelay`. Add `timingPolicy`/`inputKind` to `emitInitialDecisionEvent` and `uiShowRefereeDecisionOnSlaveDisplays`. Add down-signal emission to `nbDecisions==3` block. |
| `TimingPolicy.java` | New enum file |
| `InputKind.java` | New enum file |
| `UIEvent.java` | Add `timingPolicy`/`inputKind` fields and new constructors to `Decision` and `InitialDecision` inner classes. Existing constructors untouched. |
| `ForwarderPayloadBuilder.java` | Serialize `timingPolicy` and `inputKind` (additive, null-guarded) |
| `EventForwarder.java` | Serialize `timingPolicy` and `inputKind` (additive, null-guarded). Fix missing `singleReferee` on `Decision`. |
| `WebSocketEventForwarder.java` | Serialize `timingPolicy` and `inputKind` (additive, null-guarded) |

## Files NOT changed

| File | Why not |
|---|---|
| `DecisionElement.java` | Still consumes `isSingleReferee()` — works unchanged |
| `NCurrentAthlete.java` | Still consumes `isSingleReferee() OR isRefereeForcedDecision()` — works unchanged |
| `FOPEvent.java` | No changes to event classes needed |
| `FOPState.java` | No changes needed |
| `DecisionEventType.java` | Values already correct |

## Behavioral changes from before

1. **Solo referee input now gets a 3-second reversal delay by default.** Previously, solo input was immediate because it shared the announcer `refereeForcedDecision` bypass.
2. **`showDecisionsImmediately` feature switch (default off)** allows all referee-originated decisions to skip the reversal delay while still emitting `INITIAL_DECISION`. Uses the existing `Config.featureSwitch` mechanism — read once at session load in `init()`. To change live: edit feature switches in system settings, then reload the group.
3. **`timingPolicy` and `inputKind` are carried through the entire event chain** and serialized to external consumers. Existing fields are untouched.
4. **MQTT `refIndex < 0` (announcer device)** is now cleanly separated from solo referee input in `doPossiblySoloRefereeUpdate`.

## Backward compatibility guarantees

- All existing constructors preserved
- `singleReferee` boolean continues to be set on all events
- `d1/d2/d3` and `decisionEventType` output format unchanged
- New fields are additive and null-guarded
- Existing UI receivers (`DecisionElement`, `NCurrentAthlete`) work unchanged
- External consumers (publicresults, tracker-core) see same legacy fields plus optional new ones
