<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 65**

65.0.0: Act as record repository :
  - A separate dedicated OWLCMS instance can be used to keep and edit the records
    - A new feature toggle "recordsOnly" hides all the pages except records management
    - Default access is read-only, with capability to export selected records
    - The preparation page can be accessed by editing the URL
  - You can import the provisional records from a meet to accept them and have automatic updates with history
  - Round-trip updating is supported -- you can export a subset of the records, update the file, and reload after making corrections or approving provisional records
    - This does not delete records.
  - You can set the "active" record set to quickly pick all the records you need for a competition in a single file
  - The /competition/export URL can be used to take backups

65.0.0: Added Sinclair 2028.  
  - The default remains the 2001-2004 values.
  - You should NOT switch without analysis, as the coefficients change quite a bit.
  - Note that Q-Points or GAMX are better alternatives, for a number of reasons.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
