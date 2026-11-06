# 190 - Video Replays Tokens: Implementation Plan

Status: implemented in the Video and Tracker working trees, with documentation;
automated tests pass (Video `go test ./...`, Tracker `npm test`). Video and
Tracker behave as before when no token is configured, checked by comparing
responses and URLs against the code preceding this work. Verified by hand so
far: token generation and the show-once dialog, sign-in from the browser form
and sign-out, tokens surviving a Video restart. Remaining from 1.8: Control
Panel upgrade, Tracker jury replays with a token, revoke/regenerate with two
signed-in browsers, and a corrupt token file.

The replay request log records the path without the query string
(`owlcms/video`, `internal/httpServer/server.go`, test
`TestHandleReplayOmitsQueryFromRequestLog`).

Repositories: `owlcms/video` (most of the work) and `owlcms/tracker` (replay
clients). No OWLCMS Java change. No Control Panel change is required for the
deployment planned here (scenario 1).
This completes the token-protected URLs of [120](done/120-TOKEN_PROTECTED_URLS.md).
There is no OWLCMS replay account, role, login handoff or pairing secret; the
earlier Video design that proposed them (`docs/compliance/replay-access-control.md`)
has been removed.

Priority: **Milestone 1, tokenized access to the replays**, including the Tracker
clients that consume them, then Milestone 2 (documentation).

Deferred to [230](230-VIDEO_ACCESS_DEFERRED.md): the Cameras configuration
token, not needed when Cameras and Replays run in the same process; external
broadcaster testing through Caddy; and a second deployment, several Video
instances on one machine, feasible but complicated.

## 1. Agreed behaviour

### Deployment (scenario 1)

One laptop per platform. Each runs Video with both the Cameras and Replays
modules in one process, on one HTTP port (8091):

```text
                          LAN (trusted switch)                     Outside
OBS, jury, Tracker ──▶ machine A  Video :8091  ◀──┐
                   ──▶ machine B  Video :8091  ◀──┤  Caddy (LAN, e.g. 192.168.1.10)
                                                  │    a.replays.example.org ─▶ A
                                                  │    b.replays.example.org ─▶ B
                                                  └── router: 80, 443 ─▶ Caddy  ◀── broadcasters
```

- Inside the venue, OBS, the jury page and the Tracker reach each machine
  directly over HTTP on 8091.
- Outside, broadcasters reach Caddy over HTTPS, one DNS name per platform, all
  on port 443. Caddy chooses the machine from the name and forwards one HTTP
  hop over the trusted switch. The router forwards only 80 and 443 to Caddy,
  never 8091. Port 80 is used by Caddy to obtain and renew its certificates.
- Video on each machine lists Caddy's LAN address as a trusted proxy (1.4).
  Caddy can run on a separate small machine or on one of the laptops; on that
  laptop the trusted address is `127.0.0.1`, on the others the laptop's LAN
  address.
- Cameras configuration is read in-process: `/api/cameras/config` is not needed
  in this deployment, and Caddy refuses it from outside. It stays open on the
  trusted LAN; its token is deferred ([230](230-VIDEO_ACCESS_DEFERRED.md)).

Several names pointing at one router address need a domain you own: for example
`a.replays` and `b.replays` as CNAME records to the router's dynamic DNS name
(`yourname.asuscomm.com`). Router, DNS, certificates and Caddy installation are
deployment steps, documented in Milestone 2, not code.

### Tokens

Replays viewer tokens, managed in the Video desktop application, authorize the
browsing page, the replay APIs, the MP4 files and live updates (`/ws`). There
can be several per platform, each with a name. No token authorizes any
management action: adding, regenerating, revoking and opening access exist
only in the desktop UI.

All viewer tokens grant the same access. They differ only by name, so that
each holder can be revoked alone: one per broadcaster ("RAI", "Eurosport"),
one for the venue's OBS, one for the Tracker. Revoking a broadcaster's token
ends that broadcaster's access, including its browser sessions, and nobody
else's.

All tokens follow the OWLCMS 69 account-token scheme:

- 12 characters drawn uniformly from
  `ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_` with
  `crypto/rand` (72 bits). The alphabet has 64 symbols, so `b & 63` on each
  random byte is uniform, as in the Control Panel's `newControlPanelToken`.
- Stored as the hex SHA-256 of the UTF-8 token, unsalted. Verification hashes
  the presented value and compares with `crypto/subtle.ConstantTimeCompare`.
- Shown once, at generation, for copying. Never recoverable afterwards.
- Valid exactly 21 days from generation, which covers setup before the event. No sliding renewal.

Viewer tokens have a name: 1 to 40 characters, letters, digits, space, `.`, `-`
and `_`, unique on the platform without regard to case. The name is not
secret: it appears in the desktop list, the logs and the holder's own page.
At most 50 viewer tokens per platform, expired ones included.

| State | Effect on protected routes |
| --- | --- |
| Open (default) | No credential needed, same as today |
| Protected | Any one of the platform's valid viewer tokens, or a session obtained with one, is required |
| Protected, no valid token (all remaining tokens expired) | Everything refused until a new token is added or access is opened |
| Security file unreadable or corrupt | Refused with 503; never treated as open |

| Action on one viewer token | Effect |
| --- | --- |
| Add (with a name) | Turns Open into Protected if needed; new token valid 21 days |
| Regenerate | New value and 21 days, same name; the old value and its sessions are refused |
| Revoke | Removed; its value and its sessions are refused from the next request while other tokens remain; removing the last token opens access |
| Expiry | That token and its sessions are refused; the record stays in the list as expired until revoked, so access remains protected |

**Open access** is a confirmed action that removes every viewer token. Revoking
the last token also returns to Open, with an explicit warning before revocation.

### Clients

| Client | Credential | Notes |
| --- | --- | --- |
| OBS media source (`/replay/1`, `/videos/...`), inside | `?token=` in the URL | OBS cannot add headers. Its own token, e.g. "OBS" |
| Tracker jury replays plugin and OBS automation (browser, other origin), inside | `?token=` on every API, media and `/ws` URL | Query rather than header: avoids CORS preflight, and WebSockets cannot carry headers. Its own token, e.g. "Tracker" |
| Broadcasters, outside through Caddy | Bearer header or `?token=`; `wss://…/ws?token=` | One token per broadcaster and platform, for the lists and the files |
| Browser on the Video browsing page, inside or outside | `/?token=` once, or the sign-in form; then a session cookie | The token never appears on later requests |

Separate tokens for the venue's own clients mean revoking a broadcaster never
breaks OBS or the jury page.

### External broadcasters

The venue gives each broadcaster, for each platform they cover, two things:
the platform's address (`https://a.replays.example.org`) and a token created
for that broadcaster and named after it. The token works like a password for
that platform's replays. It ends when the operator revokes or regenerates it,
at the latest after 21 days; other broadcasters are not affected.

A broadcaster can use the token in three ways.

**1. Sign in on the replays page.** Open `https://a.replays.example.org/`.

1. Without a valid session, the page is a sign-in form: platform name, an
   **Access token** field (masked, like a password) and a **Sign in** button.
   Nothing about the replays is shown.
2. On success the browser is sent to the replays page and keeps a session
   until the token expires or is regenerated: closing the browser, or a
   restart of Video, does not sign it out. The browser's password manager may
   offer to save the token.
3. The replays page shows a **Sign out** button at the top right; hovering
   over it shows "Signed in as <token name>, valid until <date>". Sign out
   removes the session from this browser only.
4. On a wrong, expired or revoked token the form is shown again with "Token not
   accepted. It may be wrong or expired." When the operator revokes or
   regenerates that broadcaster's token, the next page load, API call or video
   request is refused, and the page returns to the sign-in form.

**2. A link that signs in.** `https://a.replays.example.org/?token=<token>` does
the same as the form in one step: Video sets the session and redirects to
`https://a.replays.example.org/` before showing anything, so the token does
not stay in the address bar. It does stay in the browser history and in any
place the link was pasted.

**3. The token on each request**, for scripts, players and automation.
Either an `Authorization: Bearer <token>` header or `?token=<token>` on the
URL (players such as OBS and WebSocket clients can only use the latter):

| Purpose | Request |
| --- | --- |
| Latest replay of camera N | `GET /replay/N.mp4` |
| Be told when a new replay is ready | `wss://a.replays.example.org/ws?token=<token>`; the "ready" message lists each camera's `videoPath` |
| Current replays, without WebSocket | `GET /api/replay-state` |
| Sessions | `GET /api/sessions` |
| Lifts of a session, with each camera's `url` | `GET /api/sessions/{session}/lifts` |
| A specific lift | `GET` the `url` from the list, joined to the base URL |

Returned paths contain no token: each request carries it again. The answers
are 401 when it is missing, wrong or expired, and 429 after repeated failures
from the same address.

Inside the venue the same three ways work at `http://<laptop>:8091`.

### Browser session details

1. `GET /?token=...` with a valid token: set the session cookie and answer
   303 to the same URL without `token`, before any HTML is rendered.
2. `GET /` without a valid token or cookie: 401 with the sign-in form. The form
   posts the token in the body; success sets the cookie and redirects with 303.
3. Later page loads, video links, API calls and the `/ws` upgrade carry only
   the cookie.
4. The session is tied to the token used to sign in. Sign out clears the
   cookie in that browser. The session is a signed value, not a server-side
   record, so a copy of it taken elsewhere stays valid until that token is
   revoked, regenerated or expires.

APIs, media and `/ws` never redirect or show the form: they answer 401.

Sessions survive browser and Video restarts and end with their token's expiry,
Regenerate or Revoke, or with Open access. Checks happen when a request or
connection starts: a download already in progress is not interrupted, and a
live `/ws` connection already open stays open until it disconnects. It only
carries notifications with video paths; fetching any of them is refused.

### Network

The wired LAN, its switch and connected hosts are trusted, no snooping. Same
token rules inside and outside; no address-based exemption (loopback, LAN,
NAT, or the trusted proxy itself).

Outside access goes through Caddy over HTTPS (deployment above). Caddy only
carries the traffic: it does not authenticate, and holds no token. Video
checks every request, from inside or outside, the same way. A router port
forward straight to 8091 also works with the same tokens, but without HTTPS
the token travels in clear on the external path, and Video sees no proxy; it
is not the recommended setup.

## 2. Milestone 1 - Replays viewer tokens

Execute the steps in order; each one is a separate change that builds, passes
its tests and leaves Video usable. Video steps: run `go test ./...` and
`go build -o video .` (`video.exe` on Windows) in the Video repository before
declaring a step done. Tests of each step are listed in 1.7 under the step's
number.

| Step | Goal | Done when |
| --- | --- | --- |
| 1.1 | `config.SecurityDir()`: a token-file directory that survives Video upgrades | 1.7 tests for 1.1 pass; the directory is logged at startup |
| 1.2 | `internal/access`: token store, verification, sessions; no UI, no HTTP | 1.7 tests for 1.2 pass |
| 1.3 | Desktop UI to add, list, regenerate, revoke tokens and open access; `publicUrl` | Manual: each action updates the list and `video-access.json` (no token value in it); the token is shown once with its URLs. HTTP behaviour unchanged |
| 1.4 | `clientOf`: client address and HTTPS through a trusted proxy; `trustedProxies` | 1.7 tests for 1.4 pass; no behaviour change yet |
| 1.5 | HTTP guard, sign-in and sign-out, browsing-page changes | 1.7 tests for 1.5 pass; 1.8 items 2 and 5 pass on the LAN |
| 1.6 | Tracker clients carry the token | 1.7 tests for 1.6 pass; 1.8 items 3 and 4 pass |
| 1.8 | Manual acceptance (LAN and Video upgrade) | All items pass |

The desktop UI (1.3) comes right after the token store (1.2) so that tokens
can be added, listed and revoked by hand while the guard is built; until 1.5
is in place they have no effect. Do not release a Video build where a token
can be added before 1.5 is complete, and do not advise operators to add one
before the Tracker update of 1.6 is installed: the jury replay page and OBS
automation would fail.

### 1.1 Security directory

The Control Panel launches Video with `--configDir <owlcms-video>/<version>`
(`controlpanel/video/launch.go`), so the configuration directory changes at
each version. The token must not. Add to `internal/config/config.go`:

```go
// SecurityDir returns the directory holding the access-token file. It is
// stable across Video versions.
func SecurityDir() string
```

Resolution order:

1. `VIDEO_SECURITYDIR`, if set (explicit override).
2. Else, if `VIDEO_CONTROLPANEL_DIR` (`config.ControlPanelDirEnv`) is set
   (launch by a Control Panel; set by `shared.BuildVideoLaunchEnv` since
   Control Panel 3.2): the parent of `ConfigDir`, which is the Video
   installation directory above the version directories.
3. Else (standalone or development launch): `ConfigDir`.

Do not use `VIDEO_CONFIGDIR`: it is the Control Panel's shared FFmpeg
configuration folder, not Video's installation directory.

Rule 2 needs no Control Panel change and also covers released Control Panels.
Without it, an upgrade would land in a new version directory with no token
file and silently open the replays. Log the resolved directory at startup.

### 1.2 `internal/access` package

New non-UI package; standard library only (`crypto/rand`, `crypto/sha256`,
`crypto/hmac`, `crypto/subtle`, `encoding/json`).

File `<SecurityDir>/video-access.json`:

```json
{
  "version": 1,
  "instanceId": "3f9a1c2e",
  "sessionKey": "<32 random bytes, base64>",
  "viewer": {
    "tokens": [
      {
        "id": "<16 random bytes, base64url>",
        "name": "RAI",
        "hash": "<hex sha-256>",
        "createdAt": "2026-11-05T20:30:00Z",
        "expiresAt": "2026-11-19T20:30:00Z"
      }
    ]
  }
}
```

- `viewer` null, absent, or with an empty `tokens` list means Open. Revoking the
  last token sets `viewer` to null; expired records remain present and protected.
- `id` is random and never reused. Regenerate replaces the record with a new
  `id`, value and dates, same name. Sessions refer to the `id`, so a revoked or
  regenerated token, or one removed by Open access and later re-added under the
  same name, never revives an old session.
- Unknown top-level fields are kept on rewrite, so a later Cameras token
  ([230](230-VIDEO_ACCESS_DEFERRED.md)) can add its own field without changing
  the version.
- `instanceId` (8 hex characters) and `sessionKey` are created with the file
  and kept. The session key is independent of the token hashes: someone holding
  the file's hashes cannot forge a session by knowing them, only by also reading
  the key, and both are in the same owner-only file.
- Load validation: unique ids, unique names (case-insensitive), valid names,
  at most 50 records. Any violation is a load error.

API:

```go
type State int
const (StateOpen State = iota; StateProtected; StateUnavailable)

type Decision int
const (Allowed Decision = iota; Missing; Invalid; ExpiredToken; ServiceUnavailable)

// Viewer is a viewer token's public description. Never contains the value.
type Viewer struct {
    ID, Name             string
    CreatedAt, ExpiresAt time.Time
}

func Open(dir string) *Service // loads; keeps a load error, never panics

// Viewer tokens
func (s *Service) ViewerState() (State, []Viewer) // sorted by name; expired ones included
func (s *Service) AddViewer(name string) (token string, v Viewer, err error)
func (s *Service) RegenerateViewer(id string) (token string, v Viewer, err error)
func (s *Service) RevokeViewer(id string) error
func (s *Service) OpenViewerAccess() error // removes every viewer token: Open
func (s *Service) VerifyViewer(token string) (Viewer, Decision)

// Browser sessions (viewer tokens only)
func (s *Service) IssueSession(id string) (value string, expires time.Time, err error)
func (s *Service) VerifySession(value string) (Viewer, Decision)
func (s *Service) CookieName() string // "owlcms_replays_" + instanceId

func (s *Service) OnChange(func()) // any mutation, and each expiry instant
```

Implementation notes:

- `VerifyViewer`: in Open state return `Allowed` and an empty `Viewer` without
  looking at the token. Reject tokens longer than 128 bytes before hashing.
  Hash once, then compare with every record's hash using
  `subtle.ConstantTimeCompare`, without stopping at the first match (at most 50
  comparisons). Match on an expired record → `ExpiredToken`; no match →
  `Invalid`.
- `AddViewer` trims the name, validates it (1-40 characters, letters, digits,
  space, `.`, `-`, `_`), refuses a duplicate name (case-insensitive) and a 51st
  record. Errors are plain messages for the dialog.
- Expiry is `!now.Before(expiresAt)`. Inject `now func() time.Time` and the
  random reader for tests.
- `OnChange` also fires at each expiry instant: keep one `time.AfterFunc` set
  to the earliest future `expiresAt`, rescheduled after `Open` and every
  mutation. Milestone 1 uses it to refresh the desktop status line and list.
- Load errors (unreadable file, bad JSON, wrong version, malformed fields)
  put the service in `Unavailable`. A missing file is a normal first start:
  Open state, file created on the first mutation. On an `Unavailable` service,
  `AddViewer` and `OpenViewerAccess` start a new file (new `instanceId` and
  `sessionKey`) after the UI confirms, which is the recovery path;
  `RegenerateViewer` and `RevokeViewer` are refused.
- Writes: marshal, write `video-access.json.tmp` in the same directory with mode
  0600, sync, close, `os.Rename` over the file (it replaces the target on
  Windows too). Update the in-memory state only after the rename succeeds.
  On failure, return the error and leave the previous state.
- Permissions: 0600 on Linux and macOS. On Windows the file inherits the ACL of
  `%APPDATA%\owlcms-video`, which is limited to the user, SYSTEM and
  Administrators; no explicit ACL code in this milestone. A standalone launch
  from a shared folder does not get that protection: say so in the docs.
- Mutations are serialized by the service mutex. One Video process per
  installation is assumed; no cross-process lock.

Session cookie value: `v1.<id>.<issuedUnix>.<sig>` where
`sig = base64url(HMAC-SHA256(sessionKey, "v1|" + id + "|" + issuedUnix))`.
`VerifySession` checks, in order: viewer state (Open → Allowed; Unavailable →
ServiceUnavailable), length at most 256, format, HMAC in constant time, then
the record with that `id`: absent → `Invalid` (revoked or regenerated),
expired → `ExpiredToken`. It returns the record, so the page can show its name
and expiry. The token never appears in the cookie; the cookie's own expiry is
the record's `expiresAt`.

### 1.3 Desktop UI (`internal/replays/app.go`)

Wiring:

- `main.go`: after `config.ConfigDir = paths.Root` and logging are set up,
  call `access.Open(config.SecurityDir())` and log the directory and state.
  Pass the service to `replays.Init` through a new `replays.Options.Access`
  field; the UI keeps it. `Open` never fails (a bad file gives
  `Unavailable`), so startup is not blocked.
- `replays.toml` (`internal/config/replays/config.go`, and the commented
  default in the embedded `config.toml`): new optional `publicUrl`:

  ```toml
  # External address of this platform's Replays page, as broadcasters see it.
  # Display only (token dialog); Video does not route or check it.
  publicUrl = "https://a.replays.example.org"
  ```

  Accepted only if it parses as an absolute `http`/`https` URL with no path,
  query or fragment beyond `/`; otherwise logged and ignored.

Screens:
- Replays menu: new item **Access Tokens...**. Status line on the Replays tab
  under the "Open replay list in browser" link, refreshed from
  `access.OnChange` through `fyne.Do`:
  - `Access: open (no token)`;
  - `Access: token required - 3 valid, next expiry <local date time>`;
  - `Access: token required - no valid token, all viewers refused`;
  - `Access: token file unreadable - all viewers refused`.
- Dialog **Replays access tokens**:
  - A list, one row per token, sorted by name: name, created, expires (or
    `EXPIRED <date>`), and buttons **Regenerate** and **Revoke**.
  - Below the list: a name entry and **Add token**; **Open access** (enabled
    when protected).
  - Add: when going from Open to Protected, confirm first: "Viewers without a
    token, including OBS and the Tracker, will be refused. Add tokens for them
    too." Name errors from `AddViewer` (empty, invalid characters, duplicate,
    50 tokens) shown inline.
  - Regenerate confirms: "<name>'s current token and its browser sessions
    stop working. Give <name> the new token."
  - Revoke confirms: "<name>'s token stops working. Other tokens are not affected."
    Revoking the last token additionally warns: "Replays become accessible
    without a token to anyone who can reach port N."
  - Open access confirms: "All tokens are deleted. Replays become accessible
    without a token to anyone who can reach port N."
  - Each action writes the file: run it in a goroutine, then refresh the list
    with `fyne.Do`.
- After a successful Add or Regenerate: a dialog titled with the token's name,
  the token in a read-only entry, a **Copy** button
  (`window.Clipboard().SetContent`), and ready-to-copy examples using
  `getReplayListHost()` and the configured port:
  - browser: `http://<host>:<port>/?token=<token>`
  - OBS media source: `http://<host>:<port>/replay/1?token=<token>`
  - Tracker replays server URL: `http://<host>:<port>/?token=<token>`

  If `publicUrl` is set, also the broadcaster forms: the page
  `https://a.replays.example.org/` with the token to type, and the sign-in link
  `https://a.replays.example.org/?token=<token>`.

  and the warning that the token is not shown again.
- Logs: "Replays access token RAI added, expires ...", "... regenerated ...",
  "... revoked", "Replays access opened, all tokens deleted". Names only, never
  the value.

### 1.4 Trusted proxy (`internal/httpServer/clientaddr.go`)

Caddy forwards each external request over one HTTP hop. Without this step
Video sees every broadcaster as Caddy's address, over plain HTTP.

Configuration, in `replays.toml` (`internal/config/replays/config.go` and the
commented default in the embedded `config.toml`):

```toml
# Addresses or CIDR ranges of reverse proxies whose X-Forwarded-* headers are
# trusted. Caddy's LAN address only. Never the router: with NAT loopback, LAN
# clients can appear to come from it.
trustedProxies = ["192.168.1.10"]
```

- Empty by default: forwarded headers are ignored, as today.
- `httpServer.SetTrustedProxies([]string)` parses the list with `net.ParseIP` /
  `net.ParseCIDR` and stores it in a package variable. Log and ignore an
  invalid entry; ignoring makes the headers untrusted, never the reverse.
  Log the resulting list.
- Called by `replays` with the loaded configuration before
  `httpServer.StartServer` (`internal/replays/app.go`, where the server is
  started today).

One function decides where a request comes from:

```go
// clientOf returns the browser's or client's address and whether its request
// was HTTPS. Forwarded headers count only when the connection itself comes
// from a trusted proxy.
func clientOf(r *http.Request) (addr string, https bool)
```

- Peer = host of `r.RemoteAddr`. If the peer is not trusted: `(peer, r.TLS != nil)`.
- If the peer is trusted: join all `X-Forwarded-For` values, split on commas,
  trim. Walk from the right, skip addresses that are themselves trusted
  proxies, and take the first other valid address. If none, use the peer.
  `https` is true when the last `X-Forwarded-Proto` value is `https`.
- Use the result, from 1.5 on, for the throttle key, the client address in log
  lines and the cookie's `Secure` attribute. Never for an access decision: no
  address, including the proxy's, replaces a token. In this step it is not yet
  called by any handler.
- The WebSocket same-origin check of 1.5 needs nothing from here: Caddy keeps
  the `Host` header, and the browser's `Origin` is `https://<name>`.
- Redirects use relative locations and need nothing either.

Caddy needs no special setting: its `reverse_proxy` sets `X-Forwarded-For` and
`X-Forwarded-Proto` itself and discards values sent by clients, unless those
clients are listed in Caddy's own `trusted_proxies`, which is not used here.
If Caddy runs on the Video machine, the trusted address is `127.0.0.1`, which
any program on that machine can use: acceptable on a trusted machine.

### 1.5 HTTP guard and pages (`internal/httpServer/auth.go`)

`SetAccess(*access.Service)` stores the service in a package variable, as the
package already does for its other state. `main.go` calls it with the service
opened in 1.3, before `replays.BuildUI` (the server is started later, from the
Replays UI). With no service set (tests of other packages, or a nil service),
the guard behaves as Open.

Credential extraction, used by every guarded route:

- Bearer token from `Authorization`; `token` query parameter. Two `token`
  parameters, or a header and a query parameter that differ, are `Invalid`.
- An explicit token, if present, is the only credential considered: if it
  fails, the request is refused even with a valid cookie.
- Otherwise the session cookie named `CookieName()`.
- On success the guard puts the matched `access.Viewer` (empty in Open state)
  in the request context, for the cookie, the WebSocket and the logs.

Route wrapping in `registerReplayRoutes`:

| Route | Mode | On refusal |
| --- | --- | --- |
| `GET /` | page | 401, sign-in form |
| `POST /auth/token` (new) | sign-in | 401, sign-in form |
| `POST /auth/signout` (new) | sign-out | 403 on a foreign `Origin` only |
| `/api/sessions`, `/api/sessions/{session}/lifts`, `/api/replay-state` | api | 401 |
| `/replay/{camera}`, `/replay/{camera}.mp4` | api | 401 |
| `/videos/*` | api | 401 |
| `/ws` | ws | 401 before upgrade |
| `/static/*` | not guarded | - |

Rules:

- **Open state**: pass every request through unchanged, CORS included, so open
  mode behaves exactly as today. Exception: a `GET /?token=` is redirected to
  the same URL without `token`, so the token does not stay in the address bar.
- **OPTIONS** on guarded routes while protected or unavailable: answer 204 in
  the guard with the CORS headers and never call the handler. `http.FileServer`
  would otherwise serve the file body for OPTIONS without authentication. In
  Open state, pass OPTIONS through unchanged as with other requests.
- **page** mode, explicit valid token: issue a session for that token's `id`,
  set the cookie, then 303 to the request URL without `token`, other
  parameters kept. Cookie valid with no explicit token: serve the page.
- **ws** mode: an explicit valid `?token=` is accepted from any origin (the
  Tracker connects from another port). A cookie alone is accepted only if the
  `Origin` header's host equals `r.Host`. Without that check, a page on another
  port of the same host would get the cookie sent on its WebSocket handshake.
  `Host` is preserved by a router port forward and by Caddy, so no origin list
  needs to be configured.
- **api** mode: Bearer, query or cookie as above; no redirect.
- `ServiceUnavailable`: 503 `Replays access configuration is unreadable`.

`POST /auth/token` (sign-in):

- Body limited with `http.MaxBytesReader` to 1 KiB; fields `token` and `next`.
  A `username` field may be present (see the sign-in page below) and is ignored.
- If an `Origin` header is present, its host must equal `r.Host`.
- `next` must start with `/`, must not start with `//`, and must not contain
  `\`; otherwise use `/`. This prevents an open redirect.
- Success: issue a session for the matched token, set the cookie, 303 to
  `next`. Failure: 401 with the form and a generic message ("Token not
  accepted. It may be wrong or expired."), the same for wrong, expired and
  revoked. The submitted value is never echoed or logged. Throttled as below.
- In Open state: 303 to `next`, no cookie.

`POST /auth/signout`:

- If an `Origin` header is present, its host must equal `r.Host`; otherwise 403.
- Clear the cookie: same name, `Path` and attributes, empty value, `Max-Age=0`.
- 303 to `/`, which shows the sign-in form while access is protected.
- The token, and copies of the session elsewhere, stay valid: the cookie is a
  signed value, not a server record. Ending a holder's access means revoking
  or regenerating its token in the desktop UI.

Cookie attributes: `HttpOnly`, `Path=/`, no `Domain` (host-only),
`SameSite=Lax`, `Expires` and `Max-Age` equal to the expiry of the token used
to sign in, and
`Secure` when the browser's request was HTTPS: `r.TLS != nil`, or a request from
the trusted proxy with `X-Forwarded-Proto: https` (1.4). Through Caddy the
cookie is therefore `Secure`; on the LAN over plain HTTP it is not. The name
includes `instanceId`: each platform already has its own host name, but
cookies ignore ports, and a deployment using one address with a port per
platform would otherwise have its instances overwrite each other's cookie.

Responses:

- API and media refusals: 401, `WWW-Authenticate: Bearer realm="owlcms-replays"`,
  plain text body. No refusal body names the reason beyond missing, invalid,
  expired or unavailable.
- Protected responses, refusals and redirects get `Cache-Control: private, no-store`.
  Wrap the `/videos/` file server and `listFilesHandler` to add it
  (`setReplayAPIHeaders` and `handleReplay` already set no-store).
- Browsing page and sign-in form get `Referrer-Policy: same-origin`: no
  referrer is sent to other sites. Not `no-referrer`: with it, browsers send
  `Origin: null` on the sign-in and sign-out form posts, and the same-origin
  check refuses them. "Same origin" is the page's own address
  (`https://a.replays.example.org`), wherever the broadcaster is.
- CORS: keep `Access-Control-Allow-Origin: *` for the API routes and add
  `Authorization` to `Access-Control-Allow-Headers`. Never send
  `Access-Control-Allow-Credentials`. With `*` and no credentials flag, a
  browser never attaches the cookie to a cross-origin `fetch`, so the wildcard
  exposes nothing protected: a cross-origin page reads data only if it
  supplies the token itself, which is how the Tracker works.

Throttling, per client address as determined in 1.4 (the broadcaster's own
address when the request comes through Caddy):

- Count only explicit credentials that fail (`Invalid`, `ExpiredToken`) and
  failed form posts. A missing credential is not counted: every browser
  arrives without one.
- 20 failures within 60 s → 429 with `Retry-After: 60` for 60 s for that address.
- Keep at most 1024 addresses; drop entries older than 2 minutes.
- Log one line per address per minute: client address, route, reason; never
  the token.
- With 72-bit tokens, throttling limits noise and log volume; it is not what
  stops guessing. Counting per broadcaster keeps one misbehaving client from
  blocking the others, which a single count for Caddy's address would do.

Access log, by token name (names are not secret): one line per sign-in
(`page` with token, form post) and per accepted `/ws` connection, with the
client address and the token's name, e.g. `replays access: RAI signed in from
203.0.113.7`. Not one line per media request. This shows who is using which
token before revoking one.

#### Browsing page and sign-in page

- `internal/httpServer/templates/videolist.html`:
  - WebSocket URL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`.
  - On close, `fetch('/api/replay-state', {cache: 'no-store'})`; if 401,
    `location.reload()` (which shows the sign-in form), otherwise reconnect
    after 1 s as today.
  - No token anywhere in the template: links and navigation rely on the cookie.
  - When access is protected, a **Sign out** button at the top right of the
    page, in a small form posting to `/auth/signout`. Its tooltip (`title`)
    is "Signed in as <token name>, valid until <local date and time>"; its
    `aria-label` carries the same text for screen readers. New `TemplateData`
    fields: `Protected bool`, `AccessName string`, `AccessExpires string`, from the
    `access.Viewer` in the request context. Not shown in Open state. The page
    reached with an explicit token (not a cookie) is never rendered: it
    redirects first (see the page rules above).

New template `internal/httpServer/templates/token.html` (sign-in page), plain
HTML, no script, `/static/css/styles.css`; the existing
`template.ParseFS(templateFiles, "templates/*.html")` picks it up:

```html
<h1>Replays{{if .Platform}} - Platform {{.Platform}}{{end}}</h1>
<form method="post" action="/auth/token">
  <input type="hidden" name="username" value="replays-{{.Platform}}" autocomplete="username">
  <label for="token">Access token</label>
  <input id="token" name="token" type="password" autocomplete="current-password"
         required maxlength="128" autofocus>
  <input type="hidden" name="next" value="{{.Next}}">
  <button type="submit">Sign in</button>
  {{if .Error}}<p class="error">Token not accepted. It may be wrong or expired.</p>{{end}}
</form>
```

- The hidden `username` (`replays-A`) lets password managers file the token
  under the right platform; the server ignores it.
- `Next` is the requested page with its parameters (`/?session=3&sortBy=time`),
  built by the server from `r.URL` with `token` removed. The template engine
  escapes it.
- The page shows only the platform name: no session list, status or video.
- Same headers as the browsing page: `Cache-Control: private, no-store`,
  `Referrer-Policy: same-origin`.

### 1.6 Tracker clients (`owlcms/tracker`)

Two Tracker components call Video:

- the jury replays plugin, which runs in the browser (page served by the
  Tracker, so Video is another origin);
- the OBS automation (`helpers-core.js`), which runs in the Tracker server
  (Node: `$lib/server`, `ws` package) and hands replay URLs to OBS.

`src/plugins/jury/replays/helpers.client.js` (browser). The replays server URL
comes from the Tracker landing page or `?replaysBaseUrl=`. Operators enter
`http://host:8091/?token=XXXX`. Today `buildReplayStateUrl`,
`buildReplaySessionsUrl`, `buildReplaySessionLiftsUrl` and `buildStatusSocketUrl`
clear `url.search`, which would drop the token. Change them, and
`buildReplayUrl` and `buildExactReplayUrl`, to keep the `token` parameter of the
configured URL and drop everything else. One helper, `replayTokenFrom(baseUrl)`,
used by all six builders. Requests use the query parameter, not a header: no
CORS preflight, and a browser WebSocket cannot carry a header.

`src/plugins/OBS/shared/helpers-core.js` (Tracker server), with a new option
in the `config.js` of the `OBS/ledwall` and `OBS/streaming` plugins. The
setting today is a bare `replayHost` (`host:port`). Add an optional
**Replays token** option and append `?token=` to:

- the `/api/replay-state` fetches (`probeReplaySystemReachability`, `fetchReplayInputUrls`);
- the `/ws` URL (today `ws://${host}/ws`);
- the media URLs built by `absoluteReplayUrl` and handed to OBS: OBS fetches
  them itself and can only use the query parameter.

Use the query parameter for the fetches and the WebSocket too, so one URL form
serves all three.

Also accept a full `https://name` in `replayHost` and derive `wss://` for the
WebSocket. Today the helper prefixes `http://` and hard-codes `ws://`, which
cannot reach Video through Caddy, for example a broadcaster running these
plugins outside the venue.

Both components:

- Never log a URL that contains the token: redact `token=` before logging.
  Browser: `logger` and `/api/client-log`. Server: `logger` calls, including
  the "Pinned replay URLs" line in `helpers-core.js`.
- On a 401 from Video, show "Replays token missing, wrong or expired" instead
  of "not reachable".
- Without a token configured, behaviour is unchanged.

Run `npm test` (vitest) in the Tracker repository for the tests of 1.7.

### 1.7 Tests

Video: `go test ./...`. Use `httptest`, an injected clock and a deterministic
random reader. No camera, FFmpeg or network is needed.

`internal/config` (1.1):

- `TestSecurityDir`: `VIDEO_SECURITYDIR` wins; with `VIDEO_CONTROLPANEL_DIR`
  set, the parent of `ConfigDir`; otherwise `ConfigDir`; `VIDEO_CONFIGDIR`
  alone has no effect.

`internal/access` (1.2):

- `TestGeneratedTokenFormat`: length 12, alphabet only, distinct values.
- `TestViewerStates`: Open accepts anything; after Add, the token verifies and a
  wrong or oversize one does not; expired at exactly `expiresAt`; Revoke and
  Regenerate refuse the old value while another token remains; revoking the
  last token returns to Open, as does Open access.
- `TestSeveralViewerTokens`: three tokens all verify and return their own
  name; revoking one leaves the other two valid; regenerating one keeps its
  name with a new `id`.
- `TestViewerNames`: trimmed; empty, too long, invalid characters and
  case-insensitive duplicates refused; 51st token refused.
- `TestPersistenceAcrossReopen`: add two tokens, `Open` again, both verify; file
  contains no token value, mode 0600 (not on Windows).
- `TestCorruptFileIsUnavailable`: bad JSON, wrong version, duplicate ids or
  names give `Unavailable`; Add replaces the file, Revoke is
  refused.
- `TestFailedWriteKeepsState`: read-only directory; Add and Revoke fail, previous
  tokens still verify.
- `TestSessionCookie`: valid, returns the token's name; tampered signature, id
  and format refused; refused after that token's expiry, Revoke or Regenerate
  while another token remains, and after Open access followed by Add with the
  same name. Revoking or regenerating one token does not affect other tokens.
- `TestOnChangeAtExpiry`: with two tokens, the callback fires at each expiry
  instant in turn.

Desktop UI (1.3): no automated test (Fyne); `go build` and the manual check of
the step table. Unit-test the `publicUrl` validation if it is a separate
function.

`internal/httpServer`, trusted proxy (1.4):

- `TestClientOf`: untrusted peer with forged `X-Forwarded-For` and
  `X-Forwarded-Proto` keeps its own address and plain HTTP; trusted peer gives
  the rightmost untrusted address and HTTPS; invalid entries skipped; CIDR
  entries; invalid configuration entries ignored.

`internal/httpServer`, guard and pages (1.5; router built with `newRouter`):

- `TestOpenModeUnchanged`: no token configured; every route answers as today,
  CORS `*` included.
- `TestProtectedRoutesRefuseWithout`: each guarded route, GET and HEAD, gives
  401; `/` returns the form; `/static/css/styles.css` still 200.
- `TestBearerAndQueryAccepted`: `/api/sessions`, `/api/replay-state`,
  `/replay/1`, `/replay/1.mp4` and `/videos/...` with header or query when a
  token is generated, and without a token in Open state; Range on `/videos/...`
  returns 206. This is the only acceptance check required for OBS media sources
  and the OBS submodules.
- `TestConflictingCredentials`: header and query differ, duplicate `token`,
  invalid explicit token plus valid cookie: all 401.
- `TestBrowserTokenExchange`: `/?token=&session=3` gives 303 to `/?session=3`
  with Set-Cookie (HttpOnly, SameSite=Lax, no Domain, expiry equal to the
  token's); follow-up with the cookie gives 200; response bodies never contain
  the token.
- `TestTokenFormPost`: success 303 to `next`; `next=//evil` and `next=https://evil`
  go to `/`; wrong token 401 without echo; foreign `Origin` refused.
- `TestSignInFormKeepsBrowserOrigin`: the sign-in page is served with
  `Referrer-Policy: same-origin`; a post from the page's own origin signs in;
  another site's origin and `Origin: null` are refused.
- `TestSignInPage`: 401 page contains the masked `token` field with
  `autocomplete="current-password"`, the `username` and `next` fields, the
  platform name, and no session list, status or video link; `next` keeps the
  query parameters except `token`.
- `TestSignOut`: clears the cookie (`Max-Age=0`, same name and path); the next
  `GET /` shows the sign-in form; the token itself still works; foreign
  `Origin` refused.
- `TestBrowsingPageShowsSignOutWithSessionTooltip`: protected page shows the
  sign-out button with the token's name and expiry as its tooltip; Open state
  shows no button.
- `TestOptionsNeverServesContent`: OPTIONS on `/videos/...` and `/replay/1`
  gives 204 with no body.
- `TestWebSocketAuthorization` (`httptest.NewServer` with gorilla's client):
  query token from a foreign origin accepted; cookie with a foreign origin
  refused; cookie with same origin accepted; after Revoke, a new connection
  with that token or its cookie is refused.
- `TestThrottle`: 20 bad tokens give 429; missing credentials never do.
- `TestThrottlePerClientBehindProxy`: two clients through the trusted proxy;
  one is throttled, the other is not.
- `TestSecureCookieBehindProxy`: token exchange through the trusted proxy with
  `X-Forwarded-Proto: https` sets `Secure`; the same headers from an untrusted
  peer do not.
- `TestNoTokenInLogs`: capture the loggers through a bad token, a good token and
  a form post; the token text is absent; the sign-in line has the token's
  name.

Tracker (1.6), `npm test`: unit tests for the six URL builders (token kept,
other parameters dropped) in a new
`src/plugins/jury/replays/tests/helpers.client.test.js`; for `https`/`wss`
derivation, token appending and log redaction in the OBS helper, in
`src/plugins/OBS/shared/tests/replay-url.test.js` (no live OBS testing is done
for the OBS submodules).

### 1.8 Manual acceptance

OBS media sources and the Tracker OBS submodules are not tested manually; they
are covered by the automated token tests in 1.7 (`TestHandleReplayAndStateAcceptGeneratedToken`
and `replay-url.test.js`). External broadcaster testing through Caddy is
deferred to [230](230-VIDEO_ACCESS_DEFERRED.md).

1. Control Panel launch: add tokens "OBS", "Tracker" and "RAI", update Video
   to another version, restart. Tokens and browser sessions still valid;
   `video-access.json` is in `owlcms-video`, not in a version directory.
2. LAN browser (`http://<laptop>:8091`):
   - open `/`, get the sign-in form, type the "RAI" token, get the replays page
     with **Sign out** at the top right, whose tooltip reads "Signed in as RAI"
     and the expiry; let the password manager save
     the token; close and reopen the browser: still signed in; Sign out: back
     to the form;
   - open `/?token=<RAI token>`: replays page, token gone from the address bar;
     session selector, sort and video links work; live status updates arrive.
3. Tracker jury replays page works with the "Tracker" token's URL, and reports
   a token problem when it is wrong.
4. Two broadcasters, "RAI" and "EBU", both signed in with pages open, and the
   Tracker jury replays page running. Revoke "RAI": RAI's next page load, video
   or API request is refused and the page shows the sign-in form; EBU and the
   Tracker continue uninterrupted. Regenerate "EBU": same for EBU only; the new
   value works. Let a short-lived test token expire (test build with a reduced
   lifetime): only its holder is refused. Revoke all: the last revoke warns
   that access will open, then everything is accessible without a token.
5. Corrupt `video-access.json` by hand: everything refused with 503; the
   dialog reports it; Add token recovers after confirmation.

Build the executable (`go build -o video .`; `video.exe` on Windows) and repeat
item 1 on Windows, macOS and Linux: cross-compiling does not test permissions
and paths.

## 3. Milestone 2 - Documentation

- Video `README.md` and `OBS.md`: enabling access tokens, one named token per
  broadcaster and one each for OBS and the Tracker, URL forms for each, 21-day
  expiry, Regenerate, Revoke and Open access, where the
  token file lives, the Windows standalone caveat of 1.2, `trustedProxies`
  and `publicUrl`.
- A one-page broadcaster guide per event, the three ways of section 1
  ("External broadcasters"): sign in with the token, the sign-in link, the
  token on each request.
- External access for scenario 1:
  - DNS: one name per platform, CNAME to the router's dynamic DNS name.
  - Router: forward 80 and 443 to the Caddy machine; do not forward 8091;
    disable remote access to the router's own admin page on those ports.
  - Caddy: one site block per name, refusing `/api/cameras/config` and
    proxying to the laptop's `:8091`; WebSockets need nothing extra; access-log
    configuration that drops query strings. Example:

    ```text
    a.replays.example.org {
        respond /api/cameras/config 404
        reverse_proxy 192.168.1.50:8091
    }
    b.replays.example.org {
        respond /api/cameras/config 404
        reverse_proxy 192.168.1.51:8091
    }
    ```

  - Each laptop: `trustedProxies` set to Caddy's address, `publicUrl` set to its
    name.
  - Certificates: obtained and renewed by Caddy through port 80; needs
    internet access at issuance and renewal.
- A direct router forward to 8091, without Caddy: possible, but the token
  travels in clear outside and Video sees no proxy.
- Release notes in Video and Tracker.

Possible later work, not planned: explicit Windows ACLs for standalone
installations, trimming the Cameras configuration response.

## 4. Not covered

- Closing live connections on revoke or expiry: a `/ws` connection already
  open stays open until it disconnects. It carries only notifications with
  video paths; fetching them, and reconnecting, are refused.
- Different access per token: all viewer tokens grant the same access. No
  token limited to some cameras, sessions or dates.
- Server-side session list or sign-out of one browser among several using the
  same token: sessions are signed values; revoking or regenerating the token
  ends all of them.
- Authentication in Caddy (`basic_auth`, header or query matchers,
  `forward_auth`, plugins): not used. Inside clients reach Video directly, so
  Video must check tokens anyway; Caddy only carries the outside traffic.
- A path per platform behind one name (`/platformA/`): Video emits
  root-relative URLs; not needed with one name per platform.
