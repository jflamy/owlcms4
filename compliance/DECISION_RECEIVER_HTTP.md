# HTTP Decision Receiver Contract

Date: 2026-03-19

This document is about the decision outcome that OWLCMS sends once there is a displayable result for the lift.

That result may come from:

- three referees having voted
- a solo referee decision
- the announcer entering the final flags outcome

For each such outcome, receivers should expect two decision events to be sent:

- one when the decision outcome is first available
- one after the reversal delay, even if that delay is empty (if the announcer enters a flag decision, there is no reversal delay afterwards)

## Endpoint Structure

There is one HTTP decision endpoint.

There are not separate HTTP endpoints for `INITIAL_DECISION` and `FULL_DECISION`.

Both events are sent to the same configured decision URL.

The receiver tells them apart using the form field named `decisionEventType`.

OWLCMS will POST the same payload to the video-data decision URL, if configured.

## How The Receiver Differentiates The Two Events

The HTTP receiver must inspect the form field:

- `decisionEventType`

Actual values currently sent:

- `decisionEventType=initialDecision` means `INITIAL_DECISION`
- `decisionEventType=FULL_DECISION` means `FULL_DECISION`

This naming is inconsistent on the wire, but it is what the receiver must expect.

## Exact HTTP Field Names

Decision POSTs use form-style fields.

The receiver may see these field names on decision posts:

- `decisionEventType`
- `updateKey`
- `mode`
- `competitionName`
- `fop`
- `fopState`
- `break`
- `fullName`
- `attemptNumber`
- `liftTypeKey`
- `d1`
- `d2`
- `d3`
- `decision`
- `singleReferee`
- `decisionsVisible`
- `down`
- `recordKind`
- `recordMessage`
- `records`

The revised payload may also include:

- `timingPolicy`
- `attemptId`
- `lotNumber`
- `attemptSequence`
- `inputKind`

## Which Fields Matter For Display

For a receiver that only needs to display decisions, the practical fields are:

- `decisionEventType`
- `d1`
- `d2`
- `d3`
- `decision`
- `decisionsVisible`
- `down`
- `timingPolicy`
- `attemptId`

## What Changes Between `INITIAL_DECISION` And `FULL_DECISION`

The endpoint does not change.

The field names do not change.

What changes is the value of `decisionEventType`:

- `initialDecision`
- `FULL_DECISION`

The receiver should treat them like this:

- `initialDecision`: an initial decision event for the lift
- `FULL_DECISION`: the final decision event for the lift

In both cases, the payload describes the current decision outcome for the lift, not the individual referee-voting process.

In normal operation, both are sent for the same lift outcome:

- `initialDecision` when the outcome first becomes available
- `FULL_DECISION` after the reversal-delay phase completes

## Display Rules For HTTP Receivers

If your receiver only displays referee lights:

- keep rendering from `d1`, `d2`, and `d3`
- you do not need `inputKind`
- you may ignore `INITIAL_DECISION` if you only care about final display state

If your receiver wants to react earlier:

- use `decisionEventType=initialDecision` to detect the initial event
- use `timingPolicy` to decide whether to wait for the final event
- use `attemptId` to match `initialDecision` and `FULL_DECISION` for the same lift

Even when the reversal delay is empty, the lifecycle is still the same conceptually:

- first event: initial outcome available
- second event: final outcome event after the delay phase

Single-light compatibility stays the same:

- `d1 = null`
- `d2 = decision`
- `d3 = null`

## HTTP Example: `INITIAL_DECISION`

```text
decisionEventType=initialDecision
decision=true
d1=
d2=true
d3=
timingPolicy=DELAYED
attemptId=37-5
```

Meaning:

- same HTTP decision endpoint as always
- this is the initial decision event because `decisionEventType=initialDecision`
- a receiver may show center light now or wait, depending on its design

## HTTP Example: `FULL_DECISION`

```text
decisionEventType=FULL_DECISION
decision=false
d1=false
d2=true
d3=false
timingPolicy=DELAYED
attemptId=12-2
```

Meaning:

- same HTTP decision endpoint as always
- this is the final decision event because `decisionEventType=FULL_DECISION`
- render the final light pattern from `d1`, `d2`, and `d3`

## Bottom Line

For HTTP, there is one decision endpoint.

The receiver distinguishes `INITIAL_DECISION` from `FULL_DECISION` by reading the `decisionEventType` field in the POST body.
