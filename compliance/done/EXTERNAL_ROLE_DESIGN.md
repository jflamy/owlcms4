# EXTERNAL Role — Read-Only Display Access for Broadcasters

Status: design approved (decisions in §10 resolved), not implemented.
Audience: the implementing agent. Extends [ACCESS_CONTROL_DESIGN.md](../ACCESS_CONTROL_DESIGN.md); its §18 (repository constraints) applies.

Line numbers are approximate; search by method name.

---

## 1. Purpose

Competition staff set up scoreboards and attempt boards with the DISPLAYS role. External broadcasters (TV and streaming crews) also need to open the same boards, in addition to staff. Their access must:

- cover every display page that DISPLAYS can open (§4), on **all platforms**;
- be **read-only**: keyboard input from the board must not reach the competition (§5). Broadcasters see every board, including decision boards, so keyboard refereeing must be blocked for them.

Accounts mode only. In PIN mode, every display principal is `DISPLAYS(all)` or `ADMIN(all)`, so EXTERNAL never applies.

## 2. Non-goals

- Granting individual pages per account (pages are granted by role, not one by one).
- Per-platform scoping of EXTERNAL.
- WebSocket forwarding to the tracker, export servlets, and the OBS monitors (`OBSMonitor`, `StreamingEventMonitor`). They are unchanged; see ACCESS_CONTROL_DESIGN §13 and EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.
- Unattended login (tokens, "remember me"). A broadcaster's browser logs in interactively, as any account does.
- Closing boards that are already open when a grant is revoked (§8).

## 3. Role definition

Add one value to `Role`:

| Role | grantable | platformScopable | lockable | base |
|---|---|---|---|---|
| EXTERNAL | yes | **no** | **no** | yes |

- Not platform-scopable: the grant scope is always `null` (all platforms); the account editor already forces this for non-scopable roles (ACCESS_CONTROL_DESIGN §4.3). `AccessPolicy.scopeMatches()` returns true for non-scopable roles, so platform-bound pages open on every platform and `AccessUi.selectableFops()` lists every platform.
- `expand()` returns `{EXTERNAL}` (default branch). EXTERNAL is not added to any composite (`RESULTS`, `PLATFORM`, `COMPETITION_DIRECTOR`).
- `ADMIN` expands to `baseRoles()`, which then includes EXTERNAL. This is harmless: ADMIN also holds DISPLAYS, so its input stays enabled (§5.1).
- Not lockable, so an account holding only EXTERNAL gets no login-platform step (`AccessPolicy.requiresPlatformChoice()` stays false).
- `AccessPolicy.platformsWithAccess()` counts only platform-scopable roles, so the login audit record (`LoginView.loginAuditPlatforms`) of an EXTERNAL-only account lists no platform. Acceptable: the role itself means all platforms, read-only.
- `GrantsField.grantableRoles()` lists it automatically. The label uses the translation key `Access.Role.EXTERNAL` (§9).

## 4. Pages

Rule: **every page that lists DISPLAYS also lists EXTERNAL, except `JuryScoreboardPage`.** Each such page adds EXTERNAL to its existing annotation, for example:

```java
@RequiresRole(value = { Role.DISPLAYS, Role.EXTERNAL }, platformBound = true)
@RequiresRole(value = { Role.DISPLAYS, Role.JURY, Role.EXTERNAL }, platformBound = true)
```

`AccessPolicy.canOpenWithRoles()` already accepts any role in the list, so the access logic does not change. `PageRule.isDisplayPage()` still returns true because DISPLAYS stays in the list, so PIN-mode display login is unaffected.

### 4.1 Pages

All current `@RequiresRole` annotations containing DISPLAYS (ACCESS_CONTROL_DESIGN §6.1):

| Group | Classes |
|---|---|
| Attempt boards | `PublicFacingAttemptBoardPage`, `AthleteFacingAttemptBoardPage` |
| Decision boards | `PublicFacingDecisionBoardPage`, `AthleteFacingDecisionBoardPage` |
| Scoreboards | `PublicScoreboardPage`, `PublicNoLeadersPage`, `WarmupScoreboardPage`, `WarmupNoLeadersPage`, `PublicMultiRanksPage`, `WarmupMultiRanksPage`, `PublicRankingOrderPage`, `WarmupRankingOrderPage`, `WarmupLiftingOrderPage`, `PublicStartListPage` |
| Video overlays | `CurrentAthletePage`, `NCurrentAthletePage` |
| Medals and rankings | `MedalsPage`, `PublicMedalsPage`, `RankingsPage` |
| Top pages | `TopSinclairPage`, `TopTeamsPage`, `TopTeamsSinclairPage` |
| Jury displays | `JuryDecisionsPage` (JURY stays in the list) |
| Other | `WodPage` |
| Navigation | `DisplayNavigationContent`, `VideoNavigationContent`, `MobileScoreboardsNavigationContent` |

Excluded: `JuryScoreboardPage` keeps `{ DISPLAYS, JURY }`. Its menu buttons are hidden for EXTERNAL automatically by `AccessUi.canOpen(...)`.

The menus need no other change:
- Buttons are already shown or hidden with `AccessUi.canOpen(targetClass, fop)` in both `NavigationPage` interfaces.
- The platform selector in `BaseNavigationContent` uses `AccessUi.selectableFops(PageRule.of(getClass()).roles())`, which returns every platform for EXTERNAL.
- `HomeNavigationContent` shows the Displays and Video buttons with `AccessUi.canOpen(...)`.

The OBS monitors are `@PublicPage` behind the `OBS_MONITORS` feature switch and are not affected.

## 5. Read-only behaviour

### 5.1 Rule

Keyboard input on a display is allowed only if the principal holds **DISPLAYS** effective on the board's platform:

```java
// AccessPolicy (pure, unit-tested)
public static boolean displayInputAllowed(Principal principal, String platform) {
    return expandedRoles(principal, platform).contains(Role.DISPLAYS);
}
```

Consequences:
- DISPLAYS, RESULTS, PLATFORM, COMPETITION_DIRECTOR, ADMIN, and the backdoor keep today's behaviour.
- PIN mode is unchanged (principal is `DISPLAYS(all)` or `ADMIN(all)`).
- An account with EXTERNAL and DISPLAYS only on platform A keeps input on A and is read-only on the other platforms.

A Vaadin-side wrapper in `AccessUi` reads the principal from `OwlcmsSession`:

```java
public static boolean displayInputAllowed(FieldOfPlay fop) {
    return AccessPolicy.displayInputAllowed(OwlcmsSession.getPrincipal(), fop != null ? fop.getName() : null);
}
```

Call it only on the UI thread (in `onAttach` or in a `@ClientCallable` request), never from event-bus or background threads. See the `OwlcmsSession` thread-safety note in `.github/copilot-instructions.md`.

### 5.2 Inputs found on these pages

| Input | Where | Effect today | Read-only behaviour |
|---|---|---|---|
| Keys `1`–`6` | `DecisionElement.js` `_readRef()` (document `keydown`); active when `enabled`, i.e. after `StartTime`, `StopTime`, or `BreakStarted` (`DecisionElementState`) | `masterRefereeUpdate` → `FOPEvent.DecisionFullUpdate` / `DecisionUpdate` posted to the **FOP**. **This enters referee decisions.** | Disabled on the client **and** rejected on the server |
| `masterShowDown` callable | `AbstractDecisionElement` | `FOPEvent.DownSignal` posted to the FOP | Rejected on the server |
| Arrow keys | `AbstractResultsDisplayPage.addKeyboardShortcuts()` (called from `AbstractDisplayPage.onAttach` and `addDialogContent`) | Changes font size or team width on that browser only | Not registered |
| Mouse click → options dialog | `AbstractDisplayPage` constructor | Local display options (lighting, sound, sections, sizing) written to that browser's URL | Unchanged (D-3) |
| Diagnostic callables (`TimerElement.clientTimer*`, `attemptBoardWeightRendered`, `scoreboardTopRendered`, `UnloadObserver`, `SoundEnabler`) | various | Tracing or local state only | Unchanged |

Almost every board contains a `decision-element` (`AttemptBoard.js`, `DecisionBoard.js`, `Results.js`, `ResultsMulti.js`, `ResultsStartList.js`, `ResultsRankingsByCategory.js`, `ResultsMedals.js`, `CurrentAthlete.js`, `NCurrentAthlete.js`, `TopSinclair.js`, `TopTeams.js`, `TopTeamsSinclair.js`). So today, any browser showing a board while the clock runs accepts referee keystrokes, and decision boards are where keyboard refereeing is normally used. That is why the server-side check in §5.3 is required. Jury displays set `jury=true`, which already makes `_readRef` ignore keys.

### 5.3 Changes

1. **`AbstractDecisionElement`**
   - Add a `boolean inputAllowed` field, computed in `onAttach` with `AccessUi.displayInputAllowed(this.fop)` and pushed to the client as the element property `inputAllowed`.
   - At the start of `masterRefereeUpdate(...)` and `masterShowDown(...)`: if `!inputAllowed`, log and return without posting any FOP event.
     ```java
     logger.info("{}ignored decision input from read-only display user={}",
             FieldOfPlay.getLoggingName(this.fop), principalName);
     ```
     The server-side check is the actual enforcement. A client-side check alone can be bypassed with browser developer tools.
2. **`DecisionElement.js`**: in `_readRef`, return immediately when `!this.inputAllowed` (next to the existing `!this.enabled || this.jury` guard). Declare `inputAllowed` as a property defaulting to `false`, so an element never accepts input before the server has decided.
3. **`AbstractDisplayPage`**: guard both calls to `addKeyboardShortcuts()` (in `onAttach` and in `AbstractResultsDisplayPage.addDialogContent`) with `AccessUi.displayInputAllowed(getFop())`. Prefer one guarded `final` entry point that subclasses do not bypass.

The FOP is fixed for the page's lifetime (it comes from the URL and is checked at navigation), so computing the flag once at attach time is enough.

### 5.4 Frontend reload

Change 2 edits frontend JavaScript. Before checking it in a browser, enable live JS reload or restart the server.

## 6. Login and session

- An account with only EXTERNAL logs in on the unified `login` page. There is no platform step (§3), and it lands on the home page, which shows Displays/Video only, with every platform selectable.
- `AccountModeAuthenticator.resolve()` re-reads grants on every navigation. Disabling the account or removing the grant takes effect at the broadcaster's next navigation.
- Login, logout and denial are already audited (`AuthAudit`, `AccessControlListener` denial log). The new server-side rejection (§5.3) is logged at `info` with the FOP prefix.

## 7. Tests

Per the `add-test-case` skill: no Vaadin UI objects in tests.

- `AccessPolicyTest` (pure):
  - EXTERNAL opens a platform-bound display page on every platform.
  - EXTERNAL cannot open a non-display page (for example an ANNOUNCER page).
  - `displayInputAllowed`: false for EXTERNAL only; true for DISPLAYS, RESULTS, PLATFORM, ADMIN; mixed case EXTERNAL + DISPLAYS(A) → true on A, false on B.
  - `requiresPlatformChoice` is false for an EXTERNAL-only account.
- `RouteAccessCoverageTest`: assert that a page lists EXTERNAL **if and only if** it lists DISPLAYS, with `JuryScoreboardPage` as the single named exception (it must not list EXTERNAL). This enforces the §4 rule for future display pages and keeps `isDisplayPage()` true for every EXTERNAL page.
- `AccessPolicyTest`: EXTERNAL cannot open `JuryScoreboardPage` and can open `JuryDecisionsPage`.
- Manual check: log in as an EXTERNAL-only account, open an attempt board, a decision board and a scoreboard, start the clock from the announcer, and press `1`, `3`, `5` on the board. No decision is recorded, and the server log shows the "ignored decision input" line.

## 8. Known limits

- **A board that is already open stays open when the grant is revoked.** It is updated by server push and never navigates again. Closing it would need an explicit push or logout to sessions of that account (out of scope).
- **Unattended browser sources** (OBS) must be logged in once by a person. After a server restart or session expiry they show the login page.

## 9. Translations

New key, delivered as a TSV in `shared/src/main/resources/i18n/` following the repository process (never edit `translation4.csv`):

| key | en |
|---|---|
| `Access.Role.EXTERNAL` | External (broadcaster) |

## 10. Decisions

| Id | Question | Decision |
|---|---|---|
| D-0 | Platform scope | All platforms; EXTERNAL is not platform-scopable. |
| D-1 | Medal, ranking and top pages | Included. |
| D-2 | Decision boards and all other boards | Included: EXTERNAL opens every page DISPLAYS opens except the jury scoreboard; keyboard refereeing is blocked (§5). |
| D-5 | Jury pages | `JuryDecisionsPage` included; `JuryScoreboardPage` excluded. |
| D-3 | Click-to-open options dialog | Kept. It changes only that browser's rendering. |
| D-4 | Role name shown to users | "External (broadcaster)". |
