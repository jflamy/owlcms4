<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>
**New in Release 63.4**

63.4.0: Fix: Registration editing page did not show did not show all the possible open categories for youth superheavy categories with lower bounds (e.g. U15 79+). The athlete can be 88 all the way to 110+.

**New in Release 63.3**

63.3.1: Fixes for random updates to timers on announcer/marshall/jury screens and other related anomalies.  Systematic change in the way sessions

63.3.0: Fix for intermittent issue on announcer/marshal screen. The athlete grid would sometime be hidden (a refresh would bring it back), due to incomplete filtering/routing of timer
events.

**New in Release 63.2**

63.2.3: Fixed the warmup scoreboard buttons to show the current attempt information by default at the top of the screen.  Public and video streaming scoreboards show a static banner instead.

63.2.2: Fix: Further fixes when reading registration files when database has bodyweight categories + prefixes and suffixes

63.2.1: Fix: All potential eligibility categories are now again selectable when opening the weigh-in form (instead of only the previously selected ones)

63.2.1: Fix: Registration File was not processing category names with a + suffix (110+, 86+)

63.2.0: Public Scoreboard
- there is now an option to select whether the header is fixed with icons on the side or displays the current athlete
- the route for the on-venue public scoreboard is now called displays/publicScoreboard instead of publicResults

**New in Release 63.1**

63.1.1: Fix: The Team Results page was showing the wrong sum for QMasters scores

63.1.1: Fix: When using keypads for refereeing, a partial decision with a missing referee would be shown if majority was reached but 3rd referee was late

63.1.1: Fix: Added a start-up consistency check for category genders and codes relative to the parent age group.

63.1.1: When using keypads for refereeing, a partial decision with a missing referee would be shown if majority was reached but 3rd referee was late

63.1.1: Added a start-up consistency check for category genders and codes relative to the parent age group.

63.1.0: If the jury gives decisions by hand signal or forgets to press, the announcer can now trigger the good/bad lift processing.

63.1.0: Marshal can now accept/reject a late (or illegal) change using big buttons instead of small checkbox

63.1.0: There is now a "clear weigh-in" button to clear weigh-in and declarations created during testing.

63.1.0: Loading an SBDE or registration file will accept either > or + as prefix when looking up the category (both SR F >86 and SR F +86 will be tried)

63.1.0: On the attempt board, first names with multiple parts that are too long to fit will be truncated on a whitespace boundary (the exceeding words will be hidden)

63.1.0: Video header now uses the BigTitle font family. Download whatever font you want to local/fonts, rename according
to conventions used in fonts.css.  Currenly BigTitle is the Noto font. This is a workaround for bundling of style sheets.

63.1.0: Changes to BaseResults.java to fix the URL format used when referring to a logo.

63.1.0: Translations were not applied to referee levels in the Referee editing page and the referee lists

63.1.0: Stabilized the startup behavior for websocket data feed (database and translations are sent on websocket open)

**New in Release 63.0**

63.0.0: Leader board at bottom no longer shows athlete who has bombed-out

63.0.0: Clear button for records now clears all records for the matching federation and age group, ignoring the original file name

63.0.0: The TeamGlobalScoring template in the Competition Results/Final Package section now works also when a championship is selected.

63.0.0: Revision of scoreboard templates and style sheets for all themes

63.0.0: Selecting multiple sessions before using a single document type button (e.g. for Cards) will produce a zip as expected.

63.3.0: Inspection of stored category codes at startup to correct potential legacy mismatches

63.0.0: Translation for zh-HANT (Traditional Chinese)

63.0.0: A notification appears naming the field that is considered invalid (useful when error is on another tab)

63.0.0: Root Athlete eligibility status can now be used on reports about additional participation categories

63.0.0: New feature toggle "manualStartNumbers" that enables manual editing of start numbers when errors were made when numbering athlete cards or handing out bibs.  This disables the automatic allocation of start numbers (must use the button on the Weigh-In page)

63.0.0: Jury reversal now works even if jury has pressed resume

63.0.0: Connection management for HTTP updates to remote event trackers

63.0.0: Event forwarding using web sockets to support enhanced tracking programs like owlcms-tracker that will eventually replace publicresults. Updates on first lifting order recalculation (reload session, decision, marshal change)

63.3.0: Leader board at bottom should no longer present person who as bombed-out

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
