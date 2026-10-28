# PEM-Based Account Mode

Status: implemented 2026-10-25; manual acceptance run and findings resolved 2026-10-28.
Implement this before the remaining [direct JSON boot work](160-DIRECT_JSON_BOOT.md).
[110](110-ACCOUNT_TOKENS.md) later adds the token section to the account page.

## Done

- **Mode selection:** a PEM key plus an admin password selects accounts mode,
  unless the administrator explicitly chose PIN (`Config.pinModeChosen`,
  persisted and exported). Order in `AccessModeResolver`: `OWLCMS_ACCESSMODE`,
  explicit PIN choice, PEM and admin password, stored mode. Selectors are never
  locked; the PEM still forces audit integrity in either mode.
- **Temporary admin password:** generated at startup when accounts mode is
  required and admin has no password; plaintext written to `owlcms.log` only;
  login page notice; forced change persisted as `UserAccount.mustChangePassword`.
- **Account page:** `AccountView` lets a named user change only their own
  password; also serves as the forced-change page.
- **Account administration:** Generic and User account tabs, must-change
  checkbox, per-grid Clear All Passwords, Recompute Generic Accounts.

Unit coverage: `AccountPasswordsTest`, `JSONExportImportTest`,
`AccountPersistenceTest`, `RoleAccountInitializationTest`.

## Manual acceptance, 2026-10-28

Run in the VS Code built-in browser against a PEM-configured development server.

- [x] **PEM startup:** without an admin password, accounts mode is not forced;
  setting the admin password brings it into force at once, creates the generic
  accounts and sends sessions to the login page.
- [x] **PIN override:** with the PEM present, PIN can be chosen from the User
  Accounts page; the choice survives restart, export and re-import, and setting
  an admin password afterwards keeps PIN. Accounts mode cannot be enabled while
  the admin has no password.
- [x] **JSON import without admin password:** replaces the accounts; the admin
  shows "password required".
- [ ] **JSON import with admin password:** not run.
- [x] **Audit integrity:** intact and configured-key in accounts and PIN mode.
- [ ] **Audit integrity without a PEM:** not run.
- [x] **Temporary admin password:**
  - generated with stored accounts mode after an import and with
    `OWLCMS_ACCESSMODE=ACCOUNTS` overriding an imported PIN choice;
  - not generated for PEM-only or explicit PIN;
  - production `logback.xml` sends the secret to `owlcms.log` only; it is absent
    from `startup.log`, the audit trail and the login page, whose notice works;
  - login forces a change: every other route returns to the account page;
  - no regeneration before the change and the flag surviving export are
    covered by `AccountPasswordsTest` and `JSONExportImportTest`, not manually.
- [x] **Account page:** a named user sees only their own account, changes only
  their own password (audited `how=self`), and gets Access Denied on
  preparation, accounts, settings, admin, audit and results routes. Request
  tampering is left to code review; PIN sessions are covered by
  `AccountPasswordsTest`.
- [x] **Account administration:**
  - the must-change checkbox forces the change, including on a session already
    open;
  - Clear All Passwords cleared only the User accounts grid, logged out the
    open session of the cleared account (audited with the reason), and its
    login then failed.

## Findings, resolved 2026-10-28

1. Account and access-mode changes are audited where they are made
   (`UserAccountRepository`, `ConfigRepository`), whatever triggered them, with
   the grants given and revoked and the cause passed down as an `AuditOrigin`
   (59e1589c6).
2. Import Database writes `application.import` with file name, size and
   SHA-256 of the JSON bytes, who imported it and the cause (2cd4a2f1c).
3. Editing forms show one translated notification on save (0d1da1c94).
4. During a forced password change the menu shows only Account, color scheme
   and Log out (3c8d242f3).

Detecting an admin reset done through a modified export, and the support
procedures, are in [145](145-SIGNED_EXPORTS.md).

Cloud note: on a hosted deployment, read `owlcms.log` inside the container, for
example through `fly ssh console`.
