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
| MQTT broker and `/mqtt` | Keep MQTT credentials; remove client password from MoquetteAuthenticator debug logging |
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
  instead. It is not available in IWF compliance mode: OWLCMS must then be started
  with a token.
- Where a page needs locality, it asks for the token once; the session remembers it.

`iwfCompliance` turns on the security rules: accounts mode, and the token as the only
proof of locality. It is independent of `tcrrCompliance` (the competition rules): an
event may use either one without the other.

| Action | Status |
| --- | --- |
| `/controlpanel/stop` (the control panel sends the token automatically) | Done |
| `/simulation/start`, `/simulation/stop`, `/simulation/status` (scripts) | Done |

## Audit pages

`admin/audit/prepare` creates a new signing key on the server's disk and shows its
fingerprint for the technical delegate to photograph at the machine.
`admin/audit/check` verifies logs at a path on the server's disk. Both require
ADMIN_PAGES, the admin password ([Admin pages](#admin-pages)), and locality. In the
cloud the key comes from a secret, so the preparation page does not apply there.

## Admin password

When the built-in admin has no password, the control panel token takes the place of
the single-use password, in either access mode:

- Accounts mode: the login page tells the admin to enter the control panel token as
  the password. After that login the admin is sent to the existing change-password page
  (`AccountView`) and must set a real password before going anywhere else.
- PIN mode: the admin pages ask for the admin password (see [Admin pages](#admin-pages));
  while there is none, that prompt accepts the token instead and leads to the same
  change-password page. The admin password is no longer set from the User Accounts
  form by an officials-PIN holder.
- The token is accepted only while the admin has no password. Once a password exists,
  only that password works; changing it requires the current one, from anywhere.

The single-use admin password written to `owlcms.log` at startup is removed: locality
is proven by the token, not by access to the log. A database whose admin has no
password (for example a hand-edited JSON loaded at startup) therefore needs the token
before anyone can use the admin account.

Recovery from a forgotten admin password is a separate mode, triggered by a startup
setting, `OWLCMS_ADMINRECOVERY=true`. Only whoever controls the OWLCMS process can set
it: a line in the control panel's `env.properties` for a local installation, or a Fly
secret in the cloud. No JSON editing is involved.

- The control panel token is mandatory: without `OWLCMS_CONTROLPANELTOKEN`, startup
  reports that recovery cannot proceed.
- Startup runs the configuration phase (database, settings, audit, built-in admin) and
  skips the competition phase: no competition loading, MQTT broker, event forwarding or
  tracker push. `OWLCMS_LOADJSON` is ignored, with a warning. No single-use password is
  generated. Startup announces recovery mode.
- A servlet filter registered in `EmbeddedJetty` on `/*` runs before every servlet,
  Vaadin included. `/controlpanel/stop` passes through; every other request gets a plain
  HTML recovery form (status 401) asking for the control panel token, the new admin
  password and its confirmation. No page, export, H2 download or simulation is
  reachable. The form is in the browser's language, from the translation file, which
  does not depend on the database.
- A submission received before the database is ready is answered "starting, try again";
  the filter never waits. The token is compared in constant time and failures are
  throttled. The password must be acceptable and confirmed.
- Once the new password is set, every request gets a page telling the operator to
  remove `OWLCMS_ADMINRECOVERY` and restart.
- Entering recovery mode, each refused token and the password change are audited.

OWLCMS started by hand, without a control panel, has no token: whoever starts it can
define `OWLCMS_CONTROLPANELTOKEN` themselves, which makes the same flows available.

## Admin pages

| Page | Access |
| --- | --- |
| `admin` (stop, restart, translation reload, links) | ADMIN_PAGES role and the admin password; no locality, so cloud installations can use it |
| `admin/backup` | ADMIN_PAGES or COMPETITION_SUPERVISOR, and the admin password for ADMIN_PAGES; no locality. The Competition Supervisor page (`lifting/competition-director`) gets a Backup button. |
| `admin/audit/prepare`, `admin/audit/check` | Also locality ([Audit pages](#audit-pages)) |
| `simulation` (button on the admin page) | ADMIN_PAGES or COMPETITION_SUPERVISOR; no locality. Scripts use `/simulation/start` and `/simulation/stop` with the control panel token. |

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
