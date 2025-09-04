## Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source.

## Development environment

The easiest way to get a local development environment is to use a devcontainer with Visual Studion Code.
Under Windows, you will need to configure `WSL 2` and `Docker Desktop` first, and install the `Dev Containers` extension.
Under MacOS, you will need to install `brew` and `docker`.  You can then checkout or fork the repository.
VS Code should offer you to run from a dev container.

You can avoid these steps by developing in the cloud, using Github Codespaces.  You would just fork the repository,
and start a codespace from the github page for jflamy/owlcms4.  When prompted, you would use the "devcontainer" workspace
definition.

## Building a production version

The actual build chain is a Github Actions workflow, in `.github/workflows/release.yaml`. 
But you can use maven to create a production build manually.

### Pre-requisites

- Install git : Installing [GitHub Desktop](https://desktop.github.com/) is the easiest way to install Git on a Windows system. Select the options to add the programs to the execution path.
- Clone this repository
- Install Java 17
- Install Maven

### Building and testing

- From the owlcms4 directory, running ``mvn -P production -am -pl clean owlcms package `` should give you 
  - `owlcms/target/owlcms.jar` a working  "uberjar" (that is, a .jar file that contains all the dependencies together in a single file).  This file can then be run using `java -jar owlcms.jar app.owlcms.Main` 
  - `owlcms/target/owlcms.zip` contains a copy of the local files required.  This is what the owlcms installers use.
- The installers are in their own repositories under https://github.com/owlcms and are are built separately.

### Building a Docker container

There is a Dockerfile in owlcms4top to build owlcms for quick testing to the fly.io cloud (see deploy.sh).  Building publicresults would be similar.

The current process for actual production builds uses the `owlcms-docker` project to build Docker containers using  `mvn package`, once the production build has been done.
