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
| `/local/*` | Verify directory/classpath confinement, decoding, traversal and symlinks; exclude sensitive files |
| MQTT broker and `/mqtt` | Done: client password no longer logged; OWLCMS's own broker connection uses a random secret (not the startup time), compared in constant time |
| Vaadin dynamic downloads/uploads | Verify resource creation and use require the appropriate page permission |
| Forwarded client IP | Trust headers only through a configured trusted proxy; never trust a caller-supplied localhost/backdoor claim |
| CorsFilter | Review credentialed cross-origin cookies and `SameSite=None` with named accounts |

IP resolution must use the socket peer unless that peer is explicitly trusted.
For a trusted proxy use its configured header convention, reject malformed
chains and prevent direct backend access around it. A loopback proxy does not
make its external callers localhost. Do not infer trust from a private address.

## Locality: the control panel token

The control panel token is the single proof that a request comes from the OWLCMS
machine. The client address is not one: a local reverse proxy makes remote requests
appear to come from localhost, and forwarding headers can be forged. The backdoor list
never counts as locality.

- The control panel generates a token per run, passes it to OWLCMS at launch
  (`OWLCMS_CONTROLPANELTOKEN`), sends it on stop, and offers a copy icon on the OWLCMS
  tab (the value is not displayed). It keeps the token in its runtime file (readable by
  its owner only) so a restarted control panel can still copy it and stop OWLCMS.
- In the cloud, the cloud app plays the control panel's role:
  [340](340-CLOUD_CONTROL_PANEL_TOKEN.md).
- Whenever OWLCMS has a token, it is required and must match, from any address,
  including from the OWLCMS machine itself.
- Only when OWLCMS was started without a token (by hand, without a control panel) is a
  direct local connection (loopback socket peer, no forwarding header) accepted
  instead. In IWF compliance mode OWLCMS refuses to start without a token (exit code
  78).
- Where a page needs locality, it asks for the token once; the session remembers it.

`iwfCompliance` turns on the security rules: accounts mode, and the token as the only
proof of locality. It is independent of `tcrrCompliance` (the competition rules): an
event may use either one without the other.

| Action | Status |
| --- | --- |
| `/controlpanel/stop` (the control panel sends the token automatically) | Done |

Simulation needs no locality: only the `simulation` page starts it, with a login; the
token-accessible `/simulation/*` script endpoints were removed.

## Audit pages

The **cp challenge** is a dialog in which the operator pastes the control panel token
before an action that needs locality (`ControlPanelChallenge`, same rule as the locality
proof above). It is asked at each such action; failures are throttled and audited.

The Audit Key Initialization button on the Administration page opens a dialog that
shows the active and pending key fingerprints and the key file, and creates a new
pending signing key on the server's disk. The new fingerprint is then shown for the
technical delegate to photograph at the machine, and OWLCMS restarts to activate it.
`admin/audit/check` verifies logs at a path on the server's disk. Both require
ADMIN_PAGES and the admin password ([Admin pages](#admin-pages)). In the cloud the key
comes from a secret, so key initialization does not apply there.

| Action | Status |
| --- | --- |
| Generating or replacing the pending key (dialog on the Administration page; the `admin/audit/prepare` page was removed) | Done: cp challenge in the dialog |
| Running a check (`admin/audit/check`) | Done: token field on the page, cp challenge at each check; opening the page needs no locality |
| Database import with `iwfCompliance` | To do: cp challenge before importing |

## Admin password

The control panel token, entered verbatim as the built-in admin's password, is the
emergency way in, at any time: for the first password of a new installation and for a
forgotten one. Fetching the token requires control of the OWLCMS process, the same as
setting a startup variable, so there is no separate recovery mode.

Done in accounts mode:

- The login page accepts user `admin` with the token as password, compared in constant
  time and throttled like a password. The login is audited with `how=control-panel-token`.
- The token becomes the admin's current password and the admin must change it: every
  page redirects to the change-password page (`AccountView`), which does not ask for the
  current password in that case. The token cannot be kept as the password.
- Once the new password is set, OWLCMS restarts, ending every session, including any
  opened by someone else who had the token. The control panel gives each OWLCMS launch,
  restarts included, a new token, so the token used for the emergency login stops
  working. In the cloud the secret stays the same until it is replaced.
- While the admin has no password, the login page says to use the token.
- The single-use admin password written to `owlcms.log` and the `OWLCMS_ADMINRECOVERY`
  recovery page are removed.
- Without a token and without an admin password, nobody can log in as admin; startup
  warns. In the cloud, set `OWLCMS_CONTROLPANELTOKEN` as a Fly secret
  ([340](340-CLOUD_CONTROL_PANEL_TOKEN.md) automates it).

Remaining, PIN mode: the admin pages ask for the admin password (see
[Admin pages](#admin-pages)); that prompt also accepts the token and leads to the same
change-password page. The admin password is no longer set from the User Accounts form
by an officials-PIN holder.

OWLCMS started by hand, without a control panel, has no token: whoever starts it can
define `OWLCMS_CONTROLPANELTOKEN` themselves, which makes the same flows available.

### Threat model

Guarantee: **nobody can reset the admin password with the token without access to the
OWLCMS server.** Server access means an account on the machine (desktop or SSH), or, in
the cloud, the operator's Fly access to the app's secrets. Someone with that access
already controls OWLCMS (database files, startup settings, the jar) and is outside the
threat model; the protection is against attackers who only reach OWLCMS over the network.

An arbitrary token given in the environment at launch is sufficient:

- The network cannot set it: only whoever starts OWLCMS chooses its environment.
- The network cannot read it: it is never logged, displayed, exported or written to the
  audit trail (the trail records `how=control-panel-token`, not the value). The control
  panel shows it only through a copy icon in its own window and keeps it in an
  owner-only runtime file.
- It cannot be guessed: 72 random bits, with failed logins throttled.
- It is short-lived: the control panel gives each launch a new token.

Typing the token in a browser on another machine is therefore acceptable: whoever types it
obtained it from the server.

Rejected, because they add nothing against network attackers and cannot stop someone
with server access:

- A secret shared by the control panel and OWLCMS, to prove the token came from the
  control panel: it can be extracted from the binaries, and the control panel's command
  line can itself start OWLCMS over SSH.
- A Unix domain socket handing the token to OWLCMS: an account on the machine can create
  the socket and serve any token.
- Address checks: a reverse proxy or port forward on the server makes remote requests
  look local, and forwarding headers can be forged.

## Admin pages

| Page | Access |
| --- | --- |
| `admin` (Administration: stop, restart, translations, backup, audit key, audit check, simulation) | ADMIN_PAGES role and the admin password; no locality, so cloud installations can use it. Audit key generation needs the cp challenge ([Audit pages](#audit-pages)). |
| `admin/backup` | ADMIN_PAGES or COMPETITION_SUPERVISOR, and the admin password for ADMIN_PAGES; no locality. The Competition Supervisor page (`lifting/competition-director`) gets a Backup button. |
| `admin/audit/check` | ADMIN_PAGES and the admin password; each check needs the cp challenge ([Audit pages](#audit-pages)) |
| `simulation` (button on the admin page) | ADMIN_PAGES or COMPETITION_SUPERVISOR, with a login (not token-accessible); no locality. |

- Accounts mode: the admin password is the normal login.
- PIN mode: the officials PIN currently gives full admin rights. The admin pages also
  ask for the built-in admin's password, once per session.
- `AdminView.checkLocalAccess` (localhost or backdoor list) is removed; the side-menu
  entry is shown to any session allowed to open the admin page.
- Tokens do not reach these pages: they are not `@TokenAccessible`.

## Database import

Import keeps its current roles (the preparation page); there is no import URL, so the
user is logged in. It is a full replacement, accounts included: anyone allowed to
import can replace the accounts. In PIN mode, and in accounts mode outside IWF
compliance, it is unchanged. In IWF compliance mode it also requires locality, which
there means the token.

## Acceptance

Verify hidden/inaccessible routes and buttons for PLATFORM(A) and combined
REGISTRATION/WEIGHIN/RESULTS accounts. Hide empty menu groups. Break controls
remain available only to authorized announcer/timekeeper/marshal roles.
Audit behavior follows the existing
[audit specification](done/AUDIT_TRAIL_SPECIFICATION.md).
