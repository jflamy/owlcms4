<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 65**

65.0.0: Act as record repository :
  - Enable a separate dedicated OWLCMS instance to keep and edit records
    - A new feature toggle "recordRepository" hides all the pages except records management
    - Default access is read-only, with capability to export selected records and the database
  - Ability to import the provisional records from a meet to accept them, thereby creating a full history
  - Round-trip updating - export a subset of the records, update the file, and reload after making corrections or approving provisional records
    - This does not delete records.
  - Define the set active record set (inactive records are not exported or editable)
  - Abiilty to export the database (including using the /competition/export backup from localhost or a machine in the backdoor list)

65.0.0: Added Sinclair 2028.  
  - The default remains the 2001-2004 values.
  - You should NOT switch without analysis, as the coefficients change quite a bit.
  - Note that Q-Points or GAMX are better alternatives, for a number of reasons.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
