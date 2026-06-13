<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 67**

67.0.0-beta12: Refactored migration to new championship data model

67.0.0-beta12: Default paper sizes can be set in the language and settings page, default based on person's location

67.0.0-beta12: Fixed conditions under which a simulation would not process a platform until a lift was done manually

67.0.0-beta12: Improved sequencing of timer and mqtt events, better enforcement of timer running or not.

67.0.0-beta11: Additional templates for team results, including total-only (when 3-medals are awarded but total only is needed) and summary variants (no athlete details)

67.0.0-beta11: When filtering athletes, the first age group of a championship was selected by default.  Now only happens if there is a single age group.

67.0.0-beta11: Competition book output now uses competition defaults and fallback translations correctly when no championship is selected.

67.0.0-beta11: Best-athlete scoring are correct when no championship filter is selected, in UI and in Excel competition results

67.0.0-beta11: Simulation can skip completed sessions and has been made more robust regarding start and stop.

67.0.0-beta11: Records would not be shown until a display order was selected or an import was made.

67.0.0-beta10: Resurrected settings to display body weight and best athlete scores on scoreboard (typically requested for Sinclair meets)
  - Done using feature toggles. Use `displayBodyWeight,displayBestScore,noBestScoreRank` for such a case.

67.0.0-beta09: Starting from a version 65 or older database (as in Update/Import) would fail when attempting to migrate to the new Championship template structure.

67.0.0-beta08: SBDE Update athlete non-lifting data mode was not processing record eligibilities, and not reapplying categories and teams correctly.

67.0.0-beta07: reworked the Records editing page
  - all features are on a single page
  - event-specific or historical prior-categories records can be marked as inactive for less clutter.
  - redid the documentation

67.0.0-beta06: fixed records import

67.0.0-beta04: Fixed timer display jitter on the attempt board (e.g. 1:12 to 1:11)

67.0.0-beta03: Scoring Systems selected in championships are now computed as a matter of course.  Additional ones can be added on the competition rules page.

67.0.0-beta02: Unify championship creation paths to correctly use the competition-level template defaults

67.0.0-beta01: Championship handling improvements
  - All default rules for medals and awards can now be set from the Competition Rules page
  - Individual Championships can inherit the defaults or override them. They are defined on the Define Championships page.
  - Each Age Group is connected to a Championship.  If the Championship name is left empty when creating the age group, a Championship with the same name will be assumed.
  - Multiple age groups can refer to the same Championship. This is how Masters championships are defined.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
