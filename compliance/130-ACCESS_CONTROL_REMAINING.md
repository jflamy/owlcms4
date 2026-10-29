# Access Control - Other Remaining Work

The role system is implemented. Its historical design is in
[done/ACCESS_CONTROL_DESIGN.md](done/ACCESS_CONTROL_DESIGN.md).
Token generation and URL protection are done: [110](done/110-ACCOUNT_TOKENS.md) and
[120](done/120-TOKEN_PROTECTED_URLS.md).
The items below were identified on 2026-10-22; implementation is still pending.

## Navigation and in-page actions

Gate one-off route openers with `AccessUi.canOpen(targetClass, platform)`:

| Source | Target |
| --- | --- |
| AnnouncerContent, BreakManagement | MedalCeremonyContent |
| CompetitionEditingFormFactory, EditChampionshipsPanel, AgeGroupActionsMenu | AgeGroupContent |
| AgeGroupContent | ChampionshipsContent |
| RecordConfigEditingFormFactory, RecordsConfigContent | RecordContent |
| PlatformContent | TCContent for that platform |
| SessionContent | RegistrationContent |
| PublicRecordsContent | preparation/records |

Check `getPage().open(...)` in AgeGroupActionsMenu, RecordContent and
TechnicalOfficialContent: gate route targets, not ordinary download links.
Download authorization belongs to the target resource.

`AthleteGridContent.breakButtons()` must return null unless the principal has
`BREAK_MANAGEMENT` on the current platform; callers already handle null.
Keep the marshal Pause button and the existing announcer/timekeeper controls.

## Non-route review

| Surface | Remaining review |
| --- | --- |
| `/simulation/*`, `/controlpanel/stop` | Confirm localhost/backdoor maintenance restrictions |
| `/local/*` | Verify directory/classpath confinement, decoding, traversal and symlinks; exclude sensitive files |
| MQTT broker and `/mqtt` | Keep MQTT credentials; remove client password from MoquetteAuthenticator debug logging |
| Vaadin dynamic downloads/uploads | Verify resource creation and use require the appropriate page permission |
| Forwarded client IP | Trust headers only through a configured trusted proxy; never trust a caller-supplied localhost/backdoor claim |
| CorsFilter | Review credentialed cross-origin cookies and `SameSite=None` with named accounts |

IP resolution must use the socket peer unless that peer is explicitly trusted.
For a trusted proxy use its configured header convention, reject malformed
chains and prevent direct backend access around it. A loopback proxy does not
make its external callers localhost. Do not infer trust from a private address.

## Remaining token-protected servlets

Specified in [120](done/120-TOKEN_PROTECTED_URLS.md#plain-servlets) (roles, PIN mode,
responses); not implemented yet. Reuse `TokenAuthenticator.authenticate` and `holds`,
as `CompetitionExport` does.

- `/competition/h2` (GET, HEAD): account token with COMPETITION_SUPERVISOR; PIN mode
  keeps the local-network rule.
- `/simulation/*` (GET, HEAD, POST): account token with PREPARATION; the form carries
  the token on its POSTs; PIN mode keeps the local rule.
- `/controlpanel/stop` (POST): the current audit `runId`, not an account token; see
  [120](done/120-TOKEN_PROTECTED_URLS.md#controlpanelstop).

## Supervisor backups

The backups page (`admin/backup`) adds COMPETITION_SUPERVISOR to its roles, gets a
Backup button on the Competition Supervisor page (`lifting/competition-director`), and
drops the same-machine restriction (`AdminView.checkLocalAccess`), which backups do not
need. Tokens do not reach it: it is not `@TokenAccessible`.

## Acceptance

Verify hidden/inaccessible routes and buttons for PLATFORM(A) and combined
REGISTRATION/WEIGHIN/RESULTS accounts. Hide empty menu groups. Break controls
remain available only to authorized announcer/timekeeper/marshal roles.
Audit behavior follows the existing
[audit specification](done/AUDIT_TRAIL_SPECIFICATION.md).
