<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**New in Release 67.4**

- 67.4.0: Enhancements to Decision Section at the bottom of the scorebords
  - Show the athlete name and optionally the participations in the various categories (subject to feature toggle `decisionSectionShowAgeGroups`)
  - Show nature of break currently under way.

- 67.4.0: Adjusted the rules for the Leaders section at the bottom of the scoreboards to better deal with 3-medal championships

- 67.4.0: New athlete Timer implementation on technical official stations (speaker, timekeeper, marshal, etc.) to fix intermittent missed starts.

- 67.4.0: Fix: After resetting to a two-minute clock, a for missed declaration deadline WAS not signaled

- 67.4.0: Fix: Correctly disable legacy HTTP event-forwarder when such a forwarding URL is cleared

- 67.4.0: Fix: For developers, kill the server cleanly when the watcher is killed by the IDE

- 67.4.0: Administrator tools to selectively repair athlete birth dates due to time zone issues
  - Add one day, or move dates to January 1 of the following year


**New in Release 67.3**

- 67.3.0: Fix for intermittent errors affecting the medals scoreboards

- 67.3.0: Fix for two doctors in a session (introduction sheets)

- 67.3.0: Fix for incomplete deletion of category participations when removing age groups
  - This could lead to a broken export if the cleanups done on category reassingment or restart had not happened

- 67.3.0c: Fix for LibreOffice date formatting issue in registration and SBDE files


**New in Release 67.2**

- 67.2.0: Borders in nested-style schedule and start lists
  - the DaySchedule* and NestedStartList* templates again have post-processing step to apply borders
  - cleaned up jxls3 directives owlcms:horizontalBorders and owlcms:verticalBorders are used to trigger the processing

- 67.2.0: Platform ordering
  - Platform can now be ordered on the Field of Play preparation page by dragging the rows.
  - If the usawSessionBlocks feature toggle is active, this will affect the sorting of sessions, so that if red is before blue, 1 red will sort before 1 blue.

**New in Release 67.1**

67.1.0:
  - Fixed links in documentation
  - Updated images for equipment setup

67.1.0: 
  - Support both a notarized DMG install and a `brew` command-line install. Updated documentation accordingly.

67.1.0: Documentation
  - General: separated initial/typical from advanced setup, updated screenshots
  - Documented Championships
  - Documented Technical Officials and IWF-style timetable assignment

67.1.0: Empty databases use IWF 2026-08 age groups by default
  - The earlier 2025 age group files remain available using the drop down on the Age Groups configuration page

67.1.0: Feature Switches Interactive Page
  - The features are now be toggled interactively from a tab on the Language and System Settings page
  - Each switch is described in the current language
  - The `OWLCMS_FEATURESWITCHES` environment variable is applied after the feature switches page; it is a comma-separated list of the switches you want turned on or off (prefix the switch with a `-` to turn it off.)

67.1.0: Optional information at the bottom of scoreboards
  - When enabled by `decisionSection` feature toggle, IWF-style display of the referee decisions and jury decisions
  - By default, decisions are shown according to TCRR, with a 3-second referee reversal delay. Use `showDecisionsImmediately` to get as-soon-as-possible display as experimented in recent IWF championships.
  - A stopwatch showing the time spent in deliberation or challenge can be enabled using `decisionSectionStopwatch`
  - Display projected rank (if lift is successful) on scoreboard if the `displayProjectedRanks` feature toggle is on.
  - Display timer at bottom of public scoreboards (useful when the attempt information is not shown at the top).  Use the `displayScoreboardTimers` feature toggle.

67.1.0: Reworked the Records page
  - all features are on a single page
  - event-specific or historical prior-categories records can be marked as inactive for less clutter.
  - redid the documentation

67.1.0: Championship handling improvements
  - All default rules for medals and awards can now be set from the Competition Rules page
  - New championships inherit the defaults and can then override them.
  - Existing Championships can be reset to the defaults and then override them.
  - Each Age Group is connected to a Championship.  If the Championship name is left empty when creating the age group, a Championship with the same name will be created using the defaults
  - Multiple age groups can refer to the same Championship.  For examples all Masters age groups belong to the same championship. If you have two Masters championships (state and national, for example) you should have two championships and attach age groups to the correct one.

67.1.0: GAMX Support
  - added GAMX variants for snatch and clean&jerk (for seniors and masters)
  - changed the format to JSON so the same tables would be used for tracker
  - packaged the tables as a zip so tracker can fetch them

67.1.0: Jury Settings and Behaviour
  - If Jury votes a second time to end deliberation/challenge, send the decision directly (unless feature toggle `requireJuryPresidentDecision` is on)
  - New "VPT in use" setting on the competition rules to apply the correct rules.

67.1.0: Adjusted the "start using collar when" threshold rules
  - Made them relative to bar weight. Specified for men's bar, adjusted for actual bar. 40 threshold means 35 for women's bar
  - 40 threshold means "when 20kg is added to the bar"; so collars are not used for 39 (20 + 2x5 + 2x2) but would be at 40 (20 + 2x5 + 2xcollars)
  - Default on new databases is now 25 as per IWF rules -- always use collars
  - If 2.5kg technique plagtes are toggled on (or childrenEquipment feature toggle is active) the threshold is 30, 
    - so that on a men's bar 25 is loaded with large 2.5kg technique plates and no collars (20 kg for women's bar)
  - Special rules (usawCollars, lightBarU13, lightBarU15) still have precedence.

67.1.0: Break timer sounds
  - Requires `breakTimerSounds` feature toggle.
  - Delays and sounds are configured in local/timing/timing.properties.
  - Defaults at 1:00, 0:30, 0:00; same sounds as for athletes (can be customized by adding .wav and .mp3 to local/sounds)

67.1.0: Notification to announcer when waiting on a decision from referee (as opposed to a stuck down signal display)

67.1.0: Adjusted the behaviour of `childrenEquipment` toggle.
  - When importing into an empty (no athletes) database, if this toggle is present, the 5kg 10kg bars and large 2.5kg and 5kg plates will be configured on all platforms.
  - Unsetting and setting this toggle reapplies the children equipment to all platforms
  - Otherwise, the childrenEqupment setting is ignored. In this way changes made (e.g. removing a light bar on a platform) are not lost.

67.1.0: Display body weight and best athlete scores on scoreboard
  - Resurrected and updated an old feature, enabled using feature toggles.
  - Use `displayBodyWeight,displayBestScore,noBestScoreRank` for a "best Sinclair score" meet.

67.1.0: Records Eligibility Report
  - On Records page: shows how many athletes are eligible for each record, to spot missing loads or mistyped federation codes

67.1.0: Additional templates for team results
  - summary tables without the athlete details
  - total-only (when 3 medals are awarded but total only is used for team rankings)

67.1.0: Team Names can be selected from the list of previously entered ones (filtering as you type)

67.1.0: Fix: ignore MQTT Devices during simulation (to avoid perturbing the flow of events)

67.1.0: Fix for "Birth year moved to previous year"
  - Birth date is now correctly stored as a local date not affected by time zones (earlier attempts to fix were incomplete).
  - Newly loaded data will be correct.  Contact maintainer if you need to fix an older database.

67.1.0: Fix: Attempt board layout
  - fixed reactive break-timer alignment and athlete picture sizing.
  - fixed attempt board CSS for long team names and long categories + barbell centering + long break durations

67.1.0: Default paper sizes can be set in the language and settings page
  - the default is based on person's location inferred from the time zone.
  - if stripping the paper size results in collisions with locally cleaned up names, both will be shown without stripping.s

67.1.0: Fixed the SBDE "Update athlete non-lifting data" mode to process record federation eligibilies and eligibility categories correctly

For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
