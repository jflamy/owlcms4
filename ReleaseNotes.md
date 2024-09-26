> [!IMPORTANT]
>
> - You should test all releases, with actual data, *several days* before a competition.
> - It is always wise to export your current database before updating.

- Maintenance log:
  - 53.0.2: When producing the final package/competition book, if no championship was selected the results were not produced by eligibility categories as they should have.
  - 53.0.2: Final package templates fixed to use the translated code for extra/out-of-competition/invited athletes
  - 53.0.1: Technical change to the build process. Software is identical to 53.0.0
  
- Selectable scoring systems for [Best Lifter](https://jflamy.github.io/owlcms4/#/ResultDocuments?id=competition-results) in a championship and [Score-based Medals](https://jflamy.github.io/owlcms4/#/ScoreBasedCompetitions) (see the links for documentation),
  - On the competition results page, it is possible to select a scoring system that will be shown in the grid.  This allows computing the best athlete for a championship using a different scoring system (for example, using Q-youth age-adjusted totals for a Youth Championship)
  - The standard templates have been updated to use the Best Athlete scoring system selected if one is picked (the default is the competition global best athlete scoring system)
  - The names have been aligned with what Dr. Huebner uses in her online calculators (Q-Youth, previously HP points or Age Factors, and Q-Masters, previously Q-age).
- Jury Sheets for examinations:
  - There are now two jury sheets in the default configuration.  One without the examination results, one with.  To print the examination version, use the `Print Entire Workbook` option (the examination results are in the second tab)

- Children Categories Bar Rules
  - The feature flag `lightBarU13` can be used to use a 15kg bar for boys in the U11 and U13 categories.  If an athlete needs the 20kg bar, the "Non-Standard bar" feature can be used to override. This is the same as removing the 20kg bar for younger boys age groups. 
- Support for Q-masters results
  - Q-masters is like SM(H)F but based on Q-points instead of Sinclair.  It is Q-points * the same age factor as SM(H)F
  - The default templates for Masters protocols, result sheets and competition books now show the Q-masters value in addition to the SM(H)F.
- Import of External Session Results: the following is now possible
  - If a session needs to be run outside or in another building a) perform weigh-in normally and enter data normally in the main database. b) Export the main database and load it into the owlcms running in the other building c) Run the session, export the remote database c) Use the new feature at the bottom of the Results page to selectively read back the lifts from the remote session.
  - Only the lift information is read back.  Note that owlcms follows the rules and will determine winners according to the lifting order that would have been followed had all sessions taken place normally.
- jxls3 Templates
  - In the top cell, where `jx:area` is given, it is now possible to add a directive of the form `owlcms:fixMerges(4, [1, 2, 3])`  This would merge cells vertically in columns 1, 2, 3, starting with row 4.  The cells are merged from the non-empty value down to the next non-empty cell.  This is a workaround for a limitation/bug in jxls3.  See the `templates/schedule/DaySchedule.xlsx` file for an example.
- Locale: fixed a race condition where pages would load before it was determined that the application should switch to English because there is no translation for the local language.
- Event Publishing: fixed issue with liftType published during event forwarding to public results and video information feeds.

For other recent changes, see [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 52 release notes](https://github.com/owlcms/owlcms4/releases/tag/52.0.6)
