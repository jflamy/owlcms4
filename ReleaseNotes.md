<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**New in Release 68.0**

- 68.0.0-beta02: Fix: Referee decisions entered by keyboard or USB buttons did not register (JSON migration side-effect)

- 68.0.0-beta01: Added mobile navigation pages.
  - home page goes to a refereeing page
  - links for jury and scoreboard pages as appropriate for mobile devices

- 68.0.0-beta01: Dark mode/Light mode toggle in the main menu side bar.

- 68.0.0-alpha02: Fixed Nested-style templates
  - Restored automatic cell merging and border creation for the nested start lists and nested day schedules

- 68.0.0-alpha01: Migration to Vaadin 25
  - Change to use new CSS theming mechanism
  - Internal changes to match JSON libraries required by Vaadin 25
  - Changes of build process and Docker container build to systematically use JDK 25 as required by Vaading 25

For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
