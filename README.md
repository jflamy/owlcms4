# owlcms4
Olympic Weightlifting Competition Management System 

Ongoing rewrite of [owlcms2](https://owlcms2.sf.net) using Web Components and [Vaadin Flow](https://vaadin.com/flow))

Two main incentives:
- the old release shows its age (was initially written in 2009) and some of the underlying components cannot be upgraded, and some bugs cannot, as a consequence, be fixed
- the ability of [web components](https://www.webcomponents.org/introduction) to enable hybrid applications with portions running locally. This will make it possible to run the application in the cloud, with timers and referee decisions running locally in the browser.

Why owlcms4?  There was an owlcms3 rewrite done using Vaadin version 8, but the off-line features were too difficult to implement
and I lost interest.

Current status:
- The announcer portion (updating athlete cards and recomputing lifting order) works
- The overall navigation and layout works, using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
- Announcer and administrative screens (e.g. categories) being built using [crudui](https://github.com/alejandro-du/crudui)
- Attempt board done with browser-based timer and refere decisions, using Poymer web component templates
- Event-based design to keep clean programming modularity between screens, the field-of-play state, and the back-end data

Next steps
- Finishing integration of local timers and decisions with the back-end
- Using web component templates to implement the result displays
