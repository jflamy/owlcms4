37.6.0  Separate styles for video feed displays

- 37.6 Enhancements
  - It is now possible to have scoreboards and displays with alternate color and font schemes for video streaming, and slightly different content (for example, the scoreboard titles)  The colors and styling can be changed using the instructions in [Style Customization](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Styles) (by default they are the same as for the site scoreboards).
- 37.6 Fixes
  - Restored the publicresults remote scoreboard capability.
  

##### Highlights from recent stable releases

- The announcer and marshal screens show the 6 attempts and total for each athlete. ([#525](https://github.com/jflamy/owlcms4/issues/525))
- Migration to the most recent version of the Vaadin user interface framework. The navigation (menu, top menu bar) has been redone to use officially supported components.
- There is now a separate page for pre-competition documents. There are now separate documents for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/2400PreCompetitionDocuments).
- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- Capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- Improvements to Records eligibility. See [Records Eligibility](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Records) documentation. 
- New Weigh-in template to create an empty Weigh-in Summary (used to facilitate data entry)
- New Sinclair coefficients for the 2024 Olympiad.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_${revision}.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_${revision}.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Heroku) or (recommended) **[Fly.io](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/DigitalOcean)
