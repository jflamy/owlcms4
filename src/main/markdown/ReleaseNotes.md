<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://jflamy.github.io/owlcms4/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://jflamy.github.io/owlcms4/#/LocalControlPanel.md)**

<br>

**New in Release 69.0**

- Complies with the requirements of the October 2026 IWF RFP for TIS Certification

- IWF TCRR 2026 seven-person Adjudication Panel (jury size 4 with video playback).
- Jury votes shown on attempt boards and scoreboards during a jury review.
- Jury Control page replaces the Jury Console; "Jury President" is now "Jury Chair".
- Named accounts with role-based access; new Accounts Management page.
- Competition Supervisor page and passive speaker view.
- Tamper-resistant, machine-readable audit trails for compliance.
- New Backup page in the Admin section.
- owlcms can start directly from a database export (`OWLCMS_LOADJSON`).
- Event forwarding keys are encrypted in JSON exports.
- Conflicting changes to athlete information are clearly signaled.
- The H2 database can no longer be opened by other programs while owlcms runs (`H2ServerPort` removed).
- Imported textual data is cleaned of scripts and markup.
- Session selectors distinguish completed and upcoming sessions.
- Scoreboards can show body weight in its own column (`displayBodyWeight` feature toggle).
- Protocol templates include lot and start numbers.
- VFE team reports flag category and team-size violations.
- Optional resource monitoring (`resourceTraces` feature toggle).

-For other recent changes, see [the release repository](https://github.com/jflamy/owlcms4/releases)
