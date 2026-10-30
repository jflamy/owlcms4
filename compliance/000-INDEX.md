# Compliance Work

Updated 2026-10-30. Active specs contain decisions and remaining work;
completed designs belong in [done/](done/). Do not duplicate their requirements
in this index.

## 0xx — Bugs, do first

None open. Lenient birth-date parsing is done: [BIRTHDATE_LENIENT_PARSE.md](done/BIRTHDATE_LENIENT_PARSE.md).

## 1xx — Technical RFP compliance

| Order | Document | Scope |
| --- | --- | --- |
| 130 | [Other access work](130-ACCESS_CONTROL_REMAINING.md) | Locality by control panel token, admin pages and admin password, navigation/action gating, infrastructure/security review |
| 145 | [Signed exports](145-SIGNED_EXPORTS.md) | PEM signature inside JSON exports, import outcomes in the audit trail, support procedures with and without the key |
| 150 | [Scheduled checkpoints](150-SCHEDULED_CHECKPOINTS.md) | Tracker timer and faithful ZIP retention over the existing WebSocket |
| 160 | [Direct JSON boot](160-DIRECT_JSON_BOOT.md) | Control-panel checkpoint-folder launch, no-restart rule and Postgres acceptance |
| 170 | [Competition Director acceptance](170-COMPETITION_DIRECTOR_ACCEPTANCE.md) | Remaining accounts/passive-page browser acceptance and configuration permissions |
| 180 | [Card dialog browser check](180-CARD_DIALOG_BROWSER_CHECK.md) | Manual check that a decision closes an athlete card together with its pending confirmation |

PEM-based account mode (100), account tokens (110), token-protected URLs (120) and
export credential protection (140) are done:
[100-PEM_ACCOUNT_MODE.md](done/100-PEM_ACCOUNT_MODE.md),
[110-ACCOUNT_TOKENS.md](done/110-ACCOUNT_TOKENS.md),
[120-TOKEN_PROTECTED_URLS.md](done/120-TOKEN_PROTECTED_URLS.md),
[140-EXPORT_CREDENTIAL_PROTECTION.md](done/140-EXPORT_CREDENTIAL_PROTECTION.md). While implementing 130, remove
requirements already covered by 100–120 rather than duplicating checks.
Checkpoint collection uses existing WebSocket authentication, not personal URL
tokens.

## 2xx — Can wait

| Order | Document | Scope |
| --- | --- | --- |
| 200 | [Age-group editing](200-AGEGROUP_EDIT_CONFIRMATION.md) | Target behavior for age-group edits and athlete reassignment per championship; stops the data loss. Current behavior unchanged since version 65; not blocking for a beta |
| 210 | [Scoped dark dialogs](210-SCOPED_DARK_DIALOGS.md) | Stop display-page dark styling from leaking into ordinary pages |
| 220 | [Store panel decisions](220-STORE_PANEL_DECISIONS.md) | Persist detailed review votes and circumstances for later reports |

## 3xx — Nice to have

| Order | Document | Scope |
| --- | --- | --- |
| 300 | [Medal eligibility](300-MEDAL_ELIGIBILITY.md) | Per-championship qualification, entry-total and team-point policies for mixed-rule competitions |
| 310 | [Personal best records](310-PERSONAL_BEST_RECORDS.md) | Move live PB tracking into the record-event system |
| 320 | [Windows signing](320-WINDOWS_SIGNING.md) | Authenticode signing for control-panel and video executables |
| 330 | [Replays token](330-REPLAYS_TOKEN.md) | Video module: its own access token, generated in the app, required on replay URLs |
| 340 | [Cloud control panel token](340-CLOUD_CONTROL_PANEL_TOKEN.md) | Cloud app: generate the control panel token as a Fly secret, recover it from a marker secret name, copy icon |

Seven-person adjudication, including Tracker jury forwarding, is complete and
archived in [SEVEN_PERSON_ADJUDICATION.md](done/SEVEN_PERSON_ADJUDICATION.md),
[TRACKER_JURY_VOTES.md](done/TRACKER_JURY_VOTES.md) and
[phase-b-referee-lights-mqtt.md](done/phase-b-referee-lights-mqtt.md).
