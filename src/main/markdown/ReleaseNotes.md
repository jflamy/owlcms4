> [!CAUTION]
>
> - This is an **alpha release**, used for validating new features.  *Some features are likely to be incomplete or non-functional*.
> - **Alpha releases are not normally used in actual competitions.** - It is always wise to export your current database before updating if it contains important data.

- Children Categories Bar Rules
  - Automatic use of 15kg bar for U11 and U13 categories is now under a feature flag `lightBarU13`.  This is the same as removing the 20kg bar for younger boys age groups.  If an athlete needs the 20kg bar, the "Non-Standard bar" feature can be used.

- Support for Q-age Masters results
  - The default templates for Masters protocols, result sheets, and competition books now show the Q-age value in addition to the SM(H)F.

- Import of External Session Results: the following is now possible
  - If a session needs to be run outside or in another building a) perform weigh-in normally and enter data normally in the main database. b) Export the main database and load it into the owlcms running in the other building c) Run the session, export the remote database c) Use the new feature at the bottom of the Results page to selectively read back the lifts from the remote session.
  - Only the lift information is read back.  Note that owlcms follows the rules and will determine winners according to the lifting order that would have been followed had all sessions taken place normally.
- jxls3 Templates
  - In the top cell, where `jx:area` is given, it is now possible to add a directive of the form `owlcms:fixMerges(4, [1, 2, 3])`  This would merge cells vertically in columns 1, 2, 3, starting with row 4.  The cells are merged from the non-empty value down to the next non-empty cell.  This is a workaround for a limitation/bug in jxls3.  See the `templates/schedule/DaySchedule.xlsx` file for an example.
- Locale: fixed a race condition where pages would load before it was determined that the application should switch to English because there is no translation for the local language.

For other recent changes, see [version 51 release notes](https://github.com/owlcms/owlcms4/releases/tag/50.0.0) and [version 52 release notes](https://github.com/owlcms/owlcms4/releases/tag/52.0.6)
