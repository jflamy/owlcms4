* Release Highlights for release 4.5.2-rc6 ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Enhancement: when entering the competition groups for the start sheet, the start time is automatically calculated to be 2h after weigh-in; it can be edited if needed. (#297)
  - [x] Enhancement: Athlete cards optionally show a score. By default, the score is the total, but can be overridden in competitions where bonus points or penalties are used (some U13 competitions award points for technique, or 6/6), or use some traditional formula (#319).  This is enabled on the competition page.  The variable ${l.customScoreComputed} can then be used in the Excel templates.
  - [x] Enhancement: Clearer definition of the invited status (not eligible for individual medals) and the team member status (an athlete eligible for medals, and considered for team points.)  In team competitions that allow for substitutes or ordinary lifters, it can happen that non-team members are allowed to lift (eligible), but not score points (not team member). (#316)
  - [x] Fix:  customizable css files introduced in 4.5.1 prevented correct startup on Heroku, now corrected.
  - [x] Testing: Ability to run a competition in "simulation mode" (referees make decisions at random, but repeatable from run to run).  Used to test the screen layouts, and to populate the database to test the final package reports. To use, start in demo mode using `-DdemoMode=true`, and setup your browsers.  Stop, then start demo mode again using the Java command line to use `app.owlcms.Simulation` as the main class (#317)
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
