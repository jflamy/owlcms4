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
    - The preparation page can be accessed by editing the URL
  - You can export all the provisional records from a meet and import them to have automatic updates
  - Round-trip updating is supported -- you can export a subset of the records, update the file, and reload after making corrections or approving provisional records
    - This does not remove entries however, voided records need to be removed in the application.
  - You can set the "active" records to quickly pick all the records you need for a competition in a single file


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
