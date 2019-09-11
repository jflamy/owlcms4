[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vaadin-flow/Lobby#?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

# Beverage Buddy App Starter for Vaadin Flow
:coffee::tea::sake::baby_bottle::beer::cocktail::tropical_drink::wine_glass:

This is a Vaadin platform example application created with Java and HTML. It is used to demonstrate features of Vaadin platform.

The easiest way of using it is via [https://vaadin.com/start](https://vaadin.com/start) - you can choose the vaadin version and the package naming you want. If you want to use it with the latest vaadin version you can use this [direct link](https://vaadin.com/start/simple-ui).

The Starter demonstrates the core Vaadin Flow concepts:
* Building UIs in Java with Components based on [Vaadin components](https://vaadin.com/components), such as `TextField`, `Button`, `ComboBox`, `DatePicker`, `VerticalLayout` and `Grid` (see `CategoriesList`)
* [Creating forms with `Binder`](https://github.com/vaadin/beverage-starter-flow/blob/master/documentation/using-binder-in-review-editor-dialog.asciidoc) (see `ReviewEditorDialog`)
* Making reusable Components on the server side (see `AbstractEditorDialog`)
* [Creating a Component based on a HTML Template](https://github.com/vaadin/beverage-starter-flow/blob/master/documentation/polymer-template-based-view.asciidoc) (see `ReviewsList`)
  * This template can be opened and edited with [the Vaadin Designer](https://vaadin.com/designer)
* [Creating Navigation with the Router API](https://github.com/vaadin/beverage-starter-flow/blob/master/documentation/using-annotation-based-router-api.asciidoc) (See `MainLayout`, `ReviewsList` and `CategoriesList`)

## Prerequisites

The project can be imported into the IDE of your choice, with Java 8 or 11 installed, as a Maven project.

But additionally you need `node.js` installed in your System, and available in your `PATH`.
See the [Node.js page](https://nodejs.org/en/) for the installation instructions.

## Dependencies

Dependencies are managed by Vaadin platform and `vaadin-maven-plugin`.

## Running the Project in Developer Mode

1. Run `mvn jetty:run`
2. Wait for the application to start
3. Open http://localhost:8080/ to view the application

Note that there are some files/folders generated in the project structure automatically. You can find some information about them [here](https://vaadin.com/docs/v14/flow/v14-migration/v14-migration-guide.html#6-build-and-maintain-the-v14-project).

## Production Mode

1. Run `mvn package -Pproduction` to get the artifact.
2. Deploy the `target/beveragebuddy-2.0-SNAPSHOT.war`.

If you want to run the production build using the Jetty plugin, use `mvn jetty:run -Pproduction` and navigate to the http://localhost:8080/.

## Documentation

Brief introduction to the application parts can be found from the `documentation` folder. For Vaadin documentation for Java users, see [Vaadin.com/docs](https://vaadin.com/docs/flow/Overview.html).

## Adding new templates

To add a new template or a style to the project create the JavaScript module in the `./frontend` directory.

Then in the PolymerTemplate using the P3 element add the `JsModule` annotation e.g. `@JsModule("./src/views/reviewslist/reviews-list.js")`

### Branching information
* `master` the latest version of the starter, using the latest platform version
* `v10` the version for Vaadin Platform 10
* `v11` the version for Vaadin Platform 11
* `v12` the version for Vaadin Platform 12
* `v13` the version for Vaadin Platform 13

