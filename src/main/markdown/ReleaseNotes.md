

| Introducing the Owlcms Control Panel                         |
| ------------------------------------------------------------ |
| **New and improved installation process for owlcms**.<br><br>From now on all platforms use the same installation process.  A "Control Panel" program is now available for Windows, macOS, RaspberryPi OS and Linux. It handles installations and updates, as well as starting and stopping owlcms.<br><br>**See the [Control Panel Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) and the user guide for the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md).**<br><br>The release area in this repository is now used to host the files that the control panel fetches. |

**Maintenance Log**

- 55.3.0: The URL parameters controlling showing of records and of leaders were being ignored, so the pages could not be bookmarked
- 55.3.0: Scores such as Q-Points or Sinclair will now be shown on scoreboards during snatch (see below)
- 55.2.1: Fixed record definition import to deal with text cells that are not null but contain no text.
- 55.2.0: Inclusion of 2025 Youth body weight classes in the AgeGroups2025 age group template
- 55.2.0: Windows Installation documentation modified to refer to the owlcms_controlpanel.exe executable directly.  The installer has been sidelined while Microsoft investigates a false warning.
- 55.1.3: Changes to Language and System Settings could not be saved due to a validation done on the wrong field
- 55.1.3: When defining categories on the registration or SBDE spreadsheet, use `;` or `,` as delimiter.  Use of `/` is ambiguous and is no longer accepted.
- 55.1.2: "Single Referee" now works for keypads
- 55.1.0: Added a competition rule to use the 20kg rule for Masters athletes instead of the official 80%

**New In This Release**

- New [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) and startup instructions using the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) for updating, launching and stopping OWLCMS on a local computer.

- When medals are awarded by score like (Q-Points/Q-Masters/Q-Youth/Sinclair/SMHF/etc,), the scores will be visible during the snatch. A feature toggle `noInterimScoresInResults` can be used so that the result sheets always show 0 is no total has been set.

- Inclusion of 2025 Youth body weight classes in the AgeGroups2025 age group template

- Ability to set the duration of the clean & jerk break explicitly for a session, overriding the competition-wide rules.

  - A new Excel template variable `${session.cleanJerkBreakMinutes}` can be used to show this to the announcer if you have a specific template for athlete introductions

- Competition Rules: It is now possible to force the 20kg rule for Masters instead of the 80% rule.

- Refereeing: Selecting "Single Referee" using the ⚙menu  now works with keyboard shortcut keypads (USB, Bluetooth, Joystick).  

  - Any of the 3 referees will work, but configuring the center referee makes most sense (3 = good lift, 4 = no lift).  A single decision will trigger the down signal.

- Simplified Video Setup
  - The default style for Video Streaming is now `transparent` 
    With this change,
    
    - It is no longer necessary to crop the Current Athlete view
    - There is no need to add a green mask to have a floating scoreboard
    
    See the documentation on using [OBS](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.) for examples of using the transparent style.
    
  - The style can be changed back to `nogrid` on the System Settings > Customization page to get the black background styles identical to the on-site scoreboards.

- Templates: a new _FlatFile.xlsx template is available for Competition Results.  It is meant for statistical analysis where headers for each category make reading the file difficult.

  


For other recent changes, see [version 55 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1) and [version 5 release notes](https://github.com/owlcms/owlcms4/releases/tag/53.1.0)
