# initialDecision Message Summary

## What is new

A new decision message type is emitted and forwarded:

- `decisionEventType=initialDecision`

This message is sent when the 3-second decision delay is started (before the final decision reveal message).

It is forwarded through both:

- HTTP EventForwarder (publicresults / videodata decision endpoints)
- WebSocketEventForwarder

## URL path used

For HTTP forwarding, the decision message is posted to:

- Publicresults: `<PublicResultsURL>/decision`
- Videodata: `<VideoDataURL>/decision`

These are built from config values in Connections (base URL fields) with the `/decision` suffix appended.

For WebSocket URLs (`ws://` or `wss://`), no `/decision` suffix is appended; the same WebSocket URL is used and message type is carried in payload.

## When to use this message in video logic

Use this message when:

- You need an **early decision state** as soon as referee majority is reached.
- You want to trigger a visual state before the final `FULL_DECISION` display event.

## Important fields for good/bad lift trigger

Primary field:

- `decision`
  - `true` = good lift
  - `false` = bad lift

Supporting referee fields:

- `d1` = referee 1 decision (`true`/`false`)
- `d2` = referee 2 decision (`true`/`false`)
- `d3` = referee 3 decision (`true`/`false`)

Recommended trigger logic for video:

1. Check `decisionEventType == "initialDecision"`
2. Read `decision` for final good/bad state at this stage
3. Optionally display referee lamps from `d1/d2/d3`

## Other commonly useful context fields in the same payload

- `fullName`
- `attemptNumber`
- `liftTypeKey`
- `fop`
- `fopState`
- `break`
- `mode`
- `decisionsVisible` (expected `false` for initialDecision)
- `down` (expected `false` for initialDecision)

## Notes

- Null-valued fields are omitted from HTTP form payloads.
- Existing decision flow remains: down signal / full decision / reset still occur as before.
- For strict consumers, ensure `initialDecision` is accepted as a valid decision event type.
