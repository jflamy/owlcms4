

### ⚠️⚠️⚠️ OWLCMS INSTALLATION PROCEDURE⚠️⚠️⚠️
**Since version 55, the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) is the normal way to install and run OWLCMS. **
**See the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) for details.**

<br>

**Maintenance Log**

- Initial translations for Norwegian and Greek under artificial languages (Ido for Norwegian, Interlingue for Greek).  Will be moved to correct language code once validated.
- Bookmarks: Workaround for an issue in the user interface library that prevented the field of play argument from being shown (?fop=A).  This made the URLs not bookmarkable and might explain why browser restarts were needed.
- Sinclair at category weight: when dealing with kids/youth categories, compute Sinclair at the IWF Senior Category.  If athlete is >81 but weighs 83kg, compute at 89kg, not as super heavy...

**New In Release 57**

- Start Book Data Entry (SBDE) Advanced Registration Data Updates using a spreadsheet
  - It is now possible to update only the athletes or only the sessions.  Updating only the sessions is useful to update the referees, or for changes in schedule.
  
  - When updating the athletes, there is the option of removing them beforehand, of only adding athletes (ignoring those who are there), or of doing a full update.  When updating, it is presumed that the First Name, Last Name and Lot number are identical.  Updating this way would be used for a schedule change -- changing sessions, or for changing categories.
  
- Message of the day
  - At startup, a message can be displayed on the home page if something urgent needs to be communicated by the application maintainer.  The files containing the messages are located in the source code repository, and can be changed or removed as required.
  - When running locally, a check that a recent-enough version of the control panel was used it made.

- Technical Officials 
  - Fix: Technical Officials were not restored when importing a .json export.  Since they were exported, re-importing will now work.
  - A new attribute has been added.  `affiliation` can be used to store the club, region, etc. if needed.
- Masters sessions
  - Sessions can be designated as Masters sessions.  Older age groups are first during weigh-in, when attributing start numbers. Scoreboards are grouped by age group, oldest first.
  - There is now an extra column on the groups page of the registration/SBDE groups tab to indicate that a session is Masters.  TRUE indicates that it is, FALSE that it is not, and empty uses the default "Masters presentation order" setting for the competition.
- Age Group definitions
  - The only two Championship Types that are now used are MASTERS and DEFAULT
  - MASTERS indicates that the category code names are already gendered (M35 contains the gender M).  MASTERS also indicates that the athletes in this age group get the 80% rule if the competition is under IMWA rules (and that the 80% rule has not been turned off for the competition as a whole)
  - DEFAULT indicates that only the gender is used to name the category. Therefore, there can only be one DEFAULT for men, and one DEFAULT for Women. If the code is "Open", the category will be "W 64" instead of "Open W 64".
  - Any championship that is neither MASTERS or DEFAULT has no special treatment.

- Competition Rules
  - Added the ability to specify whether IMWA or UWML rules are used.  
    - IMWA implies 80% rule and  team scoring that gives less points to winners of one or two-person categories.  
    - Not selecting IMWA keeps IWF 20kg rule and IWF team scores, which is what UWML uses.
- Masters team scores
  - New IMWA template to use the IMWA team scoring rules (the template sheet to award points differently is ignored)
- Best Athlete Awards
  - The Competition Results page now has an additional checkbox to restrict the listing to category winners.  In many federations, the best lifter athlete must also be a category winner.
- Updating session data using SBDE
  - Fix: session data updating was not functional, now fixed
  - Enhancement: if the file name is renamed to end with `_sessions.xlsx` or if the feature switch `noAthleteUpdates` is present, then only the sessions tab is processed.  This allows changing referee information and scheduled times without touching the athletes.
- Team Results
  - The Team Results page now also shows the combined Men + Women team results
- Scoreboards
  - The Best Team Points scoreboard correctly allows selecting the championship, and allows for selecting Male, Female, Male and Female, Mixed or all teams.
  - Same fixes for the Best Teams Scores - the score shown is the one for the Best Athlete as set in the competition rules.
  - Fix: Leader board would sometimes be shown when there was no current athlete (at the end of a session), with out-of-date information.
- Templates 
  - `${session.referee1AsTO}` returns a TechnicalOfficial object if one is found in the list of Technical Officials that matches the session referee1. 
    - There is an `AsTO` variant for all the roles (`announcerAsTO`, `marshal1AsTO` etc.)
    - You can then do `${session.referee1AsTO.federationId}` to get the `federationId` of the official.   The fields available are `lastName`, `firstName`, `level`, `federationId`, `federation`, `iwfId`, `affiliation`.
    - The format used for matching is  `lastName, firstName`.  If you populate the TechnicalOfficial list first and use the drop downs in the session editing, the match will be good
  - `${session.masters}` template variable can be used to show whether a session is tagged as Masters or not
  - `${athlete.totalPoints}`computes team points according to IWF rules except if the session is a Masters session. Then uses IMWA or UWML rules according to the competition rules.




For other recent changes, see [version 55.3 release notes](https://github.com/owlcms/owlcms4/releases/tag/55.3.0) and [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1)
