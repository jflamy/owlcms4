Specific changes for release 4.23.0-beta02  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))

- [x] Armenian and Danish translations
- [x] Team membership selection page: sorting enabled and URL parameters are updated so refresh works.
- [x] Top L20eams allow selection of the age group and their URL so refresh goes back to the same display. In a multi-age group meet, teams are different according to age group
- [x] Updated the scoreboards for Top Sinclair, Top Teams, and Top Sinclair Teams.  The Team-oriented displays allow the selection of the team or age division displayed (since JR team is not the same as senior team in a multi-category meet)

Changes for release 4.23

- [x] Explicit support for participation to multiple age groups (#433)

  - An athlete will, by default, be eligible and ranked separately in all the active categories in which the age and qualifying total are met.   
  - Participation to an eligible category can be removed or added back on the registration page for the athlete.
  - Multiple age group scoreboard option shows ranks for all age groups present in the currently lifting group.
  - Final results packages can be produced for each age group, or combined for all age groups (when all age groups contribute to team points)
  - Qualifying totals can be entered in the AgeGroup Excel file for each age group category, and also from the Age Group web page.
  - Robi scores are now computed based on age group. If an athlete is eligible for JR and SR, the JR Robi will appear in the JR final package, and the SR Robi in the Senior final package.
- [x] Teams are now separate by age group or age division

  - Teams can be defined by Age Group (e.g. JR, SR, U15, U17) or by Age Division together (e.g. Masters, or all age Uxx age groups combined)
  - Team-oriented scoreboards allow selecting which age group is shown (e.g. JR Team is different from the SR team)
- [x] Moved the Language and Time Zone settings to the Configuration section, since they concern the system and not the weightlifting rules.  Renamed pages accordingly.
- [x] Enhancement: the program now forces start numbers when lifting starts if they have not been attributed at weigh-in (useful for small competitions not using cards with novice users)
- [x] All resources in the local directory take precedence over the built-in ones (visual styles, templates, age group definitions, sounds, etc.)
  - You can also add a file with an extension for your locale (e.g. ex: _hy, _ru, _fr) and it will be used for things like marshal cards, starting list, etc.
  - You can zip the local directory on your laptop and upload the to a cloud-based setup (see the System configuration page from the Preparation section) (#366)
- [x] Fix: allow locale extension (ex: _hy, _ru, _fr) to override the Excel templates for Weigh-in, Starting Sheet, Marshal cards, etc.  This had been lost when the sheets were changed to include the translations from the translation sheet.
- [x] Marshall screen now shows decisions (#411). This is for setups where athlete cards are used and where it is difficult to see the scoreboard or inconvenient to switch tabs.
- [x] Clearer error message when athlete A cannot move down because B has attempted same weight on a bigger attempt number (if so, A should have lifted before B, cannot move down.)

Key Highlights from recent stable releases

- [x] Top of scoreboards was not updating to first athlete on break end (#431)
- [x] Final results package had erroneous rankings for youth and juniors (#432)
- [x] Timing stats spreadsheet correctly produced again, was empty (#430)When a display is first started, a dialog offers to enable warning sounds or not.  Warnings are silenced by default; they should normally be enabled on only one display per room, to avoid warnings coming from several directions. See the [documentation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Displays#display-settings) for details (#407)
- [x] Fix for registration file upload: empty group start and weigh-in times were causing a generic error message when uploading an .xlsx file instead of being ignored.
- [x] Implemented the <u>rules to prevent athletes from moving down their requested weight illegally</u>.  Moving down is denied if the athlete should already have attempted that weight according to the official lifting order.  The exact checks resulting from applying the TCRR to that situation are spelled out in the [documentation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Announcing#rules-for-moving-down). (#418)
- [x] Violations of <u>rules for timing of declarations</u> (before initial 30 seconds), and for changes (before final warning) are now signaled as errors (#425, #426). Overriding is possible for officiating mistakes.
- [x] CSS style sheets for attempt board and decision board are now editable in local/styles (#424)
- [x] For cloud-based competitions, setting the time zone can now be done directly from the Competition Information page. (#422).
- [x] Marshall can no longer edit or overwrite lift results by mistake. An explicit checkbox is required to enable edit (#286)
- [x] Solo mode where a single technical official uses the good lift/bad lift buttons on the announcer screen now correctly supports decision reversal within 3 seconds, and correctly ignores multiple clicks. (#281)
- [x] iPads now supported as refereeing device with Bluetooth buttons (running either the athlete-facing time+decision display or the attempt board display.)   iPads require that sound be enabled by touching a screen button once when the board is started. (#408). 
- [x] Workaround applied for iPad unpredictable response time (from 0.1 to 3 sec. lag) when used as display. The iPad will "skip ahead" to the correct remaining time as soon as the start command is received, and "skip back" on a stop. Only applies to iPads, ignored by all other platforms. (#419) 
- [x] Support for large competitions on Heroku. Added documentation for [economical use of Heroku professional tiers](https://jflamy-dev.github.io/owlcms4-prerelease/#/HerokuLarge). Heroku now provides the memory defaults for all configurations.
  If you are limited to using the free setup and need to stretch it to its maximum, set the `_JAVA_OPTIONS` configuration variable to something like `-Xmx384m -XX:MaxMetaspaceSize=80m`
- [x] New: added a new item for video broadcasts in the technical configuration section. Video capture using OBS or similar streaming software is awkward when a PIN or password is set.  If it is known that the video operator is working from a safe setting (such as a home network) , a "backdoor" setting (OWLCMS_BACKDOOR if using an environment variable) can be used to allow password-less login from a comma-separated list of addresses.  Use with care.

* [x] Improvement: New scoreboard with multiple IWF age group rankings (Youth, Junior, Senior).  Final package also includes the three rankings. (#372)


Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalWindowsSetup)
    
    > If you get a blue window with `Windows protected your PC`, or if Microsoft Edge gives you warnings, please see this page : [Make Windows Defender Allow Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/DefenderOff)
    
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Heroku** cloud, no download is necessary. Follow the [Heroku Cloud Installation](https://jflamy-dev.github.io/owlcms4-prerelease/#/Cloud) to deploy your own copy.  See also the [additional configuration steps for large competitions on Heroku](https://jflamy-dev.github.io/owlcms4-prerelease/#/HerokuLarge).

  - For **Kubernetes** deployments, see `k3s_setup.yaml` file for [cloud hosting using k3s](https://jflamy-dev.github.io/owlcms4-prerelease/#/DigitalOcean) or `k3d_setup.yaml` for [home hosting](https://jflamy-dev.github.io/owlcms4-prerelease/#/k3d).  For other setups, download the `kustomize` files from `k8s.zip` file adapt them for your specific cluster and host names. 
