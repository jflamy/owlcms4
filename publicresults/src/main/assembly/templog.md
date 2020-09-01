# Changelog

## Release Repositories (31/01/2020)
There are two release channels: one repository contains stable releases, the second one contains prereleases (for testing and early adopters of new features).

Clicking on the link of your choice in the table will get you to the correct location for downloading or installing.

|                                                              | STABLE releases                                              | PRELIMINARY releases                                         |
| :----------------------------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **Competition site installation packages** (owlcms and publicresults).<br />These packages are installed on a laptop and run at the competition site. No internet link is required. | [Downloads](https://github.com/owlcms/owlcms4/releases)<br />(if in doubt, use this link) | [Downloads](https://github.com/jflamy-dev/owlcms4-prerelease/releases) |
| **Cloud-based Competition Management**<br />This installs the competition management software in the cloud. The competition site must have good internet access, but nothing needs to be installed on laptops other than a browser. | ([Cloud installer](https://github.com/owlcms/owlcms4-heroku/blob/master/README.md)) | [Cloud installer](https://github.com/jflamy-dev/owlcms4-heroku-prerelease/blob/master/README.md) |
| **Cloud-based Public Results**<br />This enables the public to see the competition results by replicating the scoreboard in the cloud.  Either the competition is run in the cloud, or there is internet access to enable the competition site software to send updates to the public scoreboard. | [Cloud installer](https://github.com/owlcms/publicresults-heroku/blob/master/README.md) | [Cloud installer](https://github.com/jflamy-dev/publicresults-heroku-prerelease/blob/master/README.md) |
| **Documentation**                                            | [Documentation site](https://owlcms.github.io/owlcms4/#/index) | [Documentation Site](https://jflamy-dev.github.io/owlcms4-prerelease/#/index) |

**For V4.3 and older releases, go to [the legacy release repository](https://github.com/jflamy/owlcms4/releases) and scroll down.**
---

## 4.3.4-rc1 (24/01/2020)
**Bug-fix release for refereeing using phone/tablets**
If you do not use phones or tablets for refereeing, you can keep using version 4.3.3.

* Release Highlights for release 4.3.4 

  - [x] Fix: Time allowed to athlete was not always updated correctly when referees used phone/tablet/screen to enter decisions.  The clock would not move to 2:00 but would remain at previous remaining time.  This did not affect decisions entered using buttons on the decision screen, or by the announcer (#291)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

 
---

## 4.3.3 (19/01/2020)
* Release Highlights for release 4.3.3 
  - [x] Fix: Alignment of athlete cards on US Letter paper was wrong (#278)
  - [x] Fix: Danish translations on attempt board and group substitutions on templates (#285, #287)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

 
---

## 4.3.2.2 (15/01/2020)


Version 4.3 is the new official owlcms4 and all development work will now take place on this branch.   This new version introduces an improved way to manage age groups and categories. *[Click here to read the documentation](https://jflamy.github.io/owlcms4/#/Categories)* .

* Release Highlights for release 4.3.2.1 
  - [x] Fix: Alignment of athlete cards on US Letter paper was wrong (#278)
  - [x] Fix: New-style long category names with age groups like `U17 M >96` did not fit on the athlete card. Also allowed long team/club names.
  - [x] Fix: collected the various bug fixes for 4.3.1 related to long categories fitting correctly on the scoreboards/leaderboards.
  - [x] Fix: Leaderboard did not appear in v4.3.1.2 due to an unforeseen consequence of optimizing the way categories are compared with one another (categories refer to age groups which refer back to categories) (#274)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.3.2.1 (11/01/2020)


Version 4.3 is the new official owlcms4 and all development work will now take place on this branch.   This new version introduces an improved way to manage age groups and categories. *[Click here to read the documentation](https://jflamy.github.io/owlcms4/#/Categories)* .

* Release Highlights for release 4.3.2: 
  - [x] Fix: New-style long category names with age groups like `U17 M >96` did not fit on the athlete card. Also allowed long team/club names.
  - [x] Fix: collected the various bug fixes for 4.3.1 related to long categories fitting correctly on the scoreboards/leaderboards.
  - [x] Fix: Leaderboard did not appear in v4.3.1.2 due to an unforeseen consequence of optimizing the way categories are compared with one another (categories refer to age groups which refer back to categories) (#274)
  - [x] Fix: athletes with no group could cause the weigh-in and registration page to fail. (#275)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.3.1.2 (10/01/2020)


Version 4.3 is the new official owlcms4 and all development work will now take place on this branch.   This new version introduces an improved way to manage age groups and categories. *[Click here to read the documentation](https://jflamy.github.io/owlcms4/#/Categories)* .

* Version 4.3.1.2 fixes small bugs in 4.3.1: wide category names were not displayed correctly (#269), category names were not shown on the starting list (#270), and saving an edited age group had been broken (#271)
* Release Highlights for release 4.3.1: 
  - [x] Enhancement: Scoreboard with *leaderboard*. A new scoreboard is available that also shows the top 3 athletes for the current category at the bottom.  This is primarily useful for larger competitions that have a single category split over several (A, B, ...) groups based on entry total. (#241)
  - [x] Enhancement: better handling of very long names for athletes and for teams.  A narrow column is used for teams if all the teams in a group have short names (such is country codes or team codes). If the names of teams are long, the system will switch to using a wider column, and abbreviate ("...") automatically names that exceed the available width. If you have extremely long names, you may try using the zoom function of the browser to go down to 90%, this often allows the browser to fit the name. (#266)
  - [X] Enhancement: It is now possible for the translator to select the style in which the heavyweight category is shown for a given locale (>109 vs 109+ or some other convention) (#263)


* Summary of the main changes between 4.2 and 4.3.0 
  - [x] Major update: Automatic assignment of category based on age, bodyweight, and active age groups.  Each age group has its own applicable bodyweight categories.  Age groups can be activated/deactivated.  When an athlete is eligible to several age groups, a sensible default is applied, and the choice can be overridden at registration/weigh-in. See https://jflamy.github.io/owlcms4/#/Categories for details. (#252, #256)
  - [X] Enhancement: The 20kg or the Masters 15/10 rules is now applied based on the category of the athlete.  You can therefore mix and match Masters and non-Masters athletes in the same group.

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.3.0 (02/01/2020)
Version 4.3.0 is a *new* release that introduces a *much* cleaner way to manage age groups and categories. *[Click here to read the documentation](https://jflamy.github.io/owlcms4/#/Categories)* 

The previous  4.2.11 release is still available [below](https://github.com/jflamy/owlcms4/releases/tag/4.2.11) but you should certainly consider using this newer release if you have a little time to test beforehand to make yourself comfortable.

**edit:** if you downloaded the file just after its release (before 15h36 EST) there was a glitch the Windows installer, now fixed.

* Release Highlights for release 4.3.0: 
  
  - [x] Major update: Automatic assignment of category based on age, bodyweight, and active age groups.  Each age group has its own applicable bodyweight categories.  Age groups can be activated/deactivated.  When an athlete is eligible to several age groups, a sensible default is applied, and the choice can be overridden at registration/weigh-in. See https://jflamy.github.io/owlcms4/#/Categories for details. (#252, #256)
  - [X] Enhancement: The 20kg or the Masters 15/10 rules is now applied based on the category of the athlete.  You can therefore mix and match Masters and non-Masters athletes in the same group.
  - [X] Fix: Updates to the various screens now sent using parallel threads for improved response time on announcer screen. (#259)
  - [X] Fix: Timing stats to measure the rate of lifting (athletes per hour) were wrong (#257)
  - [x] Enhancement: All preparation windows now open in a new tab, this is more convenient for preparing a meet and more consistent with the behavior of the other screens (#258)
  - [x] Fix: Processing of URL parameters was inconsistent when there were multiple sessions going on concurrently and pages were refreshed (e.g after a network outage or system restart) (#260)
  - [x] Fix: corrected counter-intuitive context menu on scoreboards -- black-on-white and white-on-black cues now shown according to current selection and with matching colors (#261)
  - [x] Enhancement: Thank you note added for translators on the "About" page (#262)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.11 (19/12/2019)
* Release Highlights: 
  * Fix: the final results package could not be produced in v4.2.9 and v4.2.10; an update to the widely-used library used to create Excel files introduced an incompatible change which was undetected on the other types of result documents. (#254)
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Open Issues](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.10 (15/12/2019)
* Release Highlights: 
  * Enhancement: added `de` translations for German, thanks to Martin Moreno (#253)
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Open Issues](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.9 (12/12/2019)
* Release Highlights: 
  * Fix: (Masters) the results screen used by the announcer to announce medals was not correctly sorting the results according to age groups (#250).  The same display now shows the age group (#248)
  * Enhancement: added `fr_FR` translations for French federation usage (age group names, language usage) (#251)
  * Experimental: for kids competitions using different bars for boys and girls, allow a non-standard lifting order (girls-snatch boys-snatch girls-c&j boys-c&j) to limit bar changes. This is currently enabled by setting the environment variable `OWLCMS_GENDERORDER=true` or adding <nobr>`-DgenderOrder=true`</nobr> to the startup flags (on the command line for Linux and Mac or, for Windows, in the `owlcms.l4j.ini` file) (#249).  This flag has no effect if all athletes in the group are of the same gender.
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Open Issues](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.8 (29/11/2019)
* Release Highlights: 
  * Enhancement: added checkbox to enable Masters W75 and W80+ age groups for national federations that have gender-equality rules (#242)
  * Enhancement: make it easier to run a competition with athletes of all ages by running it as an "extended Masters" competition.  When selecting the "Masters" setting on the competition information page,
    * Additional masters-style age groups are automatically assigned for:  kids (12 and under), youth (13-17 inclusive), junior (18-20 inclusive), and senior (21-34 inclusive).  As in Masters competitions, older age groups get smaller start numbers.
    * Some athletes 35 and over may want to compete as Senior instead of Masters.  This can be done on a case-by-case basis by setting their age division to SENIOR which will override the age group calculation. (#246)
    * Some athletes over 30 may want to compete as Masters, if the Masters national federation allow M30 and W30 age groups. This is achieved by explicitly setting the MASTERS age division on the concerned athletes, which overrules the default assignment as senior. (#246)
  * Enhancement: in a Masters competition, allow M30 and 30 groups by explicitly setting the MASTERS age division for the athletes (see above, this is the same rule as when running a mixed-age-group competition as Masters meet.) (#246)
  * Enhancement: the ordering of the fields on the Registration and Weigh-in pages is more consitent.  The only difference is that on the Registration page the identification fields come first, whereas on the weigh-in page the weight and declaration fields come first. (#247)
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Open Issues](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.7 (23/11/2019)
* Release Highlights: 
  * Fix: violations of the 20kg rule are now shown immediately on field entry and not only on submit  (#217).
  * Enhancement: show the start number on the technical official and jury screens (#243)  Also show a warning sign and mouse-over if no start numbers have been assigned.  Also show the start number on the athlete card edited by the Marshall or announcer (#245)
  * Fix: Down signal no longer emitted on browsers if server-side sound has been selected (#244)
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Open Issues](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.6 (14/11/2019)
* Release Highlights: 
  * Fix: in Masters mode, could not use "Edit Athlete Entries" in version 4.2.5 (#226)
  * Fix: The starting list should not fail when some athletes are not assigned to a group (#240)
  * Enhancement: new "during presentation" break type; system switches automatically to that mode when the "before presentation" timer expires, miscellaneous cleanups of break timer management (#238, #226, #233) 
  * Enhancement: changed the translation process to allow several persons to work at once using a cloud-based copy. Updated the [translations instructions](https://jflamy.github.io/owlcms4/#/Translation) accordingly.
  * Minor: fixed translations on English-language athlete cards (#237)
  * Enhancement: improved Swedish translation.
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Open Issues](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.5 (09/11/2019)
* Release Highlights: 
  * Fix: on weigh-in, entering a body weight would needlessly clear the selected category if there was one (#221)
  * Fix: The results screen for the officials was not sorting the Robi, Sinclair, SMM results correctly (#223)
  * Fix: The results screen for the officials was not reliably sorting by category then by total rank as expected (#222) 
  * Fix: Formatting of Jury/Referee Examination sheet to fit on one page  (#227)
  * Enhancement: initial translation for Swedish language.
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc) and [Work in progress](https://github.com/jflamy/owlcms4/projects/1)

- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` from the Assets section below and follow [Windows installation instructions](https://jflamy.github.io/owlcms4/#/LocalWindowsSetup.md) 
    
    > If you get a blue window with `Windows protected your PC`, or if your laptop works very hard performing an anti-virus scan that takes very long, see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file from the Assets section below and follow [Local Linux and Mac OS instructions](https://jflamy.github.io/owlcms4/#/LocalLinuxMacSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file from the Assets section below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.4 (27/10/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) 
* Release Highlights: minor usability and cosmetic fixes
  * Fix: add missing Jury / Referee Examination sheet generation button on weigh-in page #213
  * Fix: starting weight / athlete cards were not tracking the selected group correctly #212
  * Enhancement: filter to show only the medals on the results screen used by the announcer #214
  * Enhancement: sign the Windows executable with an Authenticode certificate to quiet down Windows Defender #215
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
    
    > If the installer does not start right away and your computer seems to be working very hard, or if you get a blue window with "Windows protected your PC", see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.3 (24/10/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) 
* Release Highlights: minor usability and cosmetic fixes
  * Cosmetic fix to scoreboard information during breaks #199
  * Fix: Announcer grid should be refreshed after reload button is used #211
  * Enhancement: On registration and weigh-in, prevent errors by showing categories matching body weight and gender #210
  * Fix: Pencil and trashcan should be removed from the display grids since editing and deletion is done in pop-up #206
  * Fix: Sinclair and Robi coefficients should be rounded to 3 digits on the user interface #209
  * Fix: Athlete cards should show an empty bodyweight and not 0kg after an import #203
  * Fix: Importing a registration sheet should not change the "use birth year" and "enforce 20kg rule" settings #203
  * Enhancement: translations for Russian and Danish updated #208
  * Enhancement: updated user interface framework to Vaadin Flow 14.0.10
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
    
    > If the installer does not start right away and your computer seems to be working very hard, or if you get a blue window with "Windows protected your PC", see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.2 (23/10/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) 
* Release Highlights: 
  * Fix for decisions not showing up in borderline cases (mostly related to time not being restarted. #200, #201
  * Fix for missing notifications to announcer and jury if athlete declared the same weight as the automatic progression in order to keep the right to two changes. #202
  * Cosmetic fix to scoreboard information during breaks #199
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
    
    > If the installer does not start right away and your computer seems to be working very hard, or if you get a blue window with "Windows protected your PC", see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.1 (14/10/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) 
* Release Highlights: 
  * Fix for user interface library memory issues preventing automatic page reload on server restart (#188)
  * Improved usability of Category editing page: new categories shown in proper location, checkbox to make category active or not (#196)
  * Fix for Lifting Order page not waking up automatically at end of introduction timer (#198)
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
    
    > If the installer does not start right away and your computer seems to be working very hard, or if you get a blue window with "Windows protected your PC", see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.2.0 (12/10/2019)
> Version 4.2 is a technical update to use the long-term support (LTS) version of the [Vaadin](https://vaadin.com/) software that is used to build the user interface.  This LTS update provides five years of technical stability for the underlying platform and add-ons. 
>From now on all bug fixes and improvements will be in the 4.2 series.

- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
* Release Highlights: 
  * Enhancement: For events that assign more than 20 athletes to a group, it is now possible to use the browser zoom (Control + and Control -) to fit more lifters on the screen. Control 0 resets the browser zoom. (#192)
  * Enhancement: Clicking on the scoreboard, lifting order, top Sinclair screens allows toggling between black background and white background (#185)
  * Fix: Federation membership number was missing on the athlete information forms (registration and weigh-in) (#189)
  * Fix: changing an athlete's requested lift when the platform was inactive would wake up some of the displays (#194)
  * Fix delay on showing the down arrow on the other screens when using server-side sound (#193)
  * Enhancement: Allow capture of bodyweight and starting declarations on the registration spreadsheet to facilitate dry-runs and testing (#190)
  * All fixes and features from version 4.1.20 have been merged
* Known Issues:
  * Annoyance: If the server is restarted, the various browsers should reload automatically.  On some configurations with low memory (Heroku Cloud) the automatic restart is not working due to an acknowledged [bug](https://github.com/vaadin/flow/issues/6635) in the user interface library.  The workaround is to reload the pages with F5 or Ctrl-R.
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
    
    > **If the installer does not start right away and your computer seems to be working very hard, or if you get a blue window with "Windows protected your PC", see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)**
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

---

## 4.1.20 (04/10/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
* Release Highlights:  
  * Fix for decision buttons being ignored on the decision screen (but working on attempt board) -- regression introduced in 4.1.19
  * Fix lifting order not recomputed when changing current athlete but clock not having started
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
    > **If the installer does not start right away and your computer seems to be working very hard, or if you get a blue window with "Windows protected your PC", see this page : [Make Windows Defender Allow Installation](https://jflamy.github.io/owlcms4/#/DefenderOff)**
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.19 (30/09/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
* Release Highlights: 
  * Referee buttons are now disabled between lifts and during breaks to prevent spurious decisions and problems with keypads that auto-repeat, referees that keep the button pressed, etc.
  * Jury members get a notification about who just lifted that remains visible during deliberation
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.2.0-beta3 (30/09/2019)
**Note: this is a pre-release version.  For the current stable version, look for the latest 4.1 version**

- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
- Release Highlights: 
  - This version is a technical update to use the long-term support version of the [Vaadin](https://vaadin.com/) software that is used to build the user interface.
  - All updates and features from version 4.1.19 have been merged
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.18 (28/09/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
* Release Highlights: 
  * Fix for referee reversal wrongly registering as a no-lift. Multiple reversals would cause no-lift for next athlete(s).
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.17 (19/09/2019)
**Note: If you use refereeing devices, please use [4.1.18](https://github.com/jflamy/owlcms4/releases/tag/4.1.18) which fixes a problem with decision reversal**

- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
* Release Highlights: 
  * Fix for scoreboard switching from break timer to lift info if marshall updates during a break 
  * Fix for notifications on marshall changes sometimes delayed until next user action (rare, random)
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.16 (17/09/2019)
**Note: If you use refereeing devices, please use [4.1.18](https://github.com/jflamy/owlcms4/releases/tag/4.1.18) which fixes a problem with decision reversal**

- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
* Release Highlights: 
  * It is now possible to print a single athlete card during weigh-in, allowing the athlete to sign a clean official card immediately in the weigh-in room if a printer is available.
  * Fixed spurious sound during pause, and standardized behaviour of displays at the end of a group and when switching groups during a break.
  * Marshall now informed of changes to current athlete made by announcer
  * Updated French, Russian and Danish translations
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed+sort%3Aupdated-desc)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.15 (12/09/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
- Release Highlights: Minor bug fix release
  - Fix for attempt board switching from pause display to weight display if marshall made a change to current athlete during a break
  - Missing translation of a few words on technical official screens
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.14 (10/09/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and the [live demo](https://jflamy.github.io/owlcms4/#/?id=demo) site
- Release Highlights:
  - The final results package is back
  - If the sounds are generated on the server for a given platform, then the browsers on that platform will not emit sound (only the server will).
  - Jury-initiated breaks now correctly indicate "Jury Deliberation"
  - Improved error-handling when parsing translation files
- [Change Log](https://github.com/jflamy/owlcms4/issues?q=is%3Aissue+is%3Aclosed)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local installation instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud installation instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.13 (07/09/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and for directions to [live demo](https://jflamy.github.io/owlcms4/#/?id=demo)
- Release Highlights:
  - Year of birth now the default for registration and printouts.  Full Birth Date can be selected on the competition information page if desired.
  - All sounds now emitted correctly when using the master laptop sound adapter for a platform.  Indication on platform configuration screen as to whether sound is generated on master laptop or via browser.
  - On registration and weigh-in screen, the list of categories is filtered based on the athlete's gender
- [Change Log](https://github.com/jflamy/owlcms4/milestone/51?closed=1) for this release (enhancements and fixes)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local Instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local Instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud Instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.12 (05/09/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#/index) for an overview and for directions to [live demo](https://jflamy.github.io/owlcms4/#/?id=demo)
- Release Highlights:
  - Russian translation
  - Top five male/female athletes by Sinclair Ranking (new scoreboard), including kg needed to be best lifter by bodyweight
  - Lists now accept double-clicks to select items and open dialog boxes
- [Change Log](https://github.com/jflamy/owlcms4/milestone/50?closed=1) for this release (enhancements and fixes)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local Instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local Instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud Instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)


---

## 4.1.11 (01/09/2019)
- See the [Web Site](https://jflamy.github.io/owlcms4/#) for an overview and for directions to [live demo](https://jflamy.github.io/owlcms4/#/?id=demo)
- Release Highlights:
  - Improvements to break management (easier to notice that another official has started a break, more intuitive behaviour of timer)
- [Change Log](https://github.com/jflamy/owlcms4/milestone/49?closed=1) for this release (enhancements and fixes)
- Installation Instructions :
  - For **Windows**, download `owlcms_setup.exe` below and follow [Local Instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Linux** and **Mac OS**, download the `owlcms.zip` file below and follow [Local Instructions](https://jflamy.github.io/owlcms4/#/LocalSetup.md) 
  - For **Heroku** cloud, download the `owlcms.zip` file below and follow [Cloud Instructions](https://jflamy.github.io/owlcms4/#/Heroku.md)

