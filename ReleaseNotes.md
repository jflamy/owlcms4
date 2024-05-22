**Version 48.0.3**

> [!IMPORTANT]
>
> - You should test all releases, with your own data, several days before a competition.

- 48.0.3: Fix: When printing all the athlete cards for all sessions, they were not correctly ordered by session.
  
- Options for scoreboard ordering
  - If the "Display categories ordered by age group" checkbox selected in the competition rules, the younger age groups are shown first on the scoreboard (lower start numbers and earlier weigh-in order).
  - If this checkbox is *not* selected, then the athletes are grouped first by bodyweight category and then by lot number (strict TCRR interpretation).  A feature toggle `bwClassThenAgeGroup` is available to group the athletes by age group *within* the bodyweight category.

- Championships and Final Results Package
  - The second column, previously empty, of the AgeGroups file is now used for a Championship Name. The third column is the championship type (IWF, MASTERS, etc.) 
  - You can name the Championship in your local language. For example: Youth, Junior, Senior, Masters, U13, U15, Junior High, Senior High, whatever you need.
  - A Championship Name normally corresponds to one final package with separate teams and medals, but you can still produce packages separately for age groups within a championship
  - There is now a single tab in the AgeGroups Excel file.  The tab with Robi records is removed.  The previous format will still work if you leave the Robi tab as the first tab.  If you have your own AgeGroups files, see the [AgeGroups Conversion Instructions](#agegroups-conversion-instructions) at the bottom of this page
- Weigh-in 
  - A "Quick Mode" is added to the weigh-in page, to be used when a weigh-in sheet has been filled in.  This adds an "Update and Next" button to move to the next athlete in the weigh-in order without having to select the athlete in the grid. The ordering of the athletes from the weigh-in form is respected when an athlete moves up.
  - The weigh-in form has been fixed for formatting issues and is now in Portrait mode, for both US letter and international A4 formats.
  - The eligibility categories are now shown on the form so that they are confirmed by the coach and cross-checked during data entry.
- Break Management:
  - It is now possible to switch seamlessly from an interruption (for example a Technical Pause because the platform is broken) to a timed countdown (after repair, timed warmup before resuming), and vice-versa
  - Spurious interruptions of the countdown are now prevented and signaled to the announcer (e.g. normally a technical pause during a countdown should not interrupt the countdown, and if stopping the countdown is indeed required a decision on when to restart will be required).
- Timekeeper: The Group Selection menu is disabled by default.  When using an iPad, the timekeeper page is reloaded after a sleep, and may still contain the previous group, and this causes the competition to return to the last session.  To re-enable the menu selection, the feature switch `enableTimeKeeperSessionSwitch` can be used.
- Results Tie-Break
  - The actual lift time is used when breaking ties between two athletes in two different groups. This is compatible with rule 6.8.2 for the normal IWF context where groups for a category are in descending D-C-B-A sequence. 
  - This works when competition times have not been entered for the sessions.  
  - This also works as a tie-breaker for situations where multiple championships run on several platforms and no group ordering is possible (for example, tie-breaking an Open championship taking place concurrently with other championships).
- Record Exports:
  - The Export All button now exports all records (old and new).  New records are identified by a group name in the "Group" column.
  - For heavyweight categories the format >109 with a leading `>` is used instead of 999.  This is what is used in the record boxes on the scoreboard. 
- Athlete Registration, Weigh-in and Competition Documents
  - The athlete editing page now requires a registration category (you will have to use the "ignore errors" to have none)
    - The best choice is calculated automatically as the youngest category (based on maximum age), and if equal, the one with the narrowest age range (so an eligible Master will be chosen over Senior)
    - The category can be overridden explicitly if within the eligible categories.
  - The weigh-in, registration and document lists show athletes without categories, to allow fixing data entered using spreadsheets.
  - If a single Championship is being competed, it is selected by default.
  - When a session has been selected on the registration or weigh-in page, the `Add` button will assign that session as default for the new athlete.
  - When loading a registration or SBDE file, if explicit category names are used, they can be entered exactly as displayed in the current language.
- Scoreboards:
  - For the default age group, "W 64" is now shown instead of "Open W 64".  Other age groups are shown as before ("U15 W 64").
  - Style sheets: improvements made for handling narrow (vertical) devices.  Ellipsis ("...") should now work correctly.
- Attempt Board:
  - Very long family names (over 18 characters) are split over two lines; they will now split in a balanced way (except on Safari and iPad/iPhone browsers)
- Publicresults and Video Event Forwarding:
  - The publicresults scoreboard now reflects the owlcms coach scoreboard faithfully (all the data is sent over)
  - The information sent for publicresults can now be sent to a second URL for interfacing with video production software. A web server at that URL can extract the information published by owlcms, and transform it for use by video software such as vMix.  See https://github.com/nemikor-solutions/wise-eyes for an example.
  - publicresults and video listeners now get all information about ceremonies and jury decisions, in addition to breaks and interruptions.  Jury decisions are sent on the `/decision` endpoint.
  - /update endpoint is updated every 15 seconds.  publicresults computes a hashcode before putting the event on its event bus. Individual publicresults user sessions compare the hashcode and ignore duplicate events.  This deals with publicresults being restarted randomly and users joining in at random.
- Rankings: Fix of a pre-existing issue: Sinclair and other global rankings could be wrong on scoreboards (they would be shown as odd numbers 1, 3, 5, ...)
- Records: Fix: exporting all records from an empty database (to obtain a template) yielded a file with an incorrect validation criterion.
- Registration files
  - The simple registration file accepts entries with explicit categories with no athlete birth date.
  - The headers of the simple registration file can be in English in addition to the local language.
  - The full SBDE export now has dates in ISO `yyyy-MM-dd` format to avoid issues when reading back in a different locale.
  - Fix: in v47 the SBDE reader was inverting the declarations and personal best values.
- If the Feature Toggle `AthleteCardEntryTotal` is enabled, the Entry Total is shown in the title of the Athlete Card.
- Jury Sheet texts and labels are now all in the translation file and can therefore be in the local language.
- Other fixes:
  - If a weight change occurred when the down signal or decision lights were on, there was an extremely small probability that the display would not reset and switch to the next athlete.

###### AgeGroups Conversion Instructions

It is recommended to do the following changes (the built-in AgeGroups files have been updated, you can use them as example to update your own).
- For Youth, Jr and Sr championships, or U13 U15 U17 age groups championships that take place simultaneously, put a championship name on each row, because each normally has its own teams and medals.
- For Masters championships, you can put "Masters" in your local language as the championship name on all the line for the MASTERS  lines
- If you have simultaneous Masters championships in the same meet, copy the MASTERS section for each, and use the championship name column to distinguish the two (example: PanAm Masters and SouthAm Masters).  Also rename the AgeGroups (M55 and SAM55 for example).
- You can have "combined age group championships" if you wish.  This is the same as for Masters -- you put the same championship name on the age groups that belong together.  You might have a combined U15 and U17 championship in high schools where there is a single combined team score, you would have the same championship name for both age groups.
