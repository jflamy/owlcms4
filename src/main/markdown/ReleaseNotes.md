

### ⚠️⚠️⚠️ OWLCMS INSTALLATION PROCEDURE⚠️⚠️⚠️
**Since version 55, the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) is the normal way to install and run OWLCMS. **
**See the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) for details.**

<br>

**Change Log**

- 56.0.5: Some of the Competition Information in Start Book Data Entry (SBDE) Excel was not being read correctly; also added capability to read the end date from cell N1.
- 56.0.4: `.xlsm` extension from templates is now correctly preserved when using the Documents page
- 56.0.3: publicresults now can default to lifting order scoreboard. See below.
- 56.0.3: publicresults will no longer emit simultaneous requests for configuration files.
- 56.0.2: Translation updates: Spanish, German, Romanian, Hungarian, Russian
- 56.0.1: Q-Points are now set to 0 for men bodyweights under 45kg, and women bodyweights under 40kg, as the function is not valid for such weights (Q-Youth should be used.)
- 56.0.1: Fixed an oversized "Leaders" row on the Lifting Order scoreboard that could happen when an athlete has withdrawn from CJ
- 56.0.1: Updated Jury Replays add-on module documentation.
- 56.0.1: Added back the instructions for manual installation using java and the .zip file for setups where the control panel can't run

**New In Release 56**

- Competition Information
  - The competition end date can be captured. See also Templates, below.
  - Entering the end date in cell N1 of the SBDE file now works.
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
- publicresults: 
  - added `OWLCMS_LIFTINGORDER` environment variable. Set to `true` to change the default scoreboard order.  On fly.io, this can be done by setting a secret `OWLCMS_LIFTINGORDER` with value `true` on the application's management page.
  - Throttle requests to download the configurations. With a large number of platforms, there could be a large number of requests coming through continuously, causing slow startup.




For other recent changes, see [version 55.3 release notes](https://github.com/owlcms/owlcms4/releases/tag/55.3.0) and [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1)
