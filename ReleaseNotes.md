### **Changes for release 4.33.0-alpha06**

**Changes in 4.33.0**

- 4.33.0-alpha06: added announcer notifications for record attempts and for successful new records
- 4.33.0-alpha06: fixes for reported issues with records: declined (0) attempts issue, update of CJ records.
- 4.33-0-alpha05: Lack of local/records folder or files no longer causes errors.  The folder is now created by default, with a README.
- 4.33-0-alpha05: When a record is broken, the display is updated on subsequent attempts. Broken records are listed in the log file. The Excel file must be changed manually (since it is "official")
- 4.33.0-alpha04: Added highlighting of records being challenged; record section color scheme now in colors.css.
- 4.33.0-alpha03: Show records on scoreboards
  - Records are read from Excel spreadsheets located in the local/records directory. All files in the folder are loaded when the program starts; all the tabs in each spreadsheet are read, you can put all the record entries on the same sheet if you prefer. See the following folder for examples: [Sample Record Files](https://www.dropbox.com/sh/sbr804kqfwkgs6g/AAAEcT2sih9MmnrpYzkh6Erma?dl=0) . Note that these files are samples, and most likely out of date - send a copy if you update them.   You will need to copy the files, and maybe translate the record names (second column).
  - All records for which the current athlete is between the low and high ages (inclusive) and between the body weight boundaries will be shown.  If records from several federations/associations are loaded, all are shown, whether the athlete is eligible or not, because it is interesting to compare.
  - It is possible to hide the records if there are two many athletes in a group (using the screen dialog or by setting `records=false` in the URL parameters)
- 4.33.0-alpha02: Made SMF behave the same as Sinclair in preparation for SMF-based team championships (allow comparison when no clean-and-jerk has been made yet).  Fixed generated test data for team membership.
- 4.33.0-alpha01: Team Sinclair scoreboard now updates on every successful lift.
- 4.33.0-alpha00: Announcer can act as solo athlete-facing referee. A setting on the announcer screen (⚙) enables emitting down signal on decision so it is heard and shown on displays.
- 4.33.0-alpha00: Round-robin "fixed order" option for team competitions.  If this option is selected in the Competition Non-Standard Rules, athletes lift according to their lot number on each round. The lot number can be preset at registration or drawn at random depending on competition rules.
- 4.33.0-alpha00: 24h time will now be used in the date-time picker when using English outside of the "traditional" English-speaking countries ("AU", "GB", "IN", "NZ", "PH", "US", "ZA").  On a laptop, the country is obtained from the operating system.  If using English in the cloud, we recommend setting the `OWLCMS_LOCALE` environment variable to `en_SE` in order to get English with consistent ISO date and 24h time formatting throughout the program.
- 4.33.0-alpha00: Armenian translation

### Highlights from recent stable releases

- On weigh-in or registration forms, if a change in category results, a confirmation is required (#499)
- Requested weight is now shown on attempt board (including loading chart) and on top of scoreboard during breaks (but hidden during ceremonies).
- Announcer is notified when weight on bar needs to change.
- Officials scheduling and registration templates:
  - Registration import-export spreadsheet changed to add additional columns for Marshal2 and TechnicalController2. Added a report page in the export that shows the assignments for each official.
  - The Start List document has an Officials tab that shows official assignments for each group according to the introduction order.
- New parameterized scoreboards.  Colors can be changed in the `styles/colors.css` file (*The default colors are the same as the previous defaults).*  See [Customization](https://owlcms.github.io/owlcms4-prerelease/#/UploadingLocalSettings) for how to proceed. The scoreboards can be zoomed in or out using the  `Ctrl+` and `Ctrl-` keys to accommodate more lines, or to make text bigger with smaller groups.
- Improved management of ceremonies : see [Breaks and Ceremonies](https://owlcms.github.io/owlcms4-prerelease/#/Breaks) procedures, and [Result Documents](https://owlcms.github.io/owlcms4-prerelease/#/Documents) for the medals spreadsheet.
- [Jury](https://owlcms.github.io/owlcms4-prerelease/#/Jury) console now supports summoning the referees either individually or all together. The jury console now allows direct reversal/confirmation of lifts (#435, #427)  
- The [Refereeing](https://owlcms.github.io/owlcms4-prerelease/#/Refereeing) screen (typically used on phones or tablets) now displays notifications when a decision is expected or when the jury summons the referee.
- It is now possible to build affordable physical devices to receive instructions from owlcms (decision expected, jury calls referee) using MQTT. Software and circuit schematics are available at [this location](http://github.com/jflamy/owlcms-esp32).
- Video Streaming: Support for Open Broadcaster Software (OBS) automated scene switching.  Added [documentation](https://owlcms.github.io/owlcms4-prerelease/#/OBSSceneSwitching) for all supported transitions.
- It is now possible to choose and override the Excel templates for competitions cards, the start list, the starting weight sheet, the results (protocol), and the final package (attempts, sinclair, robi, team results, etc.)
- It is now possible to Export and Import the database content (#449).  This allows taking a snapshot of the database in the middle of a competition. It also allows bringing back a Heroku database for local use, and conversely, setting up a competition locally prior to loading on Heroku.
- Explicit support for participation to multiple age groups (#433)
- All resources in the local directory take precedence over the built-in ones (visual styles, templates, age group definitions, sounds, etc.)
- Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://owlcms.github.io/owlcms4-prerelease/#/Announcing#rules-for-moving-down). (#418)
- Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://owlcms.github.io/owlcms4-prerelease/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://owlcms.github.io/owlcms4-prerelease/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
