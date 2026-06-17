<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 67**

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

67.0.0: Default paper sizes can be set in the language and settings page, default based on person's location

67.0.0: Display body weight and best athlete scores on scoreboard
  - Resurrected and updated old feature, enabled using feature toggles.
  - Use `displayBodyWeight,displayBestScore,noBestScoreRank` for a "best Sinclair score" meet.

67.0.0: Fixed the Competition Results competitonResults template
  - When no championship is selected, and no best athlete scheme is forced by the dropdown, the best athlete score for each age group is used

67.0.0: Fixed the SBDE "Update athlete non-lifting data" mode to process record federation eligibilies and eligibility categories correctly

67.0.0: Fixed authentication and interference issues when attempting to feed publicresults and tracker at the same time, or multiple trackers

67.0.0: Robustness: Improved sequencing of timer and mqtt events, better enforcement of timer running or not.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
