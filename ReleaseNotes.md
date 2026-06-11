<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 67**

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
