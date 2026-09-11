# Personal Best Records: Tracker Acceptance

Status: tracker and plugin implementation complete.

Companion: [Personal Best Records implementation plan](personal-best-records.md).

No tracker or tracker-core migration remains. Validate these criteria after OWLCMS
implements Personal `RecordEvent` tracking:

- Tracker-core retains one complete semantic `records` collection, including
  Personal-family rows.
- Books exclude semantic rows whose `recordFederation` is exactly `Personal` or a
  valid `Personal:<source>`, without removing them from hub state.
- Standard scoreboards render `recordClass === "recordBoxPersonal"` separately
  from ordinary record blocks.
- Ordinary-only, ordinary-plus-PB, and PB-only scoreboard payloads render correctly.
- A later UPDATE refreshes PB values; an absent block, athlete change, or break
  clears stale PB values.
- PB titles and optional highlights supplied by OWLCMS render without tracker-side
  PB computation.
- Desktop and mobile scoreboard layouts remain readable without overlap.
- OBS selects the new-record scene only from producer-computed
  `recordKind === "new"`; a PB-only improvement does not produce that value.

OWLCMS remains authoritative for PB identity, persistence, improvement, reversal,
display-block generation, and aggregate decision classification.