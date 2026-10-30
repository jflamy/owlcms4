<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**Development log**

- 69.0.0-beta02: New Security section on the Administration page.
  - Switching the access mode from PIN to Account logins/tokens asks for the control panel token.
  - "Check IWF compliance" lists the IWF requirements met or not
  - Database exports include the access mode; an import restores the exported settings as they are.
  - `OWLCMS_ACCESSMODE` is no longer read: the access mode is set only on the Administration page or by an import.
- 69.0.0-beta02: PIN mode
  - The control panel token is accepted as the officials PIN, so a forgotten PIN can be replaced in the settings.
  - The officials PIN, display PIN and MQTT password are stored as PBKDF2 hashes; older values are converted the next time they are entered. PIN logins are throttled.
  - The backdoor address list (`OWLCMS_BACKDOOR`) is removed. The Administration page no longer depends on the browser's address; it is shown only when OWLCMS was started by the control panel.

- 69.0.0-beta02: The update keys of forwarding connections encrypted in Accounts mode.

- 69.0.0-beta02: OWLCMS 69 requires control panel 3.10.0 or later, which gives OWLCMS a new control panel token at each launch. The home page says so when an older control panel is used.

- 69.0.0-beta02: MQTT refereeing devices are told when a competition has no jury (jury size 0) instead of being sent a jury size of 3.
  - Blue Owl versions up to 3.0.0 treat it as a jury of 3, as before.

- 69.0.0-beta02: Admin password emergency access: logging in as `admin` with the control panel token as the password always works, for the first password of a new installation or a forgotten one.
  - The admin must then set a new password, after which OWLCMS restarts, ending every session.
  - The single-use admin password is no longer written to `owlcms.log`.

- 69.0.0-beta02: Robustness/correctness of user accounts management and audit trail

- 69.0.0-beta02: Audit logs are always sealed, with the audit signing key (PEM) when present, else with the built-in key, which only allows testing.
  - A PEM no longer turns on `iwfCompliance` or accounts mode.

- 69.0.0-beta02: Some URLs accept a `?token=` from a user account, so OBS and scripts work without a login.
  - Tokens are generated and revoked by going on the user that would normally use the pages and using Generate Token
  - Navigation and operator input pages never accept a token.
  - Streaming display pages need a STREAMING token (or DISPLAYS)
  - Venue-only pages (athlete-facing boards, jury scoreboard) need DISPLAYS.
  - Competition export and H2 database download URLs need a RESULTS token.
  - In PIN mode, the display PIN can be passed as `?token=` on display pages.

- 69.0.0-beta01:  Registration spreadsheets accept birth dates typed with `/`, `.`, `-` or spaces,
  - DMY or MDY formats are accepted, based on what is in the file (>13 indicates days)
  - If both formats are found, spreadsheet is rejected. If all dates are ambiguous the "official" order for the location is used.
  - YYYY-MM-DD are accepted.

- 69.0.0-beta01: TCRR Compliance feature toggle to hide configuration sections or restrict acceptable values

- 69.0.0-alpha04: Adjudication screen improvements
  - Clear all decisions when review vote is called
  - Only jury decisions are shown in main area if only the jury is voting
  - Initial referee decisions are always shown at top right of the screen

**New in Release 69.0**

- Complies with the October 2026 IWF Technical and Competition Rules and Regulations (TCRR).
  - TCRR compliance mode fixes the Jury at four members, enforces the 20 kg rule and IWF team points, uses GAMX for Best Athlete, and disables IMWA rules, gender-inclusive competition rules and alternative lifting orders.
- IWF TCRR 2026 seven-person Adjudication Panel (jury size 4 with video playback).
- Jury votes shown on attempt boards and scoreboards during a jury review.
- Tracker forwarding uses protocol 69.0.0 and requires OWLCMS Tracker 3.0.0-beta01 or newer (tracker-core 2.0.0-beta01 or newer); 
  earlier tracker versions are not compatible.
- Jury Control page replaces the Jury Console; "Jury President" is now "Jury Chair".
- Named accounts with role-based access; new Accounts Management page.
- Competition Supervisor page and passive speaker view.
- Tamper-resistant, machine-readable audit trails for compliance.
- New Backup page in the Admin section.
- owlcms can start directly from a database export (`OWLCMS_LOADJSON`).
- Event forwarding keys are encrypted in the database and in JSON exports when using accounts mode.
  - The encryption key is derived from the audit signing key (PEM). Without a PEM key the built-in key is used, which only hides the values.
  - A key encrypted with another PEM key cannot be used: it is treated as not set and must be entered again.
  - In PIN mode the keys are stored as before, so a competition prepared on one computer runs unchanged on another.
- Conflicting changes to athlete information are clearly signaled.
- Registration spreadsheets accept more birth-date formats, and unusable birth dates are reported before anything is imported.
- The H2 database can no longer be opened by other programs while owlcms runs (`H2ServerPort` removed).
- Imported textual data is cleaned of scripts and markup.
- Session selectors distinguish completed and upcoming sessions.
- Scoreboards can show body weight in its own column (`displayBodyWeight` feature toggle).
- Protocol templates include lot and start numbers.
- VFE team reports flag category and team-size violations.
- Optional resource monitoring (`resourceTraces` feature toggle).

-For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
