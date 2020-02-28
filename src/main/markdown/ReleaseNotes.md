* Release Highlights for release ${project.version} ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Fix: release 4.5.1 for customizable css files did not work correctly on Heroku.
  - [x] Enhancement: Ability to run a competition in "simulation mode" (referees make decisions at random, but repeatable from run to run).  Used to test the screen layouts, and to test the final package reports. (#317)
* Since last stable release 4.5.0 
  - [x] Fix: Changing the gender of an age group did not correctly reflect on the categories inside. (#318) An age group created via the user interface with the default gender F could not be fixed to be M.
  - [x] Enhancement: In order to facilitate video streaming overlays (e.g. with OBS Studio), the URL http://my.address:8080/displays/currentathlete gives a special screen with only the current athlete shown on the scoreboard.  Edit the file local/styles/currentathlete.css to customize the look and feel. (#312)
* Workarounds/Known Issues
  - [ ] Workaround: (#304) When the browser screens have already been started and owlcms is restarted, occasionally the *very first* "push" update from the server is not handled by one of the browsers.  A refresh of the page is sufficient to restore things.

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
