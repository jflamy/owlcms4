### **Changes for release ${revision}**  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

4.30.0-rc04: Final release candidate.

- Fixed a tiny timing problem that could cause the automated tests to fail
- New protocol sheet templates with snatch, cj and total ranks.

#### New in release 4.30

- [x] Improved management of ceremonies 
  - Documented new and improved [Breaks and Ceremonies](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Breaks) procedures, including the Medal Ceremony process.  Ceremonies take place without interfering with countdown timers.
  - Documented the [Result Documents](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Documents), including the new Medals spreadsheet for the announcer.

- [x] Selectable behavior for public vs warmup scoreboards selection is done when display starts or is clicked)
  - [x] Public Scoreboards switch to display of medal winners during the medal ceremony.   They revert to the normal display at the end of the ceremony
  - [x] Warmup room displays can ignore the medal ceremony and keep the scoreboard
- [x] Usability/understandability changes
  - Moved database import/export to main preparation page
  - Moved the reload of translation file to bottom of the "Languages and Settings" configuration page
- [x] Additional protocol templates with predefined paper sizes. Also included a version with snatch, clean&jerk and total ranks.

#### Highlights from recent stable releases

- [x] [Jury](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Jury) console now supports summoning the referees either individually or all together. 
- [x] The [Refereeing](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Refereeing) screen (typically used on phones or tablets) now displays notifications when a decision is expected or when the jury summons the referee.
- [x] It is now possible to build affordable physical devices to receive instructions from owlcms (decision expected, jury calls referee) using MQTT. Software and circuit schematics are available at [this location](http://github.com/jflamy/owlcms-esp32).

- [x] Video Streaming: Support for Open Broadcaster Software (OBS) automated scene switching.  Added [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/OBSSceneSwitching) for all supported transitions.
- [x] It is now possible to choose and override the Excel templates for competitions cards, the start list, the starting weight sheet, the results (protocol), and the final package (attempts, sinclair, robi, team results, etc.)

- [x] The jury console now allows direct reversal/confirmation of lifts (#435, #427)  

- [x] It is now possible to Export and Import the database content (#449).  This allows taking a snapshot of the database in the middle of a competition. It also allows bringing back a Heroku database for local use, and conversely, setting up a competition locally prior to loading on Heroku.

- [x] Explicit support for participation to multiple age groups (#433)

- [x] All resources in the local directory take precedence over the built-in ones (visual styles, templates, age group definitions, sounds, etc.)

- [x] Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Announcing#rules-for-moving-down). (#418)

- [x] Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.


##### **Installation Instructions :**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
