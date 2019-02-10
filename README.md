# owlcms4
Olympic Weightlifting Competition Management System (rewrite using [Vaadin Flow](https://vaadin.com/flow))

Ongoing rewrite of [owlcms2](https://owlcms2.sf.net).

Three main incentives:
- the old release shows its age (was initially written in 2009) and some of the underlying components cannot be upgraded, and some bugs cannot, as a consequence, be fixed.
- web applications have progressed immensely, and through the use of [web components](https://www.webcomponents.org/introduction) it is now possible to create interactive applications running locally in the browser with much greater ease 
- The folks at Vaadin have evolved their very nice platform to integrate the system running on a laptop (or on the cloud), with the web components running on the various displays. This will make it possible to run the application in the cloud, with timers and referee decisions (down signal) running locally to avoid the latency.

Why owlcms4?  There was an owlcms3 rewrite done using Vaadin version 8, but the off-line features were too difficult to implement
and I lost interest.

Current status: overall navigation and layout done, figured out how to do the multiple administrative screens easily, key learning on web elements and newfangled web development done (working interative mockup of the attempt board with timer and decisions).