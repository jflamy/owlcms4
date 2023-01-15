37.1.0-rc01:  Release Candidate.  Usable in a real competition, test with your own data beforehand.

- 37.1 Enhancements
  - The announcer and marshal screens show the 6 attempts and total for each athlete. ([#525](https://github.com/jflamy/owlcms4/issues/525))
  - Updated Spanish, German, Danish, Swedish, and Russian translations.
  - Enhancements for testing
    - Added OWLCMS_USECOMPETITIONDATE to compute ages according to the competition date (useful when loading a database from a previous year).
    - Added OWLCMS_FEATURESWITCHES to load feature switches from the command line.
  
- 37.1 Fixes
  - The screen for defining how many plates are available and for defining non-standard bars could occasionally fail to load in odd circumstances.


##### Highlights from recent stable releases

- Migration to the most recent version of the Vaadin user interface framework. The navigation (menu, top menu bar) has been redone to use officially supported components.
- There is now a separate page for pre-competition documents. There are now separate documents for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://owlcms.github.io/owlcms4-prerelease/#/2400PreCompetitionDocuments).
- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- Capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- Improvements to Records eligibility. See [Records Eligibility](https://owlcms.github.io/owlcms4-prerelease/#/Records) documentation. 
- New Weigh-in template to create an empty Weigh-in Summary (used to facilitate data entry)
- New Sinclair coefficients for the 2024 Olympiad.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_37.1.0-rc02.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_37.1.0-rc02.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://owlcms.github.io/owlcms4-prerelease/#Heroku) or (recommended) **[Fly.io](https://owlcms.github.io/owlcms4-prerelease/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean)
