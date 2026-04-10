<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**New in Release 65**

65.1.1: The Competition Results full eligibility results did not show the correct score for the age group (would sometimes fall back to competition-wide)

65.1.0: Fix: it was previously possible to accidentally create two platforms with the same name, which prevented results from being produced. Integrity checks at startup and when importing will now correct this situation.  User interface checks have been added.

65.1.0: JXLS templates now receive "championship", "ageGroupPrefix" (the age group without the gender), and "gender" as template variables

65.0.0: Act as record repository :
  - Enable a separate dedicated OWLCMS instance to keep and edit records
    - A new feature toggle "recordRepository" hides all the pages except records management
    - Default access is read-only, with capability to export selected records and the database
  - Ability to import the provisional records from a meet to accept them, thereby creating a full history
  - Round-trip updating - export a subset of the records, update the file, and reload after making corrections or approving provisional records
    - This does not delete records.
  - Define the set active record set (inactive records are not exported or editable)
  - Abiilty to export the database (including using the /competition/export backup from localhost or a machine in the backdoor list)

65.0.0: Improved kill behavior:
  - On macOS and Linux, using kill (kill -TERM) targeting owlcms is intercepted and interpreted as intentional stoppage without restart

65.0.0: Added Sinclair 2028.  
  - The default remains the 2001-2004 values.
  - You should NOT switch without analysis, as the coefficients change quite a bit.
  - Note that Q-Points or GAMX are better alternatives, for a number of reasons.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
