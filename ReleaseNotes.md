> [!WARNING]
>
> - This is a release candidate [(see definition)](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation. *It is still a preliminary release*
> - You should test all releases, with actual data, *several days* before a competition. This is especially important when considering the use of a release candidate.



| Introducing the Owlcms Control Panel                         |
| ------------------------------------------------------------ |
| Starting with release 55, the installation process for running owlcms on a local machine changes.<br><br>Previously, only Windows had a full installer.  From now on all platforms use the same installation process: a "Control Panel" program available for Windows, macOS, RaspberryPi OS and Linux handles installations and updates, as well as starting/stopping owlcms.<br><br>See the [Control Panel Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) and the instructions for using the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md).<br><br>From now on the release area now only includes the owlcms files that are loaded by the control panel.  The control panel has its own [repository](https://github.com/owlcms/owlcms-controlpanel). |

Version Log

- 55.0.0-rc02: added a new template for exporting competition results as a flat file.
- 55.0.0-rc01: Mac installer for Intel mac has been renamed, adjusted documentation
- 55.0.0-beta02: Minor fixes to the documentation
- 55.0.0-beta01: Added per-session explicit override of clean & jerk break duration

**New In This Release**

- New [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.) and startup instructions using the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) for updating, launching and stopping OWLCMS on a local computer.

- Ability to set the duration of the clean & jerk break explicitly for a session, overriding the competition-wide rules.

  - A new Excel template variable `${session.cleanJerkBreakMinutes}` can be used to show this to the announcer if you have a specific template for athlete introductions

- Simplified Video Setup
  - The default style for Video Streaming is now `transparent` 
    With this change,
    
    - It is no longer necessary to crop the Current Athlete view
    - There is no need to add a green mask to have a floating scoreboard
    
    See the documentation on using [OBS](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.) for examples of using the transparent style.
    
  - The style can be changed back to `nogrid` on the System Settings > Customization page to get the black background styles identical to the on-site scoreboards.

- Templates: a new _FlatFile.xlsx template is available for Competition Results.  It is meant for statistical analysis where headers for each category make reading the file difficult.

  


For other recent changes, see [version 54 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1) and [version 53 release notes](https://github.com/owlcms/owlcms4/releases/tag/53.1.0)
