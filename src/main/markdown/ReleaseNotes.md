

### ⚠️⚠️⚠️ OWLCMS INSTALLATION PROCEDURE⚠️⚠️⚠️
**Since version 55, the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) is the normal way to install and run OWLCMS. **
**See the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) for details.**

<br>

**Change Log**

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
- Templates 
  - `${session.masters}` template variable can be used to show whether a session is tagged as Masters or not
  -  `${athlete.totalPoints}`computes team points according to IWF rules except if the session is a Masters session. Then uses IMWA or UWML rules according to the competition rules.




For other recent changes, see [version 55.3 release notes](https://github.com/owlcms/owlcms4/releases/tag/55.3.0) and [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1)
