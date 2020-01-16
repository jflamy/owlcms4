

Version 4.3 is the new official owlcms4 and all development work will now take place on this branch.   This new version introduces an improved way to manage age groups and categories. *[Click here to read the documentation](https://jflamy.github.io/owlcms4/#/Categories)* .

* Release Highlights for release 4.3.2 ([Full Log](https://github.com/jflamy/owlcms4/issues?utf8=%E2%9C%93&q=is%3Aclosed+is%3Aissue+project%3Ajflamy%2Fowlcms4%2F1+))
  - [x] Fix: New-style long category names with age groups like `U17 M >96` did not fit on the athlete card. Also allowed long team/club names.
  - [x] Fix: collected the various bug fixes for 4.3.1 related to long categories fitting correctly on the scoreboards/leaderboards.
  - [x] Fix: Leaderboard did not appear in v4.3.1.2 due to an unforeseen consequence of optimizing the way categories are compared with one another (categories refer to age groups which refer back to categories) (#274)

- Installation Instructions :
  
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
