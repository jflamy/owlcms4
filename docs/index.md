# Olympic Weightlifting Competition Management System 

This system is a full rewrite of [owlcms2](https://owlcms2.sourceforge.io/#!index.md), which has been used to manage Olympic Weightlifting competitions world-wide since 2009.  This new version has been rebuilt for robustness and simplicity.

## Features

- The following key features are present
  - The current release is able to run a **regular** or **masters** competition, with or without a **jury**.
  - Ability to run in the **cloud**: Decisions, timers and sounds are handled locally in the browser to provide better feedback.
  - **Announcer, marshall and timekeeper** screens (updating athlete cards and recomputing lifting order).
  - **Attempt board** showing current athlete information, remaining time and weight requested. 
  - **Support for refereeing devices** (see this [page](Refereeing.md) for discussion) 
    - Any keypad that can be programmed to generate the digits 0 to 9 can be used to enter decisions (see the demo walkthrough below for the key mapping).  
    - Mobile phones or tablets can also be used.
  - **Athlete-facing decision display** (decision display reversed to match referee positions as seen from platform). Refereeing keypads are typically connected to this laptop.
  - **Scoreboard** for public or warm-up room display.  Shows timer, down and decision lights.
  - **Athlete Registration and Weigh-in screens**, including production of **weigh-in sheet** with starting weights and **athlete cards**.
  - Working entry screens for defining a competition (general info, groups, categories, etc.)
  - **Multiple fields of play** (platforms)
  - **Upload of registration sheet** (same format as owlcms2, in either xls or xlsx format)
  - **Countdown timer for breaks** (before introduction, before first snatch, break before clean and jerk, technical break)
  - Production of **group results/protocol sheets**
  - Option to treat the competition as a **Masters competition** with proper processing of age groups.
  - **3 and 5-person jury**.  Jury members see referee decisions as they happen. Jury members see their vote outcome once all jurors have voted.

The software is meant to comply with current IWF Technical Competition Rules and Regulations (TCRR) and with the current Masters Weightlifting rules.  TCRR Requirements regarding equipment are outside our scope (such as the presence of indicator lights and buzzers on refereeing devices, etc.)

## Installation

[Ready-to-run Releases](https://github.com/jflamy/owlcms4/releases) are available at [https://github.com/jflamy/owlcms4/releases](https://github.com/jflamy/owlcms4/releases) . The releases can be run either locally (on any machine where Java8 is available), or on the cloud.

Installation Instructions are available for both [local installations](LocalSetup.md) and for [running on the Heroku cloud service](Heroku.md).

Until which time documentation for owlcms4 is fully updated, you may wish to refer to the [owlcms2](https://owlcms2.sourceforge.io/#!Running.md) documentation.  The general concepts are the same.

#### Requirements

- The server software will run on any recent laptop acting as a server (or on a cloud) with Java8 installed.
- For the user interface and displays, use a recent version of Chrome or Firefox on a laptop (Windows, Mac, Linux).  Apple iPads are not supported as I do not have access to a development Mac to iron out the glitches.
- In order to drive the displays, you may use any laptop or miniPC (such as a [Raspberry Pi](https://www.raspberrypi.org/products/raspberry-pi-3-model-b/)).  You need one laptop or miniPC for each display.

## Demo

A [Live demo](https://owlcms4.herokuapp.com) of the current build is available on the Heroku cloud service.

- Note that the cloud demo application is not pre-loaded and uses a zero-cost service, so the first load can take a minute. This is *not* indicative of subsequent cloud loads neither is it indicative of local performance (both of which start in a few seconds)
- There is a single demo database, which resets itself periodically when the Heroku application times out. So if someone else is playing around, you may see surprising things.
- Suggested steps for a walkthrough:
  - Click on "Lifting Group" in the menu
  - Click on "Announcer". A new tab opens.  Select a group ("M1" or "M2") in the top bar.
  - You can start and stop the clock with the "play" and "pause" buttons.
  - Start time for the athlete and stop the clock after a few seconds.
  - The announcer can enter manual flag/thumbs-up/down decisions using the buttons at the right.
  - Go back to the first home tab you opened and go to "Setup Displays" in the menu
  - Start an Attempt Board and click on the black area. You can then use the keyboard keys 1 3 5 to enter white and 2 4 6 to enter red decisions.  Down signal will appear after two identical.
  - You can also start a Result Board.
  - If you go back to the main screen and change the group, you should see all the screens change to the new group.

## Licensing and Notes

This is free, as-is, no warranty *whatsoever* software. If you just want to run it as is for your own club or federation, just download from the [Releases](https://github.com/jflamy/owlcms4/releases) page and go ahead. You should perform your own tests to see if the software is fit for your own purposes and circumstances.

If however you wish to provide or host the software as a service to others, or if you create a modified version, the license *requires* you to make full sources and building instructions available for free, so that anyone who wants to compile or further modify your version can do so on their own (see the [License](https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt) for details.).  You may contact the author to seek alternative licensing agreements.

### Contributing

For the current status, see

- [Project board](https://github.com/jflamy/owlcms4/projects/1) This shows what we are working on, and our work priorities.
- [Issues and enhancement requests](https://github.com/jflamy/owlcms4/issues) This is the complete log of requests and planned enhancements.

## Credits

The software is written and maintained by Jean-François Lamy, IWF International Referee Category 1 (Canada)

Thanks to Anders Bendix Nielsen (Denmark), Alexey Ruchev (Russia) and Brock Pedersen (Canada) for their support, feedback and help testing the software.

See the file [pom.xml](pom.xml) for the list of Open Source software used in the project.# owlcms4 Setup Instructions
