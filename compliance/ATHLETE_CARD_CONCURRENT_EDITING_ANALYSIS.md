# Concurrent Opening of Athlete and Registration Cards — Remaining Work

Status: the save-time protections are implemented (strict dialog modality,
`ConcurrentEditCheck` per-FOP guarded saves, stale-save confirmation on the
athlete card, Close-only refusal on registration/weigh-in, session-in-progress
check, `ConcurrentEditCheckTest`). The full analysis and the implementation
record are archived in
[done/ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md](done/ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md).

Decision 2026-10-22: presence tracking and warning-on-open (§4
`AthleteEditingRegistry`, `CardPresence`, `UIEvent.CardAlsoOpened`, and the
related §7 files and §8 registry tests) **will not be done**. The save-time
check is the retained protection.

Still open:

- A live-browser close-on-decision scenario for the dependent-dialog lifecycle
  has not been exercised (the existing tests do not cover dialog lifecycle).
- `BreakManagement` (content of `BreakDialog`) unregisters from the FOP bus only
  in the "end break" handler; closed any other way, it stays subscribed until
  the page is left. Component-detach unregistration would fix it.
- §6.4 follow-up below.

## 6. Save-time check — follow-up

### 6.4 Follow-up, not in this iteration

**Write only the fields the user changed.** Replace the full `conditionalCopy` and `merge` writes with a write of only the fields this form edited, so that untouched fields keep the other station's value. This would remove most confirmations, but it is a larger change in two complex form factories and must be tested against weigh-in and declaration validation.

Adding `@Version` (optimistic locking) is **not** recommended in this iteration. FOP code also saves the athlete (good/no lift, automatic progressions), so a version check would reject legitimate card saves during lifting and would need retry and merge handling everywhere.

## 9. Remaining decisions

1. Default button of the stale-save confirmation: **Reload card** (decided 2026-10-22; implemented 2026-10-23: "Discard my changes" has the primary style and the focus, placed last).
