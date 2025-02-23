



| Installation and Update                                      |
| ------------------------------------------------------------ |
| Since version 55, owlcms is installed and updated using the *owlcms control panel*. See the [Control Panel Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) and the user guide for the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md). |

**Change Log**

- 56.0.0-rc01: Top Score (Top Sinclair) scoreboard would require a manual refresh when switching groups
- 56.0.0-rc01: fixed inverted test for showing interim scores without totals during score-based medal session (and on corresponding scores spreadsheet)
- 56.0.0-beta04: Extra information sent to drive jury replays was confusing the competition simulator.
- 56.0.0-beta03: Importing and Exporting of Technical Officials now uses local language for headers and TO levels.
- 56.0.0-beta02: Competition end date
- 56.0.0-beta02: Documentation for self-service jury replays added
- 56.0.0-beta01 Current Session information is sent in the MQTT timer start messages.
- 56.0.0-alpha02 .xslm files are now visible in template lists
- 56.0.0-alpha02 Include winning order fix from v55.
- 56.0.0-alpha02 Timing summary
- 56.0.0-alpha00 Technical Officials can be listed on a page, with their IDs and federation

**New In Release 56**

- Competition Information
  - The competition end date can be captured.
- Technical Officials
  - A new button is available on Prepare Competition to define a list of TOs.
  - Import and Export from Excel are supported. Table headers and TO Levels are translated to the current language. It is always possible to import a file exported in English.
  - The Assignment of Officials on the Session page now allows to pick from the list (the choices are auto-completed as you type)
  - A summary report of who was assigned to what role including the total number of sessions per TO is also available.
- Timing Summary
  - Now use the rules for the Clean & Jerk break, as well as the overrides that were entered on each session
- Templates 
  - `${competition.endDate}` is now available as a variable.
  - Templates with Excel macros can now be used:  `.xlsm` files are visible as templates.
- Self-service Jury Replays
  - The documentation for the self-service jury module is now included.



For other recent changes, see [version 55.3 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.3.0) and [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1)
