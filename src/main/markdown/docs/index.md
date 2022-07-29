# Olympic Weightlifting Competition Management System 

This software is a full rewrite of a package which has been used to manage Olympic Weightlifting competitions world-wide since 2009.

The application can be used for anything from a club meet using a single laptop all the way up to a national championship with several platform, full jury, integration with streaming, and public internet scoreboards.

## Demo

The following videos and demos are available

- [Simple setup](Demo1): running a minimal setup for a small club meet with a single person controlling the meet.
- [Full setup](Demo2): running a regular or virtual competition with technical officials
- Live demo: after watching the videos, try the [Live demo](Demo3) site, or better yet, [install your own copy](installationOverview) and experiment.

## Features

The following list is a sampling of the many features available.  <u>*Click on the images if you wish to view them full-sized*</u>.  The images are taken from the demo site.

  - Run a **regular** or **masters** competition, with or without a **jury**.
  
  - Ability to run in the [**cloud**](EquipmentSetup#cloud-access-over-the-internet).  Decisions, timers and sounds are handled locally in the browser to provide better feedback.
  
- **[Scoreboard](Displays#scoreboard)** for public or warm-up room display.  Current and next lifters are highlighted.   If record information has been loaded, the records being attempted are highlighted

     ![020_Scoreboard](img/Records/records.png ':size=800')

     Leaders from current and previous groups can be shown for multi-group competitions. The leaders and records sections can be shown or hidden on demand.

     ![024_Scoreboard_Leaders](img/Displays/024_Scoreboard_Leaders.png)

     If athletes can win medals in several categories, a multi-rank scoreboard is available

     ![hvmDyjbdr2](img/Displays/hvmDyjbdr2.png)

     The top part of the scoreboard contains the same information as the attempt board, and shows the down signal and decisions.

     ![022_Scoreboard_Decision](img/Displays/022_Scoreboard_Decision.png)

- [**Announcer and Marshall**](Announcing) screens (updating athlete cards and recalculation of lifting order).  
  
    ![020_EditLifterCard](img/Lifting/020_EditLifterCard.png  ':size=350')
  
- [**Timekeeping**](Announcing#Starting_the_clock) Time can be [managed by the announcer](Announcing#Starting-the-clock)  (useful for smaller meets) or a dedicated [timekeeper screen](Announcing#Timekeeper) can be used. The timekeeper screen can be conveniently operated from a phone or tablet.
  
    <img src="img/Lifting/050_Timekeeper.png" alt="050_Timekeeper.png" width=350 style="border-style:solid; border-width: thin" />

- **[Attempt Board](Displays#attempt-board)** showing current athlete information, remaining time, weight requested, down signal and decision.
  
    ![032_Attempt_Running](img/Displays/032_Attempt_Running.png ':size=350' )  ![038_Attempt_Decision](img/Displays/038_Attempt_Decision.png ':size=350') 
  
- **Support for refereeing devices**
  
  - Any USB or Bluetooth [**keypad**](Refereeing#usb-or-bluetooth-keypads) that can be programmed to generate the digits 0 to 9 can be used to enter decisions
    
      ![refereeingSetup](img/equipment/refereeingSetup.jpg ':size=350')  ![030_iPad_Flic](img/Refereeing/030_iPad_Flic.jpg ':size=350')
    
  - [**Mobile phones or tablets**](Refereeing#mobile-device-refereeing) can also be used.  These devices can provide notifications to the referees.
    
      ![mobile_ref](img/Refereeing/mobile_ref.png ':size=350')
      
  - [**Physical devices with visual and audio feedback capability**](Refereeing#full-feedback-keypad)  Schematics are available to build affordable devices that support referee reminders and jury summoning to comply with IWF TCRR.
  

![device](https://camo.githubusercontent.com/c0d799a3bd35c47d4c4aa1d7caa508f32866820ca9ca7e24b20510b0ab27dbd1/68747470733a2f2f776f6b77692e636f6d2f63646e2d6367692f696d6167652f77696474683d313932302f68747470733a2f2f7468756d62732e776f6b77692e636f6d2f70726f6a656374732f3332323533343534333030383436353439312f7468756d626e61696c2e6a7067 ':size=450') 

- **[Athlete-facing display](Displays#attempt-board)** (the decision display matches the referee positions as seen from platform). Refereeing keypads are typically connected to this laptop.
  
    ![044_AF_Down](img/Displays/044_AF_Down.png ':size=350') ![048_AF_Decision](img/Displays/048_AF_Decision.png ':size=350')
  
-  **[Records](Records)**  Record information can be provided using Excel files. Records for multiple federations and events can be loaded.  Record is then shown on the scoreboards, and notifications are given to the officials when records are attempted or set.  If a record is improved, the record information is updated.
  
-  **[Lifting order display](Displays#lifting-order)**. Useful for the marshal or for regional championships to help newer coaches.

    ![Lifting](img/Displays/025_LiftingOrder.png  ':size=350')
    
- [**Team Competitions and Sinclair Competitions**](Displays#Top-Teams-Scoreboard).  Team Results are computed in either the IWF points system or as a sum of Sinclair scores. The competition secretary has access to the full details.
  
  ![050_TeamScoreboard](img/Displays/050_TeamScoreboard.png ':size=350')
![060_TopSinclair](img/Displays/060_TopSinclair.png ':size=350')
    ![061_TopTeamSinclair](img/Displays/061_TopTeamSinclair.png ':size=350')
  
- [**3 and 5-person jury**](Refereeing#jury).  Jury members see referee decisions as they happen. Jury members see their vote outcome once all jurors have voted. 

    ![070_Jury](img/Refereeing/070_Jury.png  ':size=350')
  
- **[Athlete Registration](Registration) and [Weigh-in](WeighIn) screens**, including production of **[weigh-in sheet](WeighIn#starting-weight-sheet)** with starting weights and **[athlete cards](WeighIn#athlete-cards)**.

    ![042_AthleteCards](img/WeighIn/043_AthleteCards.png ':size=350')

- [**Upload of registration sheet**](Registration#uploading-a-list-of-athletes) Upload a list of athletes with their team, group, entry totals etc. (same format as owlcms2, in either xls or xlsx format)
  
    ![073_excel](img/Preparation/073_excel.png ':size=350')
  
- Multiple **[simultaneous age divisions](Preparation#defining-age-divisions-and-categories)**: ability to award separate medals according to age division (e.g. youth vs junior vs senior) .  Simultaneous inclusion of Masters and non-masters groups athletes is possible.
  
     ![020_ageGroupList](img/Categories/020_ageGroupList.png ':size=350')
  
- [**Competition Parameters**](Preparation#competition-information) :  screens for defining a competition (general info, location, organizer, etc.) and special rules that apply (for example, enforcing or not the 20kg rule, etc.)
  
    ![030_Competition](img/Preparation/030_Competition.png ':size=350')

- **[Multiple fields of play](Preparation#defining-fields-of-play-platforms)** (platforms): simultaneous competition platforms within the same competition.
  
  ![IMG_1610](img/ZoomVideo/IMG_1610.jpg)
  
- **[Countdown timer for breaks](Announcing#breaks)** (before introduction, before first snatch, break before clean and jerk, technical break)

    ![070_IntroTimer](img/Displays/070_IntroTimer.png ':size=350')
    
- Production of **[group results (protocol sheets)](Documents#group-results)** and of the **[final result package](Documents#competition-package)**
  
    ![SessionResults](img/Documents/SessionResults.png  ':size=350')
    
- Option to treat the competition as a **[Masters competition](Preparation#masters)** with proper processing of age groups (older age groups presented first)
  
- **[Video Streaming Scene Switching](OBSSceneSwitching)** When using OBS (or similar software) to stream a competition, a special status window can be monitored to switch scenes, trigger replays, or provide information as to the course of the competition (for example, jury deliberation, etc.)
  
- [**Multiple languages**](Preparation#display-language). Currently English, French, Spanish, Danish, Swedish, German, Portuguese, Russian, Ukrainian, Armenian.
  
- **[Color and Visual Styling Customization](Styles)**  The colors of the displays are controlled by Web-standard CSS stylesheets, the format used by web designers world wide. A tutorial is given for the common case of adjusting the color scheme to local preferences.
  
    ![colors](img/Displays/colors.png ':size=350')
  
- Etc.  Refer to the side menu for the full list of topics.

## Support

- [Discussion list](https://groups.google.com/forum/#!forum/owlcms)  If you wish to discuss the program or ask questions, please add yourself to this discussion [group](https://groups.google.com/forum/#!forum/owlcms).  You can withdraw at any time.
- [Issues and Feature Requests](https://github.com/jflamy/owlcms4/projects/1)  Use the "Issues" icon at the top of the page.
- [Project board](https://github.com/jflamy/owlcms4/projects/1) This shows what we are working on, and our work priorities.  Check here first, we may actually already be working on it