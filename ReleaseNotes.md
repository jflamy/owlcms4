4.30.2: 

Fixes/improvements on rules processing

- For Masters athletes, the 80% rule was being checked on the weigh-in form, but the 20kg rule was being used on the athlete card.
- For 20kg rule and 80% rule, if the athlete lowers the first snatch such that raising the CJ is required, the athlete can wait before changing the first CJ and the marshal can ignore the error message.  The system will show a message whenever that athlete card is opened until the first CJ is fixed.
- Medal eligibility categories are no longer systematically recomputed at weigh-in. This facilitates the processing of (for example) masters that opt out of senior medals in spite of having made the total.

Technical Fixes

- Fixed override of templates and styles using a zip file for laptop configurations. Cloud override was working but an incompatible change made by the H2 database had broken that option.
- Fix: the server was needlessly sending the instruction to show down signal back to the display with the keypads.  Emitting the sound a second time on top of the first could cause a delay on some computers.

### **Changes for release 4.30.2**  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

Improved management of ceremonies 

- Documented new and improved [Breaks and Ceremonies](https://owlcms.github.io/owlcms4/#/Breaks) procedures, including the Medal Ceremony process.  Ceremonies take place without interfering with countdown timers.
- Documented the [Result Documents](https://owlcms.github.io/owlcms4/#/Documents), including the new Medals spreadsheet for the announcer.

- Selectable behavior for public vs warmup scoreboards selection is done when display starts or is clicked. Public Scoreboards switch to display of medal winners during the medal ceremony.   They revert to the normal display at the end of the ceremony. Warmup room displays ignore the medal ceremony and keep the scoreboard

- Additional protocol templates with predefined paper sizes. Also included a version with snatch, clean&jerk and total ranks.

Usability/understandability changes

- Moved database import/export to main preparation page
- Moved the reload of translation file to bottom of the "Languages and Settings" configuration page



#### Highlights from recent stable releases

- [x] [Jury](https://owlcms.github.io/owlcms4/#/Jury) console now supports summoning the referees either individually or all together. 
- [x] The [Refereeing](https://owlcms.github.io/owlcms4/#/Refereeing) screen (typically used on phones or tablets) now displays notifications when a decision is expected or when the jury summons the referee.
- [x] It is now possible to build affordable physical devices to receive instructions from owlcms (decision expected, jury calls referee) using MQTT. Software and circuit schematics are available at [this location](http://github.com/jflamy/owlcms-esp32).

- [x] Video Streaming: Support for Open Broadcaster Software (OBS) automated scene switching.  Added [documentation](https://owlcms.github.io/owlcms4/#/OBSSceneSwitching) for all supported transitions.
- [x] It is now possible to choose and override the Excel templates for competitions cards, the start list, the starting weight sheet, the results (protocol), and the final package (attempts, sinclair, robi, team results, etc.)

- [x] The jury console now allows direct reversal/confirmation of lifts (#435, #427)  

- [x] It is now possible to Export and Import the database content (#449).  This allows taking a snapshot of the database in the middle of a competition. It also allows bringing back a Heroku database for local use, and conversely, setting up a competition locally prior to loading on Heroku.

- [x] Explicit support for participation to multiple age groups (#433)

- [x] All resources in the local directory take precedence over the built-in ones (visual styles, templates, age group definitions, sounds, etc.)

- [x] Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://owlcms.github.io/owlcms4/#/Announcing#rules-for-moving-down). (#418)

- [x] Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.


##### **Installation Instructions :**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://owlcms.github.io/owlcms4/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://owlcms.github.io/owlcms4/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://owlcms.github.io/owlcms4/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://owlcms.github.io/owlcms4/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://owlcms.github.io/owlcms4/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
