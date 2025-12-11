<!-- markdownlint-disable -->

⚠️⚠️⚠️
**To install and run OWLCMS, you need to use the OWLCMS Control Panel.** This location contains the release notes and the software modules that the control panel will install for you.

- **The OWLCMS Control Panel can be downloaded at [this location](https://github.com/owlcms/owlcms-controlpanel/releases). and you can refer to the [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md)**
- **User Documentation for the Control Panel is located at [this location](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md)**

<br>
**Maintenance Log**

64.0.0-beta01: Feature set deemed complete. Fixes/adjustments only expected.

**New in Release 64**

64.0.0: Support for GAMX 2.0 scores: GAMX + age-adjusted variants GAMX-M (Masters), GAMX-U (Kids+Youth), GAMX-A (13-40).  Also added CAT_GAMX, GAMX computed at IWF JR/SR category weight.

64.0.0: For performance, only the scoring systems required by the best athlete and medals are computed by default; additional ones can be selected if needed by local templates

64.0.0: Fix: Athletes not weighed-in are not propagated to the introduction sheet

64.0.0: Fix timer visual stutter on initial 1:00 or 2:00 clock start 

64.0.0: Require explicit field of play parameters instead of inheriting through the user's session (merged sanitization from 63.3.x)

64.0.0: New websocket protocol for updated tracker
- Uses the v2 export format
- Additional information for tracker applications (e.g. time remaining on breaks, precalculated display information)

64.0.0: New v2 JSON export format
- A new cleaner export format is available when the feature toggle `v2export` is active.  Historical unfortunate naming fixes,
symbolic references/natural keys for readability.
- An extra button is visible when
the toggle is active.  The new format uses natural keys for better readability.
- Import distinguighes the two formats automatically.

For other recent changes, see [the release repository](https://github.com/owlcms/owlcms4/releases)
