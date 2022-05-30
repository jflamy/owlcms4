### **Changes for release ${revision}**

- Changes since 4.32.0
  - 4.32.0-beta03: Setting to keep best N results from team scores (point and sinclair) was not being applied to.
  - 4.32.0-beta02: Weight changes during breaks were not always shown on top of scoreboards
  - 4.32.0-beta01: Requested weight is shown on attempt board and top of scoreboard during breaks (hidden during ceremonies).
  - 4.32.0-alpha01: Announcer is notified when required weight change
  - 4.32.0-alpha00: first release.  The only changes are in templates, release be used for competitions.

### Changes for release 4.32

- Requested weight is shown on attempt board and top of scoreboard during breaks (hidden during ceremonies).
- Announcer is notified when weight on bar needs to change.
- Officials scheduling:
  - Registration import-export spreadsheet changed to add additional columns for Marshal2 and TechnicalController2. Added a report page in the export that shows the assignments for each official.
  - The Start List document has an Officials tab that shows official assignments for each group according to the introduction order.

### Highlights from recent stable releases

- New parameterized scoreboards.  Colors can be changed in the `styles/colors.css` file (*The default colors are the same as the previous defaults).*  See [Customization](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/UploadingLocalSettings) for how to proceed. The scoreboards can be zoomed in or out using the  `Ctrl+` and `Ctrl-` keys to accommodate more lines, or to make text bigger with smaller groups.
- Improved management of ceremonies : see [Breaks and Ceremonies](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Breaks) procedures, and [Result Documents](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Documents) for the medals spreadsheet.
- [Jury](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Jury) console now supports summoning the referees either individually or all together. The jury console now allows direct reversal/confirmation of lifts (#435, #427)  
- The [Refereeing](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Refereeing) screen (typically used on phones or tablets) now displays notifications when a decision is expected or when the jury summons the referee.
- It is now possible to build affordable physical devices to receive instructions from owlcms (decision expected, jury calls referee) using MQTT. Software and circuit schematics are available at [this location](http://github.com/jflamy/owlcms-esp32).
- Video Streaming: Support for Open Broadcaster Software (OBS) automated scene switching.  Added [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/OBSSceneSwitching) for all supported transitions.
- It is now possible to choose and override the Excel templates for competitions cards, the start list, the starting weight sheet, the results (protocol), and the final package (attempts, sinclair, robi, team results, etc.)
- It is now possible to Export and Import the database content (#449).  This allows taking a snapshot of the database in the middle of a competition. It also allows bringing back a Heroku database for local use, and conversely, setting up a competition locally prior to loading on Heroku.
- Explicit support for participation to multiple age groups (#433)
- All resources in the local directory take precedence over the built-in ones (visual styles, templates, age group definitions, sounds, etc.)
- Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Announcing#rules-for-moving-down). (#418)
- Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. Je
