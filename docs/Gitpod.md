OWLCMS is a Java and JavaScript-based project that follows the modern open-source practices.  It relies on Maven and Git for its build and version control.

A complete, fully-functional copy of a development environment can be obtained in the cloud in a matter of minutes using the CloudSpaces feature of GitHub.  This feature is based on a browser-based version of VS Code.  You can optionally connect a local VS Code or other IDE to the cloud-based version.

### Create the CloudSpaces workspace

Go to to the jflamy/owlcms4 repository and create a fork.  Use the button at the top to open a CloudSpaces workspace.

You may be offered to install some workspace extensions to deal with Java, accept them.

### Running the application

To run the application, open the Java Projects section.  There will be a triangle next to the `owlcms` project name.  You can start the application using the triangle -- just select the Main class when asked what class to run.

![100_running](img/Gitpod/100_running.png)

### Framework Documentation

The application is built using the [Vaadin framework, version 24](https://vaadin.com/docs/latest/).   The frontend modules (all the scoreboards, the timers, the decisions) are coded in JavaScript and are located in the `frontend` directory.
