##### **Changes for release 4.27.0-alpha05**  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

4.27.0-alpha05: Experiment: show type of break on the red break management button.

4.27.0-alpha05: fix displays/monitor on initial startup (String.contentEquals() is not null-safe contrary to other similar functions)

4.27.0-alpha04: show notification on attempt board for Jury confirmation/reversal. 

4.27.0-alpha04: added Medals as a break type. 

- Attempt and scoreboards display "Medal Ceremony" message. 
- New BREAK.MEDALS state to allow for OBS scene switching if desired.

4.27.0-alpha03: Ability to monitor field of play state to control OBS scene switching.  See [Example video](https://user-images.githubusercontent.com/678663/147373848-89b91086-b16d-48c0-8f48-445f6c1ca828.mp4)

4.27.0-alpha02: moving all release repositories to github.com/owlcms

4.27.0-alpha00: initial release with new version of H2 and keyboard shortcuts for timekeeping.

###### New in release 4.27

- [x] **<u>Local Database Format Change</u>**  The H2 database is used on local installs.  The H2 team will no longer support their previous database format, so a conversion process is necessary. 
  - If you <u>do **not** need to keep the previous version</u> of your database because you always start use a new registration sheet or start from scratch, <u>you have nothing to do</u> (a new database will be created on first start of the new version.
  - If you are running in the cloud, there is nothing to do, the cloud-based database engine is Postgres.
  - <u>If you wish to reuse your prior database content</u>, then a conversion is required between the previous format and the new format.  Fortunately, the process is quick and straightforward.  Please follow the [conversion instructions](https://owlcms.github.io/owlcms4-prerelease/#/ImportExport)
  - If you wish to keep copies of previous meets and have kept database backups, the suggestion is to install version 4.26 and export each of the databases.
- [x] Enhancement: ability to monitor field of play state to control OBS scene switching.  See [Example video](https://user-images.githubusercontent.com/678663/147373848-89b91086-b16d-48c0-8f48-445f6c1ca828.mp4).
- [x] Enhancement: keyboard shortcuts to start (`,` )and stop (`.`) the clock available on announcer and timekeeper screens.

###### Key Highlights from recent stable releases

- [x] Security updates: updated libraries as new versions were made available.
- [x] Change: Default for birth dates changed to be full date instead of birth year, to match IWF TCRR.
- [x] COVID: Updated documentation for Zoom broadcasts to cover sharing the scoreboard in high resolution and allowing participants to switch between athlete and scoreboard views.
- [x] Enhancement: Capability to simulate a competition and perform load testing (see [documentation](https://owlcms.github.io/owlcms4-prerelease/#/Simulation)). Large (180 athletes) competitions have been tested 18 displays, with one and two platforms, with and without publishing to a publicresults site.

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
  - Shortcuts are defined to support a jury keypad. See [documentation](https://owlcms.github.io/owlcms4-prerelease/#/Refereeing#jury-console-keypad) for details
  
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

- [x] When a display is first started, a dialog offers to enable warning sounds or not.  Warnings are silenced by default; they should normally be enabled on only one display per room, to avoid warnings coming from several directions. See the [documentation](https://owlcms.github.io/owlcms4-prerelease/#/Displays#display-settings) for details (#407)

- [x] Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://owlcms.github.io/owlcms4-prerelease/#/Announcing#rules-for-moving-down). (#418)

- [x] Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.

- [x] iPads now supported as refereeing device with Bluetooth buttons (running either the athlete-facing time+decision display or the attempt board display.)   iPads require that sound be enabled by touching a screen button once when the board is started. (#408). 


**Installation Instructions :**

  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://owlcms.github.io/owlcms4-prerelease/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://owlcms.github.io/owlcms4-prerelease/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://owlcms.github.io/owlcms4-prerelease/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
