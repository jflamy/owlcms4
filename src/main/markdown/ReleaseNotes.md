<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 66**

62.2.2: Masters: medals now respect oldest age groups first protocol order

62.2.2: When loading Registration or SBDE, use both "Session" and "Group" to look up the translated keywords.

66.2.2: IMWA medals now exclude athletes who finish below a category qualifying total.

66.2.2: When exporting records, prefix the file name with the record federation and record name (if selected as filters)

66.2.2: Fix: Protocol sheets and results books no longer include stale provisional records from another event

66.2.2: Accept stop requests from localhost over http to allow clean stop requests from the controlpanel CLI

66.2.1: _IWF_FlatFile template; use this to produce competition results in the flat file format expected by IWF

66.2.1: The scoreboards and attempt boards now normalize official country names (e.g. Australia) to their IOC code (AUS) for the purpose of locating flags.

66.2.1: Fix: Lower-Thirds now always show the Challenge status correctly.

66.2.0: Fix: Updates to running session
  - Editing referees, description and CJ break duration from the web page will now work for the session in progress
  - Advanced registration file update has a new "safe" mode to update only the TOs and descriptions.

62.2.0: In the source directory, the script to scrape IWF records now correctly handles the Nation

66.2.0: Fix: on the mobile device Jury president keypad, the referee decisions were cleared at the beginning of a break

66.2.0: Fixed Jury Decision sizing on attempt board (esp. 4K displays)

66.2.0: Referee Timetable
  - Addition of a WEIGHIN role to the TO teams time table to handle the situation where specific referees are assigned to the weighin (a subset of the session referees, or someone else)
  - Improved the referee assignment logic

66.2.0: Normalization of country flags.  
  - If a flag is not directly found under the team ame, it will be looked up in a Country-to-IOC code map so that only the acronym names are needed.
  - This table is sent to tracker along with the flags

66.1.0: Fixed attempt board display of clean & jerk attempt number

66.1.0: (source repository) Updated the tools/records script to fetch IWF and EWF records

66.1.0: Template for IWF flat file format added (use with the Registration Category Results)

66.1.0: Mixed Team Championships
  - Fixed Excel output to respect the championship scoring scheme
  - Use radio buttons to select explicit, top n mixed, or top n per gender for the mixed championship UI

66.1.0: Mixed team championships: when top N mixed is blank or 0, the men and women top-N fields are enabled and use the standard men and women labels

66.1.0: Timetable-based technical official generation now clears stale assignments and applies the correct 3-person and 5-person jury rotation

66.1.0: Session Edit Details now reloads the current session instead of reopening stale data

66.1.0: Updated templates
  - VFE Teams form now one sheet per team
  - PanAm protocol with all ITO roles listed

66.1.0: Fixed public scoreboards to show the session name in the header when the description is blank
 
66.1.0: Ranking order and medal scoreboard improvements
  - medals shown as circles, row heights reduced
  - show category headers
  - respect styling for warmup room, public and broadcast variants

66.0.1: Athlete Cards production gets an error message and denial if lot numbers have not been assigned

66.0.1: Athlete with no birth date is now correctly excluded from challenging records

66.0.1: Session results scoring system drop down was not taken into account.  Now behaves the same as competition results.

66.0.1: Lack of lot numbers is now a stern warning on pre-competition document production instead of a blocker.

66.0.1: Improved registration file processing
  - no automatic inference of categories if any of the birth, gender or body weight information is missing
  - blank categories are accepted if not inferable (assumed to be filled interactively)
  - explicitly listed categories never are completed by inference

66.0.0: Mixed Team Championships
  - Allow explicit nomination of a mixed team, or implicit men and women combined
  - A gender-equitable scoring system (e.g. GAMX) can be applied, independently of the gendered teams scoring
  
66.0.0: Enhanced definition of Championships
  - Championships use the default competitions settings (checkbox, on by default)
  - Championships are used to override define the awards
    - best athlete scoring systems
    - medaling rules (total or per event), medaling on score or lifted weight
    - points awarded
    - scoring systems for gendered teams and for mixed teams
  
66.0.0: Mobile device versions for jury member devices and jury president keypad
  - Can act as jury member using a phone or tablet, and act as jury president using a tablet

66.0.0: Cleaner look for jury decisions on attempt board
  - Also clarified instructions for the announcer when the jury gives the decision by pressing the buttons

66.0.0: Record Management
  - the record editing page will now correctly warn that editing an existing record and changing the age or bodyweight categories does NOT create a new record
  - the sorting order in the grid is now done correctly when existing records are adjusted for new categories
  - the exports using the dataExchange templates now use a 999 as the marker for the super heavy category.

66.0.0: Configurable timing
  - For large school-age competitions, creating a `local/timing/timing.properties` as in [this example](https://github.com/jflamy/owlcms4/issues/1386#issuecomment-4170813795) allows changing the durations of the one minute and two minute intervals to have the lifts proceed faster.
  - The values of the warnings times are also propagated to owlcms-tracker so they can be used (e.g. to change timer colours)

66.0.0: Clean-up of the solo referee/announcer/3-referee behavior wrt reversal delay and initial decision
  - Solo referee decisions have a reversal delay, same as 3 referees.  Only the visual rendering changes (single referee light instead of 3)
    - in solo referee mode, the first decision received from any referee is automatically the majority
    - an indicator is propagated to modulate display
  - Announcer input of a decision has no reversal delay because it usually follows flags or some incident
    - no INITIAL_DECISION event is therefore sent
    - unless the announcerTriggersInitialDecision feature toggle is sent (for example, to always have good/bad lift videos on a listener)

66.0.0: showDecisionsImmediately feature toggle (off by default, TCRR still indicates a 3-second delay)
  - Show the decision as soon as the 3 referees have given it. Note: this always sends INITIAL_DECISION

66.0.0: Fix: For Competition Results templates, when a category is selected, the records for the category are extracted in the "records" variable.
  - The standard "protocol" look templates (Total, SnCjTot) show the records 

66.0.0: Fix: The Jury scoreboard again has the current attempt info.

66.0.0: Fix: when producing competition results by registration category, the lot number was being used as identifier for the athlete, leading to problems if no lot numbers had been assigned (all were 0, collapsing to a single athlete)

66.0.0: Fix: reported scores on result sheets are now correctly zero when the athlete is done and has no total

66.0.0: Fix: the noInterimScoresInResults toggle was not systematically applied on the competition results page
  - the individual session pages still show the current session in-progress scores as they may be required to plan for awards

66.0.0: Fix: updating the websocket event forwarding URL did not correctly reset the forwarder

66.0.0: Fix: It was no longer possible to export only the best provisional records

66.0.0: Fix: Updated the "Out of Competition" translation string for Eligibility Status (was "Invited")

66.0.0: Fix: During weigh-in the eligible categories could be cleared because they were mistakenly considered inactive.

66.0.0: Fix: Stale error notification on Announcer/Marshal screens when athlete waits until first CJ to adjust 20kg rule violation

66.0.0: Fix: (merged from 65.1) Display the correct score according to the age group on the eligibility category competition results


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
