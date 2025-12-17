# Impact Analysis: Accepting Referee Decisions When Clock Has Not Started

## Current Behavior

When the FOP is in `CURRENT_ATHLETE_DISPLAYED` state (clock not yet started), referee decision events (`DecisionFullUpdate` and `DecisionUpdate`) fall through to the `else` block in the state machine (lines 898-901), which generates an error notification and ignores the decision.

**Relevant code (lines 876-901):**
```java
case CURRENT_ATHLETE_DISPLAYED:
    checkDeferredWeightChanges();
    if (e instanceof TimeStarted) { ... }
    else if (e instanceof WeightChange) { ... }
    else if (e instanceof JuryDecision) { ... }
    else if (e instanceof ForceTime) { ... }
    else if (e instanceof CeremonyDone) { ... }
    else {
        pushOutUIEvent(new UIEvent.Notification(...ERROR...));
        // unexpectedEventInState(e, CURRENT_ATHLETE_DISPLAYED);
    }
    break;
```

## Existing Mechanism to Leverage

There is already a `clockStoppedDecisionsAllowed` flag (line 240) that is used in specific scenarios:
1. Set to `true` in `resumeLifting()` when returning from a break with the same clock owner (line 2761)
2. Set to `true` in `transitionToLifting()` when the current athlete is the clock owner and coming from BREAK/INACTIVE states (line 3211)
3. Set to `false` in `resetEmittedFlags()` (line 2724)

However, this flag is not currently checked in the state machine when processing decisions.

---

## Proposed Solutions

### Option A: Direct State Setup (Recommended)

**Concept:** When a referee decision arrives in `CURRENT_ATHLETE_DISPLAYED` state, the code directly sets up the required preconditions (clock owner, down signal preparation, weight tracking) and transitions to `TIME_STOPPED` state without actually starting the timer. The decision is then processed inline.

**Implementation Steps:**

1. **Modify `CURRENT_ATHLETE_DISPLAYED` case** (lines 876-902):
   - Add handlers for `DecisionFullUpdate` and `DecisionUpdate` events
   - Before processing, call a new method `setupForDecisionWithoutClock()`
   - Show notification to announcer/timekeeper that decision was accepted without clock

2. **Create `setupForDecisionWithoutClock()` method:**
   ```java
   private void setupForDecisionWithoutClock(FOPEvent e) {
       // Set up clock ownership as if timekeeper started it
       setClockOwner(getCurAthlete());
       
       // Reset emitted flags - CRITICAL for down signal to work
       // This sets downEmitted=false so emitDown() will actually emit
       resetEmittedFlags();
       
       prepareDownSignal();
       setWeightAtLastStart();
       
       // Clear any previous lift result
       setGoodLift(null);
       
       // Do NOT call resetDecisions() - we want to process incoming decisions
       
       // Transition to TIME_STOPPED (as if athlete already lifted bar)
       setState(TIME_STOPPED);
       setClockStoppedDecisionsAllowed(true);
       
       // Notify announcer/timekeeper
       pushOutUIEvent(new UIEvent.Notification(
           getCurAthlete(), e.getOrigin(),
           UIEvent.Notification.Level.ERROR,
           "Decision.ClockNotStarted",
           5000, this));
   }
   ```

3. **Add new translation key** `Decision.ClockNotStarted`:
   - English: "Decision received - clock was not started (accepting decision anyway)"
   - Create TSV file for all translations

**Files Affected:**
- `FieldOfPlay.java` - State machine modification (lines 876-902)
- `translation4.csv` or new TSV file - New notification string

**Pros:**
- Timer never actually starts - no display artifacts
- Simple logic - reuses existing decision processing flow
- No changes to decision processing methods (`processRefereeDecisions()` handles down signal + outcome)
- Clear notification to operators about unusual situation

**Cons:**
- Mirrors some setup logic from `transitionToTimeRunning()` (necessary to ensure proper flow)

---

### Option B: Micro Clock Start (Actually Starts Timer)

**Concept:** Actually start the clock briefly using the existing `transitionToTimeRunning()` method, then immediately stop it. Re-post the decision event to be processed in `TIME_STOPPED` state.

**Implementation Steps:**

1. **Modify `CURRENT_ATHLETE_DISPLAYED` case:**
   ```java
   } else if (e instanceof DecisionFullUpdate || e instanceof DecisionUpdate) {
       // Micro-start: simulate clock start and immediate stop
       doMicroClockStartForDecision(e);
   }
   ```

2. **Create `doMicroClockStartForDecision()` method:**
   ```java
   private void doMicroClockStartForDecision(FOPEvent originalEvent) {
       // Show error notification
       pushOutUIEvent(new UIEvent.Notification(
           getCurAthlete(), originalEvent.getOrigin(),
           UIEvent.Notification.Level.ERROR,
           "Decision.ClockNotStarted",
           5000, this));
       
       // Perform full clock start transition (sets clock owner, prepares down signal, etc.)
       transitionToTimeRunning();
       
       // Immediately stop the clock
       setState(TIME_STOPPED);
       getAthleteTimer().stop();
       
       // Now re-post the decision event to be processed in TIME_STOPPED state
       fopEventPost(originalEvent);
   }
   ```

**Files Affected:**
- `FieldOfPlay.java` - State machine modification
- Translation files - New notification string

**Pros:**
- Fully reuses existing `transitionToTimeRunning()` logic - no code duplication
- Guarantees all preconditions are set correctly (clock owner, weight at start, etc.)
- Decision processing happens in correct state (TIME_STOPPED)

**Cons:**
- Timer briefly starts (may flash on displays)
- Need to ensure no unwanted side effects from timer start/stop
- Event re-posting adds complexity

---

## Comparison of Options

| Aspect | Option A (Direct State Setup) | Option B (Micro Clock Start) |
|--------|-------------------------------|------------------------------|
| Timer actually starts | **No** | Yes (briefly) |
| Uses existing transition method | No (direct setup) | Yes (`transitionToTimeRunning()`) |
| Display artifact risk | **None** | Timer may flash briefly |
| Decision processing | Inline | Re-posted to state machine |
| Code duplication | Some | **None** |
| Complexity | Lower | Higher |

---

## Recommended Approach: Option A (Direct State Setup)

**Rationale:**
1. **No display artifacts** - Timer never starts, no risk of flashing on scoreboards
2. **Simpler implementation** - No event re-posting needed
3. **Clear intent** - Code explicitly handles this edge case
4. **Reuses existing patterns** - Similar to how decisions are handled in `TIME_STOPPED` state
5. **Lower risk** - No timer manipulation side effects to worry about

---

## Notification Mechanism

The existing `UIEvent.Notification` system (lines 881-884, 699-702) should be used. This notification appears on:
- Announcer screen
- Timekeeper screen  

The notification class supports:
- `Level.ERROR` (red, middle position) ← Recommended for this case
- `Level.WARNING` (yellow, top position)
- `Level.INFO` (blue, bottom position)
- `Level.SUCCESS` (green, bottom position)

Example of existing similar notification (line 881-885):
```java
pushOutUIEvent(new UIEvent.Notification(null, e.getOrigin(),
    UIEvent.Notification.Level.ERROR,
    "JuryDecision.MustAnnounceFirst",
    3000, this));
```

---

## New Translation Key Required

**Key:** `Decision.ClockNotStarted`

**English:** `Decision received but clock was not started – accepting decision anyway`

**Context:** Warning notification shown to announcer/timekeeper when referees give a decision before the clock was started

---

## Testing Considerations

1. **Single referee mode** - Verify `doPossiblySoloRefereeUpdate()` works correctly
2. **Three referee mode** - Verify `updateRefereeDecisions()` and `processRefereeDecisions()` work correctly
3. **Verify state transitions** - Should end up in `DOWN_SIGNAL_VISIBLE` or `DECISION_VISIBLE`
4. **Verify notification appears** on announcer and timekeeper screens
5. **Verify subsequent lift works** - Lifting order updates correctly, next athlete displayed

---

## Risk Assessment

| Risk | Severity | Mitigation |
|------|----------|------------|
| Decision recorded for wrong athlete | Medium | Verify `curAthlete` is correct before processing |
| Timer state corruption | Low | Setting `TIME_STOPPED` state explicitly |
| Missing weight-at-start validation | Low | Call `setWeightAtLastStart()` before processing |
| Down signal not properly prepared | Low | Call `prepareDownSignal()` before processing |
| Down signal not emitted | **High** | Must call `resetEmittedFlags()` to set `downEmitted=false` |

---

## Implementation Status

**Status: ✅ IMPLEMENTED**

---

## Supported Decision Sources

The implementation handles decisions without clock for four distinct scenarios:

| Source | Event Type | Handler Method | Description |
|--------|-----------|----------------|-------------|
| **3-Referee Devices** | `DecisionFullUpdate` | `updateRefereeDecisions()` | Bulk update from decision board/keypad |
| **MQTT 3-Referee Mode** (default) | `DecisionUpdate` | `updateRefereeDecision()` | Normal mode: each referee presses button, waits for all 3 |
| **Solo Referee Mode** | `DecisionUpdate` | `simulateDecision()` | Configurable mode: first referee to decide acts as all 3 |
| **Announcer Forced Decision** | `ExplicitDecision` | `simulateDecision()` | Announcer UI forced good/no-lift |

**Note:** Solo Referee Mode is a configuration setting. When enabled, the first `DecisionUpdate` received (from any referee 1, 2, or 3) triggers `simulateDecision()` to set all 3 referee votes to that value. The method `doPossiblySoloRefereeUpdate()` checks this setting and routes to the appropriate handler.

---

## State Machine Changes

Two states required modification to handle decisions when clock owner is not set:

### 1. `CURRENT_ATHLETE_DISPLAYED` state — Clock was never started

```java
} else if (e instanceof DecisionFullUpdate) {
    // Accept 3-referee decision even though clock was not started
    setupForDecisionWithoutClock(e);
    updateRefereeDecisions((DecisionFullUpdate) e);
    uiShowUpdateOnJuryScreen(e);
} else if (e instanceof DecisionUpdate) {
    // Accept MQTT referee decision even though clock was not started
    // This also handles "solo referee" mode (MQTT referee 0 = full decision)
    setupForDecisionWithoutClock(e);
    doPossiblySoloRefereeUpdate(e);
} else if (e instanceof ExplicitDecision) {
    // Accept announcer forced decision even though clock was not started
    // Same flow as MQTT solo referee
    setupForDecisionWithoutClock(e);
    simulateDecision((ExplicitDecision) e);
}
```

### 2. `TIME_STOPPED` state — Session reload cleared clock owner

```java
} else if (e instanceof DecisionFullUpdate) {
    if (getClockOwner() == null) {
        setupForDecisionWithoutClock(e);
    }
    updateRefereeDecisions((DecisionFullUpdate) e);
    uiShowUpdateOnJuryScreen(e);
} else if (e instanceof DecisionUpdate) {
    if (getClockOwner() == null) {
        setupForDecisionWithoutClock(e);
    }
    doPossiblySoloRefereeUpdate(e);
}
```

---

## Decision Flow Diagrams

### 3-Referee Mode (DecisionFullUpdate)

```
CURRENT_ATHLETE_DISPLAYED
    │
    ▼ DecisionFullUpdate received
setupForDecisionWithoutClock()
    │ ├─ resetDecisions()           // Clear stale decisions from previous lift
    │ ├─ setClockOwner(curAthlete)
    │ ├─ prepareDownSignal()
    │ ├─ setState(TIME_STOPPED)
    │ └─ decisionReceivedWithoutClock = true
    ▼
updateRefereeDecisions()
    │ ├─ Store individual referee votes
    │ └─ processRefereeDecisions() when all 3 decided
    │        ├─ notifyDecisionWithoutClock()  // ERROR notification
    │        ├─ emitDown()
    │        └─ setState(DOWN_SIGNAL_VISIBLE)
    ▼
Normal decision flow continues
```

### MQTT Individual Referee Mode (DecisionUpdate, 3-referee)

```
CURRENT_ATHLETE_DISPLAYED
    │
    ▼ DecisionUpdate received (ref 1, 2, or 3)
setupForDecisionWithoutClock()
    │ (same as above - but only full setup on first decision)
    ▼
doPossiblySoloRefereeUpdate()
    │ ├─ Check: is Solo Referee Mode enabled?
    │ │      └─ NO → call updateRefereeDecision() for one referee
    │ │               └─ waits for remaining referees
    │ ▼
    │ (When 3rd referee decides)
    └─ processRefereeDecisions()
           ├─ notifyDecisionWithoutClock()  // ERROR notification
           ├─ emitDown()
           └─ setState(DOWN_SIGNAL_VISIBLE)
```

### Solo Referee Mode (DecisionUpdate, first decides for all)

```
CURRENT_ATHLETE_DISPLAYED
    │
    ▼ DecisionUpdate received (any referee 1, 2, or 3 - first one wins)
setupForDecisionWithoutClock()
    │ (same as above)
    ▼
doPossiblySoloRefereeUpdate()
    │ ├─ Check: is Solo Referee Mode enabled?
    │ │      └─ YES → call simulateDecision() with first referee's vote
    │ ▼
simulateDecision()
    │ ├─ notifyDecisionWithoutClock()  // ERROR notification (immediate)
    │ ├─ resetDecisions()
    │ ├─ Set all 3 referee votes to first referee's value
    │ └─ processRefereeDecisions()
    │        ├─ emitDown()
    │        └─ setState(DOWN_SIGNAL_VISIBLE)
    ▼
Normal decision flow continues
```

### Announcer Forced Decision (ExplicitDecision)

```
CURRENT_ATHLETE_DISPLAYED
    │
    ▼ ExplicitDecision received (from Announcer UI)
setupForDecisionWithoutClock()
    │ (same as above)
    ▼
simulateDecision()
    │ ├─ notifyDecisionWithoutClock()  // ERROR notification (immediate)
    │ ├─ resetDecisions()
    │ ├─ Set all 3 referee votes to forced value
    │ └─ processRefereeDecisions()
    │        ├─ emitDown()
    │        └─ setState(DOWN_SIGNAL_VISIBLE)
    ▼
Normal decision flow continues
```

---

## Key Implementation Details

### `setupForDecisionWithoutClock()` Method

| Element | Purpose |
|---------|---------|
| `resetDecisions()` | **Critical:** Clears stale decisions from previous lift (prevents single button triggering full decision) |
| Conditional check | Only does full setup if `clockOwner == null` OR `clockOwner != curAthlete` (handles weight change scenarios) |
| `setClockOwner(getCurAthlete())` | Sets the athlete the decision will be applied to |
| `resetEmittedFlags()` | Sets `downEmitted=false` so down signal can be emitted |
| `prepareDownSignal()` | Prepares audio tone for down signal |
| `setWeightAtLastStart()` | Records weight for record validation |
| `setGoodLift(null)` | Clears any previous lift result |
| `setState(TIME_STOPPED)` | Puts FOP in correct state for decision processing |
| `setClockStoppedDecisionsAllowed(true)` | Enables decision processing in TIME_STOPPED state |
| `decisionReceivedWithoutClock = true` | Tracks that decision came without clock |

### `notifyDecisionWithoutClock()` Helper Method

Consolidated notification code used by both `processRefereeDecisions()` and `simulateDecision()`:

```java
private void notifyDecisionWithoutClock(Object origin) {
    if (decisionReceivedWithoutClock) {
        pushOutUIEvent(new UIEvent.Notification(
            getCurAthlete(), origin,
            UIEvent.Notification.Level.ERROR,
            "Decision.ClockNotStarted",
            5000, this));
    }
}
```

### Notification Timing

| Decision Source | When Notification Appears |
|----------------|---------------------------|
| 3-Referee Mode | When 3rd referee decides (in `processRefereeDecisions()`) |
| MQTT Individual | When 3rd referee decides (in `processRefereeDecisions()`) |
| MQTT Solo Referee | Immediately (in `simulateDecision()` before down signal) |
| Announcer Forced | Immediately (in `simulateDecision()` before down signal) |

### Flag Management

| Flag | Purpose | Set When | Reset When |
|------|---------|----------|------------|
| `decisionReceivedWithoutClock` | Tracks that decision came without clock for notification | In `setupForDecisionWithoutClock()` | In `resetEmittedFlags()` (called for each new lift) |

---

## Stale Decisions Bug Fix

**Problem:** Decisions from previous lifts could remain in the referee decision array. When a new decision arrived, the combination of old + new decisions could immediately trigger a full decision.

**Solution:** `setupForDecisionWithoutClock()` now **always** calls `resetDecisions()` first, before checking if full setup is needed.

```java
private void setupForDecisionWithoutClock(FOPEvent e) {
    // ALWAYS reset stale decisions from previous lift
    // This prevents single button press from triggering full decision
    resetDecisions();
    
    // Only do full setup if clock owner doesn't match current athlete
    if (getClockOwner() == null || !getClockOwner().equals(getCurAthlete())) {
        // ... full setup code ...
    }
}
```

