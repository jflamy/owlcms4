* Release Highlights: 
  * Enhancement: added checkbox to enable Masters W75 and W80+ age groups for national federations that have gender-equality rules (#242)
  * Enhancement: make it easier to run a competition with athletes of all ages by running it as an "extended Masters" competition.  When selecting the "Masters" setting on the competition information page,
    * Additional masters-style age groups are automatically assigned for:  kids (12 and under), youth (13-17 inclusive), junior (18-20 inclusive), and senior (21-35 inclusive).  As in Masters competitions, older age groups get smaller start numbers.
    * Some athletes over 35 may want to compete as Senior instead of Masters.  This can be done on a case-by-case basis by setting their age division to SENIOR which will override the age group calculation. (#246)
    * Some athletes over 30 may want to compete as Masters, if the Masters national federation allow M30 and W30 age groups. This is achieved by explicitly setting the MASTERS age division on the concerned athletes, which overrules the default assignment as senior. (#246)
  * Enhancement: in a Masters competition, allow M30 and 30 groups by explicitly setting the MASTERS age division for the athletes (see above, this is the same rule as when running a mixed-age-group competition as Masters meet.) (#246)
  * Enhancement: the ordering of the fields on the Registration and Weigh-in pages is more consitent.  The only difference is that on the Registration page the identification fields come first, whereas on the weigh-in page the weight and declaration fields come first. (#247)
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Open Issues](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)
