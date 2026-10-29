# 120 Implementation Plan: STREAMING role and token access to display pages

Status: done 2026-10-29.

Implements the Vaadin part of [120-TOKEN_PROTECTED_URLS.md](120-TOKEN_PROTECTED_URLS.md).
Read the spec first; this plan says how, the spec says what. Where they disagree, stop
and ask the maintainer.

Already done (do not redo): account tokens (110), `TokenAuthenticator` with
`authenticate(token, clientIp)` and `holds(account, role)`, the export servlet check,
`AuditActor.token`, `AuthAudit.tokenRefused`, `UserAccountRepository.findByTokenHash`.

Out of scope: H2 download, `/simulation/*`, `/controlpanel/stop`, supervisor backups,
the video module (330), disconnecting open displays on revocation.

## Rules for this work

- Read `AGENTS.md` and `.github/copilot-instructions.md`. In particular:
  - Never run Maven while the dev server is up. Check with
    `curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://localhost:8080/`
    (000 means down). If it is up, ask the maintainer to stop it and wait.
  - Focused Maven test runs are pre-authorized (skill `run-maven-test`); read the
    Surefire reports. Never `mvn package` or `install`.
  - Never commit, push or stage without the maintainer's explicit go. Never add a
    Copilot `Co-authored-by` trailer.
  - Never edit `shared/src/main/resources/i18n/translation4.csv`. New or changed keys
    go into a TSV (skill `update-translations`).
  - No hard-coded UI strings; no fully qualified class names in Java sources.
  - Avoid `OwlcmsSession` and `UI.getCurrent()` in new code: capture values in the
    listener (the edge) and pass them as parameters. The one deliberate exception is
    `AccessUi.currentPrincipal()` (phase 4).
  - Logging: skill `owlcms-logging-format`. Never log a token or a display PIN.
- Do not create Vaadin components in unit tests (skill `add-test-case`). Reading
  annotations from a page's `Class` literal is fine: it does not initialize the class.
- After editing Java, check the Problems panel for the touched files (skill
  `verify-java-fix`).
- Stop at the end of each phase, report, and wait for the maintainer.

## Phase 1: STREAMING role

### 1.1 `owlcms/src/main/java/app/owlcms/access/Role.java`

- Add `STREAMING(true, false, false, true)` (grantable, not platform-scopable, not
  lockable, base). Place it right after `DISPLAYS`.
- Change `EXTERNAL` to `EXTERNAL(true, false, false, false)` (no longer a base role).
- In `expand()`, add `case EXTERNAL -> EnumSet.of(RESULTS, STREAMING);`.
- Do not add STREAMING to the COMPETITION_SUPERVISOR expansion.
- Update the enum's Javadoc to say: DISPLAYS is the venue's screens, STREAMING what a
  streaming client may show, EXTERNAL a composite of RESULTS and STREAMING.

Consequences to keep in mind (no code needed): ADMIN expands to every base role, so it
gains STREAMING and loses EXTERNAL; expansion is one level, so EXTERNAL does not get
DISPLAYS through RESULTS and has no display keyboard input.

### 1.2 `access/AccountLandingPolicy.java`

The switch in `defaultWorkPage` is exhaustive: add STREAMING to the `HOME` case.

### 1.3 Page roles

In each file below, change only the `@RequiresRole` roles; keep `platformBound` as is.

Replace `Role.EXTERNAL` by `Role.STREAMING`:

- `nui/displays/scoreboards/`: `CurrentAthletePage`, `JuryDecisionsPage` (keeps
  `Role.JURY`), `MedalLeadersPage`, `MedalsPage`, `NCurrentAthletePage`,
  `PublicMedalLeadersPage`, `PublicMedalsPage`, `PublicMultiRanksPage`,
  `PublicNoLeadersPage`, `PublicRankingOrderPage`, `PublicScoreboardPage`,
  `PublicStartListPage`, `RankingsPage`, `WarmupLiftingOrderPage`,
  `WarmupMultiRanksPage`, `WarmupNoLeadersPage`, `WarmupRankingOrderPage`,
  `WarmupScoreboardPage`, `WodPage`
- `nui/displays/attemptboards/`: `PublicFacingAttemptBoardPage`,
  `PublicFacingDecisionBoardPage`
- `nui/displays/top/`: `TopSinclairPage`, `TopTeamsPage`, `TopTeamsSinclairPage`
- Launchers: `nui/displays/DisplayNavigationContent`,
  `nui/displays/VideoNavigationContent`,
  `nui/home/navigation/MobileScoreboardsNavigationContent`

Remove `Role.EXTERNAL`, leaving DISPLAYS only (venue-only pages):

- `nui/displays/attemptboards/AthleteFacingAttemptBoardPage`
- `nui/displays/attemptboards/AthleteFacingDecisionBoardPage`

Unchanged: `JuryScoreboardPage` (DISPLAYS, JURY). After the edit,
`grep -rn 'Role.EXTERNAL' owlcms/src/main/java` must only find `Role.java` and
`AccountLandingPolicy.java`.

### 1.4 Translations

Create `shared/src/main/resources/i18n/streaming_role_translations.tsv`: first line is
the header of `translation4.csv` with tabs instead of commas, then one tab-separated
row per key, with a value for every column (English where unsure).

| Key | English |
| --- | --- |
| `Access.Role.STREAMING` | Streaming |
| `Access.Role.EXTERNAL` | External (results and streaming) |

`Access.Role.EXTERNAL` already exists ("External (broadcaster)"); the TSV carries its
new text. Phase 5 adds three more keys to the same TSV.

### 1.5 Tests

Update, in `owlcms/src/test/java/app/owlcms/`:

- `access/RouteAccessCoverageTest`: rename
  `externalIsListedExactlyOnPagesListingDisplays` to
  `streamingIsListedOnStreamablePages`. Expected: STREAMING is listed exactly on the
  pages listing DISPLAYS, except `JuryScoreboardPage`,
  `AthleteFacingAttemptBoardPage` and `AthleteFacingDecisionBoardPage`; no page lists
  EXTERNAL. Keep the existing jury assertions, rewritten for STREAMING.
- `access/AccessPolicyTest`:
  - `externalOpensDisplayPagesOnEveryPlatformButNothingElse`: use pages listing
    STREAMING; EXTERNAL now also opens a page requiring RESULTS. Add the same test for
    a STREAMING grant, which opens STREAMING pages and not RESULTS pages.
  - `externalIsReadOnlyAndNeverAsksForAPlatform`: keep; add the same assertions for
    STREAMING.
  - `externalWithScopedDisplaysHasInputOnlyOnThatPlatform`: keep as is.
  - Base-role test: assert STREAMING is a base role and EXTERNAL is not.
- `access/AccountLandingPolicyTest`: add `Map.entry(Role.STREAMING, WorkPage.HOME)`.
- `data/account/RoleAccountInitializationTest`: the expected count goes from 23 to 24
  (a `streaming` generic account); assert `streaming` exists.
- `access/TokenAuthenticatorTest.resultsRoleIsHeldDirectlyOrThroughACompositeRole`:
  EXTERNAL now holds RESULTS; STREAMING and DISPLAYS do not.

Run `RouteAccessCoverageTest,AccessPolicyTest,AccountLandingPolicyTest,`
`RoleAccountInitializationTest,TokenAuthenticatorTest`, then `AllTests`.

Stop and report.

## Phase 2: `@TokenAccessible`

### 2.1 Annotation

New `owlcms/src/main/java/app/owlcms/access/TokenAccessible.java`, modeled on
`RequiresRole`: `@Retention(RUNTIME) @Target(TYPE)`, **not** `@Inherited` (each page
opts in explicitly; `WarmupNoLeadersPage` extends `WarmupScoreboardPage`, and both
must carry it). Javadoc: "A display output page that an account token, or the display
PIN in PIN mode, may open. The page's roles still decide."

### 2.2 `access/PageRule.java`

- Add a record component `boolean tokenAccessible` (last). Every `new PageRule(...)`
  is in this file; pass `false` except in `parse` for a `@RequiresRole` page, where it
  is `pageClass.isAnnotationPresent(TokenAccessible.class)`.
- Keep `roles(Set<Role>, boolean)` returning `tokenAccessible = false`. Add a static
  factory `tokenRoles(Set<Role>, boolean platformBound)` with `true`, for tests.

### 2.3 Annotate

Add `@TokenAccessible` to the 27 display output pages: the 24 pages of the first list
in 1.3 that are not launchers, plus `AthleteFacingAttemptBoardPage`,
`AthleteFacingDecisionBoardPage` and `JuryScoreboardPage`. Not on the three launchers,
nor on `OBSMonitor` or `StreamingEventMonitor` (public).

### 2.4 Test

In `RouteAccessCoverageTest`, add `tokenAccessibleExactlyOnDisplayOutputPages`: over
`routeClasses()`, a page is token-accessible if and only if it is a subclass of
`AbstractDisplayPage` (check with `AbstractDisplayPage.class.isAssignableFrom`, no
instantiation); every token-accessible page has kind ROLES and lists DISPLAYS. Assert
the three launchers are not token-accessible.

Run `RouteAccessCoverageTest`. Stop and report.

## Phase 3: pure decision logic

New `owlcms/src/main/java/app/owlcms/access/TokenNavigation.java`. No Vaadin types,
no session access, no I/O except through parameters. It decides; the listener
(phase 6) acts.

### 3.1 Inputs

```java
/** What the URL says, before any authentication. */
sealed interface Inputs {
    record NoToken(String platform) implements Inputs {}      // platform may be null
    record Present(String token, String platform) implements Inputs {}
    record Malformed() implements Inputs {}                  // two or more token values
    record BadPlatform() implements Inputs {}                // see rules below
}

static Inputs readInputs(Map<String, List<String>> query, boolean platformBound,
        List<String> platformNames)
```

- `token`: absent or every value blank -> no token. Exactly one nonblank value ->
  that value, stripped. Two or more values -> `Malformed`.
- Platform, only when `platformBound` (otherwise `null`):
  - `fop` present: must have exactly one value; decode it with
    `URLDecoder.decode(value, UTF_8)` (the same decoding as
    `FOPParametersReader.readParams` and `AccessControlListener.targetPlatform`); it
    must be in `platformNames`. Otherwise `BadPlatform`.
  - `fop` absent: the only platform if `platformNames.size() == 1`, else
    `BadPlatform`.
  - Platform rules apply only when a token is present or the tab is token-bound; the
    listener calls `readInputs` only then.

With these rules the existing parameter readers pick the same platform afterwards
(same decoding; with a single platform there is no other choice), so they need no
change.

### 3.2 Accounts-mode decision

```java
sealed interface Decision {
    record NotToken() implements Decision {}   // continue with the existing logic
    record Refuse(int status, String reason, String username, String tokenId)
            implements Decision {}
    record Allow(TokenTab tab, boolean newlyBound) implements Decision {}
}

static Decision decideAccounts(PageRule rule, Map<String, List<String>> query,
        List<String> platformNames, TokenTab existing, String clientIp,
        TokenAuthenticator authenticator, Function<Long, UserAccount> findById)
```

`TokenTab` is defined in phase 4. Order (statuses: 401 unauthorized, 403 forbidden,
429 throttled; reasons go to the audit):

1. If the URL has no `token` value (absent or blank) and `existing == null`:
   `NotToken`.
2. If the URL has a `token` value and `!rule.tokenAccessible()`: `Refuse(403,
   "not-token-page", null, null)`. Do not authenticate.
3. If the URL has no `token` and `existing != null` and `!rule.tokenAccessible()`:
   `Refuse(403, "not-token-page", existing username, existing tokenId)`.
4. `readInputs(...)`: `Malformed` -> `Refuse(401, "malformed", ...)`; `BadPlatform`
   -> `Refuse(403, "platform", ...)`.
5. Token in URL: `authenticator.authenticate(token, clientIp)`:
   `Throttled` -> `Refuse(429, "throttled")`; `Invalid` -> `Refuse(401, "invalid")`;
   `Authenticated(account, token)` -> continue with that account and token id,
   `newlyBound = true`. (A token in the URL always wins over an existing binding.)
6. No token in URL, `existing != null`: `account = findById.apply(existing.accountId())`;
   refuse 401 `"revoked"` if the account is null, disabled, or no longer holds a token
   whose id equals `existing.tokenId()`; else continue, `newlyBound = false`.
7. Build `Principal.ofToken(account)`; if
   `!AccessPolicy.canOpen(principal, rule, platform)`:
   `Refuse(403, "forbidden", username, tokenId)`.
8. `Allow(new TokenTab(account.getId(), tokenId, principal), newlyBound)`.

### 3.3 PIN-mode decision

```java
sealed interface PinDecision {
    record NotApplicable() implements PinDecision {}
    record Refuse(int status, String reason) implements PinDecision {}
    record Accept() implements PinDecision {}
}

static PinDecision decidePin(PageRule rule, Map<String, List<String>> query,
        boolean displayPinExpected, boolean ipAllowedForDisplay, String clientIp,
        Predicate<String> displayPinMatches, LoginThrottle throttle)
```

1. No token value, or `!rule.tokenAccessible()`, or `!displayPinExpected`:
   `NotApplicable` (existing PIN flow; the token is ignored).
2. Two or more token values: `Refuse(401, "malformed")`.
3. `!ipAllowedForDisplay`: `Refuse(403, "display-ip")`.
4. Throttle key `"#displaypin@" + clientIp`: if
   `throttle.blockedSeconds(key, clientIp) > 0`: `Refuse(429, "throttled")`.
5. `displayPinMatches.test(value)`: `throttle.success(key, clientIp)`, `Accept`; else
   `throttle.fail(key, clientIp)`, `Refuse(401, "invalid")`.

### 3.4 Tests: `access/TokenNavigationTest`

Pure JUnit 4, like `TokenAuthenticatorTest` (reuse its `issue(...)` pattern with an
in-memory `Map<String, UserAccount>` and `new LoginThrottle(() -> 1_000_000L)`). Use
`PageRule.of(SomePage.class)` for real rules and `PageRule.tokenRoles(...)` for
synthetic ones. Cover:

- `readInputs`: absent, blank, one, two token values; `fop` absent with one and two
  platforms; unknown `fop`; two `fop` values; encoded name (`Plateau%20A`); page not
  platform-bound ignores `fop`.
- Accounts matrix on `PublicScoreboardPage`, `AthleteFacingDecisionBoardPage`,
  `JuryScoreboardPage`, `DisplayNavigationContent`, `RankingsPage`, for tokens of
  STREAMING, EXTERNAL, DISPLAYS, DISPLAYS scoped to A, JURY, COMPETITION_SUPERVISOR,
  RESULTS: STREAMING and EXTERNAL open the first and `RankingsPage` and are refused 403
  on the venue-only pages; DISPLAYS opens all output pages; scoped DISPLAYS is refused
  on platform B; JURY opens only the jury scoreboard; every token is refused 403 on
  the launcher, without calling the authenticator.
- Binding: no token plus binding re-validates; revoked token, replaced token,
  disabled and deleted account give 401; a role removed gives 403; a token in the URL
  replaces the binding.
- Invalid token 401, throttling 429 after `LoginThrottle.FREE_FAILURES` failures.
- PIN decision: each branch of 3.3.

Run `TokenNavigationTest`. Stop and report.

## Phase 4: per-tab token identity

### 4.1 `access/Principal.java`

- Add `TOKEN` to `AuthSource`.
- Add `static Principal ofToken(UserAccount account)`: like `of(account, null)` with
  source TOKEN. Javadoc: a principal authenticated by an account token for one tab;
  rebuilt from the account at each navigation.

Check that code testing `source() == ACCOUNT` keeps doing the right thing for TOKEN:
`AccountPasswords.mustChangePassword` (false: no forced password change for a token),
`AccessPolicy.requiresPlatformChoice` (false), `AccountLandingPolicy` (not used),
`AccountTokens.setOwnToken` (refused), `AccountModeAuthenticator.resolve` (not reached
for a bound tab), `AdminView.checkLocalAccess` (not reached). No change expected.

### 4.2 `access/TokenTab.java`

```java
/** The token identity of one browser tab; never holds the token itself. */
public record TokenTab(Long accountId, String tokenId, Principal principal) {
    public static TokenTab of(UI ui) { return ComponentUtil.getData(ui, TokenTab.class); }
    public static void bind(UI ui, TokenTab tab) { ComponentUtil.setData(ui, TokenTab.class, tab); }
}
```

The binding lives on the `UI`, so it ends when the tab closes or reloads (a reload
creates a new UI, and the URL's token authenticates again). It is never put in the
HTTP session.

### 4.3 `access/AccessUi.java`

Add `public static Principal currentPrincipal()`: the current UI's `TokenTab`
principal if any, else `OwlcmsSession.getPrincipal()`. Javadoc: UI thread only; a
token tab and a password tab in the same browser each see their own identity. Use it
in place of `OwlcmsSession.getPrincipal()` in the four methods of `AccessUi`, and in
`nui/displays/attemptboards/AbstractAttemptBoardPage` (the log line near
`displayInputAllowed`).

No other reader needs it: display pages have no `OwlcmsLayout`, and a token tab
cannot reach any other page.

## Phase 5: plain refusal view

### 5.1 Exception and view

- `access/TokenRefusedException.java`: `RuntimeException` with an `int status`
  (401, 403 or 429) and `status()`; message not used.
- `access/TokenRefusedView.java`: `Div implements HasErrorParameter<TokenRefusedException>`,
  no `@Route`, no `@ParentLayout`, no `OwlcmsLayout`. In `setErrorParameter`, read
  `parameter.getCaughtException()`, set the text from the status, return the status.
  Texts come from translation keys, added to the phase 1 TSV:

  | Key | English |
  | --- | --- |
  | `Access.Refused.Unauthenticated` | Unauthenticated |
  | `Access.Refused.Forbidden` | Forbidden |
  | `Access.Refused.TooManyRequests` | Too Many Requests |

  Nothing else on the page: no buttons, no account name, no token.
- `AccessControlListener` already returns at once for targets without `@Route`, so
  the error view is never itself checked.
- Vaadin discovers `HasErrorParameter` classes by annotation scanning, as for routes.
  Phase 7 checks the view is the one shown; if Vaadin shows its default error page
  instead, stop and report.

## Phase 6: wire `AccessControlListener`

File `access/AccessControlListener.java`. Keep its structure; insert after the
`PUBLIC` branch and before `resolvePrincipal`:

```java
UI ui = event.getUI();
String clientIp = AccessUtils.getClientIp();
Map<String, List<String>> query = queryParameters.getParameters();
List<String> platforms = OwlcmsFactory.getFOPs().stream().map(FieldOfPlay::getName).toList();
if (Config.getCurrent().isAccountsMode()) {
    // decideAccounts(rule, query, platforms, TokenTab.of(ui), clientIp,
    //     new TokenAuthenticator(UserAccountRepository::findByTokenHash, LoginThrottle.shared()),
    //     UserAccountRepository::findById)
    // Refuse: AuthAudit.tokenRefused(clientIp, path, reason, username, tokenId);
    //         event.rerouteToError(new TokenRefusedException(status), ""); return;
    // Allow:  TokenTab.bind(ui, tab); if newlyBound -> AuthAudit.tokenAccepted(...); return;
    //         (do not call OwlcmsSession.setPrincipal, do not run the password checks)
    // NotToken: fall through to the existing logic
} else {
    // decidePin(rule, query, PinModeAuthenticator.displayPinExpected(),
    //     displayIpAllowed, clientIp, AccessUtils::displayPinMatches, LoginThrottle.shared())
    // Refuse: audit, rerouteToError, return
    // Accept: OwlcmsSession.setDisplayAuthenticated(true); fall through (resolveDisplay
    //         then returns the display principal)
    // NotApplicable: fall through
}
queryParameters = withoutToken(queryParameters);   // nothing saves a token for the login redirect
```

- `withoutToken`: a private helper returning `QueryParameters` without the `token`
  key. Pass the result to `resolvePrincipal`, so `AccountModeAuthenticator` and
  `PinModeAuthenticator` never store a token in `OwlcmsSession.setRequestedQueryParameters`.
- Supporting changes:
  - `PinModeAuthenticator`: add `public static boolean displayPinExpected()` from the
    existing `pinExpected(config.getParamDisplayPin(), config.getDisplayPin())`.
  - `AccessUtils`: add `public static boolean displayPinMatches(String value)` with
    the comparison logic of `checkPassword` for the display PIN, without touching the
    session or the IP list, and with `MessageDigest.isEqual` for both comparisons
    (override in clear, stored hash via `Config.encodeUserPassword`). Do not change
    `checkPassword` itself.
  - `ipAllowedForDisplay`: true when `Config.getCurrent().getParamDisplayList()` is
    blank, else `AccessUtils.isIpAllowedForDisplay(clientIp)`.
- `AuthAudit`: add `tokenAccepted(String clientIp, String path, String username,
  String tokenId)` writing `auth.token-accepted` with `AuditActor.token(...)` and
  detail `path`. Update the Javadoc of `tokenRefused` with the reasons used here:
  `invalid`, `throttled`, `forbidden`, `not-token-page`, `malformed`, `platform`,
  `revoked`, `display-ip`. PIN-mode refusals use `tokenRefused` with a null username.
- The existing `logger.info("access denied user={} path={} platform={}", ...)` logs
  the path only; keep it that way. Never log `query`.

## Phase 7: redaction

- `shared/src/main/java/app/owlcms/utils/URLUtils.java`: add
  `public static String redactToken(String pathWithQuery)` replacing the value of
  every `token` parameter by `***` (regex on `(^|[?&])token=[^&]*`). The `shared`
  module has no tests: unit test it in `owlcms` (`access/UrlRedactionTest`).
- Use it wherever a URL with its query is logged or put in an error message:
  - `URLUtils.replaceState`: the "CRITICAL: replaceState would change FOP value!"
    message.
  - `apputils/queryparameters/FOPParametersReader.doUpdateUrlLocation`: the
    `updatingLocation` debug log.
  - `nui/shared/BaseNavigationContent` (`new location` debug), and the trace logs in
    `nui/referee/RefContent` and `nui/referee/JuryMobileContent`.
- Then `grep -rn 'getPathWithQueryParameters\|getQueryString\|getRequestURI' owlcms/src/main/java shared/src/main/java`
  and confirm no other log prints a query string.

## Phase 8: tests and checks

1. Server down. Run `TokenNavigationTest,TokenAuthenticatorTest,RouteAccessCoverageTest,`
   `AccessPolicyTest,AccountLandingPolicyTest,RoleAccountInitializationTest`, the
   redaction test, then `AllTests`. All must pass.
2. Ask the maintainer to start the server normally (full restart: new classes,
   annotations and an error view are not hot-swapped), and to provide tokens for a
   STREAMING, a DISPLAYS and an EXTERNAL account (User Accounts form, Update).
3. Accounts mode, with `curl -s -o /dev/null -w '%{http_code}'` for statuses and the
   built-in browser for rendering (2 platforms, or adapt the `fop` cases):

   | URL | Token | Expected |
   | --- | --- | --- |
   | `displays/publicScoreboard?fop=A` | STREAMING | page renders; after a reload, still renders; the address bar keeps `token` |
   | `displays/publicScoreboard?fop=A&video=true&public=true` | STREAMING | renders in video/public style |
   | `displays/athleteFacingDecision?fop=A` | STREAMING | 403, plain "Forbidden" |
   | `displays/athleteFacingDecision?fop=A` | DISPLAYS | renders |
   | `displays/juryScoreboard?fop=A` | STREAMING | 403 |
   | `displays/publicScoreboard?fop=A` | EXTERNAL | renders |
   | `displays`, `video`, `mobile/scoreboards`, `preparation`, `ref` | any valid | 403 |
   | `displays/publicScoreboard?fop=A` | wrong value | 401; after repeated tries, 429 |
   | `displays/publicScoreboard` (no `fop`, 2 platforms) | STREAMING | 403 |
   | `displays/publicScoreboard?fop=Z` | STREAMING | 403 |
   | `displays/publicScoreboard?fop=A` | none, no session | login page, then back to the page |
   | `competition/export/json/2` | EXTERNAL | 200 (EXTERNAL now has RESULTS) |
   | `competition/export/json/2` | STREAMING | 403 |

4. Same browser: a password tab logged in as admin and a token tab. The admin tab
   keeps its pages; in the token tab, navigating to `preparation` gives 403.
5. Revoke the STREAMING token: the open display keeps running; a reload gives 401.
6. Ordering check (temporary, then removed): add
   `logger.warn("{}", LoggerUtils.whereFrom())` at the start of
   `RefContent.setParameter` and of the `PublicScoreboardPage` constructor. Open `ref`
   and a STREAMING token page refused for platform reasons: the 403 appears and
   neither trace is logged. Remove both traces.
7. PIN mode with a display PIN (maintainer switches the mode):
   `displays/publicScoreboard?fop=A&token=<display PIN>` renders without a prompt;
   the same on `athleteFacingDecision`; a wrong value gives 401;
   `displays?token=<PIN>` shows the display PIN login page. Without a display PIN,
   displays open as before.
8. `grep -rn '<each token and the PIN>' owlcms/logs` finds nothing.
9. Audit: `auth.token-accepted` once per tab binding, `auth.token-refused` with the
   reasons above, `device=token id=<id>` and never a token.

## Phase 9: documents

- `compliance/120-TOKEN_PROTECTED_URLS.md`: in the Vaadin sections, mark the display
  pages as implemented (status line at the top and a short "State" note), as was done
  for the export.
- `src/main/markdown/ReleaseNotes.md` (skill `update-release-notes`), current beta
  section, one entry per theme:
  - STREAMING role: what it opens; EXTERNAL now means results plus streaming.
  - Display pages accept `?token=` in accounts mode, and the display PIN in PIN mode,
    so OBS opens them without a login prompt.
  - The Video Streaming page is now at `/video` (was `/video=true`).
- Tell the maintainer the TSV file to import.

Stop. The maintainer decides on commits (suggested: phase 1; phases 2 to 7 together;
phase 9).
