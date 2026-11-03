<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**

- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**Development log**

- 69.0.0-beta03: Fixes for edge cases when closing the athlete card on decisions.

- 69.0.0-beta03: The Competition Supervisor can open Language and System Settings, and the Security and Audit tab of the Administration page (access mode, IWF compliance check, shared passwords). Audit key initialization, audit integrity verification and account management remain admin-only.

- 69.0.0-beta03: Fixed: plate, collar and bar changes made on the technical controller page were not sent to the tracker. The tracker now receives the platform equipment and the bar selected by OWLCMS with every update.

- 69.0.0-beta03: New `3DattemptBoard` feature switch: the attempt board shows the bar and plates in 3D, with real plate thicknesses and markings. Light bars are shown in color (5 kg purple, 10 kg green, 15 kg gold, other non-standard bars bronze). The technical controller page keeps the flat display.

- 69.0.0-beta02: Attempt boards show jury votes by default when a jury is configured; the display option can turn them off for an individual board.
  - With the feature on, the decision stays visible while jury votes are pending; a supervisor can override the clock start if needed.

- 69.0.0-beta02: Scoreboards show jury decisions by default (`juryDecisionsOnScoreboard` feature switch is now on by default).

- 69.0.0-beta02: Warmup scoreboards show the bottom decision section by default (`decisionSectionOnWarmupScoreboard` feature switch is now on by default).

- 69.0.0-beta02: Checkpoints: the tracker's new "Checkpoints" page
  - copies the competition and its logs from OWLCMS to a folder on the tracker machine at regular intervals
  - requires the competition supervisor token when running with accounts enabled, the Officials PIN in simple mode
  - If the OWLCMS machine is lost, OWLCMS can be started on the checkpoint machine from that folder.

- 69.0.0-beta02: In Simple Access mode, the export, H2 database and checkpoint URLs require the Officials password (`?token=` or `Authorization: Bearer`) when one is set; the local network alone is no longer enough.

- 69.0.0-beta02: Each OWLCMS start writes new audit log files instead of appending to the previous ones, so a closed audit file never changes.

- 69.0.0-beta02: The application log `owlcms.log` also rolls over at 10 MB (`owlcms_<date>.<index>.log`).

- 69.0.0-beta02: OWLCMS 69 requires control panel 3.10.0-alpha02 (pre-release) or later, which gives OWLCMS a new control panel token at each launch. The home page says so when an older control panel is used.

- 69.0.0-beta02: New Security section on the Administration page.
  - Switching the access mode between Simple Access (shared passwords) and User Accounts asks for the control panel token. Simple Access is the new name of PIN mode.
  - "Check IWF compliance" lists the IWF requirements met or not
  - Database exports include the access mode; an import restores the exported settings as they are.
  - `OWLCMS_ACCESSMODE` is no longer read: the access mode is set only on the Administration page or by an import.

- 69.0.0-beta02: Simple Access mode (formerly PIN mode)
  - Moved from the System Settings to the Administration page (Security and Audit tab).
  - "Remember this browser" on the officials and display password logins: the browser is let in for 30 days, or until the password changes. This allows removal of the Backdoor list.
  - The control panel token is accepted as the officials password, so a forgotten password can be replaced on the Administration page.

- 69.0.0-beta02: "Imports are allowed" on the Administration page (System tab). When it is off, the registration, SBDE and database imports on the Preparation page are refused.  Used to prevent accidental imports after Competition has started.

- 69.0.0-beta02: Imports need the Preparation, Registration or Competition Supervisor role; database and SBDE exports need the Results permission (Results, Competition Director, Competition Supervisor, External). The buttons are hidden from other accounts.

- 69.0.0-beta02:  "Capture Anomaly" (System tab): asks what happened, records it in the audit trail and the application log, then downloads the database export and the logs.

- 69.0.0-beta02: Buttons that open another page (medal ceremony, championships, age groups, records, record eligibility report, team schedule, registration from the sessions page, plates and collars from the platforms page) are shown only to accounts allowed to open that page.

- 69.0.0-beta02: MQTT refereeing devices are told when a competition has no jury (jury size 0) instead of being sent a jury size of 3.
  - Blue Owl versions up to 3.0.0 treat it as a jury of 3, as before.

- 69.0.0-beta02: Admin password emergency access: logging in as `admin` with the control panel token as the password always works, for the first password of a new installation or a forgotten one.
  - The admin must then set a new password, after which OWLCMS restarts, ending every session.

- 69.0.0-beta02: Robustness/correctness of user accounts management and audit trail

- 69.0.0-beta02: URLs meant to be used externally (e.g., from OBS) accept a `?token=` parameter to work without a login.
  - Tokens are generated and revoked on the account configuration of a user with the appropriate permissions
    - Streaming display pages need a STREAMING token (or DISPLAYS)
    - Venue-only pages (athlete-facing boards, jury scoreboard) need DISPLAYS.
    - Competition export and H2 database download URLs need a RESULTS token.
  - Navigation and operator input pages never accept a token.
  - In Simple Access mode, the display password can be passed as `?token=` on DISPLAY pages.

- 69.0.0-beta01:  Registration spreadsheets accept birth dates typed with `/`, `.`, `-` or spaces,
  - DMY or MDY formats are accepted, based on what is in the file (>13 indicates days)
  - If both formats are found, spreadsheet is rejected. If all dates are ambiguous the "official" order for the location is used.
  - YYYY-MM-DD are accepted.

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
  - In Simple Access mode the keys are stored as before, so a competition prepared on one computer runs unchanged on another.

- Conflicting changes to athlete information are clearly signaled.

- Registration spreadsheets accept more birth-date formats, and unusable birth dates are reported before anything is imported.

- The H2 database can no longer be opened by other programs while owlcms runs (`H2ServerPort` removed).

- Imported textual data is cleaned of scripts and markup.

- Session selectors distinguish completed and upcoming sessions.

- Scoreboards can show body weight in its own column (`displayBodyWeight` feature toggle).

- Protocol templates include lot and start numbers.

- VFE team reports flag category and team-size violations.

- Optional resource monitoring (`resourceTraces` feature toggle).

-For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
