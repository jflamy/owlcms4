<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 67.1**

67.1.0 Feature Switches Interactive Page

  - The features can now be toggled interactively from a tab on the Language and System Settings page
  - Each switch is documented (and the documentation is translated)
  - A backward compatible string is exported in addition to a new extensible JSON torage format; starting with this version the new format is used on import if present.

67.1.0: Fix for "Birth year moved to previous year"

  - Birth date is now correctly stored as a local date not affected by time zones (earlier attempts to fix were incomplete).
  - Newly loaded data will be correct.  Contact maintainer if you need to fix an older database.

67.1.0: Adjusted the "start using collar when" threshold rules
  - Made them relative to bar weight. Specified for men's bar, adjusted for actual bar. 40 threshold means 35 for women's bar
  - 40 threshold means "when 20kg is added to the bar"; so collars are not used for 39 (20 + 2x5 + 2x2) but would be at 40 (20 + 2x5 + 2xcollars)
  - Default on new databases is now 25 as per IWF rules -- always use collars
  - If 2.5kg technique plagtes are toggled on (or childrenEquipment feature toggle is active) the threshold is 30, 
    - so that on a men's bar 25 is loaded with large 2.5kg technique plates and no collars (20 kg for women's bar)
  - Special rules (usawCollars, lightBarU13, lightBarU15) still have precedence.

67.1.0: GAMX Support
  - added GAMX variants for snatch and clean&jerk (for seniors and masters)
  - changed the format to JSON so the same tables would be used for tracker
  - packaged the tables as a zip so tracker can fetch them

67.1.0 Records Eligibility Report

- On Records page: shows how many athletes are eligible for each record, to spot missing loads or mistyped federation codes

67.1.0: Adjusted the behaviour of `childrenEquipment` toggle.

  - When importing into an empty (no athletes) database, if this toggle is present, the 5kg 10kg bars and large 2.5kg and 5kg plates will be configured on all platforms.
  - Unsetting and setting this toggle reapplies the children equipment to all platforms
  - Otherwise, the childrenEqupment setting is ignored. In this way changes made (e.g. removing a light bar on a platform) are not lost.

67.1.0: Attempt board layout fixes
  - fixed reactive break-timer alignment and athlete picture sizing.
  - fixed attempt board CSS for long team names and long categories + barbell centering + long break durations

67.1.0: Notification to announcer when waiting on a decision from referee (as opposed to a stuck down signal display)

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

67.1.0: Additional templates for team results
  - summary tables without the athlete details
  - total-only (when 3 medals are awarded but total only is used for team rankings)

67.1.0: Default paper sizes can be set in the language and settings page
  - the default is based on person's location inferred from the time zone.
  - if stripping the paper size results in collisions with locally cleaned up names, both will be shown without stripping.s

67.0.0: Display body weight and best athlete scores on scoreboard
  - Resurrected and updated an old feature, enabled using feature toggles.
  - Use `displayBodyWeight,displayBestScore,noBestScoreRank` for a "best Sinclair score" meet.

67.1.0: Fixed the SBDE "Update athlete non-lifting data" mode to process record federation eligibilies and eligibility categories correctly

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
