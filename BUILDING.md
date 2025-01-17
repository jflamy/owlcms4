## Building and Packaging

This is a standard Maven project that creates a uberJar with all the dependencies.  If you wish, you can build the binaries from this source.  

This file describes how to build the program without changing it.  If you want to change the code, please see the [Contributing](https://owlcms.github.io/owlcms4/#/Gitpod) documentation.

### Pre-requisites

- Install git : Installing [GitHub Desktop](https://desktop.github.com/) is the easiest way to install Git on a Windows system. Select the options to add the programs to the execution path.
- Clone this repository
- Install Java 17 or newer
- Install Maven


### Building and testing

- From the owlcms4 directory, running ``mvn -P production -am -pl clean owlcms package `` should give you 
  - `owlcms/target/owlcms.jar` a working  "uberjar" (that is, a .jar file that contains all the dependencies together in a single file).  This file can then be run using `java -jar owlcms.jar app.owlcms.Main` 
  - `owlcms/target/owlcms.zip` which is used for actual packaging by the owlcms/owlcms-controlpanel project

### Building a Docker container

There is a Dockerfile in owlcms4top to build owlcms for quick testing to the fly.io cloud (see deploy.sh).  Building publicresults would be similar.

### Release Builds

A full release workflow is in `.github/workflows/release.yaml`
