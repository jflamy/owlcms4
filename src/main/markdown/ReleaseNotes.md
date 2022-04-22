### **Changes for release ${revision}**

4.31.0-alpha02: Fix: lift name / total now translated on the Medal report spreadsheet.

4.31.0-alpha02: Fix: in a round-robin competition, it is possible to have weight changes lower than the weight on last clock start.

4.31.0-alpha02: Fix: when importing a database with a local definition override zip, the override was not  applied (a restart was required).

4.31.0-alpha02: Fix: changing to today's date in the competition editing page resulted in an empty date.

4.31.0-alpha01: Fix: unpleasant interactions between Hibernate, Vaadin and Postgres with respect to Blob storage could prevent configuration updates from being stored

4.31.0-alpha01: Fix: the password "salt" was sometimes recomputed when it should not have, preventing password from being deciphered.

4.31.0-alpha00: Experimental scoreboard styling

- New experimental scoreboard: URL `displays/results` (no button yet).  Browser-zoomable with Ctrl+ Ctrl- Ctrl0. Size of team column is configurable in new stylesheet `styles/results.css` (target is to make resizable interactively)
- New `styles/colors.css` file is shared by the new results scoreboard and attempt/decision boards.  Changing color can be done once for new scoreboard and attempt/decision board.  Note: old scoreboards still use old stylesheets.

### **Changes for 4.31.* releases**

- Improvement: Break triggered by marshal indicates "Marshal Issue" to other TOs.
- Fix: Password issues. In some circumstances, password access to the application would stop working, requiring the use of configuration variables to override.
- Fix: When running a round-robin competition, the weight requested between rounds can be lower than that at the last clock start.

### Highlights from recent stable releases

- Improved management of ceremonies 

  - Documented new and improved [Breaks and Ceremonies](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Breaks) procedures, including the Medal Ceremony process.  Ceremonies take place without interfering with countdown timers.
  - New Medals spreadsheet for the announcer see [Result Documents](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Documents).
  - Selectable behavior for public vs warmup scoreboards.  Warmup scoreboards do not switch during the medal ceremonies.
  - Additional protocol templates with predefined paper sizes. Also included a version with snatch, clean&jerk and total ranks.
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

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean). For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
