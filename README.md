# owlcms4
Olympic Weightlifting Competition Management System 

Ongoing rewrite of [owlcms](https://owlcms2.sourceforge.io/#!index.md).

Main incentives for this rewrite:
- Robustness: owlcms was initially written in 2009. Some of the underlying components can no longer be updated to fix bugs.
- Flexibility: Decisions, timers and sounds handled locally in the browser.  This will enable running the main system in the cloud for those who wish to, and will reduce or eliminate the need for ethernet cables.
- Simplify the design.  Many things that had to be painstakingly coded in the original version are now built-in modern frameworks (database handling and sophisticated user interfaces for example.)  The rewrite is at least 2 times smaller than the original.

Current status: Quite close to minimal viable product ("MVP") able to run a regional competition
- Working announcer, marshall and timekeeper screens (updating athlete cards and recomputing lifting order).
- Working attempt board and results board, with timing and decisions handled locally in the browser. USB/Bluetooth keypresses are processed directly in the browser for refereeing.
- Working Athlete Registration and Weigh-in screens, including producing weigh-in sheet
- Working entry screens for defining a competition (general info, groups, categories, etc.)
- Working athlete cards, weighin sheet (w/ starting weights), group results sheet
- A [live demo](https://owlcms4.herokuapp.com) of the current build is available on the Heroku cloud service. Note that the cloud demo application is not pre-loaded and uses their free tier, so the first load can take a minute. This is *not* indicative of subsequent loads and is not indicative of local performance (both of which start in a few seconds).

Next steps
- Athlete-facing option for decision-lights
- More intuitive way to select current group and current field of play
- Upload of athlete registrations
- Intermission timer
- Additional validations on weight request (athlete card)
- *Minimal* packaging and documentation for early users/testers

Design notes:
- Local timer and decision done using new Web standard [Web Components](https://www.webcomponents.org/introduction)
- [Vaadin Flow](https://vaadin.com/flow) is used for programming because it integrates natively with Web Components and enables the use of robust libraries
    - The overall navigation and layout is done using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
    - Administrative screen (e.g. categories) are built using [crudui](https://github.com/alejandro-du/crudui)
- Event-based design, strict separation between the presentation, the field-of-play business layer, and the back-end data
    - JPA is used to ensure datababse independence (H2 locally, Postgres on Heroku cloud, etc.)
- Why is it called owlcms4? First there was owlcms. Did a major cleanup, and moved the code to sourceforge, owlcms2 was born. A few years back I started an owlcms3 rewrite, but it was too tedious to implement the off-line features I wanted, so I gave up until Vaadin Flow came out to rekindle my interest.
