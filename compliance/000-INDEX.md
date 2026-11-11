# Compliance Work

Updated 2026-11-11. Active specs contain decisions and remaining work;
completed designs belong in [done/](done/). Do not duplicate their requirements
in this index.

## 0xx — Bugs, do first

| Order | Document | Scope |
| --- | --- | --- |
| 010 | [Jury display holds](010-JURY_DISPLAY_HOLDS.md) | The only specification of how long the attempt boards and the scoreboard decision block stay on a decision, a vote or a jury outcome, for every jury. Code done (set A); Playwright scenarios in progress (set B of the [implementation plan](010-IMPLEMENTATION_PLAN.md)) |

Lenient birth-date parsing is done: [BIRTHDATE_LENIENT_PARSE.md](done/BIRTHDATE_LENIENT_PARSE.md).

## 1xx — Technical RFP compliance

| Order | Document | Scope |
| --- | --- | --- |
| 190 | [Video replays tokens](190-VIDEO_REPLAYS_TOKEN.md) | Phase 1: named, revocable viewer tokens managed in the Video app; sign-in page and token URLs; Tracker clients |

PEM-based account mode (100), account tokens (110), token-protected URLs (120) and
export credential protection (140) are done:
[100-PEM_ACCOUNT_MODE.md](done/100-PEM_ACCOUNT_MODE.md),
[110-ACCOUNT_TOKENS.md](done/110-ACCOUNT_TOKENS.md),
[120-TOKEN_PROTECTED_URLS.md](done/120-TOKEN_PROTECTED_URLS.md),
[140-EXPORT_CREDENTIAL_PROTECTION.md](done/140-EXPORT_CREDENTIAL_PROTECTION.md). The configuration
stale-save check (136), the other access work (130) and direct JSON boot (160) are done too:
[136-CONFIG_COARSE_CONFLICT.md](done/136-CONFIG_COARSE_CONFLICT.md),
[130-ACCESS_CONTROL_REMAINING.md](done/130-ACCESS_CONTROL_REMAINING.md),
[160-DIRECT_JSON_BOOT.md](done/160-DIRECT_JSON_BOOT.md).
Signed exports (145) are done: [145-SIGNED_EXPORTS.md](done/145-SIGNED_EXPORTS.md), with its
[implementation plan](done/145-IMPLEMENTATION_PLAN.md).
The browser check that a decision closes an athlete card, together with its pending
confirmation, passed for announcer and referee decisions (180):
[180-CARD_DIALOG_BROWSER_CHECK.md](done/180-CARD_DIALOG_BROWSER_CHECK.md).
The Competition Supervisor page and the passive speaker passed browser acceptance in
accounts and Simple Access modes, and the supervisor's configuration access is decided
(170): [170-COMPETITION_DIRECTOR_ACCEPTANCE.md](done/170-COMPETITION_DIRECTOR_ACCEPTANCE.md).
Checkpoint collection uses existing WebSocket authentication, not personal URL
tokens. Scheduled checkpoints (150), including OWLCMS collection, the tracker plugin and
control panel launch from a checkpoint, are implemented and archived in
[150-SCHEDULED_CHECKPOINTS.md](done/150-SCHEDULED_CHECKPOINTS.md).

## 2xx — Can wait

| Order | Document | Scope |
| --- | --- | --- |
| 220 | [Store panel decisions](220-STORE_PANEL_DECISIONS.md) | Persist detailed review votes and circumstances for later reports |
| 230 | [Video access, deferred](230-VIDEO_ACCESS_DEFERRED.md) | Cameras configuration token; several Video instances on one machine; Caddy external-access acceptance |

Age-group editing (200) is done: [200-AGEGROUP_EDIT_CONFIRMATION.md](done/200-AGEGROUP_EDIT_CONFIRMATION.md),
with its [implementation plan](done/200-IMPLEMENTATION_PLAN.md) and
[acceptance story](done/200-SCENARIO_OLD_AND_VERY_OLD.md).
Scoped dark dialogs (210) are done: [210-SCOPED_DARK_DIALOGS.md](done/210-SCOPED_DARK_DIALOGS.md).

## 3xx — Nice to have

| Order | Document | Scope |
| --- | --- | --- |
| 300 | [Medal eligibility](300-MEDAL_ELIGIBILITY.md) | Per-championship qualification, entry-total and team-point policies for mixed-rule competitions |
| 310 | [Personal best records](310-PERSONAL_BEST_RECORDS.md) | Move live PB tracking into the record-event system |
| 320 | [Windows signing](320-WINDOWS_SIGNING.md) | Authenticode signing for control-panel and video executables |
| 340 | [Cloud control panel token](340-CLOUD_CONTROL_PANEL_TOKEN.md) | Cloud app: generate the control panel token as a Fly secret, recover it from a marker secret name, copy icon |
| 350 | [Tracker rank and score columns](350-TRACKER_SCOREBOARD_RANK_COLUMNS.md) | Forward the lift-rank decision from OWLCMS; tracker Score column and medal rank, matching the OWLCMS scoreboard per championship |
| 370 | [Document template settings](370-DOCUMENT_TEMPLATE_SETTINGS.md) | Store per-document template choices apart from `Competition`, so printing never counts as a change to the competition settings |

Seven-person adjudication, including Tracker jury forwarding, is complete and
archived in [SEVEN_PERSON_ADJUDICATION.md](done/SEVEN_PERSON_ADJUDICATION.md)
and [TRACKER_JURY_VOTES.md](done/TRACKER_JURY_VOTES.md).

The Tracker reverse proxy (360) is complete and archived in
[360-TRACKER_PROXY.md](done/360-TRACKER_PROXY.md).
