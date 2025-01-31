## Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source. 

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
