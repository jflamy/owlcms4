



⚠️⚠️⚠️ 
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)** 
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**



<br>

**Maintenance Log**

60.0.0-rc04: Further fixes for the championship - age group filter cascade on the Final Package page.

60.0.0-rc03: The final results competition book ("final package") was not using a competition-wide best athlete scoring scheme (neither the default nor the drop-down value)

60.0.0-rc03: The Competition Results grid Championship Filter could show the Masters championship twice and get confused when filtering.

60.0.0-rc02: Ranks for medals awarded using a scoring system other than TOTAL were not updating live on the scoreboard (the score and the score-based ranks are additional columns when the registration category is score-based)

60.0.0-rc02: "Sinclair at category weight" was not being computed for men.

60.0.0-rc01: Fixed filtering options in the Record grid.  Current Provisional now means best new record from the current competition,  Current Official means best official, Current All means best including provisional.

60.0.0-rc01: Fixed an issue when exporting the database that would cause the category string to be null.

**New in Release 60.0**

60.0.0: When producing several documents together (either multiple sessions, or a document set), the zip produced now includes a `print.bat` script for Windows.  Extract All on the zip followed by double-clicking on print.bat will print all the documents on the default printer.

60.0.0: New page showing the records in a filtering grid, to allow choosing what is exported. Clicking on a record allows for editing. See [documentation](https://owlcms.github.io/owlcms4-prerelease/#/2500RecordsManagement)


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases) 
