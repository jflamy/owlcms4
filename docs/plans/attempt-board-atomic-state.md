# Attempt Board Atomic State — Impact Analysis and Implementation Plan

## 1. Problem

During the 2026-08-30 Guayaquil session (and at least one other occasion), the attempt
board briefly showed an empty weight followed by the `kg` label, while the main
scoreboard showed "waiting for next session".

Log analysis of `owlcms_2026-08-30.log` shows:

- All backend lifting-order updates carried valid requested weights (185 → 186 → 187 → 190).
- At 15:55:31 a `loadGroup(null)` occurred while the FOP was still `TIME_RUNNING`
  (no `switching group to …` line, origin `Unknown Source`), producing
  `group null athletes=0` and `updateDisplay: fop is null or liftType is null` on 5 scoreboards.
- The bare `kg` symptom was also observed at other times, without a null group.

Two distinct defects:

1. **State defect (separate issue):** something can call `loadGroup(null)` on a live FOP.
   Not addressed by this plan; tracked separately.
2. **Rendering defect (this plan):** the attempt board publishes its display state as
   ~20 independent Lit properties from multiple `ui.access` blocks, sometimes nested,
   sometimes with explicit `UI.push()` in the middle of building a state. The browser
   can observe a mixture of two logical states — e.g. `weight=""` while
   `mode=CURRENT_ATHLETE`, which renders a bare `kg` (the unit is a static property set
   once in the constructor and is always rendered when the weight row is visible).

## 2. Root cause of the rendering defect

In `AbstractAttemptBoard` (owlcms/src/main/java/app/owlcms/displays/attemptboard/AbstractAttemptBoard.java):

| Pattern | Instances | Consequence |
|---|---|---|
| Helper methods (`doEmpty`, `doNotEmpty`, `doDone`) open their **own** `ui.access` while already being called from within a `ui.access` block (`slaveOrderUpdated` → `doAthleteUpdate` → `doEmpty`) | 3 | The nested command is **queued**, not executed inline: the properties inside it are applied in a *later* round-trip than the caller's properties |
| `doEmpty` mutates `weight` **before** entering `ui.access` | 1 | `weight=""` can ship with the *previous* state's other properties |
| `spotlightRecords` → `ui.push()` in the middle of `doAthleteUpdate` | 1 | Partial state (names/weight set, mode/records not yet) can be flushed to the browser |
| `slaveOrderUpdated` ends with `getUI().ifPresent(UI::push)` after calling helpers that queued nested accesses | 1 | Push happens before queued property changes are applied |
| Handlers read `fop.getState()` *inside* the queued command | several | State applied may not match the event that triggered it (already partially mitigated; becomes irrelevant with snapshots) |

Fixing the boundaries one by one is fragile. The robust fix is to make the display
state **atomic by construction**: one immutable snapshot per event, published as a
single JSON property, rendered as a whole by Lit.

## 3. Target design

### 3.1 Backend state object

New class `app.owlcms.displays.attemptboard.AttemptBoardState` (immutable, built by a
builder or static factory methods, one per board situation):

```
sequence          long     monotonically increasing per board instance
mode              String   BoardMode name (WAIT, INTRO_COUNTDOWN, LIFT_COUNTDOWN,
                           CURRENT_ATHLETE, INTERRUPTION, SESSION_DONE, CEREMONY,
                           LIFT_COUNTDOWN_CEREMONY)
breakType         String?  only when mode is a break mode
competitionName   String   for WAIT mode
lastName          String
firstName         String
teamName          String
teamFlagImg       String   html fragment or ""
athleteImg        String   url or ""
category          String
startNumber       int      0 when not applicable
attempt           String   formatted attempt text or ""
weight            String   requested weight or "" (never null)
recordAttempt     boolean
recordBroken      boolean
recordMessage     String
recordMessageSpeed int
nameSizeOverride  String   or ""
firstNameSizeOverride String or ""
```

Factory methods mirror today's helpers so every field is **always** assigned
(populate-or-clear, no field survives from the previous state):

- `forCurrentAthlete(fop, athlete, sequence)` — asserts `athlete != null` and
  requested weight > 0; logs an error (with `LoggerUtils.whereFrom()`) and falls back
  to `forWait(...)` if violated. This makes the "impossible state" *visible in the log*
  instead of visible on the platform screen.
- `forWait(fop, sequence)` (WAIT / inactive / null group)
- `forBreak(fop, sequence)` (interruptions, countdowns, ceremonies)
- `forDone(fop, group, sequence)` (SESSION_DONE)

### 3.2 Publication

One method, the only place that touches the element for board state:

```java
private void publish(AttemptBoardState s) {
    // caller is already inside ui.access
    this.getElement().setPropertyJson("boardState", s.toJson());
}
```

Rules:

- Only `@Subscribe` handlers and `onAttach/syncWithFOP` call `ui.access`; exactly one
  access per event, no nesting.
- `doAthleteUpdate`, `doBreak`, `doEmpty`, `doNotEmpty`, `doDone`, `doInactive`,
  `syncWithFOP` become **lock-assuming** builders: they compute and return an
  `AttemptBoardState` (or call `publish`) and never call `ui.access` or `UI.push()`.
- Explicit `UI.push()` calls are removed from state-building code. At most one push at
  the end of the handler (server push is enabled; even that is normally unnecessary).
- Snapshot inputs (athlete, weights, records) are captured from the event or from FOP
  *before* queueing wherever possible, so the applied state matches the triggering event.

### 3.3 Properties that stay OUTSIDE the snapshot

| Property | Reason |
|---|---|
| `kgSymbol`, `STOP`, `t` (translations), `autoversion`, `platformName`, `stylesDir`, `video`, `publicFacing`, `showBarbell`, `athletePictures` | static configuration, set once at attach |
| `decisionVisible` | timing-critical, toggled by DownSignal/Decision/Reset events that must stay ordered with the child `decision-element`; it is a single boolean so it is atomic by itself |
| timers | separate child components (`timer-element`) with their own server channel — unchanged |
| plates | server-side child element in the `barbell` slot; rebuilt inside the same single `ui.access` as the snapshot publication |
| jury notification dialog | independent overlay, unchanged |

### 3.4 Lit component (`AttemptBoard.js`)

- Add property `boardState: { type: Object }`.
- `render()` reads exclusively from `this.boardState` for all fields listed in 3.1
  (`mode`, names, weight, attempt, category, startNumber, flags/pictures, records).
- `decisionVisible` and the static properties are read as today.
- Guard: if `boardState.sequence` is lower than the last rendered sequence, ignore
  (protects against out-of-order application after reconnection).
- The existing `attemptBoardWeightRendered` trace callback is updated to report
  `boardState.sequence` — this gives us end-to-end verification that what the browser
  rendered is exactly one published snapshot.
- Transition period: keep the old individual property bindings working (`this.boardState?.weight ?? this.weight`) so the Java and JS changes do not need to be deployed in
  lock-step during development; remove the fallbacks in the final commit.

## 4. Affected files

| File | Change | Size |
|---|---|---|
| `owlcms/src/main/java/app/owlcms/displays/attemptboard/AttemptBoardState.java` | new | ~200 lines |
| `owlcms/src/main/java/app/owlcms/displays/attemptboard/AbstractAttemptBoard.java` | refactor all handlers/helpers to snapshot+publish | large but mechanical |
| `owlcms/src/main/frontend/components/AttemptBoard.js` | render from `boardState` | moderate |
| `owlcms/src/main/java/app/owlcms/displays/attemptboard/AttemptBoard.java`, `DecisionBoard.java` | none expected (inherit) | — |
| `playwright/src/main/java/playwright/AttemptBoardDisplayMatcher.java`, `AttemptBoardSnapshotReader.java` | read the same DOM test-ids; verify they still pass; extend to assert sequence coherence | small |

Out of scope (unchanged): `DecisionElement`, `TimerElement`s, `PlatesElement` internals,
scoreboards (`BaseResults` etc.), `WebSocketEventForwarder`/`EventForwarder` (they have
their own aggregation), publicresults.

## 5. Risks and mitigations

| Risk | Severity | Mitigation |
|---|---|---|
| Decision/down-signal ordering regression | high | `decisionVisible` deliberately left as-is; no change to those handlers except removing dead pushes |
| Missed call path still setting an individual property | medium | after refactor, grep-gate: no `setProperty("` for snapshot-owned names outside `publish()` |
| Behavior differences in break/ceremony flows (many modes) | medium | factory methods copy today's logic 1:1; Playwright display tests + manual walkthrough of intro/ceremony/lift-countdown/jury/technical/done sequences |
| Stale fields surviving between athletes | medium | snapshot constructor requires every field (builder with mandatory fields); no partial updates possible |
| Out-of-order snapshot application client-side | low | sequence-number guard in Lit |
| Performance (full JSON per event) | low | snapshot is < 1 KB; replaces many individual property diffs |

## 6. Validation plan

1. **Invariant logging:** `forCurrentAthlete` logs an error whenever asked to build a
   state with no athlete or non-positive weight — this catches the *other* bug
   (`loadGroup(null)` on a live FOP) at its point of visual impact.
2. **ATTEMPT_TRACES:** existing feature switch now logs `sequence` on the server at
   publication and on the client at render (`attemptBoardWeightRendered`), giving a
   1:1 audit trail: every rendered frame maps to exactly one published snapshot.
3. **Playwright:** run existing attempt-board display matchers; add a scenario that
   hammers weight changes while the clock runs and asserts the weight cell is never
   empty while the `kg` unit is displayed.
4. **Manual session walkthrough:** intro countdown → ceremony → lift countdown →
   lifting → jury break → resume → group done, on both attempt board and
   athlete-facing decision board.

## 7. Execution order

1. Create `AttemptBoardState` (+ `toJson()`), no callers. Compile.
2. Refactor `AbstractAttemptBoard`: convert helpers to lock-assuming builders; single
   `ui.access` per handler; add `publish()`; keep legacy `setProperty` calls
   temporarily inside `publish()` (write both snapshot and old properties) so the JS
   can be migrated independently. Compile + Playwright.
3. Migrate `AttemptBoard.js` to render from `boardState` with fallbacks.
4. Remove legacy property writes and JS fallbacks. Compile + full validation.
5. Separately: investigate and fix the `loadGroup(null)`-on-live-FOP path (root cause
   of the 15:55:31 incident); the new error log from step 1 will pinpoint the caller
   if it recurs.
