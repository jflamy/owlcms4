

### ⚠️⚠️⚠️ OWLCMS INSTALLATION PROCEDURE⚠️⚠️⚠️
**Since version 55, the [OWLCMS Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) is used to install and run OWLCMS. See the [Control Panel Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) for details.**

<br><br>

**Change Log**

- 56.0.1: Added manual installation instructions back, updated Jury Replays instructions.
- 56.0.0: Stable Release

**New In Release 56**

- Competition Information
  - The competition end date can be captured. See also Templates, below.
- Technical Officials
  - A new button is available on Prepare Competition to define a list of TOs.
  - Import and Export from Excel are supported. Table headers and TO Levels are translated to the current language. It is always possible to import a file exported in English.
  - The Assignment of Officials on the Session page now allows picking a TO from the list (the choices are auto-completed as you type)
  - A summary report is available about who was assigned to what role, including the total number of sessions per TO.
- Timing Summary
  - Now use the rules for the Clean & Jerk break, as well as the overrides that were entered on each session
- Templates 
  - The `${athlete.categoryScore}` template variable was not working correctly for eligibility categories other than the main registration category (the total would always be shown instead of the actual score.)
  - `${competition.endDate}` is now available as a variable.
  - Templates with Excel macros can now be used:  `.xlsm` files are visible as templates.
  - Competition Book (Final Package) template now deals correctly with out-of-competition athletes, which are identified by a negative rank.
- Experimental Self-service Jury Replays
  - The documentation for the self-service jury module is now included.  The module is not yet ready for full use, but this is necessary to gather feedback.



For other recent changes, see [version 55.3 release notes](https://github.com/owlcms/owlcms4/releases/tag/55.3.0) and [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1)
