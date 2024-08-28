> [!IMPORTANT]
>
> - You should test all releases, with actual data, *several days* before a competition.
> - It is always wise to export your current database before updating if it contains important data.

- Maintenance releases:
  - 52.0.1  An overzealous check was preventing the production of USAW-style schedules.
- **MIGRATION NOTES**
  - If you have created your own document templates, or if you created a "kit" of local documents for your federation, some templates have been moved to more specific folders and you may need to move your own templates.  Install a clean copy of the application and look at the locations in `local/templates`.  Use the updated `Prepare Competition > Documents` page to check that the buttons show your templates.
- Documents: New and Improved Documents page
  - All documents needed to prepare and run the competition are here (Results are still on their own page).
  - The Competition-wide documents such as the Start List are handled as in the previous versions.
  - Documents like the Athlete Cards can be produced for one or more sessions.  If more than one session is selected, a zip file is produced, otherwise the Excel is produced.
  - Document sets can be produced, for example, a weigh-in form together with the cards.  When a document set is selected, a zip file is produced. The document set can be produced for one or more sessions.
  - If more than one copy of a document is needed (for example, two weigh-in forms for each session), you can adjust your template by duplicating the tab.  Same for jury forms if you want to avoid printing 3 copies manually.
  - Schedule: added a Simple schedule template that uses the description of the sessions. 
- Scoreboards:
  - Fix: Immediately after the 3rd snatch, while the decision lights were shown, the bottom line of the scoreboard would be stretched. Now fixed.
- SBDE export:
  - Converted the template to jxls3, resulting in massive speed improvement (~7 seconds for 1000 athletes).



For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/51.0.0-rc02)
