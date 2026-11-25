<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**

- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**New in Release 69.1**

- 69.1.0: Added missing MQTT events for stand-alone clocks (`setTime`, `breakSetTime`, `breakStart`, `breakStop`).

- 69.1.0: Usability: Progress bar on import file restart, based on prior startup time.

- 69.1.0: The Top Score, Top Teams and Top Team Score displays now follow the scoreboard look (headers, spacing, column groups and font sizes) for all styles, including public and video.
  - The display options for font size and team width now apply to these displays.

**New in Release 69**

- 69.0.0: Potentially dangerous imports are now blocked while a lifting session is active, with clear platform warnings: registration, SBDE, database, and age-group imports.
  - JSON imports already warn about requiring an immediate full restart
  - The other imports can accept a delayed restart.

- 69.0.0: Server stop/restart controls are shown only in User Accounts mode to the appropriate administration and supervision accounts.

- 69.0.0: Usability: disable password-manager autofill on non-login password, PIN, and key configuration fields.

- 69.0.0: MQTT device configuration publishes only the global competition jury size for backward compatibility.

- 69.0.0: Event forwarding connections from Control Panel or environment variables are not saved or exported and shown read-only with a badge.

- 69.0.0: IWF compliance is enabled and prerequisites checked in an Administration dialog, with immediate actions for the live settings.

- 69.0.0: Fix: removed longstanding stray =GOOGLETRANSLATE() formulas from the master translation spreadsheet that affected 7 translation values.

- 69.0.0: Fix: Mistakenly hidden age group labels on the multi-agegroup scoreboards are now shown

- 69.0.0: Usability: Starting an introduction or before snatch countdown now prompts for a session if none was selected.
  - Previously this could cause the attempt board to remain on the "waiting" state and require a refresh.

- 69.0.0: Projected rank as shown at bottom of scoreboard was incorrectly mentioning CJ if athletes had withdrawn from CJ (considered that CJ had started)

- 69.0.0: Starting the clock while jury votes are missing
  - The Timekeeper, active announcer and Competition Supervisor can start the clock after confirming. Other screens then close their warning.

- 69.0.0: Scores computed at the category weight (category Sinclair, Q-points and GAMX) used the next category up for an athlete exactly at a category limit (a 70 kg athlete was scored at 75 kg).
  - The "kg needed" on the Top Sinclair scoreboard is computed at the same senior category limit as the score, instead of the athlete's registration category.

- 69.0.0: Network name announcement and shutdown fixes for robustness
  - mDNS records expire after 120 seconds, and network name announcement is disabled on fly.io.
  - OWLCMS shutdown waits at most 500 ms for mDNS announcement withdrawal.

- 69.0.0: As a matter of principle, update Docker base image for fly.io deployments to mitigate Java CVE (local version was already ok)

- 69.0.0: Using the transparent stylesheet for video streaming scoreboards was showing a misaligned grid. Fixed by correctly importing and overriding the public style.

- 69.0.0: "Remember me" checkbox on login page: server-side remembered logins let the same browser re-open OWLCMS without entering a PIN or password until the next day. A database export or backup cannot be used to forge a remembered login.
  - Controlled for both Simple Access and User Accounts by one Administration setting (on by default). The IWF compliance check lists it as a requirement not met when it is on.
  - Admin accounts always enter their password.
  - Logging out, changing the PIN/password or disabling the account ends it; turning the setting off prevents remembered logins in both access modes.
  - "Forget all remembered browsers" requires every browser to enter its PIN or password again.

- 69.0.0: PIN mode
  - Logout is a real logout: it ends the browser session and forgets the remembered login on that browser, so the PIN or password is asked next time. Until a logout, closing and reopening the browser keeps a remembered login until midnight.

- 69.0.0: Narrow desktop browser windows no longer switch to the mobile referee/jury home page. Phones and tablets still use the mobile home.

- 69.0.0: Limited registration data editing during sessions now starts as soon as the session is loaded
  - The limited registration data editing form then shows a marshal-type tab, and the same checks are made as for the marshal and announcer
  - The limited form is closed if opened during a lift and a decision for that athlete comes in.ß

- 69.0.0: Simulation preserves entered body weights and starting weights. Missing starting weights are derived from the entry total when available.

- 69.0.0: Audit log improvements
  - seal cleanly and reliably on shutdown after MQTT and server services stop; shutdown failures are logged without preventing audit finalization.
  - show the difference from the previous requested weight for athlete declarations and changes.
  - clock events and weight changes show the scoreboard time and the exact milliseconds.
  - the `_plain_` files are for people. Each `_sealed_` line starts with the same readable text and ends with `json=` and a self-contained JSON object meant for programs: sequence, time, station and user, athlete, attempt, raw values before and after, previous requested weight, clock in milliseconds, and the other details.
  - Record audit details list the lift and age group first.

- 69.0.0: In mixed-gender sessions, the moving down checks were not enabled because first man could be lower than last woman. Now fixed (going from women to men resets the values as if it was a new session)

- 69.0.0: Multi-language behaviour improved
  - Pages always use their own locale, no matter the locale of the page that triggered an event (for example, record notifications and the device testing page).
  - Printed documents and downloaded kits are produced in the locale of the page used to create them.
  - A page's locale is, in order of priority: the locale chosen with the language menu for the browser session, the default locale in the Language and System Settings, then the first browser locale (in the browser's order of preference) for which there are available translations, then English.
  - The menu also lists the browser's own locale (language and country) when the language is translated, Java supports that combination, and it is not already listed. For example, a browser set to Spanish (Chile) adds "español (Chile)", and one set to Spanish (Argentina) adds "español (Argentina)". Both show the same Spanish text, since there is no Chilean or Argentinian column in the translation definitions, but dates follow each country's conventions (14-11-2026 in Chile, 14/11/2026 in Argentina). The country also selects country-specific templates when they exist.

- 69.0.0: Make the weigh-in and registration page filters fit on 1366x768 laptopsß

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
  - "Remember this browser" on the officials and display password logins: the browser is let in until midnight, or until the password changes. This allows removal of the Backdoor list.
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
