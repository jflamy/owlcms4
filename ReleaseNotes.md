> [!WARNING]
>
> - This is a release candidate [(see definition)](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation. *It is still a preliminary release*
> - You should test all releases, with actual data, *several days* before a competition. This is especially important when considering the use of a release candidate.

- Maintenance log:
  - rc01: Translations for de, dk, ro, hu, pt, fr
  - beta04: Fixed an empty cell in the translation file that was preventing languages from being loaded.
  - beta03: Fixed the propagation of the selected Best Athlete scoring system to the classes that need to know (e.g. Athlete)
  - beta02: Changed the standard final package templates to use the selected Best Athlete scoring system
  - beta02: Updated the Result Documents documentation.
  - beta01: Jury sheets no longer have a forced page break. Scoring sheet in a second tab.
  - beta01: Updated translations to use Q-Youth and Q-Masters for the Huebner age+bodyweight scaled totals.
- Ability to override the Best Athlete scoring system
  - On the competition results page, it is possible to select a scoring system that will be shown in the grid.  This allows computing the best athlete for a championship using a different scoring system (for example, using Q-youth age-adjusted totals for a Youth Championship)
  - The standard templates have been updated to use the Best Athlete scoring system selected if one is picked (the default is the competition global best athlete scoring system)
- Children Categories Bar Rules
  - The feature flag `lightBarU13` selects using the 15kg bar for boys in the U11 and U13 categories.  This is the same as removing the 20kg bar for younger boys age groups.  However, if an athlete needs the 20kg bar, the "Non-Standard bar" feature can be used.
- Support for Q-masters results
  - Q-masters is like SM(H)F but based on Q-points instead of Sinclair.  It is Q-points * the same age factor as SM(H)F
  - The default templates for Masters protocols, result sheets and competition books now show the Q-masters value in addition to the SM(H)F.
- Import of External Session Results: the following is now possible
  - If a session needs to be run outside or in another building a) perform weigh-in normally and enter data normally in the main database. b) Export the main database and load it into the owlcms running in the other building c) Run the session, export the remote database c) Use the new feature at the bottom of the Results page to selectively read back the lifts from the remote session.
  - Only the lift information is read back.  Note that owlcms follows the rules and will determine winners according to the lifting order that would have been followed had all sessions taken place normally.
- jxls3 Templates
  - In the top cell, where `jx:area` is given, it is now possible to add a directive of the form `owlcms:fixMerges(4, [1, 2, 3])`  This would merge cells vertically in columns 1, 2, 3, starting with row 4.  The cells are merged from the non-empty value down to the next non-empty cell.  This is a workaround for a limitation/bug in jxls3.  See the `templates/schedule/DaySchedule.xlsx` file for an example.
- Locale: fixed a race condition where pages would load before it was determined that the application should switch to English because there is no translation for the local language.

For other recent changes, see [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 52 release notes](https://github.com/owlcms/owlcms4/releases/tag/52.0.6)
