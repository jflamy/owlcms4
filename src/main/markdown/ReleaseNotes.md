<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**Maintenance Log**

- 68.0.0-rc06: Introduction sheets now calculate the clean and jerk break duration using only athletes who have weighed in.

- 68.0.0-rc06: IMWA athletes below the category qualifying total keep their rank and team points but do not receive a Total medal.

- 68.0.0-rc06: The default threshold for the longer CJ break is now 6 athletes or fewer.

- 68.0.0-rc05: Loosened whitelisting requirements
  - localhost/admin and localhost/simulation no longer require whitelisting so they can be run where the controlpanel runs.
  - .../competition/export endpoints and .../competion/h2 endpoints are accepted from private network addresses (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16 as well as link-local 169.254.0.0/16)

- 68.0.0-rc05: Fix inconsistent application of 0-athlete champonship hiding.

- 68.0.0-rc04: Jury decisions are optionally shown on attempt board

- 68.0.0-rc04: Fix: registration file load clears the teams correctly when explicitTeams feature toggle is on

- 68.0.0-rc04: Fix: Remove Exception when opening athlete card from the registration page.

**New in Release 68.0**

- 68.0.0: Migration to Vaadin 25 user interface toolkit
  - Change to use new CSS theming mechanism (the selectors have changed slightly)
  - Systematic cleanup of CSS files to use import+overrides instead of copies
  - **If you have customized your CSS files, you will need to review and potentially adjust them**.
- 68.0.0: Jury decisions can be shown on the attempt board (off by default).  
  - This relies on the jury size being set in competition options, and on being selected on the attempt board (URL parameter showJuryDecisions=true)
  - If you change the jury size, reload the session (only the reloaded sessions will pick up the new size)
- 68.0.0: Decision reversal from the announcer/marshal screen, accessed by clicking on the cell for the attempt
- 68.8.0: Dedicated page for medal ceremonies on the "run lifting" page; this causes public scoreboards to display the medals
  - The page estimates what categories fit on the page to make it easier to go through the presentations
- 68.8.0: Public scoreboards switch to the "start list" page until the snatch countdown is started, and to medals when ceremonies take place
- 68.8.0: Championships management
  - Championships can be re-ordered
  - Medal presentations and other such documents will follow the championship ordering.
  - Empty championships are hidden from championship dropdown filters by default
  - Medals can be awarded for the 3 events, for the total, or only for the two lifts. 
  - Team points can be awarded for total only, even if the championship awards the 3 medals.
  - Deletion of a championship also deletes the associated age groups (after confirmation)
- 68.0.0: Event forwarding destinations can now be added, removed, and activated individually; local Tracker connections managed by the Control Panel is treated specially.
- 68.0.0: Reorganized display launchers into tabs, with consistent warm-up and public display choices and Video Streaming available under Displays.
- 68.0.0: The default paper size can be selected (a default is picked based on presumed location).  This hides the irrelevant templates.
- 68.0.0: Added mobile navigation pages for phones and tablets
  - When on mobile, the home page redirects to a refereeing page
  - Links for jury and scoreboard pages are added as appropriate for mobile devices
- 68.0.0: Usability: Changed the wording for the CJ break duration settings to be inclusive (6 or fewer, 10 or more)
- 68.0.0: Cleanup and standardization of template headers for protocols, jury protocols, and competition results
- 68.0.0: Technical official spreadsheet imports now report invalid team-role values in the upload dialogue.
- 68.0.0: SBDE imports matching for existing athletes is now case-insensitive and diacritic-insensitive.
- 68.0.0: Normalization of names to Olympic Data Format guidelines, unless `dontFixNames` feature toggle is on.
- 68.0.0: Mixed teams, by default, combine the men's and women's teams (so up to 16 athletes in IWF settings)
- 68.0.0: Updated the "Top X" displays to use the correct championship-configured ranking system.
- 68.0.0: Interactive editing of IWF-style technical official team assignment table
- 68.0.0: owlcms.local published as a local network mDNS host name alias so that http://owlcms.local reaches the OWLCMS server
- 68.0.0: 15kg bar with no plates was erroneously preferred to 5kg or 10kg bar + kid bumpers
  - 5kg bar never gets collars (they are rated to 20kg)
- 68.0.0: Migrate birth dates to directly readable canonical ISO8601 textual format to avoid interpretation, time zone, and conversion issues.
- 68.0.0: Dark mode/Light mode toggle in the main menu side bar.



For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
