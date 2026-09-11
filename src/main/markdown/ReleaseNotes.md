<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**New in Release 68.0**

- 68.0.0-beta17: Collar threshold was not being exported/imported in JSON files.

- 68.0.0-beta17: Changed the wording for the CJ break duration override rules to be inclusve (6 or less, 12 or more)

- 68.0.0-beta17: added missing fields on the Registration and SBDE session definition tabs

- 68.0.0-beta16: Event forwarding destinations can now be added, removed, and activated individually; local Tracker connections managed by the Control Panel is treated specially.

- 68.0.0-beta16: Cleanup and standardization of template headers for protocols, jury protocols, and competition results

- 68.0.0-beta16: Post-processing of documents to adjust borders improved to correctly infer the border widths.

- 68.0.0-beta16: Registration page: addition of a athlete correctly filters as birth date and gender are entered

- 68.0.0-beta16: Records with no valid lift type are flagged on import and import is prevented.

- 68.0.0-beta15: Medal sheets and a dedicated Medal Ceremony control page can now be opened from a completed announcer session; the ceremony controls are also available from Run Lifting Session.

- 68.0.0-beta15: Championship/Age Group no longer selected by default on the team results page (same as comp results now)

- 68.0.0: Mixed teams can now combine the men's and women's teams directly. This is the default mixed-team selection mode.

- 68.0.0: Championships can award medals for snatch, clean-and-jerk, and total; total only; or the two lifts only. Older JSON and spreadsheet imports preserve their existing medal mode.

- 68.0.0: Team points can now be configured per championship to count all three placings, total only, or snatch and clean-and-jerk only. Championships awarding total-only medals use total-only team points.

- 68.0.0: Updated the "Top X" displays to use each championship's configured ranking system.

- 68.0.0: Reorganized display launchers into tabs, with consistent warmup and public display choices and Video Streaming available under Displays.

- 68.0.0: Fixes for the start list scoreboard layouts

- 68.0.0: Systematic cleanup of CSS files to use import+overrides instead of copies

- 68.0.0: Interactive editing of IWF-style technical official team assignment table

- 68.0.0: Platform management fixes
  - Editing platform order and plates/bar configuration now preserves each setting
  - Renaming a platform now prompts for a restart to apply the change safely

- 68.0.0: Team ranks are available in JXLS result templates as ${team.rank}

- 68.0.0: Fixed countdowns during medal ceremonies on athlete-facing attempt and decision displays

- 68.0.0: Unified medal ceremony and results displays for consistent content and styling

- 68.0.0: Fixed current-competition record identification in record management and result exports if the competition name was changed along the way

- 68.0.0: Result template selectors now show templates matching the configured paper size

- 68.0.0: Decision reversal from the announcer/marshal screen, accessed by clicking on the cell for the attempt

- 68.0.0: owlcms.local published as local network host name alias is now working on macOS also.

- 68.0.0: Jury announcement dialogs now close on all announcer screens when lifting resumes

- 68.0.0: Q-Points calculation for light athletes clamped at the minimum mathematically meaningful bodyweight for each gender (40/45)

- 68.0.0: Changing an Athlete's category preserves the participation in the previously selected championships if still eligible according to the new age and meeting qualifiying totals.

- 68.0.0: Improved message clarity and cell highlighting for 20kg rule violations
  - The athlete card can now be reached from the Registration page as well.

- 68.0.0: Fix: changing the CJ Break duration in the session editing form was no longer correctly taken into account at the time of the break

- 68.0.0: Further improvements to attempt board robustness on marginal network conditions (full updates instead of incremental)

- 68.0.0: Redone: Decision displays on technical official stations for robustness improvement.

- 68.0.0: Technical-official web page support sorting, and import correctly processes inactive officials

- 68.0.0: Loading chart fixes and adjustments
  - lightBarU13 and lightBarU15 clarified to not inadverently interfere with other settings
  - added `noCollars5kgBar` to prevent 2.5kg collars from being used on kid bar for weights under 20kg

- 68.0.0: Fix: 15kg bar with no plates was erroneously preferred to 5kg or 10kg bar + kid bumpers

- 68.0.0: Enhancements to the Decision Section at the bottom of scoreboards
  - Show the athlete name and, optionally, their category participations when the `decisionSectionShowAgeGroups` feature toggle is enabled
  - Show the nature of the break currently under way

- 68.0.0: Adjusted the rules for the Leaders section at the bottom of scoreboards to better support three-medal championships

- 68.0.0: Migrate birth dates to directly readable canonical ISO8601 textual format to avoid interpretation, time zone, and conversion issues.

- 68.0.0: Athlete Timer on technical official stations (speaker, timekeeper, marshal, etc.) redone to fix intermittent missed starts on MQTT events

- 68.0.0: Fix for missed declaration deadline not signaled after resetting a two-minute clock

- 68.0.0: Fix to correctly disable legacy HTTP event-forwarder when the URL is cleared

- 68.0.0: For developers, improved application shutdown when its launcher is killed by the IDE

- 68.0.0: Administrator tools to selectively repair athlete birth dates due to time zone issues
  - Add one day, or move dates to January 1 of the following year

- 68.0.0: If feature toggle `trackerExtra` is enabled, then the OWLCMS_REMOTE and OWLCMS_VIDEODATA environment variables are considered as additions instead of overrides
  - This allows the environment variables to be treated as connections 3 and 4.
  - When there are duplicates destinations, the last password seen wins according to the order publicresults, videodata, OWLCMS_REMOTE, OWLCMS_VIDEODATA

- 68.0.0: Added mDNS binding to owlcms.local so [http://owlcms.local:*port*]( http://owlcms.local:*port*) should now work as stable URL

- 68.0.0: Deletion of a championship also deletes the associated age groups (after confirmation)

- 68.0.0: Fixed medal screen to avoid (rare) occasional, unpredictable exceptions

- 68.0.0: Fix for keyboard/USB keypad referee decisions that were not registering.

- 68.0.0: Added mobile navigation pages.
  - home page goes to a refereeing page
  - links for jury and scoreboard pages as appropriate for mobile devices

- 68.0.0: Dark mode/Light mode toggle in the main menu side bar.

- 68.0.0: Fixed Nested-style templates
  - Restored automatic cell merging and border creation for the nested start lists and nested day schedules

- 68.0.0: Migration to Vaadin 25
  - Change to use new CSS theming mechanism
  - Internal changes to match JSON libraries required by Vaadin 25
  - Changes of build process and Docker container build to systematically use JDK 25 as required by Vaading 25

For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
