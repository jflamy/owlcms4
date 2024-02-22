**Version 46.0 beta**	

> [!CAUTION]
>
> Beta versions are meant for testing and translation. Some features may be incomplete or broken.
>
> You should always carefully test any new release several days before using it, with your own data.

##### 46.0

> Summary of changes for people who have performed customizations
>
> - Save your Excel templates as .xlsx files instead of .xls 
> - If you have created your own AgeGroups files, delete the first sheet that previously contained the Robi records.

New in version 46

- (beta07) Fixed Final Package sheet for QPoints.
- (beta06) Flic2 button shortcuts: the Flic2 sends NUMPAD_DECIMAL for "." and NUMPAD_DIVIDE for "/".  Added these shortcuts for stopping and starting the clock, respectively, on the announcer and timekeeper screens.
- (beta05) Selectable Scoring System
  - New options on the "Non-Standard Rules" section of the competition rules.
    - The scoring system shown on the scoreboards is now selectable (Sinclair, Robi, SMM, Q-Points, etc.)
    - The ranks according to the scoring system can be shown or not on the scoreboard
    - Total-based ranks can be hidden if the competition is done according to the scoring system only (e.g. a Sinclair-based competition)
- Simplified initial registration spreadsheet
  - The download of the empty registration spreadsheet now produces a very simple spreadsheet by default. 
  - The buttons on the preparation page were rearranged to facilitate understanding.
  - The previous full registration with import/export has been moved at the bottom to an "Advanced" section, and is now called "Start Book Data Entry (SBDE)".  The columns in the SBDE file are in a fixed order and have been rearranged for easier understanding.
  - The empty registration spreadsheet can be customized by adding any of the columns from the full registration if desired (for example, to add the coach or additional info).  Simply use the same translated column heading as the SBDE file. The initial registration spreadsheet columns can be in any order (contrary to the start book data entry).
- Ability to annotate athlete with A/B/C/... when categories are split across multiple sessions.  
  - The letter is shown on the leaders section of the scoreboards.  
  - Added the entry to the new simplified initial registration sheet and the full start book data entry.
- Templates and documents
  - All the supplied Excel templates are now .xlsx by default.  As a consequence, all the Excel files produced from the default templates are also in .xlsx format.  The .xlsx format is directly recognized by Office, LibreOffice, Google Sheets and Microsoft 365 Web. 
  - Your existing .xls templates will work as before, but we recommend updating your local templates to .xlsx format.
  - The mechanism used for Excel downloads has changed.  Microsoft Edge has special options for modern Office document formats such as `.xlsx`.  Without the change, some downloads of `.xlsx` files would fail when using Edge's "open immediately" option.
- Age Groups
  - Enhancement: The age groups definitions can now be exported
  - Fix: Previously, if you added a non-IWF category for an age group using the age group file it was ignored.
  - Change: The age group definitions is now the first sheet in the spreadsheet file, but for backward compatibility, if there are two sheets the first one is ignored (it used to be the Robi definitions)
- Robi
  - Change: The definition of the Robi reference records is no longer in the AgeGroups file. It is now in a separate RobiCategories.xlsx file. 
  - If you have a national Robi classification system based on your national records, you can edit the RobiCategories.xlsx file.
- Usability: the down signal is shown a little bit longer on all the boards (including publicresults)
