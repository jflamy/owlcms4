<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 64**

64.1.0: A zero eliminates the athlete from score-based medals (same as for total medals)

64.1.0: The Jury Decision Display from the Video Streaming page
- will now keep the initial jury decisions visible during deliberation. This can be controlled using the `keepInitial` URL parameter (default = true)
- and also keep the second vote visible until the clock starts for the next athlete.  URL parameter `keepFinal` is true by default
- this does NOT change the behavior of the Jury deliberation page that clears the decisions in order to allow for a second vote

64.1.0: The Rankings selection on the Video Streaming page now works again.

64.1.0: The Rankings screen from the Video Streaming page
  - now shows more athletes (same as the regular session handling)
  - shows the medal winners highlighted with the medal color
  - showMedals=auto is a new parameter on the URL: medals shown when category is done.
  - showMedals=true or false overrides the default.

64.1.0: Fixed potential memory leak when trying to connect to an inexistent (or irresponsive) remote websocket site using the tracker protocol

64.1.0: The previous groups leaders section will include medalists in Snatch or CJ even if they are not top 3 total.

64.0.5: GAMX-M, U and A scores were computed as 0.00 due to age not being propagated.

64.0.4: Flags, Translations and Logos were not packaged and sent to tracker if they were in a override zip stored in the database

64.0.3: Updated the documentation to reflect Control Panel version 3 and use of SBDE format.

64.0.3: Fix: Application did not start if there was a configuration override zip in the database.

64.0.3: Usability: Adding/Removing a configuration override zip in the database no longer requires the Update button.

64.0.2: Sessions from a registration file were not correctly reset to match the Sessions tab

64.0.1: Fixed upload of registration files when the current session is not in English.

64.0.0: Improved error messages when loading a registration/SBDE sheet and athlete is not eligible to stated category (age or qualif. total)

64.0.0: 250g deduction done manually by TO is now again the default (automatic must be selected); clarified option description

64.0.0: Decisions entered when the clock was not started will now be accepted.  A red notification is given to the announcer, timekeeper and jury.

64.0.0: User-oriented startup log that can be tailed by control panel

64.0.0: Validation that there cannot be two age groups with the same code and gender. On import and from interactive editing

64.0.0: The cogwheel settings for the technical official screens (for example, single referee),
are now stored with the platform, remain selected when switching sessions, and are restored when restarting the server.

64.0.0: 64.0.0: New websocket protocol for updated tracker
- Uses the v2 export format (see below)
- Additional information for tracker applications (e.g. time remaining on breaks, precalculated display information)
- Sends all the data needed by owlcms-tracker, with full resynchronization if either end restarts

64.0.0: Support for IWF-style referee assignments by teams, including import/export of a timetable.

64.0.0: End of Competitition Behavior filtering for Best Athlete Scoring has beeen fixed
- If no championship is selected, all athletes are shown, with the global scoring scheme.  The Final package also uses the global scoring scheme.
- When a championship is selected, the best athlete scoring scheme for that championship is used.  The final package uses the championship scheme.
- Eligibility category results with no championship selected show the age-group best athlete score (global if no age-group specific score)
- Registration category results with no championship selected show the global best athlete score

64.0.0: Support for GAMX 2.0 scores: GAMX + age-adjusted variants GAMX-M (Masters), GAMX-U (Kids+Youth), GAMX-A (13-40).  Also added CAT_GAMX, GAMX computed at IWF JR/SR category weight.
- For performance, only the scoring systems required by the best athlete and medals are computed by default; additional ones can be selected if needed by local templates

64.0.0: Fix: Athletes not weighed-in are not propagated to the introduction sheet

64.0.0: New v2 JSON export format
- A new cleaner export format is available when the feature toggle `v2export` is active.  Historical unfortunate naming fixes,
symbolic references/natural keys for readability.
- An extra button is visible when
the toggle is active.  The new format uses natural keys for better readability.
- Import distinguighes the two formats automatically.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
