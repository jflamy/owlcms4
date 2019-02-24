# owlcms4
Olympic Weightlifting Competition Management System (rewrite using [Vaadin Flow](https://vaadin.com/flow))

Ongoing rewrite of [owlcms2](https://owlcms2.sf.net).

Three main incentives:
- the old release shows its age (was initially written in 2009) and some of the underlying components cannot be upgraded, and some bugs cannot, as a consequence, be fixed
- the pain of running the timers and refereeing off the a main laptop, requiring excellent networking and cabling
- the ability of [web components](https://www.webcomponents.org/introduction) to enable hybrid applications with portions running locally. This will make it possible to run the application in the cloud, with timers and referee decisions (down signal) running locally to avoid the latency. The new version of Vaadin supports this very nicely.

Why owlcms4?  There was an owlcms3 rewrite done using Vaadin version 8, but the off-line features were too difficult to implement
and I lost interest.

Current status:
- overall navigation and layout designed and prototyped using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
- created sample administrative screens using [crudui](https://github.com/alejandro-du/crudui)
- event-based design to keep clean programming modularity between screens, the field-of-play state, and the back-end data
- created first cut of announcer screen exchanging events with the module managing the field-of-play
    - I can basically run through being the announcer for a group
- first cut at a browser-running, Polymer web-component-based attempt board
- design to enable Polymer web components to be used to create the results screens
