**For the last stable version, please use the latest 4.1 version (4.1.20).**  
This release candidate ("rc") version is a public test of the next major upgrade, in order to test a broader range of configurations. The official release should be the same as this release, feature-wise.

- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
- Release Highlights: 
  - This version is a technical update to use the long-term support (LTS) version of the [Vaadin](https://vaadin.com/) software that is used to build the user interface.  This LTS update provides five years of technical stability for the underlying platform and add-ons.
  - All fixes and features from version 4.1.20 have been merged
  - This version has been tested to work on Linux (Ubuntu 18.03)
  - The only new features in 4.2.0 are minor changes to the user interfaces such as the option for white-on-black scoreboards and tweaks to the navigation screens.
  - No new features will be added to 4.1, future improvements will be in the 4.2 series.
- Known Issues:
  
  - (Annoyance) If the server is restarted, the various browsers should reload automatically.  Currently this is not working due to a [bug](https://github.com/vaadin/flow/issues/6635) in the underlying Vaadin framework which is has been acknowledged.  The workaround is to reload the pages with F5 or Ctrl-R.
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
    
    > **If the installer does not start right away and your computer seems to be working very hard, or if you get a blue window with "Windows protected your PC", see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)**
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
