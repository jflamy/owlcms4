
* Release Highlights for release 4.3 (Release Candidate 1): 
  
  - [x] Updates to the various screens now sent using parallel threads for improved response time on announcer screen. (#259)
  - [x] Automatic assignment of category based on age, bodyweight, and active age groups.  Each age group has its own applicable bodyweight categories.  Age groups can be activated/deactivated.  When an athlete is eligible to several age groups, a sensible default is applied, and the choice can be overridden at registration/weigh-in. See https://jflamy.github.io/owlcms4/#/Categories for details. (#252, #256)
  - [X] The 20kg or the Masters 15/10 rules is now applied based on the category of the athlete.  You can therefore mix and match Masters and non-Masters athletes in the same group.
  - [X] Timing stats to measure the rate of lifting (athletes per hour) were wrong (#257)
  - [x] All windows open in a new tab, this is more convenient for preparing a meet (#258)
  - [x] Scoreboards did not remember their field of play in their URL, causing confusion if there were multiple sessions going on concurrently and page was refreshed (#260)
  - [x] Fixed counter-intuitive context menu on scoreboards -- black-on-white and white-on-black cues now shown according to current selection and with matching colors (#261)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
