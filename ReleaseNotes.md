* Release Highlights for release 4.5.1 ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Fix: Changing the gender of an age group did not correctly reflect on the categories inside. (#318) An age group created via the user interface with the default gender F could not be fixed to be M.
  - [x] Enhancement: In order to facilitate video streaming overlays (e.g. with OBS Studio), the URL .../displays/currentathlete gives a special screen with only the current athlete shown on the scoreboard.  Edit the file local/styles/currentathlete.css to customize the look and feel. (#312)
* Since last stable release 4.5.0 
  - [x] Workaround: (#304) The  very first display order sometimes does not completely make it to a screen when "Countdown to Introduction" or "Start Lifting" is used. A simple "refresh" is sufficient to restore communications for the rest of the meet. It is recommended to do a dry-run "countdown" to make sure that all the screens respond.  

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
