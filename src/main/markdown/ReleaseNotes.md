> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.  
> - **Alpha releases are not normally used in actual competitions.**

- Preliminary releases change log
  - (alpha06) More intuitive sorting order on some of the documents (e.g. Introduction Sheet is now by age groups, whereas Empty Protocol is by Start Number)
  - (alpha06) The Documents page no longer includes the session editing behavior
  - (alpha06) Age Group configuration drop down now respects the "local files only" setting.
  - (alpha05) Cleaned-up Documents page so that all documents have harmonized user interface dialogs with better feedback
  - (alpha04) Added a Schedule document suitable for large events, with USAW-style additional information about categories and entry totals
  - (alpha04) Updated Marshal card to be more similar to IWF
  - (alpha03) Sessions page: to reduce observed user confusion, added an "Edit Details" button next to "Edit Athletes".
  - (alpha03) Fix empty protocol download button to use the correct template (vs weigh-in form)
  - (alpha02) Translations (ru)
  - (alpha02) Apply fix for spurious "out-of-competition" attempt board marker from 51.0.4
  - (alpha02) Apply fix for registration file import from 51.0.3
  - (alpha01) Initial version of redesigned Documents page
- Documents: complete redesign of the Documents page
  - All documents needed to prepare and run the competition are here (Results are still on their own page).
  - The Competition-wide documents such as the Start List are handled as in the previous versions.
  - Documents like the Athlete Cards can be produced for one or more sessions.  If more than one session is selected, a zip file is produced, otherwise the Excel is produced.
  - Document sets can be produced, for example, a weigh-in form together with the cards.  When a document set is selected, a zip file is produced. The document set can be produced for one or more sessions.
  - If more than one copy of a document is needed (for example, two weigh-in forms for each session), you can adjust your template by duplicating the tab.  Same for jury forms if you want to avoid printing 3 copies manually.

For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/51.0.0-rc02)
