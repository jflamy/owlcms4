# 132 — Remembered logins stored on the server

Status: implemented. Browser check completed for issuance, expiry, restart and Administration revocation. The forced\n+login-after-logout assertion remains pending on a server launched without the development PIN override. Base commit:\n+`51dcfda42` (Remove the remember and logout servlets), branch `dev69`.

## Purpose

"Remember this browser" (officials PIN, display PIN) and "Keep me logged in for today" (accounts) are a
convenience during a competition, or during setup once the release in use has been chosen. A remembered login
survives an application restart. It does not survive an export and import.

Today the cookie is signed (HMAC) with a secret stored in the database. Anyone holding a database copy (H2 backup)
can make valid cookies, the server cannot list or revoke remembered browsers, and the lifetime is whatever the
cookie says. This change replaces the signed cookie with a random token checked against a server-side table, the
standard "selector/validator" design (Spring Security's persistent token approach).

## Decisions (made with the maintainer)

- One table for both access modes. One row per browser and kind; two browsers of the same user are two rows.
- No key of any kind. Nothing is signed; the audit key is not involved. Do not derive anything from it.
- Lifetime counted in midnights, in the competition time zone (`Config.getTimeZone()`), else the server zone.
  A code constant `PIN_DAYS = 1` for the officials and display PINs: 1 means the next midnight, as for accounts.
  Accounts are always 1.
- Rows are never exported. Both imports delete every row. A database reset loses them too.
- The Administration page gets only a "Forget all remembered browsers" button, no list.
- No device names, no token rotation on use.
- The old `Config.rememberSecret` field is deleted. The column stays in older databases, unused. No migration.
- Cookies issued by earlier versions have no row: they are ignored and deleted; those users log in once.

## Rules for the implementer

Read and follow `AGENTS.md` and `.github/copilot-instructions.md` first. In particular:

- No fully-qualified class names in Java; add imports.
- No hard-coded UI strings: use `Translator.translate`. New keys go in a TSV file (step 9), never in
  `translation4.csv`.
- Logging style: skill `owlcms-logging-format` (`logger./**/warn(...)` for warnings).
- After editing Java, check errors with the Problems tool (skill `verify-java-fix`).
- Run tests only through Maven, focused: skill `run-maven-test`. Never `mvn package`.
- Tests must not create Vaadin UI objects: skill `add-test-case`. Database tests start with `TestStartup.init()`.
- After a Maven test run, the dev server on port 8081 must be restarted before use (Maven replaces its classes).
- Do not commit or push without the maintainer's explicit authorization. Never add a `Co-authored-by` trailer.

## Design

### Cookie

Same names as now (`RememberedBrowser.Kind`: `owlcms-officials`, `owlcms-display`, `owlcms-account`).

Value: `r1.<selector>.<validator>`

- `selector`: 16 random bytes (`SecureRandom`), Base64 URL-safe without padding. Identifies the row.
- `validator`: 32 random bytes, same encoding. Never stored.
- Base64 URL-safe has no `.`, so `split("\\.", -1)` gives exactly 3 parts.

Attributes: `HttpOnly`, `SameSite=Lax`, `Secure` when the request is HTTPS (`request.isSecure()` or
`X-Forwarded-Proto: https`), path = context path or `/`, `Max-Age` = seconds until the row expires. The existing
`RememberCookies.cookie(...)` helper already does this; keep it.

### Table

New entity `app.owlcms.data.access.RememberedLogin` (new package `data/access`, like `data/account`):

| Field | Type | Notes |
|---|---|---|
| `selector` | `String` | `@Id`. Row identity. |
| `validatorHash` | `String` | SHA-256 of the validator, Base64 URL-safe. |
| `credentialCheck` | `String` | SHA-256 of `validator + "\n" + credentialBasis`, Base64 URL-safe. See below. |
| `kind` | `RememberedBrowser.Kind` | `@Enumerated(EnumType.STRING)`. |
| `accountId` | `Long` | Account rows only, else null. |
| `loginPlatform` | `String` | Account rows only: the platform chosen at login, or null. |
| `issuedAt` | `Instant` | |
| `expiresAt` | `Instant` | Computed at creation (see Expiry). |
| `lastUsedAt` | `Instant` | Updated on each successful lookup. |
| `clientIp` | `String` | Of the last use. |
| `userAgent` | `String` | `@Column(length = 512)`, truncated to 512. |

Register it in `JPAService.entityClassNames()` (`owlcms/src/main/java/app/owlcms/data/jpa/JPAService.java:301`).
Plain `@Entity`, no `@Cacheable`. Not serialized to JSON; add nothing to the export DTOs.

`credentialBasis`:
- officials or display: `RememberedBrowser.pinBasis(kind, config)` (existing, keep it). It is
  `"stored:" + PIN hash`, or `"override:" + PIN` when the PIN comes from the environment.
- account: the account's `getPasswordHash()`.

Why the validator is mixed in: the override PIN is in clear inside the basis. Hashing the basis alone would put a
guessable SHA-256 of the PIN in the database. Mixed with the 256-bit validator, a database copy reveals nothing.
Why a separate column from `validatorHash`: a wrong validator (possible theft) and a changed PIN or password are
different events; only the first is audited.

Changing the PIN or the password therefore invalidates the rows without any hook in the code that changes them.
The one-time PIN hash upgrade (`AccessUtils.upgradePin`) also changes the stored hash, so remembered browsers are
forgotten once after it. Accepted.

### Expiry

`expiresAt = LocalDate.ofInstant(now, zone).plusDays(days).atStartOfDay(zone).toInstant()`

- `days`: `RememberedLogins.PIN_DAYS` (= 1) for officials and display, `RememberedLogins.ACCOUNT_DAYS` (= 1) for
  accounts.
- `zone`: `RememberedAccount.zone(config)` (existing: competition time zone, else `ZoneId.systemDefault()`).
- If the time zone setting changes later, existing rows keep their expiry.

### Lookup (on a new session)

Given the request cookies, the kind, the config and `now`:

1. No cookie of that kind: not remembered.
2. Malformed value (not `r1`, not 3 parts): delete the cookie on the response; not remembered.
3. No row for the selector: delete the cookie; not remembered.
4. `validatorHash` differs (compare with `AccessUtils.constantTimeEquals`), or the row's kind differs: delete the
   row and the cookie, audit `auth.remember-mismatch`, `logger./**/warn`; not remembered.
5. `expiresAt <= now`: delete the row and the cookie; not remembered.
6. `credentialCheck` differs from the value computed with the current basis: delete the row and the cookie;
   not remembered (log at info, no audit: the PIN or password was changed, which is audited elsewhere).
7. Account rows only:
   - account not found, or `!RememberedAccount.isRememberable(account)`: delete the row and the cookie.
   - `!config.allowsRememberedAccountLogins()`: not remembered, keep the row (the setting may come back on).
   - if `AccessPolicy.requiresPlatformChoice(principal)`, the stored `loginPlatform` must be in
     `AccessPolicy.loginPlatformChoices(principal, platformNames)`, else not remembered (keep the row). Copy this
     logic from the current `RememberedAccount.restore(...)`.
8. Success: set `lastUsedAt = now`, `clientIp`, `userAgent`; save; return the row.

"Delete the cookie on the response": use `VaadinServletResponse.getCurrent()` when not null (the authenticators run
during navigation, which has a response).

### Issue

On a successful PIN login with the box checked (`LoginView.rememberThisBrowser`) or an account password login with
"Keep me logged in" (`LoginView.completeAccountLogin`):

1. If the request already presents a cookie of that kind whose row validates (steps 3-4), delete that row: the
   browser replaces its own login instead of adding one.
2. Delete expired rows (`deleteExpired(now)`).
3. Create the row and set the cookie.
4. Cap: if the kind is officials or display and more than 200 rows of that kind exist, delete the oldest by
   `issuedAt`; for accounts, more than 20 rows for that account.
5. Audit as today: `AuthAudit.browserRemembered` (PIN) / `AuthAudit.accountLoginRemembered` (account, with the
   expiry as ISO date-time).

### Forget

- Unchecking the box on a remembered browser (`LoginView.forgetThisBrowser`): delete the row behind the presented
  cookie (only if its validator matches) and delete the cookie.
- Logout (`SessionLogout.redirectAllAndInvalidate` → `RememberCookies.forgetAll`): same, for each kind presented.
- Administration "Forget all remembered browsers": delete every row; audit `auth.remembered-browsers-forgotten`
  with the count and the admin's `AuditOrigin`.
- Switching the access mode (`SecuritySettings.switchAccessMode`, when the outcome is `CHANGED`): delete every row
  (the other mode's rows are useless). Log only; the mode change itself is audited.
- Imports: delete every row inside the import transaction.
- Startup: `deleteExpired(Instant.now())` in `Main.completeConfig()` (`owlcms/src/main/java/app/owlcms/Main.java`,
  after `AuditIntegrity.initialize(...)`).

No scheduler: the lookup rejects expired rows; cleanup only bounds the table size.

## Steps

Do them in order. After each step: Problems tool clean on the touched Java files.

### 1. Restore the working tree

The working tree holds earlier experiments (audit-key derivation, 21-day issue date) that this design replaces.
With the maintainer's agreement (given), restore these files to `HEAD`:

```bash
git checkout HEAD -- compliance/done/130-ACCESS_CONTROL_REMAINING.md compliance/done/136-CONFIG_COARSE_CONFLICT.md docs/100-AdvancedTopics/030-AdvancedSystemSettings.md owlcms/src/main/java/app/owlcms/access/AccountModeAuthenticator.java owlcms/src/main/java/app/owlcms/access/RememberCookies.java owlcms/src/main/java/app/owlcms/access/RememberedAccount.java owlcms/src/main/java/app/owlcms/access/RememberedBrowser.java owlcms/src/main/java/app/owlcms/data/config/Config.java owlcms/src/test/java/app/owlcms/access/RememberedAccountTest.java owlcms/src/test/java/app/owlcms/access/RememberedBrowserTest.java src/main/markdown/ReleaseNotes.md
```

Then `git status --short` must show only this spec file (and the index line of step 11) as changed.

### 2. Entity and repository

- Create `owlcms/src/main/java/app/owlcms/data/access/RememberedLogin.java` (fields above, getters/setters,
  no-arg constructor).
- Create `owlcms/src/main/java/app/owlcms/data/access/RememberedLoginRepository.java`, using
  `JPAService.runInTransaction(em -> ...)` like `UserAccountRepository`:
  - `RememberedLogin find(String selector)`
  - `RememberedLogin save(RememberedLogin row)` (`em.merge`)
  - `void delete(String selector)`
  - `int deleteAll()` and `int deleteAll(EntityManager em)` (for the imports, inside their transaction)
  - `int deleteExpired(Instant now)` (JPQL `delete from RememberedLogin r where r.expiresAt <= :now`)
  - `void evictBeyond(Kind kind, Long accountId, int max)`: keeps the `max` newest by `issuedAt`; for PIN kinds
    filter by kind, for accounts by `accountId`.
- Register the entity in `JPAService.entityClassNames()`.
- Delete `Config.rememberSecret` with its getter and setter (`owlcms/src/main/java/app/owlcms/data/config/Config.java`,
  field near line 235, accessors near line 744).

### 3. The store

Create `owlcms/src/main/java/app/owlcms/access/RememberedLogins.java` (pure logic plus repository calls, no Vaadin
types, so it can be tested):

```java
public final class RememberedLogins {
	public static final int PIN_DAYS = 1;
	public static final int ACCOUNT_DAYS = 1;

	/** What a successful lookup found. */
	public record Found(RememberedLogin row, UserAccount account) {}

	/** @return the new cookie value; the row is saved */
	public static String issue(Kind kind, UserAccount account, String platform, String credentialBasis,
	        Instant now, ZoneId zone, String clientIp, String userAgent);

	/** @return the row and account, or null; outcome tells the caller whether to delete the cookie */
	public static Lookup lookup(String cookieValue, Kind kind, String credentialBasis /* null for accounts */,
	        Config config, Instant now, String clientIp, String userAgent);

	/** Deletes the row behind the cookie if its validator matches. @return true if a row was deleted */
	public static boolean forget(String cookieValue, Kind kind);

	public static int forgetAll(AuditOrigin origin);

	static Instant expiry(Instant now, ZoneId zone, int days);
	static String sha256(String value);
}
```

`Lookup` is a small record: `Found found` (null when not remembered) and `boolean deleteCookie`.
For accounts, `lookup` finds the account with `UserAccountRepository.findById(row.getAccountId())` and computes the
basis from `account.getPasswordHash()`; it applies step 7 of the Lookup section and returns the platform check
inputs. The principal itself is built by `AccountModeAuthenticator` (step 5), keeping `Principal` logic there.

Audit methods to add in `owlcms/src/main/java/app/owlcms/audit/AuthAudit.java`, following `writeFor` and
`browserRemembered`:
- `rememberMismatch(String clientIp, String kind)` → action `auth.remember-mismatch`, actor `AuditActor.http(ip)`.
- `rememberedBrowsersForgotten(AuditOrigin origin, int count)` → action `auth.remembered-browsers-forgotten`.

### 4. Cookies on the response

Rewrite `owlcms/src/main/java/app/owlcms/access/RememberCookies.java` (keep its public method names, so callers
change little):
- `rememberPin(kind, request, response, host)`: basis = `RememberedBrowser.pinBasis(kind, config)`; null → return.
  Otherwise `RememberedLogins.issue(...)` with `PIN_DAYS`, add the cookie with `Max-Age` until the expiry.
- `rememberAccount(account, platform, request, response, host)`: same checks as now (accounts mode, setting,
  `isRememberable`), then `issue(...)` with the password hash.
- `forget(kind, request, response, host)` and `forgetAll(request, response)`: read the presented cookie(s) from
  `request.getCookies()`, call `RememberedLogins.forget`, then add the deletion cookie(s).
- Add `deleteCookie(kind, request, response)` for the authenticators.
- Remove `rememberSecret(...)`.
- User agent: `request.getHeader("User-Agent")`.

### 5. Authenticators and login page

- `owlcms/src/main/java/app/owlcms/access/PinModeAuthenticator.java`: `remembered(kind, config)` (line ~160)
  reads the cookie, calls `RememberedLogins.lookup` with `pinBasis(kind, config)`, deletes the cookie when told,
  returns `found != null`. Callers stay as they are. `loginRemembered(kind)` must call the lookup again (the click
  request carries the cookies) instead of trusting the page's earlier answer.
- `owlcms/src/main/java/app/owlcms/access/AccountModeAuthenticator.java`: `rememberedLogin()` uses the lookup,
  builds `Principal.of(account, null)`, applies the platform rule (from `RememberedAccount.restore`), then sets the
  session as it does now and audits `loginSucceeded(... "remembered-browser" ...)` as now.
- `owlcms/src/main/java/app/owlcms/nui/home/LoginView.java`: no change expected beyond compile fixes; it already
  calls `RememberCookies`.
- `owlcms/src/main/java/app/owlcms/access/SessionLogout.java`: no change expected.

### 6. Lifecycle hooks

- Imports, in the same transaction, right after `UserAccountRepository.replaceAll(em, ...)`:
  - `owlcms/src/main/java/app/owlcms/data/export/CompetitionData.java` (~line 351)
  - `owlcms/src/main/java/app/owlcms/data/export/v2/CompetitionDataV2.java` (~line 349)
  
  Call `RememberedLoginRepository.deleteAll(em)` unconditionally (also when the import has no accounts: the PIN
  rows must go too). Place it outside the `if (updated.getAccounts() != null)` block.
- `SecuritySettings.switchAccessMode`: after `ConfigRepository.apply(...)`, `RememberedLoginRepository.deleteAll()`.
- `Main.completeConfig()`: `RememberedLoginRepository.deleteExpired(Instant.now())` after
  `AuditIntegrity.initialize(...)`.

### 7. Administration button

In `owlcms/src/main/java/app/owlcms/nui/admin/AdminView.java`, next to `rememberAccountLoginsSwitch(...)` (~line
126 where the security tab is built, method at ~line 243): a button "Forget all remembered browsers", shown in both
access modes. On click: confirmation (reuse the existing confirmation pattern of the page, e.g.
`confirmSecurityChange` or `ConfirmationDialog`), then `RememberedLogins.forgetAll(AuditOrigin.fromUi(
"Administration security settings"))`, then a notification with the count. Capture the origin on the UI thread.
Labels via `Translator.translate` with the keys of step 9.

### 8. Remove the signed-cookie code

- `owlcms/src/main/java/app/owlcms/access/RememberedBrowser.java`: keep `Kind` and `pinBasis`. Delete `LIFETIME`,
  `VERSION`, `RANDOM`, `newSecret`, `cookieValue`, `isValid`, `remembers`, `mac`, `hmac`. Update the class comment.
- `owlcms/src/main/java/app/owlcms/access/RememberedAccount.java`: keep `isRememberable` and `zone`. Delete
  `VERSION`, `NO_PLATFORM`, `Claim`, `cookieValue`, `nextMidnight` (replaced by `RememberedLogins.expiry`),
  `restore`, `parse`, `encode`, `mac`. Update the class comment.
- `grep -rn "rememberSecret\|RememberSecret\|newSecret\|RememberedBrowser.remembers\|RememberedAccount.restore"`
  over `owlcms/src` must return nothing.

### 9. Translations

Skill `update-translations`. Create `shared/src/main/resources/i18n/remembered_logins_translations.tsv` with the
header line of `translation4.csv` (tab-separated) and rows for:

- `Admin.ForgetRememberedBrowsers`: "Forget all remembered browsers"
- `Admin.ForgetRememberedBrowsersHelp`: "Every browser must enter its PIN or password again."
- `Admin.ForgetRememberedBrowsersConfirm`: "Forget every remembered browser?"
- `Admin.ForgetRememberedBrowsersDone`: "{0} remembered browsers forgotten"

English in every column when a translation is not known. Do not edit `translation4.csv`.

### 10. Tests

Replace `owlcms/src/test/java/app/owlcms/access/RememberedBrowserTest.java` and `RememberedAccountTest.java`
with `owlcms/src/test/java/app/owlcms/access/RememberedLoginsTest.java`. Set up like `SecuritySettingsTest`
(`TestStartup.init(); JPAService.init(true, true); Config.initConfig();`, `JPAService.close()` after). Keep the
existing test `onlyEnabledNonAdministratorAccountsWithAPasswordAreRememberable` (move it). Cases:

1. Issue then lookup with the same basis: found; `lastUsedAt` updated.
2. Wrong validator (same selector): not found, row deleted, `deleteCookie` true.
3. Unknown selector and malformed values (`null`, `"garbage"`, old `v1.x.y`): not found, `deleteCookie` true
   except for `null`.
4. Lookup at `expiresAt`: not found, row deleted.
5. Changed basis (another PIN hash; for an account, another password hash): not found, row deleted.
6. Account disabled or granted the administrator role: not found, row deleted. Setting off: not found, row kept.
7. `expiry(now, zone, 1)` is the next midnight; `expiry(now, zone, 21)` across a daylight-saving change is still
   a midnight (use `America/Toronto`, a date in early March).
8. Cap: 201 officials rows → 200 remain, the oldest gone.
9. Re-issue from a browser presenting its valid cookie: the old row is gone, one row remains.
10. `forget` with a wrong validator deletes nothing.
11. Imports: call `RememberedLoginRepository.deleteAll(em)` path through the smallest existing import test if one
    exists (search `src/test` for `CompetitionDataV2`); otherwise test `deleteAll` directly.

Run:

```bash
mvn -pl owlcms -am -Dtest=RememberedLoginsTest,SecuritySettingsTest,IwfComplianceCheckTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test
```

Read `owlcms/target/surefire-reports/app.owlcms.access.RememberedLoginsTest.txt`.

### 11. Documentation

- `docs/100-AdvancedTopics/030-AdvancedSystemSettings.md` line ~38 ("Remember this browser"): replace "for 30 days"
  with "until midnight"; add: "Forget all remembered browsers on the Administration page makes every browser enter
  its PIN or password again. Remembered browsers are not kept by an export and import."
- `src/main/markdown/ReleaseNotes.md`: line ~21 "(30 days for a PIN, until midnight for an account)" → "(until
  midnight)"; line ~112 "for 30 days" → "until midnight". Add one development-log entry under the latest version
  (skill `update-release-notes`): remembered logins are stored on the server; a database copy cannot be used to
  log in; "Forget all remembered browsers" on the Administration page.
- `compliance/done/130-ACCESS_CONTROL_REMAINING.md` ~line 313-317: describe the table instead of the HMAC cookie;
  point to this spec.
- `compliance/done/136-CONFIG_COARSE_CONFLICT.md`: line ~82 drop "and Remember Browser (only when no secret exists
  yet)" (and "Two ... writers are" → "One ... writer is"); line ~183 drop `RememberCookies` from the list; line
  ~272 drop "or a Remember Browser secret".
- `compliance/000-INDEX.md`: under "1xx", one line linking this spec while active; move it to `done/` when finished.

### 12. Browser check (needs the maintainer)

Ask the maintainer to restart the 8081 server, then, at 1400x900 (skill `open-browser-1920x1080` is for displays
only):

1. Set an officials PIN on the Administration page (ask first: this changes the running database).
2. Log in with "Remember this browser" checked. The response to the Login click sets `owlcms-officials=r1....`.
3. Restart the server: the browser gets in without the PIN.
4. Log out: the cookie is deleted; the PIN is asked again.
5. Log in again remembered, then "Forget all remembered browsers": a new browser session asks for the PIN.

## Out of scope

- A list of remembered browsers with per-browser revoke (possible later: the rows have what it needs).
- Device names, token rotation, new-device notifications.
- Removing the old `rememberSecret` column from existing databases.
