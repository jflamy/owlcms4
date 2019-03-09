# owlcms4
Olympic Weightlifting Competition Management System 

Ongoing rewrite of [owlcms](https://owlcms2.sourceforge.io/#!index.md) using [ Web Components](https://www.webcomponents.org/introduction) and [Vaadin Flow](https://vaadin.com/flow)

Two main incentives for this rewrite:
- owlcms2 shows its age (owlcms was initially written in 2009). Some of the underlying components cannot be upgraded, and as a consequence some bugs
cannot be fixed.
- It is now much easier to write applications that run in the cloud, but with local components such with timers and referee decisions running locally in the browser.  This is where Vaadin Flow and Web Components come into play.

Why is it called owlcms4? First there was owlcms. The name was taken on SourceForge.net, so I did a cleanup and called it owlcms2. Then I started an owlcms3 rewrite, but it was too tedious to implement the off-line features I wanted, so I gave up until Vaadin Flow came out to rekindle my interest.

Current status: Aiming for Minimal Viable Product (MVP) that can run a club meet.
- Working announcer portion (updating athlete cards and recomputing lifting order).
- Working attempt board and results board, with timing and decisions handled locally in the browser
- Working Athlete Registration and Weigh-in screens.

Next steps towards MVP
- Countdown timer on announcer screen
- Adding missing buzzers
- Improving validations on weight request (athlete card)
- Producing the results


General notes:
- The overall navigation and layout works, using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
- Sample administrative screen (e.g. categories) built using [crudui](https://github.com/alejandro-du/crudui)
- Event-based design to keep clean programming modularity between screens, the field-of-play state, and the back-end data
