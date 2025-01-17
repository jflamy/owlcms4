> **REMINDER**
>
> - You should test all releases, with actual data, *several days* before a competition.

| Introducing the Owlcms Control Panel                         |
| ------------------------------------------------------------ |
| Starting with release 55, the installation process for running owlcms on a local machine changes.<br><br>Previously, only Windows had a full installer.  From now on all platforms use the same installation process: a "Control Panel" program available for Windows, macOS, RaspberryPi OS and Linux handles installations and updates, as well as starting/stopping owlcms.<br><br>See the [Control Panel Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) and the instructions for using the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md).<br><br>From now on the release area now only includes the owlcms files that are loaded by the control panel.  The control panel has its own [repository](https://github.com/owlcms/owlcms-controlpanel). |

Maintenance Log

- 55.0.3: Updated the installation and execution instructions on the Release Sites to match version 1.8.0 of the control panel.
- 55.0.3: Fixed the installation process to create version.txt correctly for backward compatibility

**New In This Release**

- New [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.) and startup instructions using the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) for updating, launching and stopping OWLCMS on a local computer.

- Ability to set the duration of the clean & jerk break explicitly for a session, overriding the competition-wide rules.

  - A new Excel template variable `${session.cleanJerkBreakMinutes}` can be used to show this to the announcer if you have a specific template for athlete introductions

- The "please update" message is now different when the owlcms knows it was started from the control panel.

- Simplified Video Setup
  - The default style for Video Streaming is now `transparent` 
    With this change,
    
    - It is no longer necessary to crop the Current Athlete view
    - There is no need to add a green mask to have a floating scoreboard
    
    See the documentation on using [OBS](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.) for examples of using the transparent style.
    
  - The style can be changed back to `nogrid` on the System Settings > Customization page to get the black background styles identical to the on-site scoreboards.

- Templates: a new _FlatFile.xlsx template is available for Competition Results.  It is meant for statistical analysis where headers for each category make reading the file difficult.

  


For other recent changes, see [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1) and [version 53 release notes](https://github.com/owlcms/owlcms4/releases/tag/53.1.0)
