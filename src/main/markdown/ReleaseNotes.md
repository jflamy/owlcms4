

| Introducing the Owlcms Control Panel                         |
| ------------------------------------------------------------ |
| **New and improved installation process for owlcms**.<br><br>All platforms now use the same installation process, using a "Control Panel" program.  The control panel handles installation and updates as well as starting and stopping owlcms.  It is available for Windows, macOS, RaspberryPi OS and Linux. It <br><br>**See the [Control Panel Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) and the user guide for the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md).**<br><br>The release area in this repository is now used to host the files that the control panel fetches. |

**Maintenance Log**

- 55.3.0: Record definition columns can be reordered or omitted (see below)
- 55.3.0: Added the "invited/extra/out of competition" status to the Start Book Data Entry export/import.
- 55.3.0: The URL parameters controlling showing of records and of leaders were being ignored, so the pages could not be bookmarked
- 55.3.0: Scores such as Q-Points or Sinclair will now be shown on scoreboards during snatch (see below)

**New In Release 55**

- New [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) and startup instructions using the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) for updating, launching and stopping OWLCMS on a local computer.

- Record definitions: The columns can now be reordered.  What matters is that the column header names in your match those in the documentation (see [Record File Format](https://owlcms.github.io/owlcms4-prerelease/#/2500RecordsManagement?id=record-file-format)) -- upper and lowercase does not matter.  The columns marked as optional can now be deleted from the definitions if you wish.

- When medals are awarded by score like (Q-Points/Q-Masters/Q-Youth/Sinclair/SMHF/etc,), the scores will be visible during the snatch. A feature toggle `noInterimScoresInResults` can be used so that the result sheets always show 0 is no total has been set.

- The "invited/extra/out of competition" status is now included in the Start Book Data Entry (SBDE) file. Reminder: you can add any column from the SBDE format to your registration sheet if needed.

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
