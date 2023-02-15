37.5.0  Scoreboard fixes, Template improvements

- 37.5 Enhancements
  - The blank weigh-in form is now available from the pre-competition documents page in addition to the weigh-in page. On the weigh-in page, the form has been moved to its own button. ([#630](https://github.com/jflamy/owlcms4/issues/630))
  - The empty protocol sheets are now in a separate section in the templates, and on their own button on the weigh-in page.
  - Better font size behavior when resizing the current athlete display used for streaming
- 37.5 Fixes
  - Leaders were not shown on the very first lift.
  - The warmup scoreboard title now indicates Introduction of Athletes when the group warming up is being introduced. Other ceremony titles are not shown as they do not concern the athletes of the group warming up.
  - Blinking of current athlete restored except during presentation ceremonies.
  - MQTT port override using environment variable was not being observed
  

##### Highlights from recent stable releases

- The announcer and marshal screens show the 6 attempts and total for each athlete. ([#525](https://github.com/jflamy/owlcms4/issues/525))
- Migration to the most recent version of the Vaadin user interface framework. The navigation (menu, top menu bar) has been redone to use officially supported components.
- There is now a separate page for pre-competition documents. There are now separate documents for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://owlcms.github.io/owlcms4/#/2400PreCompetitionDocuments).
- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- Capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- Improvements to Records eligibility. See [Records Eligibility](https://owlcms.github.io/owlcms4/#/Records) documentation. 
- New Weigh-in template to create an empty Weigh-in Summary (used to facilitate data entry)
- New Sinclair coefficients for the 2024 Olympiad.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_37.5.0.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_37.5.0.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://owlcms.github.io/owlcms4/#Heroku) or (recommended) **[Fly.io](https://owlcms.github.io/owlcms4/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://owlcms.github.io/owlcms4/#/DigitalOcean)
