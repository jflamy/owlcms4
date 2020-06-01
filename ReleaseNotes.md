If you already have version 4.7.4.1, this release may be skipped as the programs are identical.

* Changes for release 4.7.5  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
   * [x] No behaviour changes relative to 4.7.4.1.  The application building and packaging has been moved to Azure DevOps. The application can now be developed, built, distributed and run completely on the cloud.  The .exe files installers Windows are no longer signed, as signing the files does not eliminate the obnoxious warnings.
* Key Highlights from recent stable releases
   * [x] Translations fixed.  Spanish version available.
   * [x] Improved support for deployment under Kubernetes (#346) -- see the [preliminary notes](https://jflamy-dev.github.io/owlcms4-prerelease/#/Heroku.md).
   * [ ] Enhancement: Simplification of configuration options for loading initial data (#347) -- see the [Configuration Parameters](https://jflamy-dev.github.io/owlcms4-prerelease/#/Configuration.md) documentation. *No changes are required, old parameters still work*.
   * [x] Fix: Decision lights for the very last lift in a group would disappear too quickly on the attempt board and scoreboards (#344)
   * [x] Fix: announcer using the dropdown to go to next group now shows the correct buttons to start the next group's countdown (#345)
   * [x] Fix: interactive creation of athlete on registration page was broken (#342)
   * [x] Enhancement: Timekeeper screen now with large buttons, can be used from iPad/iPhone etc.  Also supports "," as keyboard shortcut to start clock and "." to stop clock for use with programmable keypads. (#340) 
   * [x] Fix: Changes to registration data for athletes in currently lifting groups now correctly handled (#341)
   * [x] Enhancement: When using refereeing devices, announcer sees decisions as they come in. Useful when there is no jury to remind referee or to detect device/network faults. (#328)  Can be turned off on the Competition Information page.
   * [x] Enhancement: Team Results page accessible from the "Result Documents" navigation page. 
     * [x] Shows the team points scored by each team (using the IWF scoring 28-25-24 as given at end of group) (#336)   
     * [x] Also includes the Sinclair total for the team, reflecting lifts done (#337)
   * [x] Enhancement: Team Sinclair Scoreboard for top 5 teams.  Updated on every lift. (#337)
   * [x] Enhancement: Simple team competition scoreboard.  Top 5 men and top 5 women teams are displayed. Shows how many athletes have been tallied and full team size. (#327)
   * [x] Enhancement: The final package page contains all the scores (SMM, Sinclair, Robi) same as the group results page.
   * [x] Enhancement: In order to facilitate video streaming overlays (e.g. with OBS Studio), the URL http://my.address:8080/displays/currentathlete gives a special screen with only the current athlete shown on the scoreboard.  Edit the file local/styles/currentathlete.css to customize the look and feel. (#312)
* Workarounds/Known Issues
  
  - [ ] Workaround: (#304) When the browser screens have already been started and owlcms is restarted,  occasionnaly a refresh of the page may be required.

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, no download is necessary. Follow the [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md) to deploy your own copy.
