### **Changes for release 4.30.6-rc01**

4.30.6-rc01: Restored normal logging levels, routine build before official release.

4.30.6-beta02: Fix: beta01 broke clock management when changes by another athlete were made after clock start.

4.30.6-beta01: Fix: If an athlete lifted and was set to lift again (with 2:00) *but* immediately moved up *while the decision was still shown*, then the next athlete would get a 2:00.

4.30.5 Performance improvements and minor fix

- User interface responsiveness improvements, in particular for marshal weight changes 
- Fix: Scoreboards did not switch to "group results" title at the end of the very first group.
- Spanish (Spain) translation

### **Changes for 4.30.* releases**

Fixes/improvements on rules processing

- Fix: On a two-minute clock, a late declaration is again being signaled as an error.  Also enabled time checks when the clock is forced to 1:00 or 2:00 after a marshal or loading error.
- Fix: For Masters categories, the 80% rule was not being applied systematically on weight changes
- Improvement: If a change is made to the first snatch and the first CJ needs to change,  the system will show a message whenever athlete card is opened until the first CJ is fixed.
- Fix: Default for birth dates reset to full date instead of year only.

Improved management of ceremonies 

- Documented new and improved [Breaks and Ceremonies](https://owlcms.github.io/owlcms4-prerelease/#/Breaks) procedures, including the Medal Ceremony process.  Ceremonies take place without interfering with countdown timers.
- Documented the [Result Documents](https://owlcms.github.io/owlcms4-prerelease/#/Documents), including the new Medals spreadsheet for the announcer.

- Selectable behavior for public vs warmup scoreboards selection is done when display starts or is clicked. Public Scoreboards switch to display of medal winners during the medal ceremony.   They revert to the normal display at the end of the ceremony. Warmup room displays ignore the medal ceremony and keep the scoreboard

- Additional protocol templates with predefined paper sizes. Also included a version with snatch, clean&jerk and total ranks.

Usability/understandability changes

- Document downloads : disabled the download button if no template is selected, updated the default values for protocol/start list/athlete cards, added missing paper formats.
- Moved database import/export to main preparation page
- Moved the reload of translation file to bottom of the "Languages and Settings" configuration page
- Medal eligibility categories are no longer systematically recomputed at weigh-in. This facilitates the processing of (for example) masters that opt out of senior medals in spite of having made the total.

Technical Fixes

- Fix: it is again possible to upload a zip file on a laptop configuration to override the templates/styles.
- Fix: the server was needlessly sending instructions to emit the down sound back to the computer with keypads. Emitting the sound a second time on top of the first could cause a delay on some computers.
- Fix: use of certain characters in group names was creating illegal file names and preventing downloads.

### Highlights from recent stable releases

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
