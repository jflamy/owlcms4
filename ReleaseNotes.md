**Version 48.0 release candidate**

> [!CAUTION]
>
> - This is a [release candidate](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation.  *Some features could be non-functional*. 
> - You should test all releases, with actual data, several days before a competition.  This is especially important for release candidates.

##### 48.0

- (rc06) When loading a registration or SBDE file, if explicit category names are used, they can be entered exactly as displayed in the current language.
- Athlete Registration, Weigh-in and Competition Documents
  - The editing form now requires a category.  The best choice is calculated automatically. The category must be selected manually if the birth date or body weight is missing.
  - The weigh-in, registration and document lists show athletes without categories, to allow fixing data entered using spreadsheets.
  - If a single Championship is being competed, it is selected by default.
  - When a session has been selected on the registration or weigh-in page, the `Add` button will assign that session as default for the new athlete.
- Championships and Final Results Package
  - The second column, previously empty, of the AgeGroups file is now used for a Championship Name. The third column is the championship type (IWF, MASTERS, etc.) 
  - You can name the Championship in your local language. For example: Youth, Junior, Senior, Masters, U13, U15, Junior High, Senior High, whatever you need.
  - A Championship Name normally corresponds to one final package with separate teams and medals, but you can still produce packages separately for age groups within a championship
  - There is now a single tab in the AgeGroups file.  The tab with Robi records is removed.  The previous format will still work if you leave the Robi tab as the first tab.  If you have your own AgeGroups files, see the [AgeGroups Conversion Instructions](#agegroups-conversion-instructions) at the bottom of this page
- Publicresults and Video Event Forwarding:
  - The publicresults scoreboard now reflects the owlcms coach scoreboard faithfully (all the data is sent over)

  - The information sent for publicresults can now be sent to a second URL for interfacing with video production software. A web server at that URL can extract the information published by owlcms, and transform it for use by video software such as vMix.  See https://github.com/nemikor-solutions/wise-eyes for an example.

  - publicresults and video listeners now get all information about ceremonies and jury decisions, in addition to breaks and interruptions.  Jury decisions are sent on the `/decision` endpoint.
- Break Management:
  - It is now possible to switch seamlessly from an interruption (for example a Technical Pause because the platform is broken) to a timed countdown (after repair, timed warmup before resuming), and vice-versa
  - Spurious interruptions of the countdown are now prevented and signalled to the announcer (e.g. normally a technical pause during a countdown should not interrupt the countdown, and if stopping the countdown is indeed required a decision on when to restart will be required).
- Rankings: Fix of a pre-existing issue: Sinclair and other global rankings could be wrong on scoreboards (they would be shown as odd numbers 1, 3, 5, ...)
- Records: Fix: exporting all records from an empty database (to obtain a template) yielded a file with an incorrect validation criterion.
- Registration files
  - The simple registration file accepts entries with explicit categories with no athlete birth date.
  - The headers of the simple registration file can be in English in addition to the local language.
  - The full SBDE export now has dates in ISO `yyyy-MM-dd` format to avoid issues when reading back in a different locale.
  - Fix: in v47 the SBDE reader was inverting the declarations and personal best values.
- Weigh-in
  - The weigh-in sheet has been fixed for formatting issues and is now in Portrait mode, for both US letter and international A4 formats.
  - A Quick Mode is added to the weigh-in page, to be used when a weigh-in sheet has been filled in.  This adds an "Update and Next" button to move to the next athlete in the weigh-in order without having to select in the grid.
- (alpha02) 
- (alpha02) /update endpoint is updated every 15 seconds.  publicresults computes a hashcode before putting the event on its event bus. Individual publicresults user sessions compare the hashcode and ignore duplicate events.  This deals with publicresults being restarted randomly and users joining in at random times.
- If the Feature Toggle `AthleteCardEntryTotal` is enabled, the Entry Total is shown in the title of the Athlete Card.
- Jury Sheet texts and labels are now all in the translation file and can therefore be in the local language.

###### AgeGroups Conversion Instructions

It is recommended to do the following changes (the built-in AgeGroups files have been updated, you can use them as example to update your own).

- For Youth, Jr and Sr championships, or U13 U15 U17 age groups championships that take place simultaneously, put a championship name on each row, because each normally has its own teams and medals.

- For Masters championships, you can put "Masters" in your local language as the championship name on all the line for the MASTERS  lines

- If you have simultaneous Masters championships in the same meet, copy the MASTERS section for each, and use the championship name column to distinguish the two (example: PanAm Masters and SouthAm Masters).  Also rename the AgeGroups (M55 and SAM55 for example).
- You can have "combined age group championships" if you wish.  This is the same as for Masters -- you put the same championship name on the age groups that belong together.  You might have a combined U15 and U17 championship in high schools where there is a single combined team score, you would have the same championship name for both age groups.
