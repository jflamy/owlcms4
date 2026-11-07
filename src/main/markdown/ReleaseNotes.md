<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**

- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**Development log**

- 69.0.0-beta06: Ending athlete introductions starts a fresh ten-minute before-snatch countdown on OWLCMS and Tracker displays, and automatically starts lifting when it expires.

- 69.0.0-beta06: A database last used with a version older than 69 starts with jury size 0 and video replays off.

- 69.0.0-beta06: Weigh-in and registration forms
  - Changing the snatch, the C&J or the entry total updates the 20 kg rule warning on all three fields.
  - Enter saves on the first press when the value just typed clears the warning (for example, the C&J completing the 20 kg rule). Enter never accepts warnings.

- 69.0.0-beta05: V2 database exports are signed, so changes made to the file after the export can be detected.
  - The signature uses the audit key. Without a configured key, the built-in key only shows that the file is intact, not who produced it. V1 exports are not signed.
  - Imports record in the audit log whether the file was signed, and with which key. Files are not refused for signature reasons; this is an integrity check for auditing.
  -  The Audit Integrity Verification page can check the signature of an exported database file (JSON, or a ZIP containing one). The file is not imported.

- 69.0.0-beta05: New "IWF Requirements" section in the feature switches

- 69.0.0-beta04: Age Group/Categories editing robustness fixes
  - Editing an age group: when the changes affect eligibility, a dialog previews the athletes added and removed per championship. **Apply** reassigns immediately, **Reassign Later** keeps the athletes as they are (not available when categories are deleted), **Cancel** discards the changes.
  - Deleting an age group, or changing its championship, asks for confirmation and explains the consequences.
  - The "Reassign Within Current Championships" and "Recompute All Eligibilities" buttons replace the Reset buttons and also show the athletes added and removed before applying.
  - Activating an age group, changing its ages, or moving it to another championship warns about every age gap and overlap in the championship.
  - The audit log now records age group changes (added, edited, activated, deleted, moved, championship renamed, definitions loaded) and the athletes whose categories change as a result.

- 69.0.0-beta04: Athletes whose entry total is below a category's qualifying total are no longer assigned to that category when importing a registration file
  - When editing an athlete, such categories are marked (ET<QT) and the Entry Total field shows the warning as soon as the form opens.

- 69.0.0-beta04: The athlete registration and weigh-in forms replace the "Ignore message and allow saving" checkbox with a yellow **Accept Warnings and Update** button, shown instead of **Update** while a field shows a warning.
  - Checks for entry total lower than QT improved (this happens for adaptive athletes when no separate championships are present)

- 69.0.0-beta04: Tracker connections report the OWLCMS web port so Tracker displays can open pages from the OWLCMS instance that is sending their data.

**New in Release 69.0**

- OWLCMS 69 requires control panel 3.10.0-alpha02 (pre-release) or later, which gives OWLCMS a new control panel token at each launch. The home page says so when an older control panel is used.

- Complies with the October 2026 IWF Technical and Competition Rules and Regulations (TCRR).
  - TCRR compliance mode fixes the Jury at four members, enforces the 20 kg rule and IWF team points, uses GAMX for Best Athlete, and disables IMWA rules, gender-inclusive competition rules and alternative lifting orders.

- IWF TCRR 2026 seven-person Adjudication Panel (jury size 4 with video playback).

- Jury votes shown on attempt boards and scoreboards during a jury review; updated the Jury Control page

- Tracker forwarding uses protocol 69.0.0 and requires OWLCMS Tracker 3.0.0-beta01 or newer (tracker-core 2.0.0-beta01 or newer); 
  earlier tracker versions are not compatible.

- New `3DattemptBoard` feature switch: the attempt board shows the bar and plates in 3D, with real plate thicknesses and markings. 
  - Light bars are shown in color (5 kg purple, 10 kg green, 15 kg gold, other non-standard bars bronze). The technical controller page keeps the flat display.

- Warmup scoreboards show the bottom decision section by default (`decisionSectionOnWarmupScoreboard` feature switch is now on by default).

- Checkpoints: tracker has a new "Checkpoints" page
  - copies the competition and its logs from OWLCMS to a folder on the tracker machine at regular intervals
  - requires the competition supervisor token when running with accounts enabled, the Officials PIN in simple mode
  - If the OWLCMS machine is lost, OWLCMS can be started on the checkpoint machine from that folder.

- The application log `owlcms.log` rolls over at 10 MB (`owlcms_<date>.<index>.log`).

- New Security section on the Administration page.
  - Switching the access mode between Simple Access (shared passwords) and User Accounts asks for the control panel token. Simple Access is the new name of PIN mode.
  - "Check IWF compliance" lists the IWF requirements met or not
  - Database exports include the access mode; an import restores the exported settings as they are.
  - `OWLCMS_ACCESSMODE` is no longer read: the access mode is set only on the Administration page or by an import.

- User Accounts mode: named accounts with role-based access
  - New Accounts Management page.
  - Competition Supervisor page and passive speaker view.
  - Imports need the Preparation, Registration or Competition Supervisor role; database and SBDE exports need the Results permission (Results, Competition Director, Competition Supervisor, External). The buttons are hidden from other accounts.
  - Buttons that open another page (medal ceremony, championships, age groups, records, record eligibility report, team schedule, registration from the sessions page, plates and collars from the platforms page) are shown only to accounts allowed to open that page.
  - Admin password emergency access: logging in as `admin` with the control panel token as the password always works, for the first password of a new installation or a forgotten one. The admin must then set a new password, after which OWLCMS restarts, ending every session.
  - URLs meant to be used externally (e.g., from OBS) accept a `?token=` parameter to work without a login.
    - Tokens are generated and revoked on the account configuration of a user with the appropriate permissions
      - Streaming display pages need a STREAMING token (or DISPLAYS)
      - Venue-only pages (athlete-facing boards, jury scoreboard) need DISPLAYS.
      - Competition export and H2 database download URLs need a RESULTS token.
    - Navigation and operator input pages never accept a token.
  - Robustness/correctness of user accounts management and audit trail

- Simple Access mode (formerly PIN mode)
  - Moved from the System Settings to the Administration page (Security and Audit tab).
  - "Remember this browser" on the officials and display password logins: the browser is let in for 30 days, or until the password changes. This allows removal of the Backdoor list.
  - The control panel token is accepted as the officials password, so a forgotten password can be replaced on the Administration page.
  - The display password can be passed as `?token=` on DISPLAY pages.

- "Imports are allowed" on the Administration page (System tab). When it is off, the registration, SBDE and database imports on the Preparation page are refused.  Used to prevent accidental imports after Competition has started.

- New Backup page in the Admin section.
  - "Capture Anomaly" (System tab) also does a backup: asks what happened, records it in the audit trail and the application log, then downloads the database export and the logs.

- A database last used with a version older than 69 starts with jury size 0 and video replays off. The settings can be changed afterwards on the Competition page.

- MQTT refereeing devices are told when a competition has no jury (jury size 0) instead of being sent a jury size of 3.
  - Blue Owl versions up to 3.0.0 treat it as a jury of 3, as before.

- Tamper-resistant, machine-readable audit trails for compliance.

- owlcms can start directly from a database export (`OWLCMS_LOADJSON`).

- Event forwarding keys are encrypted in the database and in JSON exports when using accounts mode.
  - The encryption key is derived from the audit signing key (PEM). Without a PEM key the built-in key is used, which only hides the values.
  - A key encrypted with another PEM key cannot be used: it is treated as not set and must be entered again.
  - In Simple Access mode the keys are stored as before, so a competition prepared on one computer runs unchanged on another.

- Conflicting changes to athlete information are clearly signaled.

- Registration spreadsheets accept more birth-date formats, and unusable birth dates are reported before anything is imported.
  - DMY or MDY formats are accepted, based on what is in the file (>13 indicates days)
  - If both formats are found, spreadsheet is rejected. If all dates are ambiguous the "official" order for the location is used.
  - YYYY-MM-DD are accepted.

- The H2 database can no longer be opened by other programs while owlcms runs (`H2ServerPort` removed).

- Imported textual data is cleaned of scripts and markup.

- Session selectors distinguish completed and upcoming sessions.

- Scoreboards can show body weight in its own column (`displayBodyWeight` feature toggle).

- Protocol templates include lot and start numbers.

- VFE team reports flag category and team-size violations.

- Optional resource monitoring (`resourceTraces` feature toggle).

For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
