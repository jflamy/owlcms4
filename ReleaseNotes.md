* Changes for release 4.6.2-rc3  ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
   * [x] Attempt board now shows last name in upper case, same as other boards.
* Key Highlights from recent stable releases
   * [x] Enhancement: When using refereeing devices, announcer sees decisions as they come in. Useful when there is no jury to remind referee or to detect device/network faults. (#328)  Can be turned off on the Competition Information page.
   * [x] Enhancement: Team Results page accessible from the "Result Documents" navigation page.  Shows the team points scored by each team (using the IWF scoring 28-25-24...) (#336)  The points are counted only for the groups that are done, not the groups in progress.  Also includes the Sinclair scores (#337)
   * [x] Enhancement: Team Sinclair Scoreboard for top 5 teams.  Updated on every lift. (#337)
   * [x] Enhancement: Simple team competition scoreboard.  Top 5 men and top 5 women teams are displayed. Shows how many athletes have been tallied and full team size. (#327)
   * [x] Enhancement: The final package page contains all the scores (SMM, Sinclair, Robi) same as the group results page.
   * [x] Enhancement: In order to facilitate video streaming overlays (e.g. with OBS Studio), the URL http://my.address:8080/displays/currentathlete gives a special screen with only the current athlete shown on the scoreboard.  Edit the file local/styles/currentathlete.css to customize the look and feel. (#312)
* Workarounds/Known Issues
  
  - [ ] Workaround: (#304) When the browser screens have already been started and owlcms is restarted, occasionally the *very first* "push" update from the server is not handled by one of the browsers.  A refresh of the page is however sufficient to restore things.

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, no download is necessary. Follow the [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md) to deploy your own copy.
