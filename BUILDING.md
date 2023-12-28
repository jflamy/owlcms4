## Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source.  

This file describes how to build the program without changing it.  If you want to change the code, please see the [Contributing](https://owlcms.github.io/owlcms4/#/Gitpod) documentation for additional information on how to setup a Gitpod or VisualStudio Code environment.

### Pre-requisites

- Install git : Installing [GitHub Desktop](https://desktop.github.com/) is the easiest way to install Git on a Windows system. Select the options to add the programs to the execution path.
- Clone this repository
- Install Java 17
- Install Maven
  - You need to add lines to your the `.m2/settings.xml` file in your home directory.  You can copy the file `.gitpod/settings.xml` if you don't have anything special in your own file, else make sure to merge the directives to unlock the owlcms repository. 

- In order to build the Windows installer, you need to run on Windows and [Innosetup](http://www.jrsoftware.org/isinfo.php) needs to be available. The portable version used in the build is found under the `installtools/main/assembly` directory, so normally there is no need to install it separately.


### Building and testing

- From the owlcms4 directory, running ``mvn -P production -am -pl clean owlcms package `` should give you 
  - `owlcms/target/owlcms.jar` a working  "uberjar" (that is, a .jar file that contains all the dependencies together in a single file).  This file can then be run using `java -jar owlcms.jar app.owlcms.Main` 
  - `owlcms/target/owlcms.zip` which is used on Linux and Mac


### Building and testing the Windows installer

- Running ``mvn package -P production`` inside the `owlcms-windows` subdirectory should give you a working installer.  This build needs to be run on a Windows machine because the installer builder is Windows-specific.
- The installer is then found in `owlcms-windows\target\owlcms_setup\owlcms_setup.exe`

### Building a Docker container

There is an `owlcms-docker` project to build Docker containers using `mvn package`, once the production build has been done.
