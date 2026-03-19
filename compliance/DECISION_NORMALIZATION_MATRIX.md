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

Three different concepts must remain separate.

### Input Kind

How the decision entered the system.

- `ANNOUNCER_ENTRY`
- `SOLO_INPUT`
- `THREE_REFEREE_INPUT`

`ANNOUNCER_ENTRY` is a semantic category, not a UI-only transport category.

It includes:

- explicit decision entry from the announcer user interface
- explicit decision entry from an announcer-operated solo referee device

### Display Form

How the decision should be rendered.

- `singleRefereeLight`
- `threeRefereeLights`

### Timing Policy

Whether the decision should wait through the reversal window or not.

- `IMMEDIATE`
- `DELAYED`

The timing policy is controlled independently per decision class.

Policy flags:

- `announcerEntryIsImmediate` default `true`
- `soloRefereeIsImmediate` default `false`
- `threeRefereeIsImmediate` default `false`

## Canonical Internal Fields

Each incoming decision event must normalize to the following canonical fields before downstream decision logic runs.

- `inputKind`
- `majorityReached`
- `decisionValue`
- `timingPolicy`
- `rawRef1`
- `rawRef2`
- `rawRef3`

Field meanings:

- `inputKind`: normalized source category of the input. Values: `ANNOUNCER_ENTRY`, `SOLO_INPUT`, `THREE_REFEREE_INPUT`
- `majorityReached`: true when this normalized input is sufficient to produce a decision lifecycle
- `decisionValue`: the semantic decision after normalization, regardless of whether it came from explicit entry, solo collapse, or three-referee majority
- `timingPolicy`: immediate or delayed after policy resolution. Values: `IMMEDIATE`, `DELAYED`
- `rawRef1/rawRef2/rawRef3`: preserved raw referee lamp values, but only when actual referee lamp inputs were received; leave unset for announcer-style explicit decisions

Derived predicates:

- `announcerEntry = (inputKind == ANNOUNCER_ENTRY)`
- `soloMode = (inputKind == SOLO_INPUT)`
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
	- this rule applies whether the explicit announcer decision came from the announcer UI or from an announcer-operated solo device

- Solo normalization also collapses to one explicit decision, regardless of device-side encoding:
	- set `inputKind = SOLO_INPUT`
	- if the device sent one numbered referee input, use that first valid input as `decisionValue`
	- if the device sent synthetic three whites or three reds, derive one solo `decisionValue` from that synthetic unanimity
	- set `majorityReached = true` once that single solo decision is identified
	- preserve the original raw pattern as forensic evidence whenever referee-lamp-style values were actually received
	- keep `rawRef1/rawRef2/rawRef3` unset only when the solo decision arrived with no referee-lamp-style raw inputs at all

## Normalization Matrix

| Incoming form | Detection | inputKind | majorityReached | decisionValue | raw referee values | timingPolicy |
|---|---|---|---|---|---|---|
| Announcer button explicit decision | announcer UI posts explicit good/bad decision | `ANNOUNCER_ENTRY` | `true` | explicit good/bad | unset; no actual referee lamp inputs exist | `announcerEntryIsImmediate ? IMMEDIATE : DELAYED` |
| Announcer-operated solo device explicit decision | parsed `DecisionUpdate.refIndex < 0` from announcer-operated solo device | `ANNOUNCER_ENTRY` | `true` | explicit good/bad | preserve raw pattern only if the device actually emitted referee-lamp-style values | `announcerEntryIsImmediate ? IMMEDIATE : DELAYED` |
| Solo mode, one numbered referee input | `soloMode=true` and first valid referee input arrives | `SOLO_INPUT` | `true` on first valid input | value of that first valid input | preserve the received raw referee pattern as forensic evidence | `soloRefereeIsImmediate ? IMMEDIATE : DELAYED` |
| Solo-capable device sends synthetic 3 whites / 3 reds | FOP configured `soloMode=true`, device emits full majority shape | `SOLO_INPUT` | `true` | derived from synthetic majority | preserve the received synthetic raw pattern as forensic evidence | `soloRefereeIsImmediate ? IMMEDIATE : DELAYED` |
| Solo mode off, device sends 3 identical referee lights | `soloMode=false`, raw 3-light pattern such as 3 white or 3 red | `THREE_REFEREE_INPUT` | `true` once majority exists | computed majority | preserve actual individual lamp values | `threeRefereeIsImmediate ? IMMEDIATE : DELAYED` |
| Normal three-referee updates before majority | `soloMode=false`, numbered referee updates, no majority yet | `THREE_REFEREE_INPUT` | `false` | none yet | preserve actual individual lamp values | no final timing decision yet |
| Normal three-referee majority reached | `soloMode=false`, numbered inputs reach majority | `THREE_REFEREE_INPUT` | `true` | computed majority | preserve actual individual lamp values | `threeRefereeIsImmediate ? IMMEDIATE : DELAYED` |

## Required Normalization Rules

### Rule 1: Solo Mode Is Enforced At Ingestion

If the FOP is in solo mode, the input must normalize to solo semantics regardless of the specific device-side encoding.

Meaning:

- do not let device-specific solo conventions leak into downstream logic
- do not allow solo devices to create separate effective code paths because they emit synthetic three-light majorities
- do not allow MQTT explicit single-input encodings such as `0 good` or `0 bad` to bypass solo normalization rules when they are true solo-referee inputs

This rule does not override announcer semantics.

If the source is known to be an announcer-operated solo device, the input normalizes as `ANNOUNCER_ENTRY`, not `SOLO_INPUT`.

Examples of solo-mode-compatible encodings that must normalize to the same internal solo semantics:

- a single numbered referee input while the FOP is configured for solo mode
- MQTT explicit single-input values such as `0 good` or `0 bad`
- synthetic three-light-equivalent solo device outputs such as three whites or three reds

When solo mode is off, this rule does not apply.

With `soloMode=false`:

- three-referee interpretation is the default
- announcer semantics are identified only by the announcer UI or by explicit announcer-equivalent single-input encoding such as MQTT `refIndex < 0`
- a raw three-light pattern does not identify announcer intent

### Rule 2: One-Light Display Is Separate From Solo Mode

One-light display is not the same thing as solo mode.

One-light display applies when:

- announcer entry is used
- solo mode is used

Three-light display applies when:

- three-referee mode is used

In this rule set, `singleRefereeLight` is a separate concept, but it is a derived predicate rather than stored state.

Specifically:

- `singleRefereeLight = true` when `inputKind` is `ANNOUNCER_ENTRY` or `SOLO_INPUT`
- `singleRefereeLight = false` when `inputKind` is `THREE_REFEREE_INPUT`

If display form is ever allowed to differ from input kind, then `singleRefereeLight` must become a stored canonical field.

### Rule 3: Timing Policy Is Separate From Input Form

Immediate versus delayed must not be inferred from input shape alone.

Instead:

- announcer entry timing is controlled by `announcerEntryIsImmediate`
- solo timing is controlled by `soloRefereeIsImmediate`
- three-referee timing is controlled by `threeRefereeIsImmediate`

This keeps rules changes separate from the normalization design.

### Rule 4: INITIAL_DECISION And FULL_DECISION Share Semantics

Once the input is normalized and majority is reached:

1. `INITIAL_DECISION` is emitted
2. `FULL_DECISION` is emitted immediately or after delay according to `timingPolicy`

Both events must carry the same semantic flags derived from normalization.

Receiver interpretation:

- `INITIAL_DECISION` is the point where downstream presentation logic decides what to show next
- `timingPolicy = DELAYED` tells the receiver it may switch to countdown or reversal-window presentation before `FULL_DECISION`
- `timingPolicy = IMMEDIATE` tells the receiver not to expect that intentional waiting phase, FULL_DECISION will come immediately after.

## Canonical Model Recommendation

The normalized event stores:

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

- `inputKind = THREE_REFEREE_INPUT` with `soloMode = true`
- `inputKind = SOLO_INPUT` with `singleRefereeLight = false`

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

HTTP can use:

- legacy `singleReferee` as compatibility alias for `singleRefereeLight`
- `announcerEntry`
- `soloMode`

WebSocket can use:

- `singleRefereeLight`
- `announcerEntry`
- `soloMode`

In both transports, timing behavior is visible through event sequencing and explicit timing metadata, not inferred indirectly from how the decision arrived.

These transport flags can be computed from `inputKind` at serialization time rather than stored independently in the normalized event.

## MQTT Explicit Single-Input Semantics

For MQTT messages, referee numbers `1`, `2`, and `3` map to internal referee indexes `0`, `1`, and `2`.

MQTT referee number `0` translates to internal `refIndex = -1`.

Normalization for MQTT referee number `0` uses the operating mode.

When `soloMode=false`, there are 3 referees, but the announcer may have a device for decision input.

- MQTT `0` input normalizes to `ANNOUNCER_ENTRY`
- the announcer decision is not subject to reversal delay and uses `announcerEntryIsImmediate`
- a device that only emits raw 3-light patterns cannot be used for announcer entry in 3-referee mode
- in that case, the announcer must use the announcer UI

When `soloMode=true`:

- MQTT `0` input normalizes to `SOLO_INPUT`
