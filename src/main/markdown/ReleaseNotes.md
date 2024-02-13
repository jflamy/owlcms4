**Version 46.0 beta**	

> [!CAUTION]
>
> Beta versions are meant for testing and translation. Some features may be incomplete or broken.
>
> You should always carefully test any new release several days before using it, with your own data.

##### 46.0

> Summary of recommended changes for people who have performed customizations (see below for details)
>
> - Save your Excel templates as .xlsx files instead of .xls 
> - If you have created your own AgeGroups files, delete the first sheet that previously contained the Robi records.

New in version 46

- Simplified initial registration spreadsheet
  - The download of the empty registration spreadsheet now produces a very simple spreadsheet by default. 
  - The buttons on the preparation page were rearranged to facilitate understanding.
  - The previous full registration with import/export has been moved at the bottom to an "Advanced" section, and is now called "Start Book Data Entry (SBDE)".  The columns in the SBDE file are in a fixed order and have been rearranged for easier understanding.
  - The empty registration spreadsheet can be customized by adding any of the columns from the full registration if desired (for example, to add the coach or additional info).  Simply use the same translated column heading as the SBDE file. The initial registration spreadsheet columns can be in any order (contrary to the start book data entry).
- Ability to annotate athlete with A/B/C/... when categories are split across multiple sessions.  
  - The letter is shown on the leaders section of the scoreboards.  
  - Added the entry to the new simplified initial registration sheet and the full start book data entry.
- AgeGroups specification cleanup and fixes
  - Fix: Previously, if you added a non-IWF category for an age group using the age group file it was ignored.
  - The definition of the Robi categories and records is no longer in the AgeGroups file. It is now in a separate RobiCategories.xlsx file.  
    - The age group definitions is now normally the first sheet in the spreadsheet file (enablement for future export of the definitions).
    - For backward compatibility, if there are two sheets the first one is presumed to be an old Robi definition sheet and it is ignored.
- Templates and documents
  - All the supplied Excel templates are now .xlsx by default.  As a consequence, all the Excel files produced from the default templates are also in .xlsx format.  The .xslx format is directly recognized by Office, LibreOffice, Google Sheets and Microsoft 365 Web. 
  - Your existing .xls templates will work as before, but we recommend updating your local templates to .xlsx format.
  - The mechanism used for Excel downloads has changed.  Microsoft Edge has special options for modern Office document formats such as `.xlsx`.  Without the change, some downloads of `.xslx` files would fail when using Edge's "open immediately" option.
- Usability: the down signal is shown a little bit longer on all the boards (including publicresults)
