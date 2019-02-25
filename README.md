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
- The overall navigation and layout works, using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
- Announcer and administrative screens (e.g. categories) being built using [crudui](https://github.com/alejandro-du/crudui)
- Attempt board done with browser-based timer and refere decisions, using Poymer web component templates
- Event-based design to keep clean programming modularity between screens, the field-of-play state, and the back-end data

Next steps
- Finishing integration of local timers and decisions with the back-end
- Using web component templates to implement the result displays
