# Olympic Weightlifting Competition Management System 

OWLCMS4 is a full rewrite of [owlcms](https://owlcms2.sourceforge.io/#!index.md), which has been used to manage Olympic Weightlifting competitions world-wide since 2009.  This new version has been rebuilt for robustness and simplicity, and can optionally be run on the cloud without a local installation.

## Features

- The following list is a sampling of the many features available.  Refer to the various sections of the documentation (see the sidebar menu at the left) to get a better sense.
  - Run a **regular** or **masters** competition, with or without a **jury**.
  
  - Ability to run in the **cloud**: Decisions, timers and sounds are handled locally in the browser to provide better feedback.
  
  - **Announcer, Marshall and Timekeeper** screens (updating athlete cards and recalculation of lifting order).
  
    ![020_EditLifterCard](img/Lifting/020_EditLifterCard.png  ':size=350')
  
  - **Attempt Board** showing current athlete information, remaining time, weight requested, down signal and decision.
  
    ![032_Attempt_Running](img/Displays/032_Attempt_Running.png ':size=350' )  ![038_Attempt_Decision](img/Displays/038_Attempt_Decision.png ':size=350') 
  
  - **Support for refereeing devices** (see this [page](Refereeing.md) for discussion) 
    - Any keypad that can be programmed to generate the digits 0 to 9 can be used to enter decisions
    
      ![refereeingSetup](img/equipment/refereeingSetup.jpg ':size=350') 
    
    - Mobile phones or tablets can also be used.
    
      ![mobile_ref](img/Refereeing/mobile_ref.png  ':size=350')
    
  - **Athlete-facing display** (the decision display matches the referee positions as seen from platform). Refereeing keypads are typically connected to this laptop.
  
    ![044_AF_Down](img/Displays/044_AF_Down.png ':size=350') ![048_AF_Decision](img/Displays/048_AF_Decision.png ':size=350')
  
    
  
  - **Scoreboard** for public or warm-up room display.  Shows athlete information, timer and decision lights. Current and next lifters are highlighted. Responsive design to accomodate old 4:3 projectors as well as 16:9 wide screens.
  
     ![020_Scoreboard](img/Displays/020_Scoreboard.png ':size=350')
  
  - **3 and 5-person jury**.  Jury members see referee decisions as they happen. Jury members see their vote outcome once all jurors have voted. 
  
    ![070_Jury](img/Refereeing/070_Jury.png  ':size=350')
  
  - **Athlete Registration and Weigh-in screens**, including production of **weigh-in sheet** with starting weights and **athlete cards**.
  
  - Working entry screens for defining a competition (general info, groups, categories, etc.)
  
  - **Multiple fields of play** (platforms)
  
  - **Upload of registration sheet** (same format as owlcms2, in either xls or xlsx format)
  
  - **Countdown timer for breaks** (before introduction, before first snatch, break before clean and jerk, technical break)
  
  - Production of **group results/protocol sheets**
  
  - Option to treat the competition as a **Masters competition** with proper processing of age groups.
  
  - Multiple languages. Currently English, French, Danish, and a partial translation to Spanish.

The software is meant to comply with current IWF Technical Competition Rules and Regulations (TCRR) and with the current Masters Weightlifting rules.  TCRR Requirements regarding equipment are outside our scope (such as the presence of indicator lights and buzzers on refereeing devices, etc.)

## Installation

Download and Installation Instructions are available for both [local setups without internet access](https://jflamy.github.io/owlcms4/#/LocalSetup.md) and for [running on the free tier of the Heroku cloud service](https://jflamy.github.io/owlcms4/#/Heroku.md).

## Demo

- See [this page](Demo) for how to access  the [demo site](https://owlcms4.herokuapp.com) and a perform a simple walkthrough
- All the screenshots for the documentation on this site come from the demo. You may therefore use the [demo site](https://owlcms4.herokuapp.com) to perform a full walkthrough.

## Licensing and Notes

This is free, as-is, no warranty *whatsoever* software. If you just want to run it as is for your own club or federation, just download from the [Releases](https://github.com/jflamy/owlcms4/releases) page and go ahead. You should perform your own tests to see if the software is fit for your own purposes and circumstances.

If however you wish to provide or host the software as a service to others, or if you create a modified version, the license *requires* you to make full sources and building instructions available for free, so that anyone who wants to compile or further modify your version can do so on their own (see the [License](https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt) for details.).  You may contact the author to seek alternative licensing agreements.

## Reporting Issues and Suggesting Enhancements

For the current work status, see the following links

- [Project board](https://github.com/jflamy/owlcms4/projects/1) This shows what we are working on, and our work priorities.  Check here first, we may actually be working on it...
- [Issues and enhancement requests](https://github.com/jflamy/owlcms4/issues) This is the complete log of requests and planned enhancements. Use this page to report problems or ask questions.
- [Translations](https://github.com/jflamy/owlcms4/tree/develop/owlcms/src/main/resources/i18n) Refer to this link if you wish to translate the screens to your own language, or fix a translation.

## Credits

The software is written and maintained by Jean-François Lamy, IWF International Referee Category 1 (Canada)

Thanks to Anders Bendix Nielsen (Denmark), Alexey Ruchev (Russia) and Brock Pedersen (Canada) for their support, feedback and help testing the software.

The software was built using the open source version of the [Vaadin Flow](https://vaadin.com/flow) framework and several open source libraries. See [this file](https://github.com/jflamy/owlcms4/blob/master/owlcms/pom.xml) for the list of Open Source software used in the project.
