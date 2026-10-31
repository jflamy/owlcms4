# Access Control - Other Remaining Work

The role system is implemented. Its historical design is in
[done/ACCESS_CONTROL_DESIGN.md](done/ACCESS_CONTROL_DESIGN.md).
Token generation and URL protection are done: [110](done/110-ACCOUNT_TOKENS.md) and
[120](done/120-TOKEN_PROTECTED_URLS.md).
The items below were identified on 2026-10-22. Done: navigation and in-page actions,
locality, security settings, audit pages, admin password, admin pages, PIN mode (now
"Simple Access" in the interface). Remaining: the [Database import](#database-import)
plan (protected actions, "Imports are allowed"), the [Non-route review](#non-route-review)
and the [Acceptance](#acceptance) checks.

## Navigation and in-page actions

Done: buttons that open another page are shown only when the session may open the
target (`AccessUi.canOpen`, or `AccessUi.canOpenOnPlatform` for a platform-bound
target), as the navigation pages already did:

| Source | Target |
| --- | --- |
| AnnouncerContent, BreakManagement | MedalCeremonyContent |
| EditChampionshipsPanel (also inside the Competition page) | AgeGroupContent, ChampionshipTemplateContent |
| AgeGroupContent, TeamResultsContent | ChampionshipsContent |
| RecordConfigEditingFormFactory, RecordsConfigContent | RecordContent |
| PlatformContent | TCContent for each platform; the column is hidden when no platform is allowed |
| SessionContent | RegistrationContent |
| RecordContent | RecordFederationComparisonReport (PREPARATION or RESULTS, like the records page; was ADMIN_PAGES) |
| TechnicalOfficialContent | TeamScheduleContent |

Not gated: the Login/Edit button of `publicRecords` (records-only mode has its own
access rules and that button is its way in), the announcer active/passive switch (same
permission), and the Administration page buttons (ADMIN_PAGES opens their targets).
Download links are not route targets: their authorization belongs to the resource.
The target page still refuses entry on its own; hiding only avoids dead buttons.

The records page (`RecordContent`) and the record eligibility report need PREPARATION
or RESULTS. Results users get every action on the records page, not only the export:
they may need to fix records and recompute the new ones.

The device testing page (`lifting/testing`) needs TC or COMPETITION_SUPERVISOR on its
platform (was ADMIN_PAGES). Break buttons stay as they are: they appear only on lifting
pages whose own permission already fits (announcer, timekeeper, marshal Pause,
Competition Supervisor, testing).

## Non-route review

| Surface | Remaining review |
| --- | --- |
| `/local/*` | Verify directory/classpath confinement, decoding, traversal and symlinks; exclude sensitive files |
| MQTT broker and `/mqtt` | Done: client password no longer logged; OWLCMS's own broker connection uses a random secret (not the startup time), compared in constant time |
| Vaadin dynamic downloads/uploads | Verify resource creation and use require the appropriate page permission |
| Forwarded client IP | Decided: no trusted-proxy work. The officials and display address lists are PIN mode only and keep reading `X-Forwarded-For` (weak, accepted). Accounts mode does not depend on the client address (login throttling and logs only); security needs accounts. |
| CorsFilter | Review credentialed cross-origin cookies and `SameSite=None` with named accounts |

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

PIN mode: an officials-PIN holder sets the admin password on the User Accounts page,
which is needed before switching to accounts mode. A forgotten officials PIN is replaced
after logging in with the control panel token ([PIN mode](#pin-mode)).

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
- PIN mode: the officials PIN gives every permission ("PIN mode stays naked"); the
  dangerous actions on these pages need the control panel token.
- Done: the side-menu entry is shown to any session allowed to open the admin page,
  in PIN mode too (the PIN mode switch to accounts is on that page).
- Done: the PIN-mode address check (`AdminView.hasLocalAccess`) and the backdoor list
  are removed ([PIN mode](#pin-mode)).
- The Administration page has two tabs: System (stop, restart, backup, simulation) and
  Security and Audit (access mode, IWF check, the PIN-mode access settings moved from
  System Settings, audit key and audit check). Without a control panel token it is
  still shown; the actions that need the token are disabled and say why.
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
- On by default. Stored directly in `Config`, so exports carry it and an import
  restores it. [136](136-CONFIG_COARSE_CONFLICT.md) protects it against stale System
  Settings and other Config editors using versioned saves and import invalidation;
  no `CompetitionControls` embeddable is introduced.
- When imports are off, the import buttons are shown disabled, with helper text saying
  why and where to turn imports back on.
- Database exports require the RESULTS permission (roles: Results, Competition
  Director, Competition Supervisor, External, admin). The secrets an export contains
  are protected by their hashes (see [PIN mode](#pin-mode) for the PINs).
- Decided 2026-10-30: the three imports (database JSON, registration, SBDE) require a new
  IMPORT permission. It is not grantable on its own: it comes with the Preparation and
  Registration roles, Competition Supervisor and admin (and Simple Access), not with
  Competition Director. Accounts without it do not see the import buttons; the back end
  checks again when the import runs and audits a refusal. The other uploads (age groups,
  officials, coaches, team schedule, records, session results) are not imports for this
  purpose.

An import is faithful: the settings are those of the export, the access mode and the
IWF compliance check included; nothing is kept from the running OWLCMS. A setting
missing from an older export takes its default (PIN mode, check off).

## PIN mode

Agreed 2026-10-30. PIN mode protects a cloud installation from random visitors; it is a
weak check by design. Done.

- The officials PIN opens the Home page and gives every permission ("PIN mode stays
  naked"). The address check on the admin pages (`AdminView.hasLocalAccess`) and the
  backdoor list (`ipBackdoorList`, `OWLCMS_BACKDOOR`) are removed; the dangerous actions
  keep the control panel token. `OWLCMS_PIN`, `OWLCMS_DISPLAYPIN`, `OWLCMS_IP` and
  `OWLCMS_DISPLAYLIST` are kept: only the operator sets them, and existing cloud apps use
  them.
- Without a control panel token (a cloud app without the secret) the Administration
  page is still shown, so the PINs can be changed; only the actions that need the token
  are disabled.
- "Remember this browser" on the officials and display PIN logins (`RememberedBrowser`):
  an HttpOnly cookie valid 30 days, holding an expiry and an HMAC of the current PIN
  hash, keyed with a secret stored in the settings and never exported (an export also
  holds the PIN hashes). The PIN arrives over the push connection, so the login page
  exchanges a one-time ticket at `/remember`, whose HTTP response sets the cookie.
  Saving the PIN, even unchanged, forgets every browser (a new salt makes a new hash).
  Officials: unchecked by default; displays: checked.
- A Logout entry for PIN sessions ends the session and shows the login page. A
  remembered browser stays remembered: the login page shows the box checked, Login
  with no PIN lets it in, and any other page lets it in directly. Unchecking the box
  on the login page forgets the browser at once, without the PIN.
- The officials PIN login also accepts the control panel token (constant time, throttled,
  audited `how=control-panel-token`), so a forgotten PIN can be replaced on the
  Administration page (Security and Audit tab); a notice says where. A token login is never remembered. The officials and
  display PIN logins are throttled like account logins.
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
