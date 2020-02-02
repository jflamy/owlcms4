* Release Highlights for release 4.4.2-rc6 ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Distribution change: separate release channels for releases and pre-releases. See [this table](https://github.com/jflamy/owlcms4/releases/tag/4.4.2-rc2) for details.
  - [x] Enhancement: Remote public results scoreboard.  The competition site sends updates to a separate scoreboard application that provides the scoreboard/leaderboard to the public. (#139, #292)
  - [x] Fix: Allowed time not updated correctly after decision when using the phone refereeing interface (#291)
  - [x] Fix: after several group changes, there could be a spinning wait indicator on group selection menu , and a need to click twice to start the intro countdown or go to lifting (#294)
  - [x] Enhancement: Portuguese translations
  - [X] Fix: Lift times were not reliably recorded, which could lead to misleading timing statistics (#288)
  - [X] Enhancement: when the platform is inactive (no group waiting for introduction) show the competition name on scoreboards instead of empty (#295)
  - [X] Enhancement: make it clear that the remote scoreboard only shows the Allowed time and does not count down (#296)

Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
