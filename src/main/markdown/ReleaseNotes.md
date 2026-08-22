<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**New in Release 68.0**

- 68.0.0-beta10: Decision displays on technical official stations, redone for robustness improvement.

- 68.0.0-beta10: Technical-official web page support sorting, and import correctly processes inactive officials

- 68.0.0-beta09: Loading chart fixes and adjustments
  - lightBarU13 and lightBarU15 clarified to not inadverently interfere with other settings
  - added `noCollars5kgBar` to prevent 2.5kg collars from being used on kid bar for weights under 20kg

- 68.8.0-beta08: Fix: 15kg bar with no plates was erroneously preferred to 5kg or 10kg bar + kid bumpers

- 68.0.0-beta07: Enhancements to the Decision Section at the bottom of scoreboards
  - Show the athlete name and, optionally, their category participations when the `decisionSectionShowAgeGroups` feature toggle is enabled
  - Show the nature of the break currently under way

- 68.0.0-beta07: Adjusted the rules for the Leaders section at the bottom of scoreboards to better support three-medal championships

- 68.0.0-beta06: Migrate birth dates to directly readable canonical ISO8601 textual format to avoid interpretation, time zone, and conversion issues.

- 68.0.0-beta05: Athlete Timer on technical official stations (speaker, timekeeper, marshal, etc.) redone to fix intermittent missed starts on MQTT events

- 68.0.0-beta05: Fix for missed declaration deadline not signaled after resetting a two-minute clock

- 68.0.0-beta05: Fix to correctly disable legacy HTTP event-forwarder when the URL is cleared

- 68.0.0-beta05: For developers, improved application shutdown when its launcher is killed by the IDE

- 68.0.0-beta05: Administrator tools to selectively repair athlete birth dates due to time zone issues
  - Add one day, or move dates to January 1 of the following year

- 68.0.0-beta04: If feature toggle `trackerExtra` is enabled, then the OWLCMS_REMOTE and OWLCMS_VIDEODATA environment variables are considered as additions instead of overrides
  - This allows the environment variables to be treated as connections 3 and 4.
  - When there are duplicates destinations, the last password seen wins according to the order publicresults, videodata, OWLCMS_REMOTE, OWLCMS_VIDEODATA

- 68.0.0-beta03: Added mDNS binding to owlcms.local so [http://owlcms.local:*port*]( http://owlcms.local:*port*) should now work as stable URL

- 68.0.0-beta03: Deletion of a championship also deletes the associated age groups (after confirmation)

- 68.0.0-beta03: Fixed medal screen to avoid (rare) occasional, unpredictable exceptions

- 68.0.0-beta02: Fix for keyboard/USB keypad referee decisions that were not registering.

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
