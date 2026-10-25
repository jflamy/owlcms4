# Compliance Work

Updated 2026-10-25. Active specs contain decisions and remaining work;
completed designs belong in [done/](done/). Do not duplicate their requirements
in this index.

## Access and recovery

| Order | Document | Scope |
| --- | --- | --- |
| 005 | [PEM-based account mode](005-PEM_ACCOUNT_MODE.md) | First priority: PEM accounts selection with admin PIN override, temporary admin password, self-service Account page, account password administration |
| 010 | [Account tokens](010-ACCOUNT_TOKENS.md) | Generate/manage tokens before URL protection |
| 020 | [Token-protected URLs](020-TOKEN_PROTECTED_URLS.md) | Then use tokens for displays, replays and all URL-based data extraction, including exports |
| 030 | [Other access work](030-ACCESS_CONTROL_REMAINING.md) | Navigation, in-page actions and non-route review |
| 040 | [Export credential protection](040-EXPORT_CREDENTIAL_PROTECTION.md) | Secrets inside saved exports; independent of URL authorization |
| 050 | [Scheduled checkpoints](050-SCHEDULED_CHECKPOINTS.md) | Tracker timer and faithful ZIP retention over the existing WebSocket |
| 060 | [Direct JSON boot](060-DIRECT_JSON_BOOT.md) | Only unfinished launch/standby work and runtime checks |
| 070 | [Competition Director acceptance](070-COMPETITION_DIRECTOR_ACCEPTANCE.md) | Remaining browser acceptance/config permissions |

Implement 005 first, before the remaining JSON boot work in 060. Implement
010 before 020. The numbering otherwise groups subjects; it does not make
independent work depend on token support. Checkpoint collection uses existing
WebSocket authentication, not personal URL tokens.

## Other independent work

| Order | Document |
| --- | --- |
| 100 | [Birth-date parsing](100-BIRTHDATE_LENIENT_PARSE.md) |
| 110 | [Age-group editing](110-AGEGROUP_EDIT_CONFIRMATION.md) |
| 120 | [Medal eligibility](120-MEDAL_ELIGIBILITY.md) |
| 130 | [Personal best records](130-PERSONAL_BEST_RECORDS.md) |
| 140 | [Store panel decisions](140-STORE_PANEL_DECISIONS.md) |
| 150 | [Tracker jury votes](150-TRACKER_JURY_VOTES.md) |
| 160 | [Concurrent athlete editing](160-ATHLETE_CARD_CONCURRENT_EDITING.md) |
| 170 | [Scoped dark dialogs](170-SCOPED_DARK_DIALOGS.md) |
| 180 | [Windows signing](180-WINDOWS_SIGNING.md) |

Seven-person adjudication is complete and
[archived](done/SEVEN_PERSON_ADJUDICATION.md), not an active implementation item.
