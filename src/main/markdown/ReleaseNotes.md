### **Changes for release ${revision}**  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

4.30.0-alpha08: Snapshot release, for testing/feedback.

- Fix for localized date format on start list and registration export: selected application locale was not correctly used when different from the system locale (e.g. on the cloud).

#### New in release 4.30

- [x] Improved break management. 
  - [x] Ceremonies (introduction of athletes and officials, medals) can take place without stopping the countdown timers.  
  - [x] Capability to present medals to a previous group without affecting countdown (Display switches back to scoreboard)
- [x] Selectable behavior for public vs warmup scoreboards selection is done when display starts or is clicked)
  - [x] Public Scoreboards  switch to display of medal winners from a previous group during the medals ceremony.   They revert to the normal display at the end of the ceremony
  - [x] Warmup room displays can ignore the medal ceremony and keep the scoreboard (

#### Highlights from recent stable releases

- [x] [Jury](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Jury) console now supports summoning the referees either individually or all together. 
  - Calling a referee starts a jury break and a notification is shown on the technical official screens.
  - This feature displays notifications when phones/tablets/laptops are used for refereeing, or with [MQTT refereeing devices]() (see below)
  - Keyboard shortcuts `H` `I` `J` `K` can be used to call referees 1, 2, 3 or all referees, respectively. The`esc` key is used to end the break and resume the competition.
  - The selection between 3 and 5 person jury has been moved to the settings (`⚙`) menu in the top bar.
- [x] The [Refereeing](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Refereeing) screen (typically used on phones or tablets) now displays notifications when a decision is expected or when the jury summons the referee.
  - The delay before the third official is reminded to enter a decision is configurable in the Options section of the Competition Information and Rules screen.
- [x] It is now possible to build affordable physical devices to receive instructions from owlcms (decision expected, jury calls referee).
  - The MQTT protocol is used for communications.  MQTT is widely used for home automation, industrial telemetry, and various "internet of things" applications
  - An MQTT server is used to broker communications between owlcms and the devices. See [MQTT setup instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/MQTT) for details for configuration of a free broker.
  - Software and circuit schematics for an affordable MQTT+WiFi refereeing box are available at [this location](http://github.com/jflamy/owlcms-esp32).

- [x] Enhancement: The down signal sound has been changed and is now customizable. Sounds can be customized by changing the .wav and .mp3 files in /local/sounds (which can also be uploaded to the cloud)
- [x] Video Streaming: Support for Open Broadcaster Software (OBS) automated scene switching.  Added [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/OBSSceneSwitching) for all supported transitions.
- [x] Enhancement: The group selection drop-down now displays the group description alongside the short group name.
- [x] Enhancement:  Timer warning and down sound are now off by default on the technical officials consoles. New `⚙` settings menu on the top bar allows turning them on or off.
- [x] Jury Console: if another official ends the break, the Jury Deliberation dialog is closed. Also, the initial notification about a good/bad lift is closed when a jury decision is given.
- [x] Fix: Margins were wrong on the original style marshal cards when using US Letter paper.

- [x] **v4.27 <u>Local Database Format Change</u>**  The H2 database is used on local installs.  The H2 team no longer supports the previous database format, so some people may need a to perform a simple conversion. 
  - If you always start from scratch (new Excel, or interactive entry), you have nothing to do (a new database will be created on first start of the new version.
  - If you are running in the cloud, you have nothing to do, the cloud-based database engine is Postgres and not H2.
  - But <u>if you run locally and wish to keep and reuse your 4.26 or earlier database content</u> a conversion is required between the previous format and the new format.  Fortunately, the process is quick and straightforward.  Please follow the [conversion instructions](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/ImportExport)
  - If you wish to keep copies of previous meets and have kept database backups, the suggestion is to install version 4.26 and export each of the databases.
- [x] Enhancement: The type of break and the countdown now shown on the announcer's red break management button
- [x] Enhancement: There is now a description field for groups, shown on the start list and in the group listing page.  This can be used to list the categories present in a group.
- [x] New: A new status monitoring window enables automatic video scene switching in OBS.  See [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/OBSSceneSwitching) and [tutorial video](https://user-images.githubusercontent.com/678663/147373848-89b91086-b16d-48c0-8f48-445f6c1ca828.mp4)
  - Added button on the display selection screen to start OBS Monitor window 
- [x] Enhancement: large notification shown on attempt board for Jury confirmation/reversal. 
- [x] Added Medals as a break type. 
  - Attempt and scoreboards display "Medal Ceremony" message. 
  - New BREAK.MEDALS state to allow for OBS scene switching if desired.
- [x] Enhancement: keyboard shortcuts to start (`,` )and stop (`.`) the clock are available on the announcer and timekeeper screens.
- [x] Athlete-facing displays (decision and attempt-board) have sound on by default.

- [x] Security updates: updated libraries as new versions were made available.
- [x] Change: Default for birth dates changed to be full date instead of birth year, to match IWF TCRR.
- [x] COVID: Updated documentation for Zoom broadcasts to cover sharing the scoreboard in high resolution and allowing participants to switch between athlete and scoreboard views.
- [x] Enhancement: Capability to simulate a competition and perform load testing (see [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Simulation)). Large (180 athletes) competitions have been tested 18 displays, with one and two platforms, with and without publishing to a publicresults site.

- [x] It is now possible to choose and override the Excel templates for competitions cards, the start list, the starting weight sheet, the results (protocol), and the final package (attempts, sinclair, robi, team results, etc.)
  - New available IWF-style layout for athlete cards that is meant to be printed and folded to give bigger areas for writing (both North American Letter and international A4 formats available)

- [x] It is now possible to export, manipulate and reload the registration data (athletes, groups, referees) in Excel format.  

  - Exporting the previously loaded data, rearranging the groups, adding/deleting athletes, changing expected category and entry totals is now possible. Until final verification of entries, the Excel sheet can be used as authoritative list of participating athletes: reloading it erases and recreates the athletes and groups.
  - Only registration data is exported.  This does not export the lifts and requested changes.  The file should not be loaded after the competition has started as it recreates the athletes from scratch.
  - Note that the format has changed to reflect the fact that category allocation is now automatic - only the gender and expected weight of the athlete are used. You need to download a new empty template, or export existing data. 
  - The new format makes it easier to cut and paste athletes (no hidden columns). Both the empty sheet and the exported sheet are now translated in the current language.
  
- [x] The jury console now allows direct reversal/confirmation of lifts (#435, #427)  
  - The jury chief can confirm and reverse lifts directly and can ask the announcer to call the technical controller.  
  - Jury actions are shown to the other technical officials consoles to keep them informed.
  - Shortcuts are defined to support a jury keypad. See [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Refereeing#jury-console-keypad) for details
  
- [x] It is now possible to Export and Import the database content (#449).  This allows taking a snapshot of the database in the middle of a competition. It also allows bringing back a Heroku database for local use, and conversely, setting up a competition locally prior to loading on Heroku.

- [x] Explicit support for participation to multiple age groups (#433)

  - An athlete will, by default, be eligible and ranked separately in all the active categories in which the age and qualifying total are met.   
  - Participation to an eligible category can be removed or added back on the registration page for the athlete.
  - "Multiple rank" scoreboard shows ranks for all age groups present in the currently lifting group.
  - Final results packages can be produced for each age group, or combined for all age groups (when all age groups contribute to team points)
  - Qualifying totals can be entered in the AgeGroup Excel file for each age group category, and also from the Age Group web page.
  - Robi scores are now computed based on age group. If an athlete is eligible for JR and SR, the JR Robi will appear in the JR final package, and the SR Robi in the Senior final package.
  - Teams are now separate by age group or age division
    - Teams can be defined by Age Group (e.g. JR, SR, U15, U17) or by Age Division together (e.g. Masters, or all age Uxx age groups combined)
    - Team-oriented scoreboards allow selecting which age group is shown (e.g. JR Team is different from the SR team)

- [x] Results improvements and fixes:
  - From the Category Results page, ability to produce a result sheet for the selected category, age group, or age division.

- [x] All resources in the local directory take precedence over the built-in ones (visual styles, templates, age group definitions, sounds, etc.)
  - You can also add a file with an extension for your locale (e.g. ex: _hy, _ru, _fr) and it will be used for things like marshal cards, starting list, etc.
  - You can zip the local directory on your laptop and upload the to a cloud-based setup (see the System configuration page from the Preparation section) (#366)

- [x] Moved the Language and Time Zone settings together with the technical settings to a renamed "Language and System Settings" page reachable from the "Preparation" section.
  - For troubleshooting, entering the "Language and System Settings" page prints the networking interfaces and addresses in the log.

- [x] Enhancement: the program now forces start numbers when lifting starts if they have not been attributed at weigh-in (useful for small competitions not using cards with novice users)

- [x] Marshall screen now shows decisions (#411). This is for setups where athlete cards are used and where it is difficult to see the scoreboard or inconvenient to switch tabs.

- [x] Clearer error message when athlete A cannot move down because B has attempted same weight on a bigger attempt number (if so, A should have lifted before B, cannot move down.)

- [x] When a display is first started, a dialog offers to enable warning sounds or not.  Warnings are silenced by default; they should normally be enabled on only one display per room, to avoid warnings coming from several directions. See the [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Displays#display-settings) for details (#407)

- [x] Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Announcing#rules-for-moving-down). (#418)

- [x] Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.

- [x] iPads now supported as refereeing device with Bluetooth buttons (running either the athlete-facing time+decision display or the attempt board display.)   iPads require that sound be enabled by touching a screen button once when the board is started. (#408). 

##### **Installation Instructions :**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
