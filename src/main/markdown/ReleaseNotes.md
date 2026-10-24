<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**New in Release 69.0**

- 69.0.0: owlcms can start directly from a database export with the `OWLCMS_LOADJSON` environment variable (a JSON or ZIP export, in either export format).
  - The database is replaced by the export before the server opens; a failed import stops the application rather than starting with an empty or partial database.

- 69.0.0: IWF TCRR 2026 seven-person Adjudication Panel (3 referees + 4 jury members), selected with jury size 4 and Video Playback Technology in use.
  - When the combined majority is opposite to the referees, the competition stops automatically with "Adjudication Panel Review"; a 2-2 jury leaves the referees' decision in place. A Challenge follows the same procedure.
  - The Jury Chair uses Call for Votes (Jury Control, or Summon All on the jury box) to have the seven officials vote again. The result waits for the Jury Chair: Confirm Good Lift, the technical rule for a No Lift, or Cancel vote result to vote again.
  - With jury size 4 and no video playback, the jury deliberates at the Chair's request and overturns the referees only when unanimous (`automaticBreakNoVPT` stops the competition automatically instead).

- 69.0.0: Jury deliberations with 3- and 5-member juries follow prior TCRR.

- 69.0.0: Per-platform tamper-resistant audit trails record competition actions and athlete changes.
  - Includes accepted and refused commands, record changes, session activity, and application start/stop events, with operator and station identification.

- 69.0.0: Optional resource monitoring logs CPU, process memory, heap and garbage-collection usage every 30 seconds.
  - Enable `resourceTraces` in System Settings. Samples appear in the normal log and a separate timestamped resource table; normal-log samples can be disabled independently.

- 69.0.0: Competition Director page for managing lifting
  - Enabled by named accounts; in PIN mode, enabled by `competitionDirectorPage` feature switch.
  - Passive speaker view follows live lifting without allowing timer control, athlete edits, or decision reversals.

- 69.0.0: Event forwarding keys are securely encrypted in JSON database exports.

- 69.0.0: The H2 database can no longer be opened by other programs while owlcms is running.
  - The `H2ServerPort` / `OWLCMS_H2SERVERPORT` setting is removed. H2 database URLs must be `jdbc:h2:file:` or named `jdbc:h2:mem:`; server URLs and options such as `AUTO_SERVER`, `FILE_LOCK` and `INIT` are rejected at startup.

- 69.0.0: VFE team reports flag category and team-size violations.

- 69.0.0: Session selectors distinguish completed and upcoming sessions, with ordering appropriate to registration, weigh-in, and results.

- 69.0.0: Attempt boards and scoreboards show the referee decisions as rectangles and the jury votes as circles during a jury review.
  - Jury votes appear on the attempt boards with the `juryDecisionsOnAttemptBoard` feature switch (or the `showJuryDecisions` page parameter) and on the scoreboards with the new `juryDecisionsOnScoreboard` feature switch; the `decisionSectionShowBothJuryVotes` switch is removed. When off, only the decision is shown.
  - While a vote is in progress the boards show who has voted; all votes are revealed as soon as the last one is in, and the decision pops over them when the Jury Chair confirms it. The scoreboards keep the verdict visible slightly longer than the attempt boards.
  - Physical keypads can show a green light while the panel vote is in progress with `panelVoteDeviceIndicators` (requires an updated Blue Owl).

- 69.0.0: The Jury Console is removed; the Jury Control page replaces it. "Jury President" is renamed "Jury Chair", in line with the IWF rules.

- 69.0.0: Referee lights are cleared when the clock starts if no jury decision was given. Referee input during an ordinary break is refused without a notification.

- 69.0.0: Optional tamper-resistant sealed audit logs for IWF/TCRR compliance (`tcrrCompliance` / `iwfCompliance` feature switches).
  - Log blocks are signed and hash-chained; an Audit Integrity Verification page checks the logs, and Audit Key Initialization prepares the signing key.
  - The SHA-256 of every JSON export is recorded in the audit log.

- 69.0.0: New Backup page in the Admin section to download a snapshot of the audit logs or a database export. The Admin menu entry is shown to administrators.

- 69.0.0: Athlete edits are protected against stale saves.
  - Athlete cards ask whether to save anyway, discard, or keep editing when the athlete was changed elsewhere; registration and weigh-in forms refuse to overwrite newer data.
  - Registration, weigh-in, team selection and results editing are refused while a session is in progress.

- 69.0.0: New EXTERNAL role for named accounts: read-only access to the display pages (except the jury scoreboard) for broadcasters.

- 69.0.0: Imported data (registration files, JSON, officials and records spreadsheets) is cleaned of scripts and markup, and translation substitutions are sanitized.

- 69.0.0: Scoreboards can show body weight in its own column (`displayBodyWeight`) instead of reusing the custom1 column.

- 69.0.0: Protocol templates updated for TCRR 2026 to include lot and start numbers.

- 69.0.0: The home page shows a caution notice when running a development, alpha, beta or release candidate version.

- 69.0.0: Experimental and untested: Named accounts with role-based access control.
  - Assign officials access to specific platforms and duties; existing PIN-based access remains available.
  - Login opens the page appropriate to the assigned role; accounts with multiple roles start on Home.
  - Enabling accounts creates missing role accounts, with platform-specific accounts for platform duties. The account list identifies accounts that still need a password.
  - Account names preserve the written case of platform names; login names are case-insensitive.
  - Platform-role accounts omit the platform suffix when there is only one platform. "Reset Platform Accounts" recreates conventional role accounts for the current platforms, clearing their passwords while preserving custom and global accounts.

-For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
