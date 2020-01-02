
* Release Highlights for release 4.3.0: 
  
  - [x] Major update: Automatic assignment of category based on age, bodyweight, and active age groups.  Each age group has its own applicable bodyweight categories.  Age groups can be activated/deactivated.  When an athlete is eligible to several age groups, a sensible default is applied, and the choice can be overridden at registration/weigh-in. See https://jflamy.github.io/owlcms4/#/Categories for details. (#252, #256)
  - [X] Enhancement: The 20kg or the Masters 15/10 rules is now applied based on the category of the athlete.  You can therefore mix and match Masters and non-Masters athletes in the same group.
  - [X] Fix: Updates to the various screens now sent using parallel threads for improved response time on announcer screen. (#259)
  - [X] Fix: Timing stats to measure the rate of lifting (athletes per hour) were wrong (#257)
  - [x] Enhancement: All preparation windows now open in a new tab, this is more convenient for preparing a meet and more consistent with the behavior of the other screens (#258)
  - [x] Fix: Processing of URL parameters was inconsistent when there were multiple sessions going on concurrently and pages were refreshed (e.g after a network outage or system restart) (#260)
  - [x] Fix: corrected counter-intuitive context menu on scoreboards -- black-on-white and white-on-black cues now shown according to current selection and with matching colors (#261)
  - [x] Enhancement: Thank you note added for translators on the "About" page (#262)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
