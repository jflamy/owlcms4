<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 67**

67.0.0-rc08: Further clean-up of attempt board processing done on event reception

67.0.0-rc07: Further clean-up of decistion display to make the actions atomic and remove the empty box intermediate state

67.0.0-rc07: Notification to announcer when waiting on a decision from referee (as opposed to a stuck down signal display)

67.0.0-rc07: Clean-up of timer display code to strictly use the LitElement reactive properties

67.0.0-rc06: Clean-up of decision display code to strictly use the LitElement reactive properties

67.0.0-rc05: Added Date hot fix capability to /admin page.  Can be used to identify athletes at risk of being in wrong category.

67.0.0-rc04: fixed attempt board CSS for long team names and long categories + barbell centering + long break durations

67.0.0-rc03: Fix for lift rank visibility
  - force computation on every session load based on the actual registration categories shown on the board.

67.0.0-rc03: Fix for missed decision display
  - remove unwarranted thread on attempt board handling of decision visibility that could create a race
  - add a timed backstop in case the decision visible event push is somehow missed


67.0.0-rc02: End Of Competition results fixed (a cache was not being used resulting in performance issue).

67.0.0: Reworked the Records page
  - all features are on a single page
  - event-specific or historical prior-categories records can be marked as inactive for less clutter.
  - redid the documentation

67.0.0: Championship handling improvements
  - All default rules for medals and awards can now be set from the Competition Rules page
  - Individual Championships can inherit the defaults or override them. They are defined on the Define Championships page.
  - Each Age Group is connected to a Championship.  If the Championship name is left empty when creating the age group, a Championship with the same name will be assumed.
  - Multiple age groups can refer to the same Championship. This is how Masters championships are defined.

67.0.0: Additional templates for team results
  - summary tables without the athlete details
  - total-only (when 3 medals are awarded but total only is used for team rankings)

67.0.0: Default paper sizes can be set in the language and settings page
  - the default is based on person's location inferred from the time zone.
  - if stripping the paper size results in collisions with locally cleaned up names, both will be shown without stripping.s

67.0.0: Display body weight and best athlete scores on scoreboard
  - Resurrected and updated old feature, enabled using feature toggles.
  - Use `displayBodyWeight,displayBestScore,noBestScoreRank` for a "best Sinclair score" meet.

67.0.0: Fixed the Competition Results competitonResults template
  - When no championship is selected, and no best athlete scheme is forced by the dropdown, the best athlete score for each age group is used

67.0.0: Fixed the SBDE "Update athlete non-lifting data" mode to process record federation eligibilies and eligibility categories correctly

67.0.0: Fixed authentication and interference issues when attempting to feed publicresults and tracker at the same time, or multiple trackers

67.0.0: Robustness: Improved sequencing of timer and mqtt events, better enforcement of timer running or not.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
