* Release Highlights: 
  * Fix: (Masters) the results screen used by the announcer to announce medals was not correctly sorting the results according to age groups (#250).  The same display now shows the age group (#248)
  * Experimental: for kids competitions using different bars for boys and girls, allow a non-standard lifting order (girls-snatch boys-snatch girls-c&j boys-c&j) to limit bar changes. This is currently enabled by setting the environment variable `OWLCMS_GENDERORDER=true` or adding <nobr>`-DgenderOrder=true`</nobr> to the startup flags (on the command line for Linux and Mac or, for Windows, in the `owlcms.l4j.ini` file) (#249).  This flag has no effect if all athletes in the group are of the same gender.
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Open Issues](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
