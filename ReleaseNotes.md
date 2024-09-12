> [!IMPORTANT]
>
> - You should test all releases, with actual data, *several days* before a competition.
> - It is always wise to export your current database before updating if it contains important data.

- Maintenance log

  - 52.1.0
    - Major Fix: Bar Loading Calculations
      - Problem: The use of a 15kg bar instead of the 20kg bar for boys based on age groups had been wrongly implemented. This could lead to situations where it was not possible to reactive the 20kg bar.  The setting of a specific bar weight could also be affected.
      - Kid groups, including mixed boys and girls sessions that use 5, 10 or 15kg bars can be handled manually by unselecting the 20kg bar for that session.
      - A feature toggle `lightBarU13` achieves the same effect automatically for children groups where the lowest age is 12 or less (for example, U13, U11, U9).
    - Further fix for running on computers running in languages for which there is no translation.  The default will now be English if the language is not explicitly set in the Language Settings.
  - 52.0.7
    - Fix: for locales (language+country) not supported, pages would open before the application was fully initialized and had switched back to English, leading to a "bundle not found" error.
    - Documentation update for feature toggles and environment variables
    - New feature toggle `masters20kg` when all athletes should follow the 20kg rule (used in Open championships where it would be unfair to apply the IMWA 80% rule to Masters only)
  - 52.0.6
    - Weigh-ins and registrations
      - When weight is cleared and category is defined, don't reset eligibilities needlessly
      - When a super-heavyweight athlete from a youth age group weighs in, correctly recompute the participation in older age groups if eligible.
    - Record Exports. Fix: The file produced by an export records definition could not be read back as is.
    - Break Timers:  if a short CJ break was running, it was not cancelled if a longer break was started manually
    - Medal Schedule: Added a new template that lists, for each category, when the medals will be handed out (computes when the last athlete from that category is done)
  - 52.0.5 
    - Publicresults: improved traces and exception catching for cleaner logs.
    - Competition Results: 
      - Recalculate Ranks now recomputes the global best lifter ranking according to the selected global scoring system
      - The championship category names are now used when producing the result sheet for a specific championship. Each athlete appears once under the most specific category he is registered in that championship.
      - Updated the Scores templates. This is used when Age-Group-Specific scores are used. Now shows the global "Best Lifter" scores as well.
  - 52.0.4 
    - Fix: If "Medals according to score" or "Show Score on Scoreboard" are selected, then the global Scoring System is displayed on the scoreboard (as was the case in previous release)
      - (Advanced usage note) If an AgeGroup-specific scoring system has been defined using the AgeGroups file, then the score and rank columns are automatically shown for any session in which such an age group is present, and the specific rankings are used instead of the global one.
    - Translations (ru)
  - 52.0.3 
    - Publicresults: fixed memory allocation configuration for using large containers in large competitions (ex: 200 simultaneous viewers of scoreboards for 3 simultaneous platforms)
    - owlcms: fixed occasional extra notification of weight changes to the announcer.
    - owlcms: ${athlete.categoryFinished} can now be used in results templates (those used in the Session Results page). When producing interim results, this variable can be used to hide athlete ranks in categories where some athletes have finished but others still need to lift.
  - 52.0.2 The new faster SBDE (Start Book Data Entry) full export was not correctly exporting the session information, now fixed.
  - 52.0.2 Fixed formatting for schedules, restored the DaySchedule with individual age groups.
  - 52.0.1 Overzealous check was preventing some customized schedules from printing.
- **MIGRATION NOTES**
  - If you have created your own document templates, or if you created a "kit" of local documents for your federation, some templates have been moved to more specific folders and you may need to move your own templates.  Install a clean copy of the application and look at the locations in `local/templates`.  Use the updated `Prepare Competition > Documents` page to check that the buttons show your templates.
- Documents: New and Improved Documents page
  - All documents needed to prepare and run the competition are here (Results are still on their own page).
  - The Competition-wide documents such as the Start List are handled as in the previous versions.
  - Documents like the Athlete Cards can be produced for one or more sessions.  If more than one session is selected, a zip file is produced, otherwise the Excel is produced.
  - Document sets can be produced, for example, a weigh-in form together with the cards.  When a document set is selected, a zip file is produced. The document set can be produced for one or more sessions.
  - If more than one copy of a document is needed (for example, two weigh-in forms for each session), you can adjust your template by duplicating the tab.  Same for jury forms if you want to avoid printing 3 copies manually.
  - Schedule: 
    - added a Simple schedule template that uses the description of the sessions. 
    - A more sophisticated DaySchedule template shows age groups and bodyweight categories, now available in local languages.
- Scoreboards:
  - Fix: Immediately after the 3rd snatch, while the decision lights were shown, the bottom line of the scoreboard would be stretched. Now fixed.
- SBDE export:
  - Converted the template to jxls3, resulting in massive speed improvement (~7 seconds for 1000 athletes).



For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/51.0.0-rc02)
