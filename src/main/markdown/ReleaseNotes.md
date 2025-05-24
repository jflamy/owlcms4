



⚠️⚠️⚠️ 
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). **  **You can then follow the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)** 
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

⚠️⚠️⚠️

<br>

**New in Release 57.2**

- Single-referee decision display
  - When the announcer selects "Single Referee Mode" from the cogwheel next to the session selector,  the first decision received from a referee is used.  
  - Any of the three referee devices can be used for the single referee, does not matter.
  - A single circular icon is used to display the decision (white with checkmark or red with X).  
  - Note that if you switch from single to multiple referees, or vice-versa, you need to refresh the athlete clock and the attempt board. A simple refresh is enough.
  
- Athlete-facing Clock: A flashing "STOP" is shown on the athlete-facing decision board when the competition is stopped.
- New `local/iwf` directory contains an Excel file with the definition of the IWF categories.
- Templates:
  - Fixed the "CompetitionResults-A4" and "CompetitionResults-LETTER" templates to default to the competition scoring system correctly, and to mark out of competition athletes correctly.
  - Fixed the SnCjTot template for Session Results to show the Technical Official roles correctly


**New In Release 57.1**

- AgeGroups
  - Updated the AgeGroups2025 file for the category change from 98 to 94 kg

- Age-Group-Specific Best Athlete
  - It is now possible to have a best athlete formula specific to an age group. For example, using Q-Youth for young athletes instead of Q-Points.
  - A new competition results template "CompetitionResults" shows the best athlete score according to the age group.
  - The scoring system used for producing results can still be explicitly selected, in which case age-group-specific scores are not used.  This is recommended when producing final packages for a championship.
  - The age group files provided in `local/agegroups` have been updated to show the additional columns.
- Recalculation of records:
  - If record files are reloaded after being modified, or record files are added or removed, or if athlete record eligibilities are changed, it is now possible to redo the computation of records.  This can be done during a competition,  or after the fact.
- Introduction Sheet:
  - The Introduction sheet also lists the officials in the IWF introduction order.

- Timing statistics redone: the capture of the first snatch and clean & jerk clocks, as well as of the last decisions for each lift are now much more precise.  A new template is available to use these values. Unfortunately, the data used for the timing statistics cannot be inferred from older databases.
- Message of the day
  - When running locally, a check that a recent-enough version of the control panel was used it made.
- Best Athlete Awards
  - Q-Points at category weight can now be used
- Scoreboards
  - Flags were not in the correct position on 4K displays, when the athlete picture was shown.
- Best Athlete:
  - Sinclair at category weight: when dealing with kids/youth categories, compute Sinclair at the IWF Senior Category.  If athlete is >81 but weighs 83kg, compute at 89kg, not as super heavy...




For other recent changes, see [version 56.0.8 release notes](https://github.com/owlcms/owlcms4/releases/tag/56.0.8) 
