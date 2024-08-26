> [!WARNING]
>
> - This is a release candidate [(see definition)](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation. *It is still a preliminary release*
> - You should test all releases, with actual data, *several days* before a competition. This is especially important when considering the use of a release candidate.

- Preliminary releases change log
  - (rc06) Schedule: added a notification when there are no athletes, for schedules that can only be produced once athletes have been assigned.
  - (rc05) Emit an error message and wait for input when another instance is running - under Windows the console would simply close.
  - (rc05) Fix the building instructions
  - (rc04) Schedule: added a Simple schedule template that uses the description of the sessions. Also made the more sophisticated DaySchedule template use the translation file.
  - (rc03) Additional translations: hu, ro, fi
  - (rc02) Fixes to template ${session.formattedRange} accessor to correctly summarize participants to a session.
  - (rc02) Updated Vaadin to 24.4.10.  publicresults was not working correctly under 24.4.7.
  - (rc01) Update the start numbers when producing empty protocol, jury and introduction sheets.
  - (rc01) jxls3 version of SBDE export (much faster)
- **MIGRATION NOTES**
  - If you have created your own document templates, or if you created a "kit" of local documents for your federation, some templates have been moved to more specific folders and you may need to move your own templates.  Install a clean copy of the application and look at the locations in `local/templates`.  Use the updated `Prepare Competition > Documents` page to check that the buttons show your templates.

- Documents: complete redesign of the Documents page
  - All documents needed to prepare and run the competition are here (Results are still on their own page).
  - The Competition-wide documents such as the Start List are handled as in the previous versions.
  - Documents like the Athlete Cards can be produced for one or more sessions.  If more than one session is selected, a zip file is produced, otherwise the Excel is produced.
  - Document sets can be produced, for example, a weigh-in form together with the cards.  When a document set is selected, a zip file is produced. The document set can be produced for one or more sessions.
  - If more than one copy of a document is needed (for example, two weigh-in forms for each session), you can adjust your template by duplicating the tab.  Same for jury forms if you want to avoid printing 3 copies manually.
  - Schedule: 
    - added a Simple schedule template that uses the description of the sessions. 
    - A more sophisticated DaySchedule template shows age groups and bodyweight categories, now available in local languages.
- Scoreboards:
  - Fix: Immediately after the 3rd snatch, while the decision lights were shown, the bottom line of the scoreboard would be stretched
- SBDE export:
  - Converted the template to jxls3, resulting in massive speed improvement (~7 seconds for 1000 athletes).



For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/51.0.0-rc02)
