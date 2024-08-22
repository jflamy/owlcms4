> [!WARNING]
>
> - This is a **beta release**, used for testing and translation. ***Some features could be non-functional***.
> - Beta releases are **not** normally used in actual competitions, except when a new feature is required. Use extreme care in testing if you intend to do so.

- Preliminary releases change log
  - (beta03) Translations (es, da, pt)
  - (beta03) Fixed categoryFinished property to work correctly in Excel reports (this property replaces categoryDone)
  - (beta03) Fixed age group length validation to correctly indicate maximum length in the interface
  - (beta03) Default USAW template - removed spurious category
  - (beta02) Fix: extra columns displaying a score and a rank were shown even if no age group with special scoring was in the session
  - (beta02) Fixed display glitch on Lifting Order scoreboard. Immediately after the 3rd snatch, while the decision lights were shown, the bottom line of the scoreboard would be stretched.  Now fixed (the number of lines in the scoreboard was miscalculated)
  - (beta01) Updated the documentation for the new Documents page 
  - (beta01) Added a formattedRange property to sessions in order to summarize the bodyweights and A/B/C groups in a session
  - (beta01) Fixed a (rare, intermittent) exception when multiple championships are present (error in name sorting)
- Documents: complete redesign of the Documents page
  - All documents needed to prepare and run the competition are here (Results are still on their own page).
  - The Competition-wide documents such as the Start List are handled as in the previous versions.
  - Documents like the Athlete Cards can be produced for one or more sessions.  If more than one session is selected, a zip file is produced, otherwise the Excel is produced.
  - Document sets can be produced, for example, a weigh-in form together with the cards.  When a document set is selected, a zip file is produced. The document set can be produced for one or more sessions.
  - If more than one copy of a document is needed (for example, two weigh-in forms for each session), you can adjust your template by duplicating the tab.  Same for jury forms if you want to avoid printing 3 copies manually.

For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/51.0.0-rc02)
