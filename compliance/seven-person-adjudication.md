# Seven-Person Adjudication Panel — Remaining Work

Status: feature implemented and tested (verified 2026-10-22). The analysis and
design are archived in
[done/seven-person-adjudication.md](done/seven-person-adjudication.md); the
device timeline in
[done/phase-b-referee-lights-mqtt.md](done/phase-b-referee-lights-mqtt.md).

Implemented: `FeatureSwitch.SEVEN_PERSON_ADJUDICATION`, derived four-member
jury (selector hidden), `AdjudicationPanel` tally, Phase A automatic stop,
Phase B review votes, Call for Votes (`V`, or Summon All / `K`), Chair override
through the Deliberation / Challenge buttons becoming No Lift / Good Lift during
a review, reason dialog, seven-cell panel row, displays and forwarders, MQTT
reset / summon / `hidden` indicators, Jury Console removed, "Jury Chair"
wording, `JuryNotification.ChairEnteredDecision` marker,
`SevenPersonAdjudicationTest`.

What remains is phase 6 of the original plan (documentation, device tests)
and one cleanup.

## 1. User documentation

- [020-Jury.md](../docs/060-Refereeing/020-Jury.md) still describes the Jury
  Console ("The jury is operated from the Jury Console page", "## Jury
  Console"), which was removed. Rewrite for Jury Control: 3/5-member jury as
  before, the `7personAdjudication` switch and the derived four-member jury,
  Phase A (automatic stop on a jury majority against the referees), Phase B
  (video review, Call for Votes, seven votes revealed together), the Chair
  override and the reason dialog, Resume behaviour. No page under `docs/`
  mentions the Adjudication Panel or Call for Votes today.
- [180-Keypads.md](../docs/100-AdvancedTopics/180-Keypads.md): add `V` (Call
  for Votes) and the review-break meaning of `K`.

## 2. MQTT documentation

[200-MQTTMessages.md](../docs/100-AdvancedTopics/200-MQTTMessages.md) lists
`owlcms/fop/decision/A :ref :decision` with `decision` as `good` or `bad` only.
Document `hidden`: sent per referee review vote during a Phase B vote, only
with VPT and `panelVoteDeviceIndicators` on; devices show it as the green
"voted" indicator; the actual `good` / `bad` follows at the last vote. Also
note that `resetDecisions` is published on Call for Votes in both four-member
modes and that `juryDeliberation` / `challenge` are not published when the
Phase A votes are kept for the review.

## 3. MQTT tests

No `MQTTMonitor` test exists. Add one with a stub publisher covering:

- `fop/decision N hidden` only when `panelVoteDeviceIndicators` is on;
- individual `fop/decision N good/bad` at the last vote, not the collective
  `fop/refereesDecision`, and not twice when the public reveal follows;
- nothing published for referees without VPT;
- `resetDecisions` on Call for Votes; `summon` only with VPT;
- `juryDeliberation` / `challenge` suppressed when
  `getPanelReviewInitialVotes() != null`.

## 4. Device tests and release coordination

- Test on firmata with the updated jury workbooks, and on Blue Owl devices with
  the updated blue-owl-core (positions 1–4, green referee / jury LEDs, red /
  white reveal at the last vote). The compatibility table in the archived
  phase-b document lists the expected result per combination.
- Then decide whether `panelVoteDeviceIndicators` can default to on.
- Release notes for owlcms and blue-owl-electron must state that Blue Owl
  devices need the updated blue-owl-core before `panelVoteDeviceIndicators`
  is enabled.

## 5. Unused translation keys

The design's "Waiting for Votes" screen was not built (the Chair override
reuses the Deliberation / Challenge buttons instead). Four keys from that
design are unused in Java but present in `translation4.csv`:
`JuryKeypad.WaitingForVotes`, `JuryKeypad.ChairOverride`, `JuryKeypad.Recall`,
`JuryKeypad.RestartVote`. Remove them in the translation tool and from
[adjudication_translations.tsv](../shared/src/main/resources/i18n/adjudication_translations.tsv)
if that file is still awaiting import.
