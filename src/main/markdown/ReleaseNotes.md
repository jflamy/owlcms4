<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>

**Change Log**

<<<<<<< HEAD
63.0.0-alpha02: Sync with changes in 62.0.4
=======
62.1.1: Don't modidify the capitalization of first names if the feature switch `dontFixNames` is present.  This will be the case automatically for jp, ar, he, el and ru languages.

62.1.0: At startup, the system will now detect and remove participations that refer to broken categories (sometimes found in
from old databases). The correct participations can then be fixed using the registration or weighin page.  You should
use the reassign ranks on the results page if you reassign categories to one or more athletes.
>>>>>>> a5852cdff (dontFixNames)

**New in Release 63.0**

63.0.0: New feature toggle "manualStartNumbers" that enables manual editing of start numbers when errors were made when numbering athlete cards or handing out bibs.  This disables the automatic allocation of start numbers (must use the button on the Weigh-In page)

63.0.0: Event forwarding using web sockets to support enhanced tracking programs like owlcms-tracker that will eventually replace publicresults.

63.0.0: Experimental: AI-generated translation for zh-HANT (Traditional Chinese)

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
