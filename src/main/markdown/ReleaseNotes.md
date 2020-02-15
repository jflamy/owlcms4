* Release Highlights for release ${project.version} ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Enhancement: ROBI score now computed for all athletes with a total. Under 17 are assumed to be under the IWF Youth age group, and youth world records are used.  Under 20 are assumed to be IWF juniors, computed using Junior records, and over 20 are IWF seniors. (#307, #308)
* Since last stable release 4.4.5 
  - [x] Fix: Start Numbers are now assigned using age group, bodyweight and lot -- removed the division as first criterion (#305).
  - [x] Potential Fix: Break timers occasionally failed to start, but worked fine after reloading the window. (#304). Root cause could not be established, but the problem has not been observed after updating the underlying library and making minor changes to the code.
  - [x] Fix: a (rare but repeatable) sequence of actions at the end of a group could lead to a loop (#306)
  - [x] Prerelease: Update tool for the cloud versions of the applications (owlcms-heroku and public-results-heroku) deployed using the `Deploy to Heroku` button (versions 4.5 or later).  Using this tool, the cloud applications can be updated on-demand without having to reinstall. See https://github.com/jflamy/owlcms4-heroku-updater (#303) 

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
