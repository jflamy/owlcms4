# Olympic Weightlifting Competition Management System 

This software is a full rewrite of a package which has been used to manage Olympic Weightlifting competitions world-wide since 2009.

The application can be used for anything from a club meet using a single laptop all the way up to a national championship with several platform, full jury, integration with streaming, and public internet scoreboards.

## Demo

The following videos and demos are available

- [Simple setup](Demo1): running a minimal setup for a small club meet with a single person controlling the meet.
- [Full setup](Demo2): running a regular or virtual competition with technical officials
- Live demo: after watching the videos, try the [Live demo](Demo3) site, or better yet, [install your own copy](installationOverview) and experiment.

## Installation

Several options are available for installation, depending on your needs. Additional details and diagrams are available in the [**Installation Overview**](InstallationOverview) and [Equipment Setup](EquipmentSetup) pages.

|                                                              |                                                              |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| **Local Setup** <br />This installs the `owlcms` program to run a competition locally, on a master laptop.  No internet connection is needed.<ul><li>[Windows Installation Instructions](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalWindowsSetup)<li>[Mac/Linux Installation Instructions](https://jflamy-dev.github.io/owlcms4-prerelease/#/LocalLinuxMacSetup)<li><nobr>[Release Notes](https://github.com/jflamy-dev/owlcms4-prerelease/releases/tag/4.23.0-rc01)</nobr></ul> | <nobr><i class="material-icons" style="transform: translatey(0.30em)">file_download</i>  **[Windows Installer](https://github.com/jflamy-dev/owlcms4-prerelease/releases/download/4.23.0-rc01/owlcms_setup.exe)**</nobr><br/><i class="material-icons" style="transform: translatey(0.30em)">file_download</i>[Mac/Linux Zip](https://github.com/jflamy-dev/owlcms4-prerelease/releases/download/4.23.0-rc01/owlcms.zip) |
| **Cloud-based Setup**<br />This installs the `owlcms` competition management software in the cloud. Good internet access is needed but no local software is needed. The `publicresults` companion program allows individuals to see the scoreboard on their own phone or tablet no matter where they are located.<ul><li>[Heroku Cloud Installation Instructions](https://jflamy-dev.github.io/owlcms4-prerelease/#/Heroku)</li><li>Configuration Options for [Large Cloud Competitions](https://jflamy-dev.github.io/owlcms4-prerelease/#/HerokuLarge)</li><li><nobr>[Release Notes](https://github.com/jflamy-dev/owlcms4-prerelease/releases/tag/4.23.0-rc01)</nobr></li></ul> | <i class="material-icons" style="transform: translatey(0.30em)">cloud_upload</i> **[Competition Management Cloud Installer](https://github.com/jflamy-dev/owlcms-heroku-prerelease/blob/master/README.md)**<br><i class="material-icons" style="transform: translatey(0.30em)">cloud_upload</i> [Public Results Scoreboard Cloud Installer](https://github.com/jflamy-dev/publicresults-heroku-prerelease/blob/master/README.md) |
| **Documentation**                                            | [Documentation site](https://jflamy-dev.github.io/owlcms4-prerelease/#/index) |

## Features

- The following list is a sampling of the many features available.  <u>*Click on the images if you wish to view them full-sized*</u>.  The images are taken from the demo site.
  
  - Run a **regular** or **masters** competition, with or without a **jury**.
  
  - Ability to run in the [**cloud**](EquipmentSetup#cloud-access-over-the-internet).  Decisions, timers and sounds are handled locally in the browser to provide better feedback.
  
  - [**Announcer and Marshall**](Announcing) screens (updating athlete cards and recalculation of lifting order).  
  
    ![020_EditLifterCard](img/Lifting/020_EditLifterCard.png  ':size=350')
  
  - [**Timekeeping**](Announcing#Starting_the_clock) Time can be [managed by the announcer](Announcing#Starting-the-clock)  (useful for smaller meets) or a dedicated [timekeeper screen](Announcing#Timekeeper) can be used. The timekeeper screen can be conveniently operated from a phone or tablet.
  
    <img src="img/Lifting/050_Timekeeper.png" alt="050_Timekeeper.png" width=350 style="border-style:solid; border-width: thin" />
  
  - **[Attempt Board](Displays#attempt-board)** showing current athlete information, remaining time, weight requested, down signal and decision.
  
    ![032_Attempt_Running](img/Displays/032_Attempt_Running.png ':size=350' )  ![038_Attempt_Decision](img/Displays/038_Attempt_Decision.png ':size=350') 
  
  - **Support for refereeing devices**
    
    - Any USB or Bluetooth [**keypad**](Refereeing#usb-or-bluetooth-keypads) that can be programmed to generate the digits 0 to 9 can be used to enter decisions
    
      ![refereeingSetup](img/equipment/refereeingSetup.jpg ':size=350')  ![030_iPad_Flic](img/Refereeing/030_iPad_Flic.jpg ':size=350')
    
    - [**Mobile phones or tablets**](Refereeing#mobile-device-refereeing) can also be used.
    
      ![mobile_ref](img/Refereeing/mobile_ref.png  ':size=350')
    
  - **[Athlete-facing display](Displays#attempt-board)** (the decision display matches the referee positions as seen from platform). Refereeing keypads are typically connected to this laptop.
  
    ![044_AF_Down](img/Displays/044_AF_Down.png ':size=350') ![048_AF_Decision](img/Displays/048_AF_Decision.png ':size=350')
  
  - **[Scoreboard](Displays#scoreboard)** for public or warm-up room display.  Shows athlete information, timer and decision lights. Current and next lifters are highlighted. Responsive design to accomodate old 4:3 projectors as well as 16:9 wide screens.  Also available is a [lifting order display](Displays#lifting-order).
  
     ![020_Scoreboard](img/Displays/020_Scoreboard.png ':size=350') ![Lifting](img/Displays/025_LiftingOrder.png  ':size=350')

- [**Team Competitions and Sinclair Competitions**](Displays#Top-Teams-Scoreboard).  Team Results are computed in either the IWF points system or as a sum of Sinclair scores. The competition secretary has access to the full details.
  
    ![050_TeamScoreboard](img/Displays/050_TeamScoreboard.png ':size=350')
![060_TopSinclair](img/Displays/060_TopSinclair.png ':size=350')
  ![061_TopTeamSinclair](img/Displays/061_TopTeamSinclair.png ':size=350')
  
  - [**3 and 5-person jury**](Refereeing#jury).  Jury members see referee decisions as they happen. Jury members see their vote outcome once all jurors have voted. 

    ![070_Jury](img/Refereeing/070_Jury.png  ':size=350')
  
  - **[Athlete Registration](Registration) and [Weigh-in](WeighIn) screens**, including production of **[weigh-in sheet](WeighIn#starting-weight-sheet)** with starting weights and **[athlete cards](WeighIn#athlete-cards)**.

    ![042_AthleteCards](img/WeighIn/042_AthleteCards.png ':size=350')

  - [**Upload of registration sheet**](Registration#uploading-a-list-of-athletes) Upload a list of athletes with their team, group, entry totals etc. (same format as owlcms2, in either xls or xlsx format)
  
    ![073_excel](img/Preparation/073_excel.png ':size=350')
  
  - Multiple **[simultaneous age divisions](Preparation#defining-age-divisions-and-categories)**: ability to award separate medals according to age division (e.g. youth vs junior vs senior) .  Simultaneous inclusion of Masters and non-masters groups athletes is possible.
  
     ![020_ageGroupList](img/Categories/020_ageGroupList.png ':size=350')
  
  - [**Competition Parameters**](Preparation#competition-information) :  screens for defining a competition (general info, location, organizer, etc.) and special rules that apply (for example, enforcing or not the 20kg rule, etc.)
  
    ![030_Competition](img/Preparation/030_Competition.png ':size=350')

  - **[Multiple fields of play](Preparation#defining-fields-of-play-platforms)** (platforms): simultaneous competition groups all within the same competition.
  
  - **[Countdown timer for breaks](Announcing#breaks)** (before introduction, before first snatch, break before clean and jerk, technical break)

    ![070_IntroTimer](img/Displays/070_IntroTimer.png ':size=350')
    
  - Production of **[group results (protocol sheets)](Documents#group-results)** and of the **[final result package](Documents#competition-package)**
  
    ![SessionResults](img/Documents/SessionResults.png  ':size=350')
    
  - Option to treat the competition as a **[Masters competition](Preparation#masters)** with proper processing of age groups (older age groups presented first)
  
  - [**Multiple languages**](Preparation#display-language). Currently English, French, Danish, Russian, Swedish, German, Portuguese and Spanish.
  
  - Etc.  Refer to the side menu for the full list of topics.

## Reporting Issues and Suggesting Enhancements

For the current work status, see the following links

- [Project board](https://github.com/jflamy/owlcms4/projects/1) This shows what we are working on, and our work priorities.  Check here first, we may actually already be working on it...

- [Issues and enhancement requests](https://github.com/jflamy/owlcms4/issues) This is the complete log of requests and planned enhancements. Use this page to report problems or suggest enhancements.

- [Discussion list](https://groups.google.com/forum/#!forum/owlcms)  If you wish to discuss the program or ask questions, please add yourself to this discussion [group](https://groups.google.com/forum/#!forum/owlcms).  You can withdraw at any time.

## Translation to Other Languages

- You are welcome to translate the screens and reports to your own language, or fix a translation.  Refer to the [translation documentation](Translation) if you wish to contribute. 


## Licensing and Notes

This is free, as-is, no warranty *whatsoever* software. If you just want to run it as is for your own club or federation, just download from the [Releases](https://github.com/jflamy/owlcms4/releases) page and go ahead.  The software is meant to comply with current IWF Technical Competition Rules and Regulations (TCRR) and with the current Masters Weightlifting rules.  TCRR Requirements regarding equipment are outside our scope (such as the presence of indicator lights and buzzers on refereeing devices, etc.)You should perform your own tests to see if the software is fit for your own purposes and circumstances.

If you wish to provide or host the software as a service to others (whether you charge a fee for that service or not), the license *requires* you to make full sources building instructions available for free.  If you host the software as is, the information in the About page is sufficient to meet the requirement.  If however you modify the code, you are **required** to make the code to your modified version and the building instructions available for free to the users of your service.  The intent of the [License](https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt) is that anyone who wants to compile or further modify your version can do so on their own, for free, and not be tied to your services unless they choose to.  You may contact the author to seek alternative licensing agreements.



## Credits

The software is written and maintained by Jean-François Lamy, IWF International Referee Category 1 (Canada)

Thanks to Anders Bendix Nielsen (Denmark), Alexey Ruchev (Russia) and Brock Pedersen (Canada) for their support, feedback and help testing the software.

The software was built using the open source version of the [Vaadin Flow](https://vaadin.com/flow) framework and several open source libraries. See [this file](https://github.com/jflamy/owlcms4/blob/master/owlcms/pom.xml) for the list of Open Source software used in the project.
