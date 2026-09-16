<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**Maintenance Log**

- 68.0.0-rc04: Fix import behavior under explicitTeams feature toggle to actually clear empty team sets

- 68.0.0-rc04: Fix Exception when opening athlete card from the registration page.

- 68.0.0-rc03: Sync with 67.5.2; Announcer page emits Down and Timer warnings agein if selected in cogwheel menu options.

- 68.0.0-rc02: Registration files will report incompatible eligibility categories (categories with no overlap in age or body weight)

- 68.0.0-rc02: Fix: Resetting and starting a timer when one was running and not stopped did not stop the original timer.

**New in Release 68.0**

- 68.0.0: Migration to Vaadin 25 user interface toolkit
  - Change to use new CSS theming mechanism
  - Systematic cleanup of CSS files to use import+overrides instead of copies
  - **If you have customized your CSS files, you will need to review and potentially adjust them**.
- 68.0.0: Decision reversal from the announcer/marshal screen, accessed by clicking on the cell for the attempt
- 68.8.0: Dedicated page for medal ceremonies on the "run lifting" page; this causes public scoreboards to display the medals
  - The page estimates what categories fit on the page to make it easier to go through the presentations
- 68.8.0: Public scoreboard will switch to the "start list" page until the snatch countdown is started
- 68.8.0: Championships can be re-ordered
  - Medal presentations and other such documents will follow the championship ordering.
- 68.0.0: Event forwarding destinations can now be added, removed, and activated individually; local Tracker connections managed by the Control Panel is treated specially.
- 68.0.0: Reorganized display launchers into tabs, with consistent warm-up and public display choices and Video Streaming available under Displays.
- 68.0.0: The default paper size can be selected (a default is picked based on presumed location).  This hides the irrelevant templates.
- 68.0.0: Added mobile navigation pages for phones and tablets
  - When on mobile, the home page redirects to a refereeing page
  - Links for jury and scoreboard pages are added as appropriate for mobile devices
- 68.0.0: Changed the wording for the CJ break duration override rules to be inclusive (6 or less, 12 or more)
- 68.0.0: Cleanup and standardization of template headers for protocols, jury protocols, and competition results
- 68.0.0: Technical official spreadsheet imports now report invalid team-role values in the upload dialogue.
- 68.0.0: SBDE imports matching for existing athletes is now case-insensitive and diacritic-insensitive.
- 68.0.0: Normalization of names to Olympic Data Format guidelines, unless `dontFixNames` feature toggle is on.
- 68.0.0: Mixed teams, by default, combine the men's and women's teams (so up to 16 athletes in IWF settings)
- 68.0.0: Championships awards
  - medals can be awarded for the 3 events, for the total, or only for the two lifts. 
  - team points can be awarded for total only even if the championship gives the 3 medals.
- 68.0.0: Updated the "Top X" displays to use the correct championship-configured ranking system.
- 68.0.0: Interactive editing of IWF-style technical official team assignment table
- 68.0.0: owlcms.local published as a local network host name alias so that http://owlcms.local reaches the OWLCMS server
- 68.0.0: 15kg bar with no plates was erroneously preferred to 5kg or 10kg bar + kid bumpers
  - 5kg bar never gets collars (they are rated to 20kg)
- 68.0.0: Migrate birth dates to directly readable canonical ISO8601 textual format to avoid interpretation, time zone, and conversion issues.
- 68.0.0: Added mDNS binding to owlcms.local so [http://owlcms.local:*port*]( http://owlcms.local:*port*) should now work as stable URL
- 68.0.0: Deletion of a championship also deletes the associated age groups (after confirmation)
- 68.0.0: Dark mode/Light mode toggle in the main menu side bar.



For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
