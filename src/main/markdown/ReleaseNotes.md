**Version 46.0**

> [!CAUTION]
>
> - *You should always carefully test any new release several days before using it, with your own data.*
> - If you have made customizations, see the [notes at the bottom.](#customization-notes) 

##### 46.0.4

- Fix: Records from national, continental, world federations again appear in separate rows according the name given, with one record box per age group+body weight.  A previous attempt to automatically accommodate conflicting names when loading from several federations has been undone. Conflicts in record names should be fixed in the source files.

##### 46.0.3

- Fix: The simplified initial registration file is now correctly read until the first empty line (instead of erroneously stopping at the first line with an empty first cell)

##### 46.0.2

- Fix: The live updating of the Sinclair/SMM/Q-Points/Robi rankings now works again.


##### 46.0.1

- Fix: IWF-format athlete cards were not working in version 46.0.0.  For this document we have returned to the previous .xls format.

  > [!IMPORTANT]
  >
  > If you had installed 46.0.0, and then install a later version, you will have .xlsx and .xls versions of the IWF card templates.  Go to your `local/templates/cards` directory and keep only the .xls versions of the IWF cards.

##### 46.0.0 

- Fix: Lines in the final package for Sinclair/SMF/Q-Points could be duplicated (once per eligible category). Now fixed.  Only Robi where the score is different for each eligible category has duplicate lines.
- Selectable Scoring System
  - New options on the "Non-Standard Rules" section of the competition rules.
    - The scoring system shown on the scoreboards is now selectable (Sinclair, Robi, SMM, Q-Points, etc.)
    - The ranks according to the scoring system can be shown or not on the scoreboard
    - Total-based ranks can be hidden if the competition is done according to the scoring system only (e.g. a Sinclair-based competition)
  - "Top Score" and "Top Team Score" scoreboards have been made more general
    - Top Sinclair, Top SMM, Top QPoints are now shown depending on the selected scoring system
    - Note that Top Robi is *not* available because they are not comparable between age groups. The same athlete gets up to three different Robi scores if Youth/JR/Sr.
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
- Age Groups
  - Enhancement: The age groups definitions can now be exported
  - Fix: Previously, if you added a non-IWF category for an age group using the age group file it was ignored.
  - Change: The age group definitions is now the first sheet in the spreadsheet file, but for backward compatibility, if there are two sheets the first one is ignored (it used to be the Robi definitions)
- Robi
  - Change: The definition of the Robi reference records is no longer in the AgeGroups file. It is now in a separate local/robi/RobiCategories.xlsx file. 
  - If you have a national Robi classification system based on your national records, you can edit the RobiCategories.xlsx file.
- Usability: the down signal is shown a little bit longer on all the boards (including publicresults)
- Flic2 button shortcuts: the Flic2 sends NUMPAD_DECIMAL for "." and NUMPAD_DIVIDE for "/" and NUMPAD_EQUALS for "=". Added these shortcuts for the stop, start, and 2 minutes shortcuts.

##### Customization Notes

> - If you have created your own Excel reports, you should save your Excel templates as .xlsx files instead of .xls.  The old format should still work, but .xlsx will be the standard from now on.
> - If you have created your own AgeGroups files, delete the first sheet that previously contained the Robi records.

