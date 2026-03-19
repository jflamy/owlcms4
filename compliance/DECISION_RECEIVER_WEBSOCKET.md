# WebSocket Decision Receiver Contract

Date: 2026-03-19

This document is about the decision outcome that OWLCMS sends once there is a displayable result for the lift.

That result may come from:

- three referees having voted
- a solo referee decision
- the announcer entering the final flags outcome

For each such outcome, receivers should expect two decision events to be sent:

- one when the decision outcome is first available
- one after the reversal delay, even if that delay is empty (if the announcer enters a flag decision, there is no reversal delay afterwards)

## Envelope Structure

WebSocket decision messages are routed by the outer message type.

The receiver first sees:

- `type=decision`

The decision fields are inside `payload`.

Example shape:

```json
{
	"version": "64.0.0",
	"type": "decision",
	"payload": {
		"decisionEventType": "FULL_DECISION",
		"d1": false,
		"d2": true,
		"d3": false
	}
}
```

## How Event Type Works In WebSocket

WebSocket receivers dispatch on outer:

- `type=decision`

Inside the payload, the current implementation carries:

- `decisionEventType=initialDecision` for `INITIAL_DECISION`
- `decisionEventType=FULL_DECISION` for `FULL_DECISION`

In both cases, the payload describes the current decision outcome for the lift, not the individual referee-voting process.

In normal operation, both are sent for the same lift outcome:

- `initialDecision` when the outcome first becomes available
- `FULL_DECISION` after the reversal-delay phase completes

Current tracker-core behavior does not use `payload.decisionEventType` as the main dispatch key.

It uses it only as part of decision display state.

## Relevant Payload Fields

Inside `payload`, the receiver may see:

- `decisionEventType`
- `decision`
- `d1`
- `d2`
- `d3`
- `singleReferee`
- `decisionsVisible`
- `down`
- `timingPolicy`
- `attemptId`
- `lotNumber`
- `attemptSequence`
- `inputKind`
- `fullName`
- `attemptNumber`
- `liftTypeKey`
- `fop`
- `mode`
- `competitionName`

## Current Consumer Behavior

- tracker-core routes on outer `type=decision`
- `FULL_DECISION` makes the decision visible even if `decisionsVisible` is not set
- non-`FULL_DECISION` plus `down=true` is treated as down-only
- there is no special dispatch path for `initialDecision`

## Display Rules For WebSocket Receivers

If the receiver only needs final display state:

- route on outer `type=decision`
- render from `d1`, `d2`, and `d3`
- `FULL_DECISION` is the event that matters most in current consumers

If the receiver wants richer lifecycle behavior:

- inspect `payload.decisionEventType`
- use `timingPolicy` to decide whether to wait after `INITIAL_DECISION`
- use `attemptId` to correlate `INITIAL_DECISION` and `FULL_DECISION`

## WebSocket Example: `INITIAL_DECISION`

```json
{
	"version": "64.0.0",
	"type": "decision",
	"payload": {
		"decisionEventType": "initialDecision",
		"decision": true,
		"d1": null,
		"d2": true,
		"d3": null,
		"timingPolicy": "DELAYED",
		"attemptId": "37-5"
	}
}
```

## WebSocket Example: `FULL_DECISION`

```json
{
	"version": "64.0.0",
	"type": "decision",
	"payload": {
		"decisionEventType": "FULL_DECISION",
		"decision": false,
		"d1": false,
		"d2": true,
		"d3": false,
		"timingPolicy": "DELAYED",
		"attemptId": "12-2"
	}
}
```

## Bottom Line

For WebSocket, there is one outer decision message type: `type=decision`.

If needed, the receiver can further distinguish `INITIAL_DECISION` from `FULL_DECISION` by reading `payload.decisionEventType`.