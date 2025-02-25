

### ⚠️⚠️⚠️ OWLCMS INSTALLATION PROCEDURE⚠️⚠️⚠️
**Since version 55, the [OWLCMS Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) is used to install and run OWLCMS. See the [Control Panel Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) to install it.**

<br><br>

**Change Log**

- 56.0.0-rc04: `.xlsm` files are now accepted as templates for pre-competition documents (in addition to results)
- 56.0.0-rc03: Fixed the inference of body weight on the registration editing form when a mix of eligible categories has been assigned to the athlete.
- 56.0.0-rc02: Only errors in eligibility for the main registration category were reported when loading a registration file or the SBDE start book data. Now errors in all requested eligibility categories are signaled.
- 56.0.0-rc01: The Top Score (Top Sinclair) scoreboard would require a manual refresh when switching groups.
- 56.0.0-rc01: fixed inverted test for showing interim scores without totals during score-based medal session (and on corresponding scores spreadsheet)

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



For other recent changes, see [version 55.3 release notes](https://github.com/owlcms/owlcms4/releases/tag/55.3.0) and [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1)
