### **Changes for release 4.32.3**

**Changes since 4.32.0**

- 4.32.3: Fix: Top Teams Sinclair was not updating on each successful lift
- 4.32.2: Amazon Firestick 2 resolution issues: reports a 1024px width. Fixed the ability to override the font size, and (for now) removed the size-responsive hiding of lift-specific ranks. Use total-only competition option to hide the lift ranks.
- 4.32.2: On weigh-in or registration forms, if a change in category results, a confirmation is required (#499)
- 4.32.2: Do not show leaderboard for last athlete when group is done
- 4.32.2: Group name is hidden on attempt board during ceremonies (#500)
- 4.32.2: Jury reversal notification sized correctly for 4K screens
- 4.32.1: Registration export now correctly opens on the Athlete registration tab.

**Changes for release 4.32**

- Requested weight is now shown on attempt board (including loading chart) and on top of scoreboard during breaks (but hidden during ceremonies).
- Announcer is notified when weight on bar needs to change.
- Adjusted margins and print areas of the various reports so they print correctly on both Excel and LibreOffice Calc.
- Officials scheduling and registration templates:
  - Registration import-export spreadsheet changed to add additional columns for Marshal2 and TechnicalController2. Added a report page in the export that shows the assignments for each official.
  - The Start List document has an Officials tab that shows official assignments for each group according to the introduction order.
- The configuration dialog for the Results scoreboard allows giving a font size (in "em" units) instead of the default (1.25em for HD screens).  The size is also read from the URL (the CSS convention of using a "." is used). This allows fine tuning the font size when broadcasting the results with OBS which does not support zooming.
- Fix: The "use best N results from team" setting was not being applied correctly on team scoreboards and the team result page.

### Highlights from recent stable releases

- New parameterized scoreboards.  Colors can be changed in the `styles/colors.css` file (*The default colors are the same as the previous defaults).*  See [Customization](https://owlcms.github.io/owlcms4/#/UploadingLocalSettings) for how to proceed. The scoreboards can be zoomed in or out using the  `Ctrl+` and `Ctrl-` keys to accommodate more lines, or to make text bigger with smaller groups.
- Improved management of ceremonies : see [Breaks and Ceremonies](https://owlcms.github.io/owlcms4/#/Breaks) procedures, and [Result Documents](https://owlcms.github.io/owlcms4/#/Documents) for the medals spreadsheet.
- [Jury](https://owlcms.github.io/owlcms4/#/Jury) console now supports summoning the referees either individually or all together. The jury console now allows direct reversal/confirmation of lifts (#435, #427)  
- The [Refereeing](https://owlcms.github.io/owlcms4/#/Refereeing) screen (typically used on phones or tablets) now displays notifications when a decision is expected or when the jury summons the referee.
- It is now possible to build affordable physical devices to receive instructions from owlcms (decision expected, jury calls referee) using MQTT. Software and circuit schematics are available at [this location](http://github.com/jflamy/owlcms-esp32).
- Video Streaming: Support for Open Broadcaster Software (OBS) automated scene switching.  Added [documentation](https://owlcms.github.io/owlcms4/#/OBSSceneSwitching) for all supported transitions.
- It is now possible to choose and override the Excel templates for competitions cards, the start list, the starting weight sheet, the results (protocol), and the final package (attempts, sinclair, robi, team results, etc.)
- It is now possible to Export and Import the database content (#449).  This allows taking a snapshot of the database in the middle of a competition. It also allows bringing back a Heroku database for local use, and conversely, setting up a competition locally prior to loading on Heroku.
- Explicit support for participation to multiple age groups (#433)
- All resources in the local directory take precedence over the built-in ones (visual styles, templates, age group definitions, sounds, etc.)
- Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://owlcms.github.io/owlcms4/#/Announcing#rules-for-moving-down). (#418)
- Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://owlcms.github.io/owlcms4/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://owlcms.github.io/owlcms4/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://owlcms.github.io/owlcms4/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://owlcms.github.io/owlcms4/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. Je
