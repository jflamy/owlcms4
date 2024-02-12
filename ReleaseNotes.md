**Version 46.0 alpha**	

> [!CAUTION]
>
> Alpha versions are NOT ready for use in actual competitions and can be non-functional.  They are used as technical testing milestones when adding new features or making important changes.
>
> You should always test any new release several days in advance, with your own data.

##### 46.0
- (alpha07) AgeGroups.xlsx specification cleanup and fixes
  - The definition of the Robi categories and records is now in a separate RobiCategories.xlsx file. 
  - The age group definitions are now the only sheet in the spreadsheet file (enablement for future export of the definitions).  For backward compatibility, if there are two sheets the first one is ignored.
  - Previously, if you added a non-IWF category for an age group using the age group file it was ignored. This is now fixed.
- (alpha06) Ability to annotate athlete with A/B/C/... when categories are split across multiple sessions.  
  - The letter is shown on the leaders section of the scoreboards.  
  - Added the entry to the new simplified initial registration sheet and the full start book data entry.
- (alpha05) Simplified initial registration spreadsheet
  - The previous full registration with import/export has been moved to an "Advanced" section, and is now called "Start Book Data Entry".  Note that columns have been moved to be in a more logical order.
  - The download of the empty registration spreadsheet now produces a very simple spreadsheet by default. 
    - The empty registration spreadsheet can be customized by adding any of the columns from the full registration if desired (for example, to add the coach or additional info). 
    - The initial registration spreadsheet columns can be in any order (contrary to the start book data entry).
  - The buttons on the preparation page were rearranged to facilitate understanding.
- Templates and documents
  - All the supplied Excel templates are now .xlsx by default.  As a consequence, all the Excel files produced from the default templates are also in .xlsx format.  The .xslx format is directly recognized by Office, LibreOffice, Google Sheets and Microsoft 365 Web. 
  - Your existing .xls templates will work as before, but we recommend updating your local templates to .xlsx format.
  - The mechanism used for Excel downloads has changed.  Microsoft Edge has special options for modern Office document formats such as `.xlsx`.  Without the change, some downloads of `.xslx` files would fail when using Edge's "open immediately" option.
- Usability: the down signal is shown a little bit longer on all the boards (including publicresults)
