# owlcms4
Olympic Weightlifting Competition Management System 

### Ongoing rewrite of [owlcms](https://owlcms2.sourceforge.io/#!index.md).

Main incentives for this rewrite:
- Robustness: owlcms was initially written in 2009. Some of the underlying components can no longer be updated to fix bugs.
- Flexibility: Decisions, timers and sounds handled locally in the browser.  This will enable running the main system in the cloud for those who wish to, and will reduce or eliminate the need for ethernet cables.
- Simplify the design.  Many things that had to be painstakingly coded in the original version are now built-in modern frameworks (database handling and sophisticated user interfaces for example.)

### Current status: 
Minimal viable product ("MVP") able to run a regular regional competition (no jury, no masters -- these will come later)
- Announcer, marshall and timekeeper screens (updating athlete cards and recomputing lifting order).
- Attempt board with timing and decisions handled locally in the browser. USB/Bluetooth keypresses are processed directly in the browser for refereeing.
- Athlete-facing board (same as attempt board, but with decision display reversed to match referee positions as seen from platform).  Either the attempt board or the athlete-facing board can be used with USB or bluetooth keypads to enter referee decisions.
- Group results board for public or warm-up room display.  Shows decision lights.
- Athlete Registration and Weigh-in screens, including production of weigh-in sheet
- Working entry screens for defining a competition (general info, groups, categories, etc.)
- Working athlete cards, weighin sheet (w/ starting weights), group results sheet
- Supports multiple fields of play (platforms)
- Upload of registration sheet (same as owlcms2, in either xls or xlsx format)
- Countdown timer for breaks (before introduction, before first snatch, break before clean and jerk, technical break)

### Next steps
- Complete information on preparation documents (start list, allow entering referees per group)

### Installation and Demo
See https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt for license. Basically, this is free, as-is, software.
If you run it on behalf of others or distribute it, you must give free access to the tool source code and to your modifications if any such that anyone can run your version for free.

[Releases](https://github.com/jflamy/owlcms4/releases) are available at https://github.com/jflamy/owlcms4/releases

[Live demo](https://owlcms4.herokuapp.com) of the current build is available on the Heroku cloud service.
- Note that the cloud demo application is not pre-loaded and uses their free tier, so the first load can take a minute. This is *not* indicative of subsequent loads and is not indicative of local performance (both of which start in a few seconds).
- Use a recent version of Chrome on a laptop.  I do not have access to a recent iPad, I don't know if it runs correctly (it doesn't work on iOS 9 :-) )
- Chrome has decided that sounds will no longer play automatically.  In order to hear the down signal, you need to go to page ``chrome://flags``. Search for ``autoplay policy`` and set it to ``No user gesture is required`` .  If you don't do this, you will not hear the sounds.
- There is a single demo database, which resets itself periodically when the Heroku application times out. So if someone else is playing around, you may see surprising things.
- Suggested steps:
    - Click on "Lifting Group" in the menu
    - Click on "Announcer". A new tab opens.  Select a group ("M1" or "M2") in the top bar.
    - For the time being, clock starts once the Microphone AND the Play icons have BOTH been clicked. Normally the announcer hits the Microphone button and the Timekeeper hits the Play button.  The announcer can enter manual flag/thumbs-up/down decisions using the buttons at the right.  A setting to start the clock on announce will soon be added.
    - You can start a Timekeeper window and test. This opens a new tab, so you can switch.
    - You can go back to the first home tab and go to "Setup Displays"
    - You can start an Attempt Board.  If you stop the time on the Announcer or Timekeeper screen, you can use the keyboard keys 1 3 5 to enter white and 2 4 6 to enter red decisions.  Down signal will appear after two identical.
    - You can start a Result Board.
    - If you go back to the main screen and change the group, you should see all the screens change to the new group.


### Design notes:
- Local timer and decision done using new Web standard [Web Components](https://www.webcomponents.org/introduction)
- [Vaadin Flow](https://vaadin.com/flow) is used for programming because it integrates natively with Web Components and enables the use of robust libraries
    - The overall navigation and layout is done using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
    - Administrative screen (e.g. categories) are built using [crudui](https://github.com/alejandro-du/crudui)
- Event-based design, strict separation between the presentation, the field-of-play business layer, and the back-end data
    - JPA is used to ensure datababse independence (H2 locally, Postgres on Heroku cloud, etc.)
- Why is it called owlcms4? First there was owlcms. Did a major cleanup, and moved the code to sourceforge, owlcms2 was born. A few years back I started an owlcms3 rewrite, but it was too tedious to implement the off-line features I wanted, so I gave up until Vaadin Flow came out to rekindle my interest.
