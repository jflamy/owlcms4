# Role-Based Access Control — Detailed Design

Status: design approved for implementation. All decisions in §17 are resolved.
Audience: the implementing agent. Read §18 (repository constraints) before writing code.

Line numbers are approximate (they drift); search by method name.

---

## 1. Goals and non-goals

Goals
- Named user accounts, each holding one or more **role grants**; each grant has a **scope**: one platform or all platforms.
- Fixed role list (enum), including **composite roles** that expand to several base roles.
- A single, global, **deny-by-default** access check on every navigation.
- Hide navigation buttons, drawer tabs and a few in-page functions the user cannot use.
- In accounts mode, the **operating platform is chosen at login** and cannot be changed live.
- Global option: officials login is either **PIN** (today's behaviour) or **ACCOUNTS**.

Non-goals (this iteration)
- Spring Security, JAAS, servlet-container security.
- Dynamic (session-scoped) route registration.
- Protecting servlets beyond today's IP rules (§13).
- Fine-grained in-page permissions beyond those listed in §10.3.
- Separate export/import of accounts.

---

## 2. Decisions already made

| Topic | Decision |
|---|---|
| Framework | Home-grown, no Spring Security. Global `BeforeEnterListener` + page annotations. |
| Roles | Fixed enum. Composite roles allowed. |
| Identity | Named accounts; an account may hold several grants. |
| Scope | Per grant: one platform or all platforms. |
| Platform at login | Accounts mode: operating platform chosen at login; no live change. |
| Display PIN | Not used in accounts mode; screens log in with an account holding DISPLAYS. |
| PLATFORM composite | ANNOUNCER + MARSHAL + TIMEKEEPER + TC + DISPLAYS. |
| RESULTS | Results pages + DISPLAYS (all platforms). Results team opens both medal/ranking screens **and** platform scoreboards. |
| Documents page | Shared: REGISTRATION, WEIGHIN, RESULTS (may be split later). |
| Athlete card | Shared: REGISTRATION, WEIGHIN. |
| Break management | ANNOUNCER and TIMEKEEPER; MARSHAL keeps its Pause button (marshal break, used to signal problems). |
| Medal ceremony page | ANNOUNCER and TIMEKEEPER. |
| ADMIN | Always all platforms, never locked; keeps the live platform selectors. |
| PIN vs accounts | Only the officials PIN is replaced. Officials IP list and backdoor keep working in both modes. |
| `OWLCMS_PIN` | Not active in accounts mode. Emergency access = backdoor list set by environment (`OWLCMS_BACKDOOR`). |
| Display IP list | Superseded by display login in accounts mode (ignored). |
| Account management | Available in both modes, so all accounts are prepared before switching. Built-in `admin` account must have a password before accounts mode can be turned on. ADMIN resets any account's password. |
| Passwords | Minimum length 8, no character-class rules. |
| OBS monitors | `OBSMonitor` and `StreamingEventMonitor` disabled by default behind a new feature switch; when enabled they stay open without login (read-only). |
| Session import | ADMIN only. |
| Login fields | PIN and password use `PasswordField` (reveal button kept); typed values are never logged. |

---

## 3. Current state (what gets replaced)

- `nui/shared/AuthorizationDispatch` (interface, `BeforeEnterObserver`): officials PIN check, backdoor, records-only mode routing. Inherited by every `OwlcmsContent` page and explicitly by `AdminView`, `RecordFederationComparisonReport`, `AthleteCard`, `RefContent`, `JuryMobileContent`, `JuryKeypadContent`.
- `nui/shared/RequireDisplayLogin` (interface): display PIN / display IP list check. Implemented by `BaseResults`, `AbstractAttemptBoard`, `WodBoard` (child components of display pages), `WarmupScoreboardPage`, `DisplayNavigationContent`, `VideoNavigationContent`.
- Coverage is opt-in: `OBSMonitor` (`displays/monitor`) and `StreamingEventMonitor` (`displays/notifications`) have no check today.
- Session flags in `OwlcmsSession`: `authenticated`, `displayAuthenticated` (`isAuthenticated()`, `computeDisplayAuthenticated()`, `setAuthenticated()`, `setDisplayAuthenticated()`), plus `requestedURL`/`queryParameters` used by `LoginView.redirect()`.
- `LoginView` (`login`): single PIN field, value-change listener, `redirect()`. `DisplayLoginView` (`displaylogin`) extends it.
- `AccessUtils`: `checkAuthenticated`, `checkDisplayAuthenticated`, `ipIsAllowedForOfficials`, `isIpAllowedForDisplay`, `checkBackdoor`, `getClientIp`, `isLocalhost`, `isLocalNetwork`, `encodePin`.
- `Config`: `pin`, `displayPin`, `ipAccessList`, `ipDisplayList`, backdoor list; `getParamPin()`, `getParamDisplayPin()`, `getParamAccessList()`, `getParamDisplayList()`, `getParamBackdoorList()` (env override via `StartupUtils.getStringParam`). Edited in `ConfigEditingFormFactory` (~336–352).

Security bug to fix while here: `LoginView` logs the typed PIN (`logger.debug("login input {}", value)`). Remove that log line.

---

## 4. Concepts

### 4.1 Access mode
`AccessMode { PIN, ACCOUNTS }` — global, stored in `Config`, env override `OWLCMS_ACCESSMODE` (§11).

### 4.2 Roles

Base roles (grant access to pages):

| Role | Platform-scopable | Lockable (§4.4) |
|---|---|---|
| ADMIN_PAGES (internal, only via ADMIN) | no | no |
| PREPARATION | no | no |
| REGISTRATION | no | no |
| WEIGHIN | no | no |
| RESULTS | no | no |
| ANNOUNCER | yes | yes |
| MARSHAL | yes | yes |
| TIMEKEEPER | yes | yes |
| TC | yes | yes |
| JURY | yes | yes |
| REFEREE | yes | yes |
| DISPLAYS | yes | **no** (default, see D-DISPLAYLOCK) |

Grantable roles (what the account editor offers) and their expansion:

| Grantable role | Expands to | Scope of expanded roles |
|---|---|---|
| ADMIN | every base role, including ADMIN_PAGES | all platforms |
| PREPARATION | PREPARATION | all |
| REGISTRATION | REGISTRATION | all |
| WEIGHIN | WEIGHIN | all |
| RESULTS | RESULTS, DISPLAYS | all |
| PLATFORM | ANNOUNCER, MARSHAL, TIMEKEEPER, TC, DISPLAYS | the grant's scope |
| ANNOUNCER, MARSHAL, TIMEKEEPER, TC, JURY, REFEREE, DISPLAYS | itself | the grant's scope |

Implementation: one enum `Role` with `boolean grantable`, `boolean platformScopable`, `boolean lockable`, and `Set<Role> expand()`. Expansion happens in exactly one place (`AccessPolicy`). Pages only ever reference **base** roles.

### 4.3 Scope
A grant's scope is either `null` (all platforms) or a platform **name**. Names, not ids, because competition import (`CompetitionData` / `CompetitionDataV2`) deletes and recreates `Platform` rows, which changes ids; names survive. Non-scopable roles always have scope `null` (editor enforces it).

### 4.4 Platform lock (accounts mode only)
- An account holding an **ADMIN** grant is never locked: no platform step at login, all platforms, live platform selectors as in PIN mode.
- Otherwise, at login, if the account's expanded roles include at least one **lockable** role, the user picks a **login platform** among the platforms covered by lockable grants (auto-selected if only one; step skipped if the competition has one platform).
- Lockable roles are then effective **only on the login platform**.
- Non-lockable roles are unaffected: DISPLAYS keeps its full scope (so the results team and the video operator can open scoreboards for any platform in their scope), and global roles have no platform.
- Changing the login platform = logout + login.

### 4.5 Principal
The authenticated subject stored in the session:
```
record Principal(AuthSource source, Long accountId, String username,
                 List<RoleGrant> grants, String loginPlatform /* nullable */)
enum AuthSource { ACCOUNT, OFFICIALS_PIN, DISPLAY_PIN, NO_PIN, BACKDOOR }
```
- PIN mode: officials PIN / `NO_PIN` / backdoor ⇒ grants `[ADMIN(all)]`, `loginPlatform = null`, **not locked** (live platform selectors keep working as today).
- PIN mode: display PIN (or implicit display access) ⇒ grants `[DISPLAYS(all)]`, not locked.
- Accounts mode: account grants; locked if `loginPlatform != null` (never for accounts with an ADMIN grant). Backdoor ⇒ `[ADMIN(all)]`, not locked.

---

## 5. Data model

New package `app.owlcms.data.account`.

`UserAccount` (`@Entity`, register in `JPAService.entityClassNames()` ~311, and add the standard `// must be listed in ...entityClassNames()` comment):

| Field | Type | Notes |
|---|---|---|
| id | Long | generated |
| username | String | unique, stored lower-case, trimmed; login is case-insensitive |
| displayName | String | shown in the layout header / denied page |
| passwordHash | String | format `pbkdf2-sha256$<iterations>$<b64salt>$<b64hash>` |
| enabled | boolean | disabled accounts cannot log in; active sessions are logged out at next navigation |
| grants | `List<RoleGrant>` | `@ElementCollection(fetch = EAGER)` |

`RoleGrant` (`@Embeddable`): `Role role` (`@Enumerated(STRING)`, grantable roles only), `String platformName` (null = all).

Built-in `admin` account:
- Seeded at startup if missing (username `admin`, grant `ADMIN`, no password, enabled).
- Cannot be deleted, disabled, renamed, or lose its ADMIN grant.
- Accounts mode cannot be turned on in the UI until it has a password (§11).
- An account without a password can never log in.

`UserAccountRepository`: follow the `CoachRepository` pattern (`JPAService.runInTransaction`). Methods: `findAll()`, `findByUsername(String)`, `save(UserAccount)`, `delete(UserAccount)`, `renamePlatform(String old, String new)`, `removePlatform(String name)`. Keep an in-memory cache (id → account) invalidated on every save/delete so per-navigation checks do not hit the database.

Platform lifecycle hooks (in the platform repository/editing code):
- Rename: call `UserAccountRepository.renamePlatform`.
- Delete: call `UserAccountRepository.removePlatform`.
- Grants naming a platform that no longer exists are ignored at runtime and shown with a warning in the account editor.

Import / reset:
- Accounts are included in `CompetitionData` / `CompetitionDataV2` exports as password hashes, never clear-text passwords.
- An import without an `accounts` property preserves the local account set. An import with that property replaces the account set with the imported accounts.
- `Config.accessMode` must not be changed by a competition import (preserve the current value, or exclude it from JSON with `@JsonIgnore` — check which mechanism `Config` import uses and follow it).

### 5.1 Password hashing
`PasswordHasher` (JDK only, no new dependency):
- `PBKDF2WithHmacSHA256`, 16-byte salt from `SecureRandom`, 32-byte key, iteration count constant (start at 310 000; must be a constant in one place, stored in the hash string so it can be raised later).
- Verify with `MessageDigest.isEqual`.
- Minimum password length 8; no character-class rules; any Unicode characters allowed (normalize with `Normalizer.Form.NFC` before hashing).
- Do **not** reuse `AccessUtils.encodePin` (single SHA-256, too fast for passwords).

---

## 6. Declaring page access

New package `app.owlcms.access` for all non-entity logic.

Annotations (runtime retention, on `@Route` classes):
- `@PublicPage` — no login required.
- `@AuthenticatedPage` — any logged-in principal.
- `@RequiresRole(value = { Role... }, platformBound = true|false)` — allowed if the principal has **any** listed base role; `platformBound = true` means the page operates on one platform and the platform check of §7.3 applies.

Every `@Route` class must carry exactly one of the three. A unit test enforces this (§16). Classes without `@Route` (Vaadin error views) are always allowed.

Additional gate: `@RequiresFeature(FeatureSwitch.X)` — when the switch is off, the listener reroutes to the not-found error (`event.rerouteToError(NotFoundException.class)`), whatever the principal. Used for the OBS monitors (§11.1).

### 6.1 Page table

`platform` column: ✓ = `platformBound = true`.

**Public / authenticated**

| Class | Route | Annotation |
|---|---|---|
| `nui.home.LoginView` | `login` | `@PublicPage` |
| `nui.home.DisplayLoginView` | `displaylogin` | `@PublicPage` (accounts mode: forwards to `login`) |
| `nui.home.AdaptiveHomeRedirect` | `` | `@PublicPage` (only forwards; targets are checked) |
| `nui.preparation.PublicRecordsContent` | `publicRecords` | `@PublicPage` |
| `nui.home.HomeNavigationContent` | `home` | `@AuthenticatedPage` |
| `nui.home.InfoNavigationContent` | `info` | `@AuthenticatedPage` |
| new `access.AccessDeniedView` | `denied` | `@AuthenticatedPage` |

**Preparation**

| Class | Route | Roles | platform |
|---|---|---|---|
| `PreparationNavigationContent` | `preparation` | PREPARATION, REGISTRATION, WEIGHIN, RESULTS | |
| `CompetitionContent` | `preparation/competition/:tab?` | PREPARATION | |
| `ConfigContent` | `preparation/config/:tab?` | ADMIN_PAGES | |
| `PlatformContent` | `preparation/platforms` | PREPARATION | |
| `SessionContent` | `preparation/sessions` | PREPARATION | |
| `RegistrationContent` | `preparation/athletes` | REGISTRATION | |
| `CoachContent` | `preparation/coaches` | REGISTRATION | |
| `TeamSelectionContent` | `preparation/teams` | REGISTRATION | |
| `TeamScheduleContent` | `preparation/team-schedule` | PREPARATION | |
| `AgeGroupContent` | `preparation/agegroup` | PREPARATION | |
| `ChampionshipsContent` | `preparation/championships` | PREPARATION | |
| `TechnicalOfficialContent` | `preparation/officials` | PREPARATION | |
| `RecordContent` | `preparation/records` | PREPARATION | |
| `RecordsConfigContent` | `preparation/recordsConfig` | PREPARATION | |
| `RecordsNavigationContent` | `records` | PREPARATION | |
| `RecordsPreparationNavigationContent` | `recordsPreparation` | PREPARATION | |
| `DocumentsContent` | `preparation/documents` | REGISTRATION, WEIGHIN, RESULTS | |
| `nui.lifting.WeighinContent` | `preparation/weighin` | WEIGHIN | |
| `displays.athletecard.AthleteCard` | `weighin/AthleteCard` | REGISTRATION, WEIGHIN | |
| new `AccountsContent` | `preparation/accounts` | ADMIN_PAGES | |

**Lifting**

| Class | Route | Roles | platform |
|---|---|---|---|
| `LiftingNavigationContent` | `lifting` | ANNOUNCER, MARSHAL, TIMEKEEPER, TC, JURY, REFEREE, WEIGHIN | |
| `AnnouncerContent` | `lifting/announcer` | ANNOUNCER | ✓ |
| `MarshallContent` | `lifting/marshall` | MARSHAL | ✓ |
| `TimekeeperContent` | `lifting/timekeeper` | TIMEKEEPER | ✓ |
| `TCContent` | `lifting/tc` | TC | ✓ |
| `JuryContent` | `lifting/jury` | JURY | ✓ |
| `MedalCeremonyContent` | `lifting/medalCeremony` | ANNOUNCER, TIMEKEEPER | ✓ |
| `WodkeeperContent` | `lifting/wodkeeper` | TIMEKEEPER | ✓ |
| `TestingContent` | `lifting/testing` | ADMIN_PAGES | ✓ |
| `nui.referee.RefContent` | `ref` | REFEREE | ✓ |
| `nui.referee.JuryMobileContent` | `jury` | JURY | ✓ |
| `nui.referee.JuryKeypadContent` | `jurykeypad` | JURY | ✓ |
| `nui.home.navigation.RefereeNavigationContent` | `mobile/refjury` | REFEREE, JURY | ✓ |
| `nui.home.navigation.JuryNavigationContent` | `mobile/juryhome` | JURY | ✓ |
| `nui.home.navigation.MobileScoreboardsNavigationContent` | `mobile/scoreboards` | DISPLAYS | ✓ |

**Displays** (all DISPLAYS unless noted)

| Class | Route | Extra roles | platform |
|---|---|---|---|
| `DisplayNavigationContent` | `displays` | | |
| `VideoNavigationContent` | `video=true` | | |
| `PublicFacingAttemptBoardPage` | `displays/attemptBoard` | | ✓ |
| `PublicFacingDecisionBoardPage` | `displays/publicFacingDecision` | | ✓ |
| `AthleteFacingAttemptBoardPage` | `displays/athleteFacingAttempt` | | ✓ |
| `AthleteFacingDecisionBoardPage` | `displays/athleteFacingDecision` | | ✓ |
| `CurrentAthletePage` | `displays/currentathlete` | | ✓ |
| `NCurrentAthletePage` | `displays/ncurrentathlete` | | ✓ (keep its existing `AccessDeniedException` logic) |
| `PublicNoLeadersPage` | `displays/publicSimple` | | ✓ |
| `PublicScoreboardPage` | `displays/publicScoreboard` | | ✓ |
| `WarmupNoLeadersPage` | `displays/resultsSimple` | | ✓ |
| `WarmupScoreboardPage` | `displays/resultsLeaders` | | ✓ |
| `PublicMultiRanksPage` | `displays/publicMultiRanks` | | ✓ |
| `WarmupMultiRanksPage` | `displays/multiRanks` | | ✓ |
| `PublicRankingOrderPage` | `displays/publicRankingOrder` | | ✓ |
| `WarmupRankingOrderPage` | `displays/resultsRankingOrder` | | ✓ |
| `WarmupLiftingOrderPage` | `displays/resultsLiftingOrder` | | ✓ |
| `PublicStartListPage` | `displays/publicStartList` | | ✓ |
| `MedalsPage` | `displays/resultsMedals` | | ✓ |
| `PublicMedalsPage` | `displays/publicMedals` | | ✓ |
| `WodPage` | `displays/wod` | | ✓ |
| `JuryScoreboardPage` | `displays/juryScoreboard` | JURY | ✓ |
| `JuryDecisionsPage` | `displays/juryDecisions` | JURY | ✓ |
| `RankingsPage` | `displays/rankings` | | |
| `TopSinclairPage` | `displays/topsinclair` | | |
| `TopTeamsPage` | `displays/topteams` | | |
| `TopTeamsSinclairPage` | `displays/topteamsinclair` | | |
| `monitors.OBSMonitor` | `displays/monitor` | `@PublicPage` + `@RequiresFeature(OBS_MONITORS)` | |
| `displays.video.StreamingEventMonitor` | `displays/notifications` | `@PublicPage` + `@RequiresFeature(OBS_MONITORS)` | |

**Results / admin**

| Class | Route | Roles | platform |
|---|---|---|---|
| `ResultsNavigationContent` | `results` | RESULTS | |
| `SessionResultsContent` | `results/results` | RESULTS | (reads `fop` but not bound) |
| `TeamResultsContent` | `results/teamresults` | RESULTS | |
| `PackageContent` | `results/finalpackage` | RESULTS | |
| `SessionImportContent` | `results/sessionImport` | ADMIN_PAGES | |
| `nui.admin.AdminView` | `admin` | ADMIN_PAGES + keep existing localhost/backdoor requirement | |
| `nui.admin.RecordFederationComparisonReport` | `admin/record-federation-report` | ADMIN_PAGES | |

Note: "reads `fop`" (`FOPParametersReader` without `isIgnoreFopFromURL() == true`) is **not** the same as platform-bound; `platformBound` is declared explicitly for that reason.

---

## 7. Runtime enforcement

### 7.1 `AccessPolicy` (pure logic, no Vaadin types, unit-tested)
```
Set<Role> expandedRoles(Principal p, String platform)   // roles effective on that platform (null = platform-independent)
boolean canOpen(Principal p, PageRule rule, String targetPlatform)
List<String> selectablePlatforms(Principal p, Set<Role> pageRoles, List<String> allPlatforms)
boolean hasCapability(Principal p, Capability c, String platform)
boolean requiresPlatformChoice(Principal p)
List<String> loginPlatformChoices(Principal p, List<String> allPlatforms)
```
`PageRule` = parsed annotation (`PUBLIC | AUTHENTICATED | ROLES(set, platformBound)`), cached per class.

`canOpen` algorithm:
1. `PUBLIC` ⇒ true. Principal null ⇒ false. `AUTHENTICATED` ⇒ true.
2. For each grant `g`, for each `r ∈ expand(g.role)` with `r ∈ rule.roles`:
   - `!rule.platformBound` ⇒ allow.
   - else allow iff scope matches: `g.platformName == null || g.platformName.equals(targetPlatform)`, **and** lock matches: `!r.lockable || p.loginPlatform == null || p.loginPlatform.equals(targetPlatform)`.
   - ADMIN expansion counts as scope "all".
3. Otherwise false.

`Capability` enum: `BREAK_MANAGEMENT` = {ANNOUNCER, TIMEKEEPER, MARSHAL} (platform-bound). Add others only when needed.

### 7.2 `AccessControlListener` (global `BeforeEnterListener`)
Registration: in `AppShell.serviceInit()`, inside the existing `addUIInitListener` lambda, add `uiInitEvent.getUI().addBeforeEnterListener(accessControlListener)`. UI-level listeners run before component `BeforeEnterObserver`s.

Algorithm for each navigation:
1. `OwlcmsFactory.waitDBInitialized()`.
2. Target class = `event.getNavigationTarget()`. No `@Route` ⇒ return (error views). `@RequiresFeature` with the switch off ⇒ `event.rerouteToError(NotFoundException.class)`.
3. Records-only mode (`Config.isRecordRepository()`): run the existing records-only logic moved verbatim from `AuthorizationDispatch` (public / secretary / admin paths) and return; accounts mode does not apply in records-only mode.
4. Resolve the principal (§7.4). If null and the rule is not `PUBLIC`: store requested path + query parameters (`OwlcmsSession.setRequestedUrl/…QueryParameters`) and `event.forwardTo(LoginView.class)`.
5. Compute target platform for platform-bound pages, using the same fallback as `FOPParametersReader.readParams`: `fop` query parameter (URL-decoded) → `OwlcmsSession.getFop()` → `OwlcmsFactory.getDefaultFOP()`.
6. `!canOpen(...)` ⇒ log at info (`username`, path, platform) and `event.forwardTo(AccessDeniedView.class)`.
7. Accounts mode: if the account was disabled/deleted since login ⇒ logout (§8.4).

### 7.3 Platform checks outside navigation
- **Selectors** (§9) prevent choosing a disallowed platform.
- **Safety net** in `OwlcmsSession.setFop(FieldOfPlay)`: if a principal is present and the new platform is not in `allowedSessionPlatforms(principal)` (= login platform ∪ scopes of non-lockable platform roles, or everything when not locked), log `logger.warn` with `LoggerUtils.whereFrom()` and ignore the change. Only apply when a Vaadin session with a principal is current (the method is also reached from background threads).
- At login with a login platform, call `OwlcmsSession.setFop(loginFop)` so pages opened without a `fop` parameter default to it.

### 7.4 Principal resolution
- Stored in `OwlcmsSession` under a new attribute key (e.g. `PRINCIPAL`). Keep the existing `authenticated` / `displayAuthenticated` flags in sync during phase 1 so untouched code keeps working; remove readers of those flags as they are migrated.
- Accounts mode: principal is rebuilt from the account cache on each navigation (grants may have changed; revocation takes effect at the next navigation). The login platform is kept from the session.
- PIN mode, no principal yet: port the **implicit** grants of today's code exactly:
  - Officials pages: `OWLCMS_PIN` set to empty ⇒ `NO_PIN` principal. No PIN expected at all ⇒ `NO_PIN`. Backdoor IP ⇒ `BACKDOOR`.
  - Display pages: no display PIN and no display list ⇒ DISPLAYS principal; backdoor ⇒ `BACKDOOR`; no display PIN and IP in display list ⇒ DISPLAYS principal; otherwise forward to `DisplayLoginView`.
  - An existing DISPLAYS-only principal visiting an officials page is sent to `LoginView` (not denied), as today.
- Accounts mode: backdoor IP ⇒ `BACKDOOR` principal (ADMIN, unlocked), as today. `OWLCMS_PIN` (empty or not), the display PIN and the display IP list play no part in accounts mode.

### 7.5 Removing the old interfaces
- Delete `AuthorizationDispatch` and `RequireDisplayLogin` (logic moved to the listener/`PinModeAuthenticator`).
- Remove them from: `OwlcmsContent` (extends list), `BaseResults`, `AbstractAttemptBoard`, `WodBoard`, `WarmupScoreboardPage`, `DisplayNavigationContent`, `VideoNavigationContent`, `AthleteCard`, `AdminView`, `RecordFederationComparisonReport`, `RefContent`, `JuryMobileContent`, `JuryKeypadContent`.
- Remove the explicit `X.super.beforeEnter(event)` calls: `AdminView` (~31), `JuryKeypadContent` (~118), `JuryMobileContent` (~97), `RefContent` (~125), `DisplayNavigationContent` (~242), `VideoNavigationContent` (~268). Keep any other logic those `beforeEnter` methods contain.
- Update `LoginView` Javadoc references.

---

## 8. Login, logout, denied

### 8.1 PIN mode
Unchanged behaviour. On success, set the principal (§4.5) in addition to the existing flags. Remove the PIN debug log.

Logging (both modes, and `DisplayLoginView`): never log the typed PIN or password, at any level. PIN and password use `PasswordField`; keep the existing reveal button; clear the field after a failed attempt.

### 8.2 Accounts mode — `LoginView`
`LoginView` renders a different form when `Config.getCurrent().getParamAccessMode() == ACCOUNTS`:
- Step 1: username `TextField` (`autocomplete="username"`), `PasswordField` (`autocomplete="current-password"`), Login button (Enter shortcut). No value-change auto-submit.
- On submit, in order:
  1. Officials IP list: `AccessUtils.ipIsAllowedForOfficials(clientIp)`; if refused, same generic error.
  2. `LoginThrottle.check(username, clientIp)`; if blocked, show the throttled message.
  3. Look up account (case-insensitive), check `enabled`, verify password.
  4. Failure: generic "invalid username or password" (no user enumeration), `LoginThrottle.fail(...)`, `logger.warn` with username and IP (never the password).
  5. Success: `LoginThrottle.success(...)`; rotate the session id with `VaadinService.reinitializeSession(VaadinRequest.getCurrent())`, then store the principal. Verify that `OwlcmsSession` data survives the rotation; if not, store the principal after rotation and re-create what is missing.
- Password verification runs on the request thread: session-id rotation needs the current `VaadinRequest`, which is not available inside `ui.access` from a worker. This is a deliberate, bounded CPU cost (a few hundred ms), not I/O; note it in a one-line comment.
- Step 2 (only if `requiresPlatformChoice`; never for accounts with an ADMIN grant): platform `ComboBox` listing `loginPlatformChoices`; preselect the platform from the stored requested URL's `fop` parameter if present and allowed; auto-continue when exactly one choice. Store `loginPlatform`, call `OwlcmsSession.setFop`.
- Redirect: requested URL if `canOpen`, else landing page (§8.5), else `HomeNavigationContent`.

`DisplayLoginView` in accounts mode: forward to `LoginView` keeping the requested URL.

### 8.3 `LoginThrottle`
In-memory, keyed separately by lower-case username and by client IP. After 5 consecutive failures: block 30 s, doubling on each further failure up to 15 min. Reset on success. Pure class, unit-tested with an injectable clock.

### 8.4 Logout
- Drawer entry in `OwlcmsLayout` next to the color-scheme entry at the bottom of the drawer; shown only in accounts mode, when a principal exists and the source is `ACCOUNT`. PIN mode stays as today (no logout).
- Action: `OwlcmsSession.invalidate()` (invalidates the HTTP session → all tabs of the browser) then `UI.getCurrent().getPage().setLocation("login")`.
- Mobile pages without layout (`RefContent`, `JuryMobileContent`, `JuryKeypadContent`) reach logout through their navigation pages, which use `OwlcmsLayout`.

### 8.5 Landing page
After login with no usable requested URL: if the principal can open exactly one **platform-bound work page** among {Announcer, Marshal, Timekeeper, TC, Jury console, RefereeNavigationContent, JuryNavigationContent}, go there with `fop=<loginPlatform>`; otherwise `HomeNavigationContent`.

### 8.6 `AccessDeniedView`
Route `denied`, `@AuthenticatedPage`, `OwlcmsLayout`. Shows translated message, display name, login platform (if any), buttons Home and Logout.

---

## 9. Platform selectors

For every selector, items = `AccessPolicy.selectablePlatforms(principal, pageRoles, allPlatforms)`; if one item or the principal is locked for the page's (lockable) roles, set the value and `setReadOnly(true)`. In PIN mode, and for ADMIN or backdoor principals in accounts mode, the list is all platforms and behaviour is unchanged (live change).

| Location | Notes |
|---|---|
| `BaseNavigationContent.createFopSelect` (~185) | used by `LiftingNavigationContent` (~84), `DisplayNavigationContent` (~266), `VideoNavigationContent` (~296), `MedalCeremonyContent` (~116) |
| `RefContent` (~294, listener ~391 calls `OwlcmsSession.setFop`) | |
| `JuryKeypadContent.createFopSelect` (~258, listener ~424) | |
| `JuryMobileContent.createFopSelect` (~178, listener ~207) | |
| `RefereeNavigationContent.createFopSelector` (~155) | |
| `JuryNavigationContent.createFopSelector` (~126) | |
| `MobileScoreboardsNavigationContent.createFopSelector` (~128) | DISPLAYS: not locked by default |

Because selectors are either restricted or read-only, the navigation buttons (which bake the platform into their `onClick` at build time) do not need rebuilding on platform change beyond what the pages already do.

---

## 10. Hiding UI

### 10.1 Navigation helpers
There are **two** `NavigationPage` interfaces: `app.owlcms.components.NavigationPage` and `app.owlcms.nui.shared.NavigationPage`. In both, every `openInNewTab*` helper sets `button.setVisible(AccessUi.canOpen(targetClass, resolveFop()))` (`AccessUi` = thin Vaadin-side wrapper that gets the principal from `OwlcmsSession` and calls `AccessPolicy`). Hidden Vaadin components do not receive client events, so hiding is also enforcement for in-page actions.

`doGroup(...)`: do not add the group (title + grid) when none of its buttons is visible.

### 10.2 Other navigation sites
- `OwlcmsLayout` drawer tabs (`createTab`, ~368–436): skip tabs whose target cannot be opened (Home, Preparation, Lifting, Displays, Results, Records, Info…).
- `HomeNavigationContent` buttons using `UI.navigate` (~149–151 and following).
- `RefereeNavigationContent`, `JuryNavigationContent`, `MobileScoreboardsNavigationContent` navigation buttons.
- One-off openers — gate each on its target route class:
  - `AnnouncerContent` (~792) → `MedalCeremonyContent`
  - `BreakManagement` (~504) → `MedalCeremonyContent`
  - `CompetitionEditingFormFactory` (~603), `EditChampionshipsPanel` (~329), `AgeGroupActionsMenu` (~112) → `AgeGroupContent`
  - `AgeGroupContent` (~194) → `ChampionshipsContent`
  - `RecordConfigEditingFormFactory` (~312), `RecordsConfigContent` (~184) → `RecordContent`
  - `PlatformContent` (~174) → `TCContent` for that platform
  - `SessionContent` (~288) → `RegistrationContent` ("Edit athletes")
  - `PublicRecordsContent` (~59) → `preparation/records`
  - `getPage().open(url, "_blank")` sites (`AgeGroupActionsMenu` ~120, `RecordContent` ~303, `TechnicalOfficialContent` ~133): check whether the URL is a route or a download; gate only routes.

### 10.3 In-page capabilities
- Break dialog: `AthleteGridContent.breakButtons()` (~1221) returns `null` (callers already null-check `this.breaks`) unless `hasCapability(BREAK_MANAGEMENT, getFop())`. This covers every `AthleteGridContent` subclass using the base top bar (TC, Jury, Testing, SessionResults…). The marshal page keeps its Pause button (it starts a `BreakType.MARSHAL` break). `AnnouncerContent.createInitialBar` (~723) and `TimekeeperContent.createInitialBar` (~184) need no gate (page roles already imply the capability).
- Medal ceremony links: §10.2.
- "Edit athletes" in `SessionContent`: §10.2.

---

## 11. Configuration

`Config`:
- New persisted field `accessMode` (String, default `PIN`), `getParamAccessMode()` following the `getParamAccessList()` pattern (~642): `StartupUtils.getStringParam("accessMode")` (⇒ env `OWLCMS_ACCESSMODE`) overrides the DB value.
- Edited in the security/access section of `ConfigEditingFormFactory` (next to PIN / display PIN / IP lists, ~336–352) as a two-value selector.
- Switching to ACCOUNTS in the UI is refused unless the built-in `admin` account has a password (translated error with a link to the accounts page). Switching back to PIN is always allowed.
- When ACCOUNTS is active: disable the PIN, display PIN and display IP list fields with an explanatory translated note (values are kept for switching back).

Interaction table (accounts mode):

| Setting | Behaviour |
|---|---|
| `Config.pin` | ignored (kept) |
| `OWLCMS_PIN` | not active; if set, log a startup warning that it is ignored |
| Display PIN (DB/env) | ignored |
| Officials IP list | applied before password check |
| Display IP list | ignored (superseded by display login) |
| Backdoor list | ADMIN principal, not locked. Setting `OWLCMS_BACKDOOR` in the environment is the documented emergency access. |

Startup: if accounts mode is active (DB or env) and the built-in `admin` account has no password, log an error stating that only backdoor access works and how to set `OWLCMS_BACKDOOR`.

### 11.1 OBS monitors feature switch
- New `FeatureSwitch.OBS_MONITORS("obsMonitors", FeatureSwitchSection.SPECIALTY_FEATURES)`, off by default (usual `FeatureSwitch` mechanism, including the `featureSwitches` env override).
- Off: `OBSMonitor` and `StreamingEventMonitor` routes give not-found (§7.2); their buttons in `VideoNavigationContent` (~109–110) are not added.
- On: both pages open without login, as today.
- Translation key: `FeatureSwitch.obsMonitors` (from `FeatureSwitch.getTranslationKey()`); check whether the other switches also have a description key and add one the same way.

---

## 12. Account administration UI

`AccountsContent` (`preparation/accounts`, ADMIN_PAGES), following the grid + form pattern of `CoachContent` (crudui, `OwlcmsCrudFormFactory`).
- Grid: username, display name, enabled, grants summary (e.g. `PLATFORM (A), RESULTS`).
- Available in both access modes, so accounts can be prepared before switching.
- Password reset: ADMIN sets a new password for any account (new password + confirmation, old password not required). No self-service password change in this iteration.
- Form: username, display name, enabled, new password + confirmation (blank on edit = unchanged; hash on save), grants editor (rows of role `ComboBox` [grantable roles] + scope `ComboBox` ["All platforms" + current platform names], scope disabled and forced to all for non-scopable roles).
- Validation: unique username (case-insensitive); password length ≥ 8; confirmation match; scope must name an existing platform; built-in `admin` protected (§5); cannot delete one's own account.
- Entry points: a button on `PreparationNavigationContent` (visible to ADMIN only via §10.1) and a link from the access section of the config page.

---

## 13. Out of scope but must keep working

- Servlets (`CompetitionExport`, `H2BackupServlet`, `SimulationServlet`, `ControlPanelServlet`): unchanged IP rules.
- MQTT (`MoquetteAuthenticator`): unchanged.
- Records-only mode: unchanged rules (§7.2 step 3).

### 13.1 TODO — review the entry points that are not Vaadin routes

Routes are covered by the annotation check (§6). The entry points below are not, and are reviewed separately after phase 1. Each is a plain servlet, filter or WebSocket endpoint, so none goes through `AccessControlListener`.

| URL | Class | Protection today | To check |
|---|---|---|---|
| `/competition/export`, `/competition/export/json/1`, `/competition/export/json/2`, `/competition/export/sbde` | `CompetitionExport` | client IP on the local network, or in the backdoor list; no login | Known not covered by roles. Decide whether roles apply. |
| `/competition/h2` | `H2BackupServlet` | same as the exports (full database backup) | Same decision as the exports. |
| `/simulation/*` (GET and POST) | `SimulationServlet` | localhost or backdoor list | Starts and stops the simulator. Confirm the IP rule is enough. |
| `/controlpanel/stop` (POST) | `ControlPanelServlet` (registered in `EmbeddedJetty`) | localhost socket address, or backdoor list | Confirm the IP rule is enough. |
| `/local/*` | `FileServlet` (shared module) | none | Believed innocuous if it is correctly confined to the `./local` directory and classpath resources, read-only. Verify the confinement (`resolvePath`, URL-decoding, `..`, encoded separators, symlinks) and that nothing sensitive can be reached. |
| `/mqtt` (WebSocket proxy) and the broker ports | `MqttWebSocketProxyEndpoint`, `MoquetteAuthenticator` | MQTT user name and password from `Config`; anonymous when none is configured | Unchanged, but see the logging item below. |
| `/VAADIN/dynamic/...` downloads and upload handlers | Vaadin | bound to the Vaadin session | Confirm they are only created by pages the principal may open. |

Cross-cutting items to review with the servlets:
- **Client IP.** `AccessUtils.getClientIp()` and `ProxyUtils.getClientIp()` use the `X-Forwarded-For` header when it is present, whoever sent it. The backdoor list, the officials whitelist and every "localhost" or "local network" rule depend on it. Decide when the header may be trusted (behind a known proxy) and when only the socket address counts.
- **CORS.** `CorsFilter` (`/*`) allows every origin with credentials and rewrites the session cookie to `SameSite=None`, but only for Vaadin UIDL and heartbeat requests and for `/VAADIN/build/` and `/web-component/`. The servlets above are not affected. Review whether a session cookie usable cross-site is acceptable once accounts exist.
- **Password in logs.** `MoquetteAuthenticator` logs the client-supplied MQTT password at debug level. Remove it (§14: never log passwords).
- **Audit.** Audit requirements are defined in [AUDIT_TRAIL_SPECIFICATION.md](AUDIT_TRAIL_SPECIFICATION.md) and [AUDIT_INTEGRITY_SPECIFICATION.md](AUDIT_INTEGRITY_SPECIFICATION.md).

---

## 14. Logging

Follow the `owlcms-logging-format` skill.
- info: successful login (username, IP, login platform), logout, denied navigation (username, path, platform), backdoor use (existing).
- warn: failed login (username, IP), throttling, `setFop` refused by the safety net, accounts mode with no admin.
- Never log passwords or hashes.

---

## 15. Translations

No hard-coded UI strings. Follow the repository process: do **not** edit `translation4.csv`; create `shared/src/main/resources/i18n/access_control_translations.tsv` with the header copied from the first line of `translation4.csv` and one row per new key. Reuse existing keys where they fit (`Login`, `Log_In`, `LoginDenied`).

Expected new keys (adjust as needed): `Access.Username`, `Access.Password`, `Access.Platform`, `Access.Continue`, `Access.LoginFailed`, `Access.TooManyAttempts`, `Access.Logout`, `Access.Denied.Title`, `Access.Denied.Message`, `Access.Mode`, `Access.Mode.PIN`, `Access.Mode.ACCOUNTS`, `Access.Mode.PinIgnored`, `Access.Accounts.Title`, `Access.Account.Username`, `Access.Account.DisplayName`, `Access.Account.Enabled`, `Access.Account.NewPassword`, `Access.Account.ConfirmPassword`, `Access.Account.Grants`, `Access.Grant.Role`, `Access.Grant.Scope`, `Access.Grant.AllPlatforms`, `Access.Grant.UnknownPlatform`, `Access.Account.ResetPassword`, `Access.Error.BuiltInAdmin`, `Access.Error.AdminPasswordRequired`, `FeatureSwitch.obsMonitors`, `Access.Error.PasswordMismatch`, `Access.Error.PasswordTooShort`, `Access.Error.DuplicateUsername`, `Access.Error.OwnAccount`, and `Access.Role.<NAME>` for every grantable role.

---

## 16. Tests

Follow the `add-test-case` skill: **no Vaadin UI objects in tests**; keep logic in `AccessPolicy`, `PasswordHasher`, `LoginThrottle` so it is testable.

- `AccessPolicyTest`: expansion (ADMIN, PLATFORM, RESULTS→DISPLAYS); scope matching; lock (lockable role on another platform refused, DISPLAYS on another platform allowed); ADMIN account never locked and never asked for a platform; results account with no login platform opening Top pages and platform scoreboards; `requiresPlatformChoice` / `loginPlatformChoices`; grants naming an unknown platform ignored; `BREAK_MANAGEMENT` for announcer, timekeeper and marshal, refused for TC/jury; feature-gated pages refused when the switch is off.
- `PasswordHasherTest`: round-trip, wrong password, tampered hash, iteration count parsed from hash, non-ASCII password, length < 8 rejected.
- `LoginThrottleTest`: thresholds, backoff doubling, cap, reset on success (injectable clock).
- `RouteAccessCoverageTest`: scan compiled classes under `app.owlcms` (walk the test classpath output directory for `.class` files; load with `Class.forName(name, false, loader)` — no initialization); every class with `@Route` has exactly one of `@PublicPage`, `@AuthenticatedPage`, `@RequiresRole`; print the role/page matrix to the test log for review.
- Running tests needs human consent (repo rule); use the `run-java-test` or `run-maven-test` skill once authorized.

---

## 17. Decisions

Resolved (already reflected above): ADMIN always all platforms and never locked (live selectors kept); DISPLAYS not lockable; MARSHAL keeps the Pause button; TIMEKEEPER opens medal ceremonies; display IP list ignored in accounts mode; `OWLCMS_PIN` inactive in accounts mode, `OWLCMS_BACKDOOR` is the emergency access; built-in `admin` account needs a password before switching; ADMIN resets passwords; OBS monitors behind a feature switch, open when enabled; password length ≥ 8, any characters; session import ADMIN only; typed PINs and passwords never logged (reveal button kept); logout in the side menu in accounts mode only, PIN mode unchanged; `WodkeeperContent` (timer control) is TIMEKEEPER, platform-bound, while `WodPage` stays DISPLAYS.

No decisions remain open.

---

## 18. Repository constraints for the implementer

- Read `.github/copilot-instructions.md` and `AGENTS.md` first.
- No `mvn`, builds or test runs without explicit human consent. No git commits/pushes without consent.
- No fully-qualified class names in Java sources; add imports.
- No hard-coded UI strings; translation keys only (§15). Never edit `translation4.csv`.
- Vaadin concurrency: never block inside `ui.access`; capture `UI` and locale on the UI thread (`OwlcmsSession` accessors return defaults off the UI thread). The login password check is the one documented exception (§8.2).
- After each Java edit, validate with the `verify-java-fix` skill (Problems panel / `get_errors`).
- Hot reload: new `@Subscribe` methods and new entities require a JVM restart.

---

## 19. Implementation phases and acceptance criteria

**Phase 1 — Global check, PIN mode only**
- `Role`, annotations (including `@RequiresFeature`), `AccessPolicy`, `AccessControlListener`, `PinModeAuthenticator`, `AccessDeniedView`; annotate every route per §6.1; remove the old interfaces (§7.5); `FeatureSwitch.OBS_MONITORS` (§11.1); PIN debug log removed.
- Accept: in PIN mode every page behaves as before (officials PIN, display PIN, IP lists, backdoor, empty `OWLCMS_PIN`, records-only mode), except that the OBS monitors now require the feature switch. `RouteAccessCoverageTest` and `AccessPolicyTest` pass.

**Phase 2 — Accounts**
- `UserAccount`, `RoleGrant`, repository + cache, built-in `admin` seeding, `PasswordHasher`, `LoginThrottle`, `Config.accessMode` + UI guard, `AccountsContent` (usable in both modes, password reset), accounts-mode `LoginView` (step 1), session-id rotation, logout, platform rename/delete hooks.
- Accept: account-bearing competition imports replace accounts while legacy imports preserve them; switching to accounts mode refused while `admin` has no password; `admin` cannot be deleted/disabled/demoted; `OWLCMS_PIN` has no effect in accounts mode; `OWLCMS_BACKDOOR` gives ADMIN access; disabled account is logged out at next navigation; throttling works; no password in logs.

**Phase 3 — Platform lock**
- Login step 2, `OwlcmsSession.setFop` safety net, URL platform check, restricted/read-only selectors (§9), landing page (§8.5).
- Accept: announcer for platform A cannot open any platform-B operating page by URL, selector or bookmark; an ADMIN account is never asked for a platform and keeps live selectors; results account opens Top pages without a platform and platform scoreboards for any platform; PIN mode selectors unchanged.

**Phase 4 — Hiding**
- §10.1–10.3.
- Accept: a PLATFORM(A) account sees only lifting buttons for A plus displays; a REGISTRATION+WEIGHIN+RESULTS account sees preparation (athletes, coaches, teams, documents, weigh-in), results and displays; empty groups and drawer tabs are hidden; the Pause button appears only for announcer, timekeeper and marshal.
