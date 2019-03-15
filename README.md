# owlcms4
Olympic Weightlifting Competition Management System 

Ongoing rewrite of [owlcms](https://owlcms2.sourceforge.io/#!index.md) using [Web Components](https://www.webcomponents.org/introduction) and [Vaadin Flow](https://vaadin.com/flow)

Two main incentives for this rewrite:
- owlcms2 shows its age (owlcms was initially written in 2009). Some of the underlying components cannot be upgraded, and as a consequence some bugs
cannot be fixed.
- It is now much easier to write applications that run in the cloud, but with local components such with timers and referee decisions running locally in the browser.  This is where Vaadin Flow and Web Components come into play.

Why is it called owlcms4? First there was owlcms. The name was taken on SourceForge.net, so I did a cleanup and called it owlcms2. Then I started an owlcms3 rewrite, but it was too tedious to implement the off-line features I wanted, so I gave up until Vaadin Flow came out to rekindle my interest.

Current status: Close to minimal viable product able to run a regional competition
- Working announcer portion (updating athlete cards and recomputing lifting order).
- Working attempt board and results board, with timing and decisions handled locally in the browser
- Working Athlete Registration and Weigh-in screens, including producing weigh-in sheet
- A [live demo](https://owlcms4.herokuapp.com) of a recent build is normally available on the Heroku cloud service. Note that the cloud demo application is not pre-loaded, so the first load can take a minute. This is not indicative of subsequent loads and is not indicative of local performance (which loads in seconds).

Next steps
- Producing the lifter cards
- Producing the result sheets
- Athlete-facing option for decision-lights
- Improving validations on weight request (athlete card)
- Minimal packaging for early users/testers

Design notes:
- The overall navigation and layout works using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
- Administrative screen (e.g. categories) are built using [crudui](https://github.com/alejandro-du/crudui)
- Event-based design, strict separation between the presentation, the field-of-play business layer, and the back-end data
