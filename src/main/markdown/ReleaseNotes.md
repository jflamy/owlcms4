<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**Change Log**

63.0.0-rc07: HTTP event forwarding: Increased parameters for connection pooling, drain connections, prevent multiple registrations to FOP event bus

63.0.0-rc06: Connection pooling for http posts from EventForwarder

63.0.0-rc05: Reversal of an accidental changes to attemptboard CSS styling that resulted in severely truncated first names 

63.0.0-rc04: Execution of the jury reversal following announce by speaker was denied if the jury had pressed resume.

63.0.0-rc03: Identify which field is wrong when trying to save (field can be on another tab)

63.0.0-rc03: Run simulations even if manualStartNumbers is set (ignore and set them)

63.0.0-rc03: (cosmetic) Body Weight were printed on Weigh-in forms -- should always be blank as a matter of principle

63.0.0-rc02 : added lot number to event forwarders.

63.0.0-rc01 : The TeamGlobalScoring template in the Competition Results/Final Package section now works also when a championship is selected.

63.3.0-beta04: Added fix of stored category codes at startup to correct potential legacy mismatches

63.3.0-beta04: Revision of scoreboard templates and style sheets for all themes

63.0.0-beta03: Sync with 62.2.6 - Order of results on results sheet, QMasters interim rankings during snatch.

**New in Release 63.0**

63.0.0: New feature toggle "manualStartNumbers" that enables manual editing of start numbers when errors were made when numbering athlete cards or handing out bibs.  This disables the automatic allocation of start numbers (must use the button on the Weigh-In page)

63.0.0: Event forwarding using web sockets to support enhanced tracking programs like owlcms-tracker that will eventually replace publicresults. Updates on first lifting order recalculation (reload session, decision, marshal change)

63.0.0: The TeamGlobalScoring template in the Competition Results/Final Package section now works also when a championship is selected.

63.0.0: Translation for zh-HANT (Traditional Chinese)

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
