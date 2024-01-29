## Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source.  

This file describes how to build the program without changing it.  If you want to change the code, please see the [Contributing](https://${env.REPO_OWNER}.github.io/${env.O_REPO_NAME}/#/Gitpod) documentation for additional information on how to setup a Gitpod or VisualStudio Code environment.

### Pre-requisites

- Install git : Installing [GitHub Desktop](https://desktop.github.com/) is the easiest way to install Git on a Windows system. Select the options to add the programs to the execution path.
- Clone this repository
- Install Java 17
- Install Maven
  
- To build the Windows installer, you need to be on Windows and [Innosetup](http://www.jrsoftware.org/isinfo.php) needs to be available. The portable version used in the build is found under the `installtools/main/assembly` directory, so there is no need to actually install it.


### Building and testing

- From the owlcms4 directory, running ``mvn -P production -am -pl clean owlcms package `` should give you 
  - `owlcms/target/owlcms.jar` a working  "uberjar" (that is, a .jar file that contains all the dependencies together in a single file).  This file can then be run using `java -jar owlcms.jar app.owlcms.Main` 
  - `owlcms/target/owlcms.zip` which is used on Linux and Mac


### Building and testing the Windows installer

- Running ``mvn package -P production`` inside the `owlcms-windows` subdirectory should give you a working installer.  This build needs to be run on a Windows machine because the installer builder is Windows-specific.
- The installer is then found in `owlcms-windows\target\owlcms_setup\owlcms_setup.exe`

### Building a Docker container

There is a Dockerfile in owlcms4top to build owlcms for quick testing to the fly.io cloud (see deploy.sh).  Building publicresults would be similar.

The current process for actual production builds uses the `owlcms-docker` project to build Docker containers using  `mvn package`, once the production build has been done.
