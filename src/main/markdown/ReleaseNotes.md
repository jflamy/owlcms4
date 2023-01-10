37.0.4: New version after update to a major release of the user interface libraries

- 37.0 Enhancements
  - Migration to the current version of the Vaadin user interface framework. The navigation (menu, top menu bar) has been redone to use officially supported components.
  - On the preparation page, items have been rearranged to be more intuitive.
  - There is now a separate page for pre-competition documents. There are now separate documents for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/2400PreCompetitionDocuments).
  - 37.0.3: Added the possibility to configure a MQTT server directly using the web interface (used for refereeing devices and jury devices with LEDs). Restructured the Settings menu for readability.
  - The main scoreboard shows the group description at the top if one is available. Warmup scoreboards still have the "lifts done" information.
  - The attempt board and decision board now show a "waiting for next group" instead of a blank screen when the platform is inactive
  - Hungarian and Romanian translations added (thanks to Attila Feri)
  - Documentation of [equipment and networking setup](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/EquipmentSetup) has been enhanced
  - Documentation of the pre-competition setup and registration activities has been redone.
  - Many small annoyances were fixed.
- 37.0 Fixes
  - 37.0.4: JSON database export would fail if an age group had no code.  Code is now mandatory. [(#620)](https://github.com/jflamy/owlcms4/issues/620)
  - 37.0.4: Added missing fields for MQTT connections on Settings page. 
  - 37.0.3: Duplicate entries in the starting list were present if an athlete was eligible for multiple categories. ([#619](https://github.com/jflamy/owlcms4/issues/619))
  - 37.0.2: fixes to documentation files.
  - The static resources (css, images, etc...) are now loaded in a way that will prevent the browser from using obsolete copies.


##### Highlights from recent stable releases

- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- Capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- Improvements to Records processing. See [Records Eligibility](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records) documentation. 
- New Weigh-in template to create an empty Weigh-in Summary (used to facilitate data entry)
- **New Sinclair coefficients for the 2024 Olympiad**.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_${revision}.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_${revision}.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Heroku) or (recommended) **[Fly.io](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean)
