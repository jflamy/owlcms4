<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>
**Maintenance Log**

64.0.0-rc07: Support for IWF-style refereeing teams and timetable

64.0.0-rc06: Removed json mode for database transfer; preparing for tracker inclusion in control panel

64.0.0-rc05: Added the values for 1st 2nd and 3rd place team points to allow owlcms-tracker computation of team points without hard-coding.

64.0.0-rc04: Further adjustments to initial owlcms-tracker handshake

64.0.0-rc04: Support "on-demand" resource requests from owlcms-tracker; additional informations sent to support IWF-style documents.

64.0.0-rc03: More adjustments to the initial handshake, tested pictures.

64.0.0-rc02: Tracker protocol updated to 2.1.0; database is now sent zipped; initial handshake redone.

64.0.0-rc01: Flag sizes on the attempt board had accidentally been reduced.

**New in Release 64**

64.0.0: Sends all the data needed by owlcms-tracker to generate IWF-style start books and results book

64.0.0: Support for IWF-style referee assignments by teams, including import/export of a timetable.

64.0.0: Decisions entered when the clock was not started will now be accepted.  A red notification is given to the announcer, timekeeper and jury.

64.0.0: Manual 250g deduction is now by default (automatic must be selected); clarified option description

64.0.0: End of Competitition Behavior for Best Athlete Scoring:
- If no championship is selected, all athletes are shown, with the global scoring scheme.  The Final package also uses the global scoring scheme.
- When a championship is selected, the best athlete scoring scheme for that championship is used.  The final package uses the championship scheme.
- Eligibility category results with no championship selected show the age-group best athlete score
- Registration category results with no championship selected show the global best athlete score

64.0.0: Support for GAMX 2.0 scores: GAMX + age-adjusted variants GAMX-M (Masters), GAMX-U (Kids+Youth), GAMX-A (13-40).  Also added CAT_GAMX, GAMX computed at IWF JR/SR category weight.

64.0.0: For performance, only the scoring systems required by the best athlete and medals are computed by default; additional ones can be selected if needed by local templates

64.0.0: Fix: Athletes not weighed-in are not propagated to the introduction sheet

64.0.0: Fix timer visual stutter on initial 1:00 or 2:00 clock start 

64.0.0: Require explicit field of play parameters instead of inheriting through the user's session (merged sanitization from 63.3.x)

64.0.0: New websocket protocol for updated tracker
- Uses the v2 export format
- Additional information for tracker applications (e.g. time remaining on breaks, precalculated display information)

64.0.0: New v2 JSON export format
- A new cleaner export format is available when the feature toggle `v2export` is active.  Historical unfortunate naming fixes,
symbolic references/natural keys for readability.
- An extra button is visible when
the toggle is active.  The new format uses natural keys for better readability.
- Import distinguighes the two formats automatically.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
