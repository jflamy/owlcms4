> [!WARNING]
>
> - This is a release candidate [(see definition)](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation. *It is still a preliminary release*
> - You should test all releases, with actual data, *several days* before a competition. This is especially important when considering the use of a release candidate.

- Preliminary releases change log
  - (rc01) Update the start numbers when producing empty protocol, jury and introduction sheets.
  - (rc01) jxls3 version of SBDE export (much faster)
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
- Scoreboards:
  - Fix: Immediately after the 3rd snatch, while the decision lights were shown, the bottom line of the scoreboard would be stretched
- SBDE export:
  - Converted the template to jxls3, resulting in massive speed improvement (~7 seconds for 1000 athletes).



For other recent changes, see [version 50 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/51.0.0-rc02)
