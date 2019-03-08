# owlcms4
Olympic Weightlifting Competition Management System 

Ongoing rewrite of [owlcms2](https://owlcms2.sourceforge.io/#!index.md) using Web Components and [Vaadin Flow](https://vaadin.com/flow)

Two main incentives for this rewrite:
- owlcms2 shows its age (it was initially written in 2009). Some of the underlying components cannot be upgraded, and as a consequence some bugs cannot be fixed
- The ability of [web components](https://www.webcomponents.org/introduction) to enable hybrid applications with portions running locally. This will make it possible to run the application in the cloud, with timers and referee decisions running locally in the browser.

Why is it called owlcms4?  There was an owlcms3 rewrite started, but it was too tedious to implement the off-line features I wanted, and I gave up until Vaadin Flow
came out to rekindle my interest.

Current status:
- The announcer portion (updating athlete cards and recomputing lifting order) works
- Working attempt board and results board, timing and decisions handled in the browser
- The overall navigation and layout works, using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
- Sample administrative screen (e.g. categories) built using [crudui](https://github.com/alejandro-du/crudui)
- Event-based design to keep clean programming modularity between screens, the field-of-play state, and the back-end data

Next steps
- Registration and weigh-in screens
