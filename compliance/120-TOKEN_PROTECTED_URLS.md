# Token-Protected URLs

Status: agreed 2026-10-25, revised 2026-10-28. The competition export URL is
implemented; the Vaadin display pages and the remaining servlets are not.
Prerequisite: [account token generation](done/110-ACCOUNT_TOKENS.md).

## Principles

- A token is a credential, not a permission. It gives at most its owner's current
  rights, re-checked on every protected request, so revocation, account disabling
  and role changes take effect at the next request.
- A URL carries the token as `?token=<value>`. Plain servlets also accept
  `Authorization: Bearer <value>`. A Vaadin page only reads the URL, because its
  later requests do not carry a custom header.
- Loopback, private-network and backdoor addresses neither replace a token nor add
  to its rights.
- Failed attempts are throttled per address. Refusals are audited with the owner and
  the non-secret token id when known. The token itself is never logged, displayed
  or echoed, and is redacted from URL diagnostics.
- Copied URLs and OBS scene collections are credentials.
  Broadcaster-facing access outside a trusted local deployment requires HTTPS.

## Credentials

| Credential | Mode | Proves | Used by |
| --- | --- | --- | --- |
| Account token | Accounts | An account; its current roles decide | Display pages, plain servlets |
| Display PIN, passed as `token` | PIN | Knowledge of the display PIN (weak shared secret) | Display pages |
| Current audit `runId` | Both | The caller runs on the same machine | `/controlpanel/stop` only |

## Roles

- DISPLAYS is for the competition venue's own screens. STREAMING is a new base role
  for what OBS or another streaming client may show. Display output pages and the
  display launchers name STREAMING where they name EXTERNAL today, except the
  venue-only pages (athlete-facing attempt and decision boards, jury scoreboard).
- EXTERNAL stays, so no stored data changes. It becomes a composite role that expands
  to RESULTS and STREAMING, for now. Whoever needs results, exports included, uses an
  EXTERNAL account. Expansion is one level: EXTERNAL does not receive DISPLAYS through
  RESULTS.
- A STREAMING account serves both uses. Logged in with a password, it browses the
  Displays navigation page to copy URLs. Its token opens the display pages only.
- Any named account may have a token, including the competition supervisor's.
  COMPETITION_SUPERVISOR expands to every base role except ADMIN_PAGES and STREAMING;
  it reaches the display pages through DISPLAYS, so its token reaches every
  token-accessible page and servlet. ADMIN is needed only for the admin pages
  (administration, backups, audit checks, accounts, settings, session import,
  testing), which no token reaches.
- The built-in admin is a bootstrap and recovery account. It has no token, and
  ADMIN_PAGES is never reached through a token.

### Adding STREAMING

- `Role.STREAMING`: grantable, base, not platform-scopable, not lockable.
- `Role.EXTERNAL`: still grantable, no longer a base role; `expand()` returns RESULTS
  and STREAMING. ADMIN gains STREAMING automatically, since it expands to every base
  role.
- Every `@RequiresRole` that lists EXTERNAL lists STREAMING instead, except the
  athlete-facing attempt and decision boards, which drop EXTERNAL and keep DISPLAYS
  only. EXTERNAL keeps its other access through its expansion. `AccountLandingPolicy` treats STREAMING like
  EXTERNAL (Home page).
- The generic accounts follow the roles: a `streaming` account is created next to the
  existing `external` one.
- Labels: a new key `Access.Role.STREAMING`, and a revised `Access.Role.EXTERNAL`
  label now that it includes results, through the translation process.
- Tests that use EXTERNAL as an account without results access (for example the
  export refusal in `TokenAuthenticatorTest`) use STREAMING instead.

## Plain servlets

| URL | Roles (accounts mode) | PIN mode | State |
| --- | --- | --- | --- |
| `/competition/export`, `/competition/export/json/1`, `/competition/export/json/2`, `/competition/export/sbde` | RESULTS (also through EXTERNAL, COMPETITION_DIRECTOR and COMPETITION_SUPERVISOR). STREAMING refused. | Local network or backdoor, as before | Implemented |
| `/competition/h2` (GET, HEAD) | COMPETITION_SUPERVISOR. The file holds password and token hashes. | Local network or backdoor, as before | To do |
| `/simulation/*` (GET, HEAD, POST) | PREPARATION (also through COMPETITION_SUPERVISOR). The form carries the token on its POSTs. | Localhost or backdoor, as before | To do |
| `/controlpanel/stop` (POST) | None: current audit `runId` | Same | To do |
| `/mqtt` | Out of scope: the MQTT broker authenticates its clients | Same | — |
| `/local/...` (logos, flags) | Public static assets | Same | — |

Responses: 401 with `WWW-Authenticate` for a missing or invalid token, 403 for a
valid token without the role, 429 with `Retry-After` when throttled. GET and HEAD
behave the same.

Full audit backups are downloaded from an admin page; no token URL is planned for them.

### Competition export (implemented)

- Refusals are audited as `auth.token-refused` (reason, path, owner and token id when
  known); successful exports as `export.json` with `device=token id=<id>`.
- A revoked token or disabled account is refused immediately.

### `/controlpanel/stop`

Stops the process. Its callers, the control panel and the updater
(`playwright/UpdateCheck`), run on the same machine as OWLCMS and have no account.
The only assertion needed is that the caller can read files on that machine. The
socket peer or forwarded address cannot prove it, because a local reverse proxy such
as Caddy makes a remote request appear to come from localhost.

- The caller reads the `runId` of the current server run from an `audit.open` entry
  and sends it with the POST. Every rolled audit file of the run repeats the same
  `runId`, so rolling does not matter.
- OWLCMS accepts only its own current `runId`. A value from an earlier run is refused.
  The backdoor list does not bypass the check.
- The accepted stop is audited with `cause=control-panel`; the value is never logged.

## Vaadin pages

A token never logs a browser in. It opens individual display pages and nothing else.
The same account used with a password still reaches every page its roles allow.

| Pages | Roles (`@RequiresRole`) | `@TokenAccessible` |
| --- | --- | --- |
| `displays/*` output pages, platform-bound: warmup and public scoreboards, ranking and lifting orders, multi-ranks, start list, medals, current athlete, WOD, public-facing attempt and decision boards | DISPLAYS, STREAMING | Yes |
| `displays/rankings`, `topsinclair`, `topteams`, `topteamsinclair` | DISPLAYS, STREAMING | Yes |
| `displays/juryDecisions` | DISPLAYS, JURY, STREAMING | Yes |
| Venue-only: `displays/athleteFacingAttempt`, `displays/athleteFacingDecision` | DISPLAYS | Yes |
| Venue-only: `displays/juryScoreboard` | DISPLAYS, JURY | Yes |
| `displays/monitor`, `displays/notifications` | Public (kept for now) | No |
| `displays` and `video` navigation pages, `mobile/scoreboards` | DISPLAYS, STREAMING | No |
| Officials, results, preparation, records and admin pages | As today | No |

COMPETITION_SUPERVISOR expands to DISPLAYS, so its token opens the same pages as a
DISPLAYS token. EXTERNAL expands to STREAMING, so its token opens the same pages as a
STREAMING token.

### Where the check runs

Verified against the source and compiled bytecode of the installed
`flow-server-25.2.8.jar`. For every navigation in a tab (first load, reload, link,
`UI.navigate`, back/forward), Vaadin runs:

1. The UI's `BeforeEnterListener`s. `AccessControlListener` is one of them, attached
   to every new UI by `AppShell.serviceInit`. No page instance exists yet: the
   listener sees only the destination class and the URL.
2. Page creation, or reuse of the existing instance.
3. The page's `setParameter()`, which runs the existing URL processing.
4. The page's own `BeforeEnterObserver.beforeEnter()`, if any.

A refusal in step 1 stops the navigation: the page is not created and its
`setParameter()` never runs. This holds for every route, including pages whose
`setParameter()` bypasses the shared parameter readers (for example `RefContent`).
The published Vaadin lifecycle documentation describes a different order; re-check
the bytecode when upgrading Vaadin.

Clicks and push updates on an open page, `history.replaceState`, plain servlets and
static files are not navigations and do not go through the listener.

`@TokenAccessible` is therefore a runtime annotation on the class, read by `PageRule`
like `@RequiresRole`. It is placed explicitly on each display output page, so a new
page is not exposed to tokens by accident. Launchers do not carry it. On venue-only
pages, the roles decide: a DISPLAYS token opens them, a STREAMING token is refused
(403).

### Accounts mode

`AccessControlListener` does the following before any logic that saves the requested
URL or forwards to the login page:

1. Resolve the destination's `PageRule`. Feature switches and public pages behave as
   today; a public page ignores `token`.
2. No `token` in the URL and no token binding on the tab: the existing password logic
   applies unchanged (login page, then back to the requested page).
3. `token` present and the page not `@TokenAccessible`: 403, without checking the
   token and without saving the URL.
4. Read the inputs, with no side effects:
   - exactly one nonblank `token` value;
   - on a platform-bound page, the platform: `fop` when present (exactly one value,
     URL-decoded, must exist), otherwise the only platform if there is one,
     otherwise 403. Never the session or default platform.
5. Authenticate: unknown or revoked token, or disabled account, gives 401; too many
   failures from the address give 429. Reuse `TokenAuthenticator` and its throttle.
6. Authorize: build the owner's principal from its current grants and call
   `AccessPolicy.canOpen(principal, rule, platform)`; 403 if refused.
7. Bind the tab: store the account id and token id on the UI (not the raw token, and
   not in the HTTP session), audit the binding once, and let the navigation continue.

A later navigation in a bound tab without `token` in the URL re-reads the account,
requires the bound token id to still exist, the account to be enabled and the page to
be `@TokenAccessible`, then repeats step 6. A bound tab never falls back to password
or PIN authentication.

The binding belongs to the tab, so a token tab and a password tab in the same browser
keep their own identities; the password login stays in the HTTP session as today.
Code that reads the principal on a display page (for example
`AccessUi.displayInputAllowed`) must see the tab's token principal.

### PIN mode

Accounts are dormant, so `token` carries the display PIN, a weak shared secret that
keeps casual users on the venue network away from the displays. The display PIN in a
URL is equivalent to a DISPLAYS token: whoever has a streaming URL in PIN mode can
also open the venue-only display pages. This is accepted. PIN mode is a transition
measure; installations that need real separation use accounts mode.

- No display PIN configured: displays are open as today; `token` is ignored.
- Display PIN configured, `token` present, page `@TokenAccessible`: compare with the
  display PIN in constant time.
  - Match: same effect as entering the PIN on the display login page (display
    authenticated in the HTTP session), then continue.
  - No match: 401, throttled per address like the PIN login page.
- `token` on a page that is not `@TokenAccessible`: ignored; the existing PIN login
  applies. The URL PIN never opens a navigation page or an officials page. It does
  open the venue-only display pages, since the display PIN stands for DISPLAYS.
- A configured display IP list still applies.

### After the check

The page's existing `setParameter()` chain runs unchanged. `public`, `video`, `dark`,
`leaders`, group and the other display parameters are processed as today and never
affect authorization. In a token tab, the reader uses the platform that was
authorized, never its own session or default fallback.

URL rewriting keeps unknown parameters, so `token` stays in the OBS URL and a reload
re-authenticates. The diagnostics that print URLs or query maps (the debug log in
`FOPParametersReader.doUpdateUrlLocation`, the FOP-mismatch error in
`URLUtils.replaceState`) redact `token`.

### Refusals

A refusal shows only the status text ("Unauthorized", "Forbidden", "Too Many
Requests"): no application layout, no buttons, no account details, no login redirect.
It works without an authenticated principal, unlike the current `denied` page. The
status code applies to the initial page request; a client-side navigation shows the
same text. Refusals are audited with `AuthAudit.tokenRefused`.

### Open displays

An open display keeps running after its token is revoked, its account disabled or its
roles changed, until its next navigation or reload. Open displays are not
disconnected on revocation.

## Replays

Replays, in the video module, does not check OWLCMS account tokens. It gets its own
token, generated in the video application: see [330](330-REPLAYS_TOKEN.md).

## Later work

- Moving some admin pages from ADMIN_PAGES to COMPETITION_SUPERVISOR. This changes
  page roles only; tokens still do not reach those pages unless they are made
  `@TokenAccessible`. Decided: the backups page (`admin/backup`) adds
  COMPETITION_SUPERVISOR to its roles, gets a Backup button on the Competition
  Supervisor page (`lifting/competition-director`), and drops the same-machine
  restriction (`AdminView.checkLocalAccess`), which backups do not need.

## Acceptance

Test allowed and refused account and platform combinations through actual OBS and
export URLs, in both modes. Test restart of OBS and of OWLCMS without interactive
login, revocation and role changes, servlet GET, HEAD and range requests, throttling
and actual credential redaction. Check that a
refused navigation never constructs the page or calls its `setParameter()`, including
`RefContent`, for direct loads, client-side navigations and reused pages. IP or
forwarded-header claims must not bypass authentication.
