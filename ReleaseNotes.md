37.0.0-rc03: New version after update to a major release of the user interface libraries

This is a [release candidate](https://en.wikipedia.org/wiki/Software_release_life_cycle#Release_candidate).
All prior features from release 35 appear to work, simulation of large competition on multiple platforms is successful. There are no known issues.
*As for all pre-releases, test with your own data before using the program in a real competition.*

- 37.0 Enhancements
  - 37.0.0-rc03: Hungarian and Romanian translations added (thanks to Attila Feri)
  - Migration to the current version of the Vaadin user interface framework. The navigation (menu, top menu bar) was redone.
  - On the preparation page, items have been rearranged to be more intuitive.
  - There is now a separate page for pre-competition documents. There are now separate documents for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://owlcms.github.io/owlcms4/#/2400PreCompetitionDocuments).
  - The main scoreboard shows the group description at the top if one is available. Warmup scoreboards still have the "lifts done" information.
  - The attempt board and decision board now show a "waiting for next group" instead of a blank screen when the platform is inactive
  - Documentation of [equipment and networking setup](https://owlcms.github.io/owlcms4/#/EquipmentSetup) has been enhanced
  - Documentation of the pre-competition setup and registration activities has been redone.
  - Many small annoyances were fixed.
- 37.0.0-rc Fixes
  - 37.0.0-rc02: The Team Results page was not loading. ([#613](https://github.com/jflamy/owlcms4/issues/613))
  - 37.0.0-rc02: The clock on the scoreboard and attempt board would not count down in certain rare conditions ([#611](https://github.com/jflamy/owlcms4/issues/611)) The announcer and technical official displays were updating correctly.
  - 37.0.0-rc02: It was not always possible to switch from one type of break to another, or to set a duration on a technical break. Using Pause and changing break type/duration should now work. ([#612](https://github.com/jflamy/owlcms4/issues/612))
- 37.0 Fixes
  - The static resources (css, images, etc...) are now loaded in a way that will prevent the browser from using obsolete copies.

##### Highlights from recent stable releases

- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- **New Sinclair coefficients for the 2024 Olympiad**.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.
- Experimental capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- New Records processing, including Eligibility Criteria and Federations (so invited athletes do not show up as breaking local records, etc.). See [Records Eligibility](https://owlcms.github.io/owlcms4/#/Records) documentation. 
- New Weigh-in template to create a Weigh-in Summary. Meant to facilitate data entry. As used in Pan-American federation, the body weight and declarations are copied one per line and countersigned by the coach/athlete. Select the template from the "Starting Weights" button on the weigh-in page.
- [Customization](https://owlcms.github.io/owlcms4/#/UploadingLocalSettings) of colors and styling of scoreboards and attempt board. 
- Improved management of ceremonies : see [Breaks and Ceremonies](https://owlcms.github.io/owlcms4/#/Breaks) procedures, and [Result Documents](https://owlcms.github.io/owlcms4-prerelease/#/Documents) for the medals spreadsheet.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_37.0.0.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_37.0.0.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://owlcms.github.io/owlcms4/#Heroku) or (recommended) **[Fly.io](https://owlcms.github.io/owlcms4/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://owlcms.github.io/owlcms4/#/DigitalOcean)
