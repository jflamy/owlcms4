

### ⚠️⚠️⚠️ OWLCMS INSTALLATION PROCEDURE⚠️⚠️⚠️
**Since version 55, the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) is the normal way to install and run OWLCMS. **
**See the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) for details.**

<br>

**Change Log**

- 57.0.0-alpha05: Added the capability to get the federation ids and other attributes of a session technical official.  See "Templates" below.
- 57.0.0-alpha04: Championship and Gender Selection on Best Teams scoreboards
- 57.0.0-alpha03: added mixed team results to Team Results page
- 57.0.0-alpha02: updated the SBDE processing to do updates of session data only
- 57.0.0-alpha01: added ability to mark sessions as using Masters start number rules.
- 57.0.0-alpha01: added ability to calculate team points using IMWA rules 

**New In Release 57**

- Masters sessions
  - Sessions can be designated as Masters sessions.  Older age groups are first during weigh-in, when attributing start numbers. Scoreboards are grouped by age group, oldest first.
  - There is now an extra column on the groups page of the registration/SBDE groups tab to indicate that a session is Masters.  TRUE indicates that it is, FALSE that it is not, and empty uses the default "Masters presentation order" setting for the competition.
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
- Templates 
  - `${session.referee1AsTO}` returns a TechnicalOfficial object if one is found in the list of Technical Officials that matches the session referee1. 
    - There is an `AsTO` variant for all the roles (`announcerAsTO`, `marshal1AsTO` etc.)
    - You can then do `${session.referee1AsTO.federationId}` to get the `federationId` of the official.   The fields available are `lastName`, `firstName`, `level`, `federationId`, `federation`, `iwfId`.
    - The format used for matching is  `lastName, firstName`.  If you populate the TechnicalOfficial list first and use the drop downs in the session editing, the match will be good
  - `${session.masters}` template variable can be used to show whether a session is tagged as Masters or not
  - `${athlete.totalPoints}`computes team points according to IWF rules except if the session is a Masters session. Then uses IMWA or UWML rules according to the competition rules.




For other recent changes, see [version 55.3 release notes](https://github.com/owlcms/owlcms4/releases/tag/55.3.0) and [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1)
