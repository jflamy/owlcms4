

Version 4.3 is the new official owlcms4 and all development work builds on 4.3.   This new branch introduces an improved way to manage age groups and categories. *[Click here to read the documentation](https://jflamy.github.io/owlcms4/#/Categories)* .

* Release Highlights for release 4.3.1: 
  - [x] Enhancement: Scoreboard with *leaderboard*. A new scoreboard is available that also shows the top 3 athletes for the current category at the bottom.  This is useful for large competitions that have multiple groups for the same category. (#241)
  - [x] Enhancement: better handling of very long names for athletes and for teams.  A narrow column is used for teams if all the teams in a group have short names (such is country codes or team codes). If the names of teams are long, the system will switch to using a wider column, and abbreviate ("...") automatically names that exceed the available width. If you have extremely long names, you may try using the zoom function of the browser to go down to 90%, this often allows the browser to fit the name. (#266)
  - [X] Enhancement: It is now possible for the translator to select the style in which the heavyweight category is shown for a given locale (>109 vs 109+ or some other convention) (#263)


* For those who skipped release 4.3.0: 
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
