> [!WARNING]
>
> - This is a release candidate [(see definition)](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate), used for final public testing and translation. *It is still a preliminary release*
> - You should test all releases, with actual data, *several days* before a competition. This is especially important when considering the use of a release candidate.

| Introducing the Owlcms Control Panel                         |
| ------------------------------------------------------------ |
| Starting with release 55, the installation process for running owlcms on a local machine changes.<br><br>Previously, only Windows had a full installer.  From now on all platforms use the same installation process: a "Control Panel" program available for Windows, macOS, RaspberryPi OS and Linux handles installations and updates, as well as starting/stopping owlcms.<br><br>See the [Control Panel Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.md) and the instructions for using the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md).<br><br>From now on the release area now only includes the owlcms files that are loaded by the control panel.  The control panel has its own [repository](https://github.com/owlcms/owlcms-controlpanel). |

**Maintenance Log**

- 55.1.0: "Single Referee" and "Session uses flags" options on the announcer settings
- 55.1.0: Fix: If the registration category was score-based (e.g., an age group with all bodyweights allowed), the weigh-in page did not show the expected eligibility categories until a body-weight was entered
- 55.1.0: Fix: Team Results web page had the wrong filter dropdowns in the header, resulting in blank page
- 55.1.0: Fix: Jury could not trigger deliberation or challenge during a CJ Break.
- 55.1.0: Sort Championship Names alphabetically in filters and the editing menus
- 55.1.0: Additional jury decision information concerning the status of records sent to video and public results event feeds

**New In This Release**

- New [Installation Instructions](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.) and startup instructions using the [owlcms Control Panel](https://owlcms.github.io/owlcms4-prerelease/#/LocalControlPanel.md) for updating, launching and stopping OWLCMS on a local computer.

- Ability to set the duration of the clean & jerk break explicitly for a session, overriding the competition-wide rules.

  - A new Excel template variable `${session.cleanJerkBreakMinutes}` can be used to show this to the announcer if you have a specific template for athlete introductions

- Refereeing: on the announcer screen, using the ⚙menu shows two labels that are new/updated

  - Selecting "Single Referee" now works with USB keypads.  Any referee number will work, but configuring the center referee is recommended (3 = good lift, 4 = no lift).  A down signal is shown if this option is selected.  The announcer can act as a referee in this mode, using the announcer console shortcuts (F2 = good lift, F4 = no lift), and a down signal is shown

  - Selecting "Session Uses Flags for Refereeing" disables the down signal when the announcer enters the decision (since the center referee has already given it verbally.)

- Simplified Video Setup
  - The default style for Video Streaming is now `transparent` 
    With this change,
    
    - It is no longer necessary to crop the Current Athlete view
    - There is no need to add a green mask to have a floating scoreboard
    
    See the documentation on using [OBS](https://owlcms.github.io/owlcms4-prerelease/#/LocalDownloads.) for examples of using the transparent style.
    
  - The style can be changed back to `nogrid` on the System Settings > Customization page to get the black background styles identical to the on-site scoreboards.

- Templates: a new _FlatFile.xlsx template is available for Competition Results.  It is meant for statistical analysis where headers for each category make reading the file difficult.

  


For other recent changes, see [version 55 release notes](https://github.com/owlcms/owlcms4/releases/tag/54.2.1) and [version 5 release notes](https://github.com/owlcms/owlcms4/releases/tag/53.1.0)
