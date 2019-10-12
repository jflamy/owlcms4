> Version 4.2 is a technical update to use the long-term support (LTS) version of the [Vaadin](https://vaadin.com/) software that is used to build the user interface.  This LTS update provides five years of technical stability for the underlying platform and add-ons. 
>From now on all bug fixes and improvements will be in the 4.2 series.

- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
* Release Highlights: 
  * Enhancement: For events that assign more than 20 athletes to a group, it is now possible to use the browser zoom (Control + and Control -) to fit more lifters on the screen. Control 0 resets the browser zoom. (#192)
  * Enhancement: Clicking on the scoreboard, lifting order, top Sinclair screens allows toggling between black background and white background (#185)
  * Fix: Federation membership number was missing on the athlete information forms (registration and weigh-in) (#189)
  * Fix: changing an athlete's requested lift when the platform was inactive would wake up some of the displays (#194)
  * Fix delay on showing the down arrow on the other screens when using server-side sound (#193)
  * Enhancement: Allow capture of bodyweight and starting declarations on the registration spreadsheet to facilitate dry-runs and testing (#190)
  * All fixes and features from version 4.1.20 have been merged
* Known Issues:
  * Annoyance: If the server is restarted, the various browsers should reload automatically.  On some configurations with low memory (Heroku Cloud) the automatic restart is not working due to an acknowledged [bug](https://github.com/vaadin/flow/issues/6635) in the user interface library.  The workaround is to reload the pages with F5 or Ctrl-R.
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
    
    > **If the installer does not start right away and your computer seems to be working very hard, or if you get a blue window with "Windows protected your PC", see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)**
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
