<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>
**New in Release 64**

64.0.0: Merge all the sanitization work done in releases 63.3

64.0.0: New websocket protocol for updated tracker
- Uses the v2 export format
- Additional information for tracker applications (e.g. time remaining on breaks, precalculated display information)

64.0.0: New v2 export format under test
- A new cleaner export format is available when the feature toggle `v2export` is active.  An extra button is visible when
the toggle is active.  The new format uses natural keys for better readability.
- Import distinguighes the two formats automatically.


For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
