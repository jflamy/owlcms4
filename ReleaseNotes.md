37.2.0-rc01:  Release Candidate.

- 37. 2 Enhancements
  
  - If you wish to build or use MQTT-enabled devices such as those in the [Blue-Owl](https://github.com/owlcms/blue-owl) project, a MQTT server is now embedded in owlcms. There is no longer a need to run a separate one and usual cases no longer need any owlcms configuration.
    - See the [embedded MQTT Server configuration documentation](https://owlcms.github.io/owlcms4-prerelease/#/MQTT). 
  
    - If you used MQTT before this release:
      - To use the embedded server, remove the `-DmqttServer` flag (or your `OWLCMS_MQTTSERVER` environment variable).  If present, owlcms will use these values to locate an *external* MQTT server.  On  Windows, inspect the `owlcms.l4j.ini` file in the installation directory and remove the definition.  You should also disable the local Mosquitto or aedes server if you wish to use the embedded one.
  
    - For cloud users wishing to use MQTT Devices, see the [documentation](https://owlcms.github.io/owlcms4-prerelease/#/MQTT) for how to reference a free external MQTT Server (the embedded server cannot be reached on Heroku or Fly.io)
  
- 37.2 Fixes
  
  - 37.2.0-rc01: Setting a password using OWLCMS_PIN or -Dpin now working again
  - 37.2.0-rc01: Connecting to the MQTT Server without a username is now possible (previously when the owlcms field was empty a username still had to be provided, but could be anything)
  - 37.2.0-beta00: the MQTT connection loop is now a separate thread to avoid blocking the user interface.
  - 37.2.0-alpha01: the MQTT server was not enabled on a new database
  - 37.2.0-alpha01: the presence/absence of the MQTT server parameter war.


##### Highlights from recent stable releases

- The announcer and marshal screens show the 6 attempts and total for each athlete. ([#525](https://github.com/jflamy/owlcms4/issues/525))
- Migration to the most recent version of the Vaadin user interface framework. The navigation (menu, top menu bar) has been redone to use officially supported components.
- There is now a separate page for pre-competition documents. There are now separate documents for each purpose instead of multiple tabs. See [Pre-Competition Documents Documentation](https://owlcms.github.io/owlcms4-prerelease/#/2400PreCompetitionDocuments).
- Customization of team points. Templates for the final results package now have an extra tab that contains the points awarded for each rank. Copy and rename the template if you need to change the point system for a given competition.
- Capability to add flags and athlete pictures on the attempt board (#508).  See [Flags and Pictures](https://owlcms.github.io/owlcms4-prerelease/#/FlagsPicture) documentation.
- Improvements to Records eligibility. See [Records Eligibility](https://owlcms.github.io/owlcms4-prerelease/#/Records) documentation. 
- New Weigh-in template to create an empty Weigh-in Summary (used to facilitate data entry)
- New Sinclair coefficients for the 2024 Olympiad.  An option on the Competition rules page allows using the previous (2020 Olympiad) values if your local rules require them.  Masters SMF and SMHF use the 2020 Olympiad values until further notice.


### **Installation Instructions**

  - For **Windows**, download `owlcms_setup_37.2.0-rc01.exe` from the Assets section below and follow [Windows Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup)

    > If you get a window with `Windows protected your PC`, or if your browser gives you warnings, please see this [page](https://owlcms.github.io/owlcms4-prerelease/#/DefenderOff)

  - For **Linux** and **Mac OS**, download the `owlcms_37.2.0-rc01.zip` file from the Assets section below and follow [Linux or Mac Stand-alone Installation](https://owlcms.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)

  - For **Cloud PaaS** installs, no download is necessary. Follow the [Heroku](https://owlcms.github.io/owlcms4-prerelease/#Heroku) or (recommended) **[Fly.io](https://owlcms.github.io/owlcms4-prerelease/#Fly)** installation instructions.

  - For self-hosted **Docker**, see [Docker](https://owlcms.github.io/owlcms4-prerelease/#/LocalWindowsSetup). For self-hosted **Kubernetes** see [Kubernetes](https://owlcms.github.io/owlcms4-prerelease/#/DigitalOcean)
