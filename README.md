# owlcms4
Olympic Weightlifting Competition Management System 

### Ongoing rewrite of [owlcms](https://owlcms2.sourceforge.io/#!index.md).

Main incentives for this rewrite:
- Ability to run in the cloud: Decisions, timers and sounds are now handled locally in the browser.  This will reduce or eliminate the need for ethernet cables since the browsers don't rely on the server for timing.
- Simplify the design.  Many things that had to be painstakingly coded in the original 2009 version are now readily available in modern frameworks.
- Obsolescence: libraries that the original version relied on are no longer maintained, making it basically impossible to fix some bugs. The new version relies on far fewer libraries, that are either very broadly used industry standards with long-term sustainability, or simple enough that they can be fixed.

### Current status: 
Able to run a regular regional competition (no jury, no masters -- these will come later)
- Announcer, marshall and timekeeper screens (updating athlete cards and recomputing lifting order).
- Attempt board with timing and decisions handled locally in the browser. USB/Bluetooth keypresses are processed directly in the browser for refereeing (see demo walkthrough below for mapping).
- Athlete-facing board (decision display reversed to match referee positions as seen from platform).  Can also be used for keypress refereeing.
- Group results board for public or warm-up room display.  Shows timer, down and decision lights.
- Athlete Registration and Weigh-in screens, including production of weigh-in sheet with starting weights and athlete cards.
- Working entry screens for defining a competition (general info, groups, categories, etc.)
- Multiple fields of play (platforms)
- Upload of registration sheet (same format as owlcms2, in either xls or xlsx format)
- Countdown timer for breaks (before introduction, before first snatch, break before clean and jerk, technical break)
- Production of group results/protocol sheets

### Next steps
- See the following pages: [project board](https://github.com/jflamy/owlcms4/projects/1) and the [open issues and enhancement requests](https://github.com/jflamy/owlcms4/issues)
- You are encouraged to register on github and submit or discuss issues and enhancements

### Licensing
This is free, as-is, no warranty whatsoever software (see the [License](https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt) for details.)  If you want to run it as is for your own club or federation, just go ahead. 
If you wish to host the software for others or modify it, the license requires you to make full sources and instructions available for free, so that anyone who wants to use the software always has the option of doing so on her own.

### Installation and Demo
[Ready-to-run Releases](https://github.com/jflamy/owlcms4/releases) are available at [https://github.com/jflamy/owlcms4/releases](https://github.com/jflamy/owlcms4/releases) .  
[Installation Instructions](<https://jflamy.github.io/owlcms4/#!index.md>) are available.  Each release includes specific installation notes as required.

[Live demo](https://owlcms4.herokuapp.com) of the current build is available on the Heroku cloud service.
- Note that the cloud demo application is not pre-loaded and uses their free tier, so the first load can take a minute. This is *not* indicative of subsequent loads and is not indicative of local performance (both of which start in a few seconds).
- Use a recent version of Chrome or Firefox on a laptop (Windows, Mac, Linux).  I do not have access to a recent iPad.
- Chrome has decided that sounds will no longer play automatically.  In order to hear the down signal, you need to go to page ``chrome://flags``. Search for ``autoplay policy`` and set it to ``No user gesture is required`` .  If you don't do this, you will not hear the sounds.
- There is a single demo database, which resets itself periodically when the Heroku application times out. So if someone else is playing around, you may see surprising things.
- Suggested steps:
    - Click on "Lifting Group" in the menu
    - Click on "Announcer". A new tab opens.  Select a group ("M1" or "M2") in the top bar.
    - You can start and stop the clock with the "play" and "pause" buttons.
    - The announcer can enter manual flag/thumbs-up/down decisions using the buttons at the right.
    - Start time for the athlete and stop the clock after a few seconds.
    - Go back to the first home tab you opened and go to "Setup Displays" in the menu
    - Start an Attempt Board and click on the black area. You can then use the keyboard keys 1 3 5 to enter white and 2 4 6 to enter red decisions.  Down signal will appear after two identical.
    - You can also start a Result Board.
    - If you go back to the main screen and change the group, you should see all the screens change to the new group.

### Building from source
This is a standard Maven project.  If you so wish, you can build the binaries from this source.  
- Install Java 8 and the support for Maven and Git in your favorite development environment. Eclipse with the M2E and EGit plugins works fine.  In the future, you will also be required to install Node.js because the Web Component support from the user interface framework is moving towards npm.
- Clone this repository.
- Running ``mvn package`` inside the owlcms subdirectory should give you a working .jar and .zip in the target directory.

You are welcome to make improvements and correct issues.  If you do, please clone this repository and create a pull request.  See the [License](https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt) if you distribute or host modified versions.

### Design notes:
- Local timer and decision is done using new Web standard [Web Components](https://www.webcomponents.org/introduction)
- [Vaadin Flow](https://vaadin.com/flow) is used for programming because it integrates natively with Web Components and enables the use of robust libraries
    - The overall navigation and layout is done using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
    - Administrative and technical official screens are built using [crudui](https://github.com/alejandro-du/crudui)
- Event-based design, strict separation between the presentation, the field-of-play business layer, and the back-end data
    - JPA is used to ensure datababse independence (H2 locally, Postgres on Heroku cloud, etc.)
- Why is it called owlcms4? First there was owlcms. Did a major cleanup, and moved the code to sourceforge, owlcms2 was born. A few years back I started an owlcms3 rewrite, but it was too tedious to implement the off-line features I wanted, so I gave up until Vaadin Flow came out to rekindle my interest.
