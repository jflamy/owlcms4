### Building from source

This is a standard Maven project.  If you so wish, you can build the binaries from this source.  

- Install Java 8 and the support for Maven and Git in your favorite development environment. Eclipse Java IDE includes the M2E and EGit plugins and works fine -- the project includes the settings file for that configuration.
- Clone this repository.
- Running ``mvn package`` inside the owlcms subdirectory should give you a working .jar and .zip in the target directory.

You are welcome to make improvements and correct issues.  If you do, please clone this repository and create a pull request.

### Design notes:

Local timer and decision is done using [Web Components](https://www.webcomponents.org/introduction)

[Vaadin Flow](https://vaadin.com/flow) is used for programming because it integrates natively with Web Components and enables the use of robust libraries

- The overall navigation and layout is done using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
- Administrative and technical official screens are built using [crudui](https://github.com/alejandro-du/crudui)
- Event-based design, strict separation between the presentation, the field-of-play business layer, and the back-end data
- JPA is used to ensure datababse independence (H2 locally, Postgres on Heroku cloud, etc.)
- Why is it called owlcms4? First there was owlcms. Did a major cleanup, and moved the code to sourceforge, owlcms2 was born. A few years back I started an owlcms3 rewrite, but it was too tedious to implement the off-line features I wanted, so I gave up until Vaadin Flow came out to rekindle my interest.