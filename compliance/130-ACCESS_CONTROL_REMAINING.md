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
- An OWLCMS started without a token (by hand, without a control panel) refuses every
  action that needs locality, and says so. There is no fallback on the client address,
  not even a direct loopback connection.

## Security settings

The access mode (PIN or accounts) and the IWF compliance check are set only in the
Security section of the Administration page (`SecuritySettings`). The Accounts page and
the System Settings no longer have an access-mode control.

- Switching the access mode, in either direction, needs the cp challenge
  ([Audit pages](#audit-pages)) and an admin password for accounts mode. It is audited
  (`ConfigRepository.save`) and logs out every session. In the cloud the cp challenge
  means the token secret; until [340](340-CLOUD_CONTROL_PANEL_TOKEN.md) sets it, cloud
  apps stay in PIN mode unless it is set by hand.
- IWF compliance is a check, not a mode (`IwfComplianceCheck`): nothing is enforced.
  When the "Check IWF compliance" box is checked, OWLCMS lists the requirements:
  - user accounts are used;
  - the audit trail is signed with an installed key (a key file, or the key secret in
    the cloud), not the built-in one; a pending key counts after the next restart;
  - OWLCMS was started with a control panel token.

  The Security section shows every requirement. The requirements not met are shown on
  the home page to the sessions that can open the Administration or Competition
  Supervisor pages, and as a notification on the Competition Supervisor page. The
  result is logged and audited at startup (`auth.iwf-compliance-check`).
- The check takes effect at once, without the cp challenge or a restart; changing it is
  audited (`auth.iwf-compliance`). It is independent of `tcrrCompliance` (the
  competition rules).
- There is no startup override: `OWLCMS_ACCESSMODE` and `OWLCMS_IWFCOMPLIANCE` were
  removed. Both settings come from the database: the Security section, or an import.
- An `iwfCompliance` entry left in the feature switches is ignored.

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
| Database import | Undecided: see [Database import](#database-import) |

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
| `admin` (Administration: stop, restart, security settings, backup, audit key, audit check, simulation) | ADMIN_PAGES role and the admin password; no locality, so cloud installations can use it. Audit key generation and security settings need the cp challenge ([Audit pages](#audit-pages)). |
| `admin/backup` | ADMIN_PAGES or COMPETITION_SUPERVISOR, and the admin password for ADMIN_PAGES; no locality. The Competition Supervisor page (`lifting/competition-director`) gets a Backup button. |
| `admin/audit/check` | ADMIN_PAGES and the admin password; each check needs the cp challenge ([Audit pages](#audit-pages)) |
| `simulation` (button on the admin page) | ADMIN_PAGES or COMPETITION_SUPERVISOR, with a login (not token-accessible); no locality. |

- Accounts mode: the admin password is the normal login.
- PIN mode: the officials PIN currently gives full admin rights. The admin pages also
  ask for the built-in admin's password, once per session.
- Done: the side-menu entry is shown to any session allowed to open the admin page,
  in PIN mode too (the PIN mode switch to accounts is on that page).
- To do: remove the PIN-mode locality check (`AdminView.hasLocalAccess`, localhost or
  backdoor list) once the admin password is asked for.
- Tokens do not reach these pages: they are not `@TokenAccessible`.

## Database import

Import keeps its current roles (the preparation page); there is no import URL, so the
user is logged in. It is a full replacement, accounts included: anyone allowed to
import can replace the accounts. Import is normal until the competition starts, and
dangerous after.

Direction agreed (2026-10-30), not implemented:

- Terminology: a **permission** is an individual right (a base role in the code, the only
  thing `@RequiresRole` checks); a **role** is a set of permissions given to an account
  (a grantable role in the code, expanded by `Role.expand()`). Many names are both: the
  Preparation role contains only the PREPARATION permission.
- Actions inside pages get required permissions, like pages do with `@RequiresRole`: an
  enum of protected actions, checked in the UI (hide or disable the button) and again
  in the back end where the action runs, with the refusal audited. Today the actions on
  the Preparation page (database import and export, registration and SBDE uploads)
  check no role: any role allowed on the page can use them.
- An **"Imports are allowed"** yes/no setting, on the Competition Supervisor page,
  changed by the COMPETITION_SUPERVISOR permission (the Supervisor or an admin) and
  audited. Manual, not automatic: test lifts are often done before the competition.
  When it is off, every import is refused, whoever asks: the database (JSON) import, the
  registration upload and the SBDE upload. To use SBDE during the competition (athlete
  names, referee allocations; results are not touched), the Supervisor turns imports on,
  uploads, and turns them off again.
- In PIN mode the setting applies too, but any PIN holder can change it: it prevents
  accidents, not people ("PIN mode stays naked").
- On by default for a new competition. Stored with the competition, so exports carry
  it and an import restores it.
- Database exports require the RESULTS permission (roles: Results, Competition
  Director, Competition Supervisor, External, admin). The secrets an export contains
  are protected by their hashes (see [PIN mode](#pin-mode) for the PINs).
- The registration upload requires REGISTRATION; the SBDE upload requires PREPARATION,
  since it also changes the technical officials and their allocations.

An import is faithful: the settings are those of the export, the access mode and the
IWF compliance check included; nothing is kept from the running OWLCMS. A setting
missing from an older export takes its default (PIN mode, check off).

## PIN mode

Direction agreed (2026-10-30), not implemented. PIN mode protects a cloud installation
from random visitors; it is a weak check by design.

- The officials PIN opens the Home page and gives every permission ("PIN mode stays
  naked"). The address check on the admin pages (`AdminView.hasLocalAccess`) and the
  backdoor list (`ipBackdoorList`, `OWLCMS_BACKDOOR`) are removed; the dangerous actions
  keep the control panel token. `OWLCMS_PIN`, `OWLCMS_DISPLAYPIN`, `OWLCMS_IP` and
  `OWLCMS_DISPLAYLIST` are kept: only the operator sets them, and existing cloud apps use
  them.
- The Administration page is hidden when OWLCMS has no control panel token.
- "Remember this browser" for the officials and display PINs: a long-lived HttpOnly
  cookie, a keyed hash of the current PIN and an expiry; changing the PIN forgets every
  browser, and a button forgets them all.
- The officials PIN login also accepts the control panel token (constant time, throttled,
  audited `how=control-panel-token`), so a forgotten PIN can be replaced in System
  Settings. A token login is never remembered.
- The officials PIN, display PIN and MQTT password are hashed with PBKDF2 like account
  passwords (today a single salted SHA-256); older hashes are accepted and converted
  at the next correct entry. Comparisons are constant-time.
- Account passwords: PBKDF2 iterations raised from 310,000 to 600,000 (current OWASP
  figure for PBKDF2-HMAC-SHA256); a stored hash with fewer iterations is replaced at
  the next successful login.

## Acceptance

Verify hidden/inaccessible routes and buttons for PLATFORM(A) and combined
REGISTRATION/WEIGHIN/RESULTS accounts. Hide empty menu groups. Break controls
remain available only to authorized announcer/timekeeper/marshal roles.
Audit behavior follows the existing
[audit specification](done/AUDIT_TRAIL_SPECIFICATION.md).
