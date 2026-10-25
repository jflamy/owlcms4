# PEM-Based Account Mode

Status: implemented 2026-10-25; manual acceptance checks remain (see Acceptance).
Implement this first, before the remaining
[direct JSON boot work](060-DIRECT_JSON_BOOT.md). Account tokens are not a
prerequisite.

## Mode selection and admin override

- When `AuditIntegrity.current().forcedByKey()` is true and
  `UserAccountRepository.adminHasPassword()` is true, select
  `AccessMode.ACCOUNTS` unless the administrator has explicitly chosen PIN mode.
- The administrator must remain able to select PIN mode with the PEM key
  present. Do not lock the access-mode selector or reuse the audit-integrity
  lock explanation for access mode.
- Persist the explicit choice so startup does not undo it. The PEM continues
  to force audit integrity, independently of the chosen access mode.
- A PEM key without an admin password must not automatically select accounts
  mode: preserve existing access-mode behavior so the operator can complete
  admin setup.
- Do not require an admin password merely to generate or activate the PEM key.
- Apply the condition during normal startup, after JSON import using the
  imported accounts and explicit mode choice, and when admin setup is completed.
- Without a PEM key, keep the existing access-mode behavior.

## Implementation summary

- `Config.pinModeChosen` (nullable `Boolean`, persisted, exported in V1 and V2
  JSON) records an explicit administrator choice of PIN. Existing databases and
  older JSON exports have no value, so an incidental stored PIN does not block
  the PEM default. `Config.setCurrent` keeps the running value when an import
  carries none.
- `AccessModeResolver.resolve` decides the mode in force, in order: a valid
  `OWLCMS_ACCESSMODE` override; `pinModeChosen`; PEM key and admin password
  (accounts); otherwise the stored mode (PIN if unset). `Config.isAccountsMode()`
  and `getParamAccessMode()` delegate to it. The PEM is considered only once
  `AuditIntegrity` is initialized.
- Selecting PIN in System Settings or on the User Accounts page stores PIN and
  `pinModeChosen=true`; selecting accounts stores ACCOUNTS and clears the choice.
  Both selectors show the mode in force and are never locked. The System Settings
  form records a choice only when the mode value is changed.
- Setting the admin password that brings accounts mode into force initializes the
  role accounts and sends all sessions to the login page.
- `AccessStartup.run()` logs at info level when the audit key selected accounts mode.
- A JSON import from the UI still requires the restart it already asks for;
  startup then applies the imported accounts and choice.

## Temporary admin password

Status: implemented 2026-10-25.

Implementation: `AccountPasswords.ensureAdminPassword` (called from
`AccessStartup.run`) generates the password and logs it through a logger routed
to `owlcms.log` only; `UserAccount.mustChangePassword` is persisted and exported
in V1 and V2 JSON (absent in older JSON means false); `AccessControlListener`
forwards flagged account sessions to the account page; `LoginView` shows the
notice while `AccountPasswords.isTemporaryAdminPasswordPending()`.

Accounts mode with an admin account that has no password currently leaves
only backdoor addresses able to log in. The backdoor list is empty by default,
so in practice nobody can. Replace this lockout with a temporary password.

- **Trigger:** only at startup, including the restart that follows a JSON
  import from the UI and an `OWLCMS_LOADJSON` start. An import does not act on
  the accounts it contains until that restart. The check runs when accounts mode
  is required and the built-in admin has no password. Accounts mode is required
  when it is stored (including by an import) or set by `OWLCMS_ACCESSMODE`,
  and the admin has not explicitly chosen PIN. A PEM key alone is not a trigger:
  PEM without an admin password stays in the existing mode, as above.
- **Generation:** a random password from a cryptographically secure source,
  long enough for the existing password rules. Store only its hash, as for any
  password, and mark the admin account as requiring a password change.
- **Disclosure:** write the plaintext once, to `owlcms.log` only. Not to
  `startup.log`, the console window, the control panel, the audit trail or any
  export. Audit that a temporary password was generated, without the secret.
- **Login page notice:** the running server remembers that it generated the
  password at this startup. Until the admin replaces it, the login page tells
  the operator that a single-use admin password was generated and is in the
  server log `owlcms.log`. The notice never contains the password.
  Typical recovery: export, remove the admin password from the JSON, start on
  an empty database, load the JSON, restart, read the password from
  `owlcms.log`, log in as admin and set a new password. With `OWLCMS_LOADJSON`
  no second restart is needed: the import runs before the startup check, so the
  same start generates the password.
- **Forced change:** after logging in with it, the admin can only set a new
  password. Every other page is refused until that is done. Saving the new
  password clears the requirement. The same forced-change page applies to any
  account flagged as described below, not only the admin.
- **Persistence:** the change-required flag is persisted and exported with the
  account, so a recovered export still forces the change. A restart before the
  change does not generate another password: the admin then has a password.
- **No other fallback:** no PIN fallback and no localhost trust. This also covers
  a JSON recovery start whose admin password was removed or whose export has no
  admin account; OWLCMS recreates admin without a password, then generates one.
- Role accounts without passwords keep their current behavior.

## Self-service account page

Status: implemented 2026-10-25.

Implementation: `AccountView` (route `AccountPasswords.ACCOUNT_ROUTE`,
`@AuthenticatedPage`) shows the session account read-only and calls
`AccountPasswords.changeOwnPassword` with the session principal; no username or
id comes from the URL or the form. Non-account sessions only see an explanation.
After a forced change it navigates to the home page. `OwlcmsLayout` shows the
**Account** entry when accounts mode is active and the principal is an account.

Moved here from
[010](010-ACCOUNT_TOKENS.md), which adds the token section later.

- Individual named users get **Account** in the left-hand menu, for their own
  account only. They can change their own password: current password, new
  password and confirmation, using the existing password rules.
- They cannot edit their username, roles, platform grants, enabled state or
  other users. Enforce ownership on the server, not only by hiding controls.
  PIN/backdoor identities have no account page.
- Administrators keep managing all users on the **User Accounts** page.
- The forced-change page is this page restricted to the password change: while
  the change-required flag is set, every other page redirects to it. The
  current password is the one just used to log in, including the temporary
  admin password.
- Changing the password clears the change-required flag. Audit password
  changes by account, never password values.

## Account administration

Status: implemented 2026-10-25.

Implementation: the User Accounts page (`AccountsContent`) has two tabs, each
a full-page grid: **Generic accounts** and **User accounts**. The selected tab
is kept in the URL (`preparation/accounts/0` or `/1`), as on the competition
page, so reloading or a link opens the same tab. `UserAccount.generic`
(persisted, exported) marks accounts created by OWLCMS for a role and platform;
the built-in admin is always generic. Role accounts generated by OWLCMS are
generic; **Add** in each grid creates an account of that group. There is no
migration: development databases are reloaded and their generic accounts
regenerated. Person accounts are named with initials, for example `speaker-rm`.
The generated Competition Supervisor account is `competition_supervisor`
(role `COMPETITION_SUPERVISOR`, which opens the Competition Supervisor page).
`COMPETITION_DIRECTOR` is the rules-defined official: it gives preparation and
results only, and no account is generated for it; grant it by hand if needed.
`AccountEditingFormFactory` has the must-change checkbox, which can be checked
only if the account has a password after the save (existing hash or a new one
typed in the form). Each grid has its own **Clear All Passwords** button
(`AccessStartup.clearPasswordsInBackground` →
`AccountPasswords.clearPasswords(generic, …)`); the access-mode switch and
**Recompute Generic Accounts** are on the generic grid.

**Recompute Generic Accounts** (`UserAccountRepository.recomputeGenericAccounts`)
brings the generic accounts in line with the current platforms without touching
passwords: a generic account whose single grant still matches a role and
platform is kept, and renamed when the generated name changes (for example
`announcer` becomes `announcer-RED` when a second platform is added). Missing
accounts are created; generic accounts that match nothing, such as those of a
deleted platform, are deleted. The built-in admin and user accounts are never
changed. Adding or renaming a platform in accounts mode creates and renames
generic accounts at once, without deleting any. Example: RED and BLUE, then
WHITE added, gives three sets; deleting RED and recomputing leaves two.

- **Must change password:** add a checkbox to the account form on the User
  Accounts page. An administrator can set or clear it for any account that has
  a password. The account's next login goes to the forced-change page. Setting
  a new password through that page clears the flag; an administrator setting a
  password in the form leaves the checkbox as chosen.
- **Clear all passwords:** one action per grid, with a confirmation dialog,
  that removes the password of every account in that grid except the built-in
  admin and the account performing the action. Cleared accounts cannot
  log in until an administrator sets a new password in the account form; the
  existing "password required" indicator shows them. Sessions of cleared
  accounts are logged out. Audit the action, the group and the number of
  accounts cleared, never password values.
- Accounts have no display name: the username identifies the person.
- Use the normal translation process for new labels and messages.

Cloud note: on a hosted deployment the operator reads `owlcms.log` inside the
container, for example through `fly ssh console`.

## Acceptance

Implemented; the manual checks below remain. Non-UI coverage: `AccountPasswordsTest`, `JSONExportImportTest`,
`AccountPersistenceTest`, `RoleAccountInitializationTest`.

- Temporary admin password: verify generation with stored, imported and
  environment accounts mode; the secret appears only in `owlcms.log`; login
  forces a change before any other page; no regeneration on restart before the
  change; the flag survives export and recovery; no generation for PEM-only or
  explicit PIN.
- Self-service account page: named users see only their own account, can change
  only their own password, and cannot reach role, grant or other-user editing
  even by URL or request tampering; PIN/backdoor sessions have no account page.
- Account administration: the must-change checkbox forces the change at the
  next login for any account; each grid's Clear All Passwords clears only that
  grid, spares the built-in admin and the acting account, logs out cleared
  accounts, and leaves them unable to log in until an administrator sets a
  password. An existing database shows its role accounts under Generic
  accounts after the first restart.

- Verify PEM startup with and without an admin password and completion of admin
  setup. No-password PEM startup must not force accounts mode or prevent setup
  through the existing access mode.
- Verify that an admin can revert to PIN with the PEM present and that the
  explicit choice survives restart.
- Verify JSON imports that replace accounts with either password state and
  preserve an explicit admin PIN choice through export and recovery.
- Verify that audit integrity remains enabled in either access mode with the
  PEM present, and that behavior without a PEM is unchanged.
- Record access-mode behavior on macOS, Windows and Linux.
