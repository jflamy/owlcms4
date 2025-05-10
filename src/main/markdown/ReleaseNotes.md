⚠️⚠️⚠️ **Since version 55, the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) is used to install and run OWLCMS.  **
**See the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) for details.** ⚠️⚠️⚠️

<br>

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
