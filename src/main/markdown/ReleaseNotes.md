* Release Highlights for release ${project.version} ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Fix: When time is started/stopped on one technical officials screen, change the timer button colors/highlights on all relevant screens (announcer, timekeeper, marshall) (#311)
* Since last stable release 4.4.5 
  - [x] Fix: When using Firefox for the scoreboards, blinking makes  the right and bottom borders of the blinking cells disappear. (#311)
  - [x] Fix: Different break timers could give different readings (#282).  Break timers now are consistent amongst themselves, and can be refreshed at will is a networking glitch arises without becoming inconsistent.
  - [x] Enhancement: ROBI score now computed for all athletes with a total. Under 17 are assumed to be under the IWF Youth age group, and youth world records are used.  Under 20 are assumed to be IWF juniors, computed using Junior records, and over 20 are IWF seniors. (#307, #308)
  - [x] Fix: Start Numbers are now assigned using age group, bodyweight and lot -- removed the division as first criterion (#305).
  - [x] Fix: a (rare but repeatable) sequence of actions at the end of a group could lead to a loop (#306)
  - [x] Prerelease: Update tool for the cloud versions of the applications (owlcms-heroku and public-results-heroku) deployed using the `Deploy to Heroku` button (versions 4.5 or later).  Using this tool, the cloud applications can be updated on-demand without having to reinstall. See https://github.com/jflamy/owlcms4-heroku-updater (#303) 
  - [x] Workaround: (#304) The  very first display order sometimes does not completely make it to a screen when "Countdown to Introduction" or "Start Lifting" is used. A simple "refresh" is sufficient to restore communications for the rest of the meet. It is recommended to do a dry-run "countdown" to make sure that all the screens respond.  

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
