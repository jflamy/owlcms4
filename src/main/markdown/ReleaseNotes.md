<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**

- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**Development log**

- 69.0.0-rc01: Scoring System Selection
  - Fixed the drop down selectors to show a logical grouping of scoring systems with separators
  - For result sheets, when there is no championship, the selector shows an explicit "Per Athlete Championship Scoring System"
  - Completed implementation of lift-specific scores GAMX-S, GAMX-SM, GAMX-J, GAMX-JM
  
- 69.0.0-beta09: When using a scoring system, the team results tree was incorrectly adding athletes without a total in the team score
  - Now a selectable behavior: Including the athletes that have no snatch but a CJ or vice-versa is now a checkbox per championship
  - Fixed issue with grid not reloading on rank recalculation
- 69.0.0-beta09: Adjusted the permissions in Accounts mode so that the preparation account (which is the actual competition secretariat) can edit results if needed
- 69.0.0-beta08: OBS status monitor changes
  - Each status monitor follows the platform named in its address (`fop=`). The Status Monitor button on the Video Streaming page opens the monitor for the platform selected on that page.
  - With several platforms, a monitor address without `fop=`, or naming a platform that does not exist, shows a message listing the platform names. Add `fop=` to existing OBS browser sources and bookmarks. Nothing changes with a single platform.
  - The lift type reported for a decision is now correct. The decision on a third snatch was reported as `liftType=CLEANJERK`; during a jury break, the lift type is that of the lift under review.
  - The monitor goes directly from the decision to the break at the end of the snatches and at the end of the session, without a momentary `CURRENT_ATHLETE_DISPLAYED`. Scene-switching rules that expect `previous=CURRENT_ATHLETE_DISPLAYED` before the automatic clean & jerk break need to be changed.
- 69.0.0-beta08: Additional fixes for jury decision edge cases
  - The speaker and the scoreboard top line move on to the next athlete while the jury votes. The attempt board and the scoreboard decision section stay on the athlete under review until the jury has voted, or until the next clock starts.
  - On the last snatch and on the last lift of the session, the decision stays visible into the break or the end of the session instead of disappearing at once or staying indefinitely.
  - The scoreboard decision section shows the final outcome for 20 seconds, whatever the jury size, so that it can be read after the rest of the scoreboard has moved on: from the referee decision when there is no jury, from the last jury vote when the jury does not ask for a review, and from the jury decision after a review. A deliberation or a challenge started during the 20 seconds takes over at once, and the next clock start ends the display.
  - A review requested after the next athlete's clock has started stops the clock and shows the reviewed athlete and decision.
  - When the review vote of a 3, 4 or 5-member jury does not reach the jury's rule (unanimity, or a majority for 5 members with video), the referees' decision stands as called: Jury Control asks the Chair to confirm it instead of asking for a technical rule, and the boards and the speaker show the reason "Referee Decision Stands as Called".
  - Attempt boards and scoreboards opened before the jury size is set on Jury Control now show the jury votes without being reloaded.
  - When the jury reviews the last snatch, the break before clean & jerk starts again for its full duration once the jury has decided. Previously lifting resumed without the break.

**New in Release 69.0**

- OWLCMS 69 requires control panel 3.10.0-alpha02 (pre-release) or later, which gives OWLCMS a new control panel token at each launch. The home page says so when an older control panel is used.

- Complies with the October 2026 IWF Technical and Competition Rules and Regulations (TCRR).
  - TCRR compliance mode fixes the Jury at four members, enforces the 20 kg rule and IWF team points, uses GAMX for Best Athlete, and disables IMWA rules, gender-inclusive competition rules and alternative lifting orders.

- IWF TCRR 2026 seven-person Adjudication Panel (jury size 4 with video playback).

- Jury votes shown on attempt boards and scoreboards during a jury review; updated the Jury Control page

- Tracker forwarding uses protocol 69.0.0 and requires OWLCMS Tracker 3.0.0-beta01 or newer (tracker-core 2.0.0-beta01 or newer); 
  earlier tracker versions are not compatible.

- The attempt board shows the bar and plates in 3D, with real plate thicknesses and markings (new `3DattemptBoard` feature switch, on by default; turn it off to get the flat display).
  - Light bars are shown in color (5 kg purple, 10 kg green, 15 kg gold, other non-standard bars bronze). The technical controller page keeps the flat display.

- Warmup scoreboards show the bottom decision section by default (`decisionSectionOnWarmupScoreboard` feature switch is now on by default).

- Separate editing modes for athletes that are not in lifting sessions (all changes allowed) and athletes in lifting sessions (only metadata can be edited, only some roles can edit athlete cards)
  - The athlete card is now accessed as a Tab on the registration editing forms, same as before for Marshal/active announcer.

- Age Group/Categories editing robustness
  - Editing an age group: when the changes affect eligibility, a dialog previews the athletes added and removed per championship. **Apply** reassigns immediately, **Reassign Later** keeps the athletes as they are (not available when categories are deleted), **Cancel** discards the changes.
  - Deleting an age group, or changing its championship, asks for confirmation and explains the consequences.
  - The "Reassign Within Current Championships" and "Recompute All Eligibilities" buttons replace the Reset buttons and also show the athletes added and removed before applying.
  - Activating an age group, changing its ages, or moving it to another championship warns about every age gap and overlap in the championship.
  - The audit log now records age group changes (added, edited, activated, deleted, moved, championship renamed, definitions loaded) and the athletes whose categories change as a result.
- Athletes whose entry total is below a category's qualifying total are no longer assigned to that category when importing a registration file
  - When editing an athlete, such categories are marked (ET<QT) and the Entry Total field shows the warning as soon as the form opens.
- The athlete registration and weigh-in forms replace the "Ignore message and allow saving" checkbox with a yellow **Accept Warnings and Update** button, shown instead of **Update** while a field shows a warning.
  - Checks for entry total lower than QT improved (this happens for adaptive athletes when no separate championships are present)
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
  - New Accounts Management page, opened from the Security and Audit tab of the Administration page.
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
