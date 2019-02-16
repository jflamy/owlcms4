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
- figured out how to do the multiple administrative screens easily using [crudui](https://github.com/alejandro-du/crudui)
- first cut at a locally-running, web-component-based attempt board
- understanding of how to do the public displays with web-component-based templates
- design patterns determined for clean separation between the ui, the field-of-play state, and the back-end data
