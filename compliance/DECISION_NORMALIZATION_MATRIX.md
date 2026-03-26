# Decision Normalization Matrix

Date: 2026-03-19

## Purpose

This specification defines the normalization strategy for incoming referee and announcer decision inputs.

The goal is to avoid multiple special paths producing equivalent behavior in different ways.

The cleanup target is:

- normalize all incoming decision inputs into one canonical internal decision shape
- keep display semantics separate from input semantics
- keep timing semantics separate from both input and display semantics

This specification is about internal normalization, not the final HTTP or WebSocket payload contract.

## Implementation Targets

This work has two separate targets.

### Target 1: Code Cleanup And Normalization

Internally, the code must normalize all incoming decision forms into one canonical model.

The normalization layer must:

- remove duplicated decision paths
- derive single-referee semantics from normalized state rather than ad hoc flags
- keep raw referee evidence separate from semantic decision state
- keep output compatibility behavior as a serialization concern, not as the internal source of truth

### Target 2: Richer `INITIAL_DECISION` Output

For consumers that inspect `INITIAL_DECISION`, the output must carry enough information to explain how the decision must be handled.

Announcer entry never emits `INITIAL_DECISION`. Only referee-originated decisions (`SOLO_INPUT`, `THREE_REFEREE_INPUT`) emit it.

Default behavior:

- `SOLO_INPUT` and `THREE_REFEREE_INPUT` always emit `INITIAL_DECISION` followed by `FULL_DECISION`
- `ANNOUNCER_ENTRY` emits only `FULL_DECISION`

When `showDecisionsImmediately` is enabled:

- `INITIAL_DECISION` is still emitted for referee-originated decisions (to trigger videos and downstream consumers)
- `INITIAL_DECISION` carries `timingPolicy = IMMEDIATE` so the receiver knows `FULL_DECISION` will follow without delay
- `ANNOUNCER_ENTRY` still emits only `FULL_DECISION`

The receiver-side decision point happens when `INITIAL_DECISION` arrives.

At that moment, the receiver must inspect `timingPolicy` to decide whether to:

- present the decision immediately as final-looking output
- or switch to countdown / reversal-window content while waiting for `FULL_DECISION`

The primary additional field is:

- `timingPolicy`

The secondary additional field is:

- `inputKind`

Rationale:

- `timingPolicy` tells downstream consumers whether `FULL_DECISION` should be expected immediately or only after the delay/reversal window
- `inputKind` explains where the semantic decision came from, which is useful for diagnostics, UI behavior, and future consumers

Consumer-facing meaning of `timingPolicy`:

- `IMMEDIATE` means the target should not wait for countdown or reversal-window presentation before treating the decision as final-looking output
- `DELAYED` means the target should understand that `INITIAL_DECISION` is available now but `FULL_DECISION` is intentionally deferred

In practice, `DELAYED` allows a target to do either of the following while waiting for `FULL_DECISION`:

- display the initial decision immediately
- show an intermediate countdown or reversal-window video/animation before the final decision event arrives

The key point is that this choice is made when handling `INITIAL_DECISION`, not inferred later from the arrival time of `FULL_DECISION`.

Compatibility constraint:

- older consumers such as publicresults must continue to work from the legacy `d1/d2/d3` output contract without requiring knowledge of `timingPolicy` or `inputKind`

## Core Concepts

Four different concepts must remain separate.

### Operating Mode

How referee-originated inputs must be interpreted for this FOP.

- `SOLO_REFEREE_MODE`
- `THREE_REFEREE_MODE`

Operating mode is configured independently from who entered the decision.

In particular:

- announcer entry can occur while the FOP is in `SOLO_REFEREE_MODE`
- announcer entry can occur while the FOP is in `THREE_REFEREE_MODE`
- `SOLO_REFEREE_MODE` only changes how referee-originated inputs are interpreted

### Input Kind

How the decision entered the system.

- `ANNOUNCER_ENTRY`
- `SOLO_INPUT`
- `THREE_REFEREE_INPUT`

`ANNOUNCER_ENTRY` is a semantic category, not a UI-only transport category.

It includes:

- explicit decision entry from the announcer user interface
- explicit decision entry from an announcer-operated MQTT device using referee number `0`

`SOLO_INPUT` means referee-originated input interpreted while the FOP is operating in `SOLO_REFEREE_MODE`.

`THREE_REFEREE_INPUT` means referee-originated input interpreted while the FOP is operating in `THREE_REFEREE_MODE`.

### Display Form

How the decision should be rendered.

- `singleRefereeLight`
- `threeRefereeLights`

### Timing Policy

Whether the decision goes through the reversal window before becoming official.

- `IMMEDIATE` — no reversal window; `FULL_DECISION` follows without delay
- `DELAYED` — reversal window applies; `FULL_DECISION` is deferred by `REVERSAL_DELAY` (currently 3000ms)

#### Current FieldOfPlay.java Behavior

Today the code does not have a `timingPolicy` field. Instead, timing is controlled by two hard-wired mechanisms:

1. **`refereeForcedDecision` bypass**: Both announcer entry and solo referee input go through `simulateDecision()`, which sets `refereeForcedDecision = true`. In `processRefereeDecisions()`, the `isRefereeForcedDecision()` check calls `showDecisionNow()` directly — no `INITIAL_DECISION` is emitted, no reversal delay applies.

2. **`REVERSAL_DELAY` constant**: Three-referee decisions that reach majority go through `processDecisionDelay()`, which calls `emitInitialDecisionEvent()` then schedules `showDecisionNow()` after `REVERSAL_DELAY` (3000ms). During this window, referees can change their vote.

Current behavior summary:

| Input path | Code mechanism | INITIAL_DECISION emitted | Reversal delay |
|---|---|---|---|
| Announcer entry | `simulateDecision` → `refereeForcedDecision=true` → `showDecisionNow` | No | None |
| Solo referee | `doPossiblySoloRefereeUpdate` → `simulateDecision` → `refereeForcedDecision=true` → `showDecisionNow` | No | None |
| Three referees (MQTT individual) | `processDecisionDelay` → `emitInitialDecisionEvent` + `showDecisionAfterDelay` | Yes | 3000ms |
| Three referees (DecisionFullUpdate, immediate=false) | same as above | Yes | 3000ms |

#### Normalization Target

The normalized model replaces the `refereeForcedDecision` bypass with explicit `timingPolicy` derived from two concerns:

1. **Announcer entry is always `IMMEDIATE`**: announcer decisions have no reversal window and never emit `INITIAL_DECISION`.

2. **`showDecisionsImmediately` feature toggle** (default `false`, changeable live): when enabled, overrides referee-originated decisions to `IMMEDIATE`. This is a live toggle — it can be changed at any time during competition and takes effect on the next decision. This applies to both solo and three-referee input. Even when `IMMEDIATE`, referee-originated decisions still emit `INITIAL_DECISION` (to trigger videos and downstream consumers).

Resulting `timingPolicy` resolution:

| inputKind | `showDecisionsImmediately = false` | `showDecisionsImmediately = true` |
|---|---|---|
| `ANNOUNCER_ENTRY` | `IMMEDIATE` | `IMMEDIATE` |
| `SOLO_INPUT` | `DELAYED` | `IMMEDIATE` |
| `THREE_REFEREE_INPUT` | `DELAYED` | `IMMEDIATE` |

Behavioral changes from current code:

- **Solo input gets reversal delay** (default): today solo has no delay because it shares the `refereeForcedDecision` bypass with announcer entry. After normalization, solo goes through `processDecisionDelay()` and gets the 3-second reversal window.
- **`showDecisionsImmediately` restores the old solo behavior globally**: when enabled, all referee-originated decisions skip the reversal delay, but `INITIAL_DECISION` is still emitted with `timingPolicy = IMMEDIATE`.

#### Required FieldOfPlay.java Evolution

To implement the normalization target, `FieldOfPlay.java` must change:

1. **Remove the solo→simulateDecision collapse**: `doPossiblySoloRefereeUpdate()` must stop routing solo referee input through `simulateDecision()`. Instead, solo input must go through the same `processDecisionDelay()` path as three-referee input.

2. **Stop setting `refereeForcedDecision` for solo input**: Only announcer entry should set `refereeForcedDecision = true`. Solo referee input must not use the forced-decision bypass.

3. **Emit `INITIAL_DECISION` for solo input**: Since solo input now goes through `processDecisionDelay()`, `emitInitialDecisionEvent()` will be called, and the 3-second reversal window will apply by default.

4. **Preserve the announcer-entry fast path**: Announcer entry continues to set `refereeForcedDecision = true` and call `showDecisionNow()` directly. Announcer entry never emits `INITIAL_DECISION`.

5. **Add `showDecisionsImmediately` live toggle**: A runtime-changeable feature toggle on the FOP. When enabled, `processDecisionDelay()` still calls `emitInitialDecisionEvent()` but then calls `showDecisionNow()` instead of `showDecisionAfterDelay()`. The toggle takes effect on the next decision without requiring a restart.

6. **Add `timingPolicy` to decision events**: Both `UIEvent.InitialDecision` and `UIEvent.Decision` (or their replacements) must carry `timingPolicy` so downstream consumers can distinguish `IMMEDIATE` from `DELAYED` without inferring it from the code path.

Derived predicate for emission:

- `isInitialDecisionEmitted(inputKind) = (inputKind != ANNOUNCER_ENTRY)`

Meaning: referee-originated decisions (`SOLO_INPUT`, `THREE_REFEREE_INPUT`) always emit `INITIAL_DECISION`; announcer entry never does.

## Canonical Internal Fields

Each incoming decision event must normalize to the following canonical fields before downstream decision logic runs.

- `operatingMode`
- `inputKind`
- `majorityReached`
- `decisionValue`
- `timingPolicy`
- `rawRef1`
- `rawRef2`
- `rawRef3`

Field meanings:

- `operatingMode`: configured referee operating mode for the FOP. Values: `SOLO_REFEREE_MODE`, `THREE_REFEREE_MODE`
- `inputKind`: normalized source category of the input. Values: `ANNOUNCER_ENTRY`, `SOLO_INPUT`, `THREE_REFEREE_INPUT`
- `majorityReached`: true when this normalized input is sufficient to produce a decision lifecycle
- `decisionValue`: the semantic decision after normalization, regardless of whether it came from explicit entry, solo collapse, or three-referee majority
- `timingPolicy`: immediate or delayed after policy resolution. Values: `IMMEDIATE`, `DELAYED`
- `rawRef1/rawRef2/rawRef3`: preserved raw referee lamp values, but only when actual referee lamp inputs were received; leave unset for announcer-style explicit decisions

Derived predicates:

- `announcerEntry = (inputKind == ANNOUNCER_ENTRY)`
- `soloMode = (operatingMode == SOLO_REFEREE_MODE)`
- `singleRefereeLight = (inputKind != THREE_REFEREE_INPUT)`

Derivation rules:

- `majorityReached` is derived from `inputKind` together with the normalized raw inputs, not from `rawRef1/rawRef2/rawRef3` alone.
- For `ANNOUNCER_ENTRY`, `majorityReached = true` as soon as an explicit good/bad decision is received.
- For `SOLO_INPUT`, `majorityReached = true` as soon as one valid solo decision is identified after normalization.
- For `THREE_REFEREE_INPUT`, `majorityReached = true` only when the preserved raw referee values contain a two-of-three majority for good or bad.

- For `THREE_REFEREE_INPUT`, compute counts from `rawRef1/rawRef2/rawRef3` ignoring unset values:
	- if at least 2 are good, then `majorityReached = true` and `decisionValue = good`
	- if at least 2 are bad, then `majorityReached = true` and `decisionValue = bad`
	- otherwise `majorityReached = false` and `decisionValue` remains unset

- Announcer normalization collapses the input immediately to one explicit decision:
	- set `inputKind = ANNOUNCER_ENTRY`
	- set `decisionValue` to the posted good/bad value
	- set `majorityReached = true`
	- leave `rawRef1/rawRef2/rawRef3` unset because no actual referee lamp inputs exist
	- this rule applies whether the explicit announcer decision came from the announcer UI or from an announcer-operated MQTT device using referee number `0`

- Solo normalization also collapses to one explicit decision, regardless of device-side encoding:
	- set `inputKind = SOLO_INPUT`
	- this only applies when `operatingMode = SOLO_REFEREE_MODE` and the source is referee input, not announcer entry
	- if the device sent one numbered referee input, use that first valid input as `decisionValue`
	- if the device sent synthetic three whites or three reds, derive one solo `decisionValue` from that synthetic unanimity
	- set `majorityReached = true` once that single solo decision is identified
	- preserve the original raw pattern as forensic evidence whenever referee-lamp-style values were actually received
	- keep `rawRef1/rawRef2/rawRef3` unset only when the solo decision arrived with no referee-lamp-style raw inputs at all

## Normalization Matrix

| Incoming form | Detection | inputKind | majorityReached | decisionValue | raw referee values | timingPolicy |
|---|---|---|---|---|---|---|
| Announcer button explicit decision | announcer UI posts explicit good/bad decision | `ANNOUNCER_ENTRY` | `true` | explicit good/bad | unset; no actual referee lamp inputs exist | `IMMEDIATE` (always) |
| Announcer MQTT explicit decision | parsed `DecisionUpdate.refIndex < 0` from announcer MQTT input | `ANNOUNCER_ENTRY` | `true` | explicit good/bad | preserve raw pattern only if the device actually emitted referee-lamp-style values | `IMMEDIATE` (always) |
| Solo mode, first numbered referee input | `operatingMode=SOLO_REFEREE_MODE` and first valid numbered referee input arrives | `SOLO_INPUT` | `true` on first valid input | value of that first valid input | preserve the received raw referee pattern as forensic evidence | `showDecisionsImmediately ? IMMEDIATE : DELAYED` |
| Solo-capable device sends synthetic 3 whites / 3 reds | `operatingMode=SOLO_REFEREE_MODE`, source is referee input, device emits full majority shape | `SOLO_INPUT` | `true` | derived from synthetic majority | preserve the received synthetic raw pattern as forensic evidence | `showDecisionsImmediately ? IMMEDIATE : DELAYED` |
| Three-referee mode, device sends 3 identical referee lights | `operatingMode=THREE_REFEREE_MODE`, raw 3-light pattern such as 3 white or 3 red | `THREE_REFEREE_INPUT` | `true` once majority exists | computed majority | preserve actual individual lamp values | `showDecisionsImmediately ? IMMEDIATE : DELAYED` |
| Normal three-referee updates before majority | `operatingMode=THREE_REFEREE_MODE`, numbered referee updates, no majority yet | `THREE_REFEREE_INPUT` | `false` | none yet | preserve actual individual lamp values | no final timing decision yet |
| Normal three-referee majority reached | `operatingMode=THREE_REFEREE_MODE`, numbered inputs reach majority | `THREE_REFEREE_INPUT` | `true` | computed majority | preserve actual individual lamp values | `showDecisionsImmediately ? IMMEDIATE : DELAYED` |

## Required Normalization Rules

### Rule 1: Operating Mode And Input Source Are Orthogonal

Operating mode and input source must be modeled independently.

Meaning:

- announcer entry does not become `SOLO_INPUT` just because the FOP is in `SOLO_REFEREE_MODE`
- solo referee semantics apply only to referee-originated inputs
- announcer entry can override or replace a missing referee-device decision in any operating mode

This rule is the reason `ANNOUNCER_ENTRY` can coexist with `operatingMode = SOLO_REFEREE_MODE`.

### Rule 2: Solo Referee Semantics Apply Only To Referee Inputs

If the FOP is in solo mode and the source is a referee input, the input must normalize to solo semantics regardless of the specific device-side encoding.

Meaning:

- do not let device-specific solo conventions leak into downstream logic
- do not allow solo devices to create separate effective code paths because they emit synthetic three-light majorities
- the first valid referee decision received is the solo decision

This rule does not override announcer semantics.

If the input is announcer entry, the input normalizes as `ANNOUNCER_ENTRY`, not `SOLO_INPUT`, even when `operatingMode = SOLO_REFEREE_MODE`.

Examples of solo-mode-compatible encodings that must normalize to the same internal solo semantics:

- a single numbered referee input while the FOP is configured for solo mode
- synthetic three-light-equivalent solo device outputs such as three whites or three reds

When the FOP is in `THREE_REFEREE_MODE`, this rule does not apply.

With `operatingMode=THREE_REFEREE_MODE`:

- three-referee interpretation is the default
- announcer semantics are identified only by the announcer UI or by explicit announcer-equivalent single-input encoding such as MQTT `refIndex < 0`
- a raw three-light pattern does not identify announcer intent

### Rule 3: One-Light Display Is Separate From Solo Mode

One-light display is not the same thing as solo mode.

One-light display applies when:

- announcer entry is used
- solo referee input is used

Three-light display applies when:

- three-referee mode is used

In this rule set, `singleRefereeLight` is a separate concept, but it is a derived predicate rather than stored state.

Specifically:

- `singleRefereeLight = true` when `inputKind` is `ANNOUNCER_ENTRY` or `SOLO_INPUT`
- `singleRefereeLight = false` when `inputKind` is `THREE_REFEREE_INPUT`

If display form is ever allowed to differ from input kind, then `singleRefereeLight` must become a stored canonical field.

### Rule 4: Timing Policy Is Separate From Input Form

Immediate versus delayed must not be inferred from input shape alone.

Current code uses `refereeForcedDecision` as an implicit bypass that conflates announcer entry and solo input into the same no-delay path. The normalization target separates them:

- announcer entry is always `IMMEDIATE` — this is not configurable
- referee-originated decisions (`SOLO_INPUT`, `THREE_REFEREE_INPUT`) are `DELAYED` by default
- `showDecisionsImmediately` (default `false`, changeable live) is a global override: when enabled, referee-originated decisions also become `IMMEDIATE`. The toggle can be changed at any time during competition and takes effect on the next decision

Derived `timingPolicy` resolution:

- `inputKind == ANNOUNCER_ENTRY` → `IMMEDIATE` (always)
- `inputKind != ANNOUNCER_ENTRY && showDecisionsImmediately` → `IMMEDIATE`
- `inputKind != ANNOUNCER_ENTRY && !showDecisionsImmediately` → `DELAYED`

After normalization, `refereeForcedDecision` is set only for announcer entry, not for solo input. Solo input goes through `processDecisionDelay()` and gets the reversal window by default.

### Rule 5: `INITIAL_DECISION` Emission Is Separate From Timing Policy

Whether `INITIAL_DECISION` is emitted is not the same question as whether the decision has reversal delay.

The derived predicate `isInitialDecisionEmitted(inputKind)` determines whether `INITIAL_DECISION` is emitted:

- `isInitialDecisionEmitted(inputKind) = (inputKind != ANNOUNCER_ENTRY)`
- for `SOLO_INPUT` and `THREE_REFEREE_INPUT`, `isInitialDecisionEmitted(inputKind)` is always `true`
- for `ANNOUNCER_ENTRY`, `isInitialDecisionEmitted(inputKind)` is always `false`
- announcer entry never emits `INITIAL_DECISION`

When `showDecisionsImmediately` is enabled, referee-originated decisions still emit `INITIAL_DECISION` (to trigger videos and downstream consumers). The `INITIAL_DECISION` carries `timingPolicy = IMMEDIATE` so the receiver knows `FULL_DECISION` will follow without delay.

### Rule 6: INITIAL_DECISION And FULL_DECISION Share Semantics

Once the input is normalized and majority is reached:

1. `INITIAL_DECISION` is emitted when `isInitialDecisionEmitted(inputKind)` is `true`
2. `FULL_DECISION` is emitted immediately or after delay according to `timingPolicy`

Both events carry the same `timingPolicy` derived from normalization (see Rule 4).

Receiver interpretation of `timingPolicy` on `INITIAL_DECISION`:

- `DELAYED` means a reversal window or countdown may be shown before `FULL_DECISION` arrives
- `IMMEDIATE` means `FULL_DECISION` will follow immediately; no reversal-window presentation is expected

For `ANNOUNCER_ENTRY`:

- `isInitialDecisionEmitted(inputKind)` is `false`
- only `FULL_DECISION` is emitted
- `timingPolicy = IMMEDIATE`

For `SOLO_INPUT` and `THREE_REFEREE_INPUT` with `showDecisionsImmediately = false` (default):

- `isInitialDecisionEmitted(inputKind)` is `true`
- `INITIAL_DECISION` is emitted with `timingPolicy = DELAYED`
- `FULL_DECISION` follows after `REVERSAL_DELAY` (3000ms)

For `SOLO_INPUT` and `THREE_REFEREE_INPUT` with `showDecisionsImmediately = true`:

- `isInitialDecisionEmitted(inputKind)` is `true`
- `INITIAL_DECISION` is emitted with `timingPolicy = IMMEDIATE`
- `FULL_DECISION` follows immediately

## Canonical Model Recommendation

The normalized event stores:

- `operatingMode`
- `inputKind`
- `timingPolicy`
- `majorityReached`
- `decisionValue`
- `rawRef1`
- `rawRef2`
- `rawRef3`

The following are exposed as derived predicates rather than stored fields:

- `announcerEntry`
- `soloMode`
- `singleRefereeLight`

The distinction between semantic decision and raw evidence is explicit:

- `decisionValue` answers: what decision does the system currently have?
- `rawRef1/rawRef2/rawRef3` answer: what referee lamp inputs actually arrived, if any?

So announcer and other explicit single-input decisions can have a valid `decisionValue` with all `rawRef*` fields unset.

In solo mode, this distinction is slightly different from announcer entry:

- `decisionValue` still holds the single semantic solo decision
- `rawRef1/rawRef2/rawRef3` should be preserved whenever raw referee-style evidence was actually received, even if that evidence is later collapsed to one solo decision

This prevents contradictory combinations such as:

- `inputKind = SOLO_INPUT` with `operatingMode = THREE_REFEREE_MODE`
- `inputKind = THREE_REFEREE_INPUT` with `operatingMode = SOLO_REFEREE_MODE`
- `inputKind = SOLO_INPUT` with `singleRefereeLight = false`

The following combinations are valid and intentional:

- `inputKind = ANNOUNCER_ENTRY` with `operatingMode = SOLO_REFEREE_MODE`
- `inputKind = ANNOUNCER_ENTRY` with `operatingMode = THREE_REFEREE_MODE`

## Canonical Display Mapping

For `singleRefereeLight=true`:

- `displayDecision = good/bad`
- `displayRef1 = null`
- `displayRef2 = good/bad`
- `displayRef3 = null`

For `singleRefereeLight=false`:

- `displayDecision = computed majority`
- `displayRef1/2/3 = actual referee lamp values`

This preserves the current center-light convention while making the reason explicit.

## Why This Cleanup Is Needed

Today the code mixes together several meanings:

- solo mode
- explicit non-numbered input such as MQTT `0`
- announcer explicit entry
- one-light display semantics
- immediate versus delayed timing

That causes equivalent behavior to enter the decision flow through multiple routes.

The normalization strategy removes that ambiguity by making every later stage consume the same canonical fields.

## Downstream Consequences

After normalization, forwarding uses the following compatibility fields.

Compatibility rule:

- existing HTTP receivers must continue to receive the legacy one-light compatibility fields
- existing WebSocket receivers must continue to receive the legacy one-light compatibility fields
- new unambiguous fields may be added alongside the legacy fields
- new fields do not replace legacy fields in either transport

HTTP can use:

- legacy `singleReferee` as compatibility alias for `singleRefereeLight`
- `announcerEntry`
- `soloMode`
- additive future fields such as `singleRefereeLight`, `inputKind`, and `timingPolicy`

WebSocket can use:

- legacy `singleReferee` as compatibility alias for `singleRefereeLight`
- `announcerEntry`
- `soloMode`
- additive future fields such as `singleRefereeLight`, `inputKind`, and `timingPolicy`

In both transports, timing behavior is visible through event sequencing and explicit timing metadata, not inferred indirectly from how the decision arrived.

These transport flags can be computed from the normalized event at serialization time rather than stored independently.

## Historical UI Receiver Compatibility

Current OWLCMS UI receivers still use the historical one-light versus three-light contract.

That contract is not driven by `inputKind` directly.

Instead, it is driven by the legacy boolean `singleReferee` and by the historical forced-decision path.

Observed current behavior:

- `UIEvent.Decision` carries `singleReferee` rather than `inputKind`
- when `singleReferee=true`, UI receivers show a single center light
- when `singleReferee=false`, UI receivers show the three individual referee lights
- historical scoreboard code also treats `refereeForcedDecision` the same as `singleReferee` for display purposes

Current Java UI compatibility examples:

- `DecisionElement` branches on `e.isSingleReferee()` and calls `showSingleDecision` versus `showDecisions`
- `UIEvent.Decision` rewrites a single-light decision into center-light form by moving any lone `ref1` or `ref3` value into `ref2`
- `NCurrentAthlete` shows one light when `getFop().isSingleReferee() || getFop().isRefereeForcedDecision()`

Implication for normalization:

- normalized `inputKind` and `timingPolicy` are the intended internal source of truth
- legacy UI receivers still require compatibility fields and center-light shaping
- HTTP and WebSocket forwarding must therefore preserve enough compatibility information for receivers that still only understand one-light versus three-light presentation
- any new unambiguous feed fields must be additive rather than a replacement for `singleReferee` and center-light compatibility

## MQTT Explicit Single-Input Semantics

For MQTT messages, referee numbers `1`, `2`, and `3` map to internal referee indexes `0`, `1`, and `2`.

MQTT referee number `0` translates to internal `refIndex = -1`.

Normalization for MQTT referee number `0` does not use the operating mode.

MQTT referee number `0` is announcer-equivalent input.

That means:

- MQTT `0` input normalizes to `ANNOUNCER_ENTRY`
- this is true in `SOLO_REFEREE_MODE`
- this is true in `THREE_REFEREE_MODE`
- the absence of a reversal timeout between `INITIAL_DECISION` and `FULL_DECISION` follows `ANNOUNCER_ENTRY`, not solo mode

When `operatingMode=THREE_REFEREE_MODE`, there are 3 referees, but the announcer may have a device for decision input.

- MQTT `0` input normalizes to `ANNOUNCER_ENTRY`
- the announcer decision is always `IMMEDIATE` and not subject to reversal delay
- a device that only emits raw 3-light patterns cannot be used for announcer entry in 3-referee mode
- in that case, the announcer must use the announcer UI

When `operatingMode=SOLO_REFEREE_MODE`:

- MQTT `0` input still normalizes to `ANNOUNCER_ENTRY`
- the first valid numbered referee input normalizes to `SOLO_INPUT`
- if the solo referee device is unavailable and the announcer enters the flags, that remains announcer entry, not solo input
