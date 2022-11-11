### Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source.

## Pre-requisites

- Install Java 11 and the support for Maven and Git in your favorite development environment. 
  - Eclipse Java IDE includes the M2E and EGit plugins and works fine -- the project includes the settings file for that configuration.
  - The build process uses Git commands and the ability to run them in a shell script; installing [GitHub Desktop](https://desktop.github.com/) is the easiest way to install Git on a Windows system. Select the options to add the programs to the execution path.
- In order to build the Windows installer, you need to run on Windows and [Innosetup](http://www.jrsoftware.org/isinfo.php) needs to be installed.
- Clone this repository.


## Building and testing

- Running ``mvn package -P production`` inside the owlcms subdirectory should give you 
  - `target/owlcms.jar` working  uberjar (a .jar file that contains all the dependencies)
  - `target/owlcms.zip` which is used on Linux and Mac
  
## Building and testing the Windows installer

- Running ``mvn package -P production`` inside the `owlcms-windows` subdirectory should give you a working installer.  This build to be run on a Windows machine because the installer builder is Windows-specific.
- The installer is then found in `owlcms-windows\target\owlcms_setup\owlcms_setup.exe`
  
## Preparing a release

1. Cleanup GitHub issue management
   - Make sure that all closed issues are closed on GitHub and assigned to the a milestone that matches the release number.
   - Go to "[Issues/Milestones](Issues/Milestones)"
     - close obsolete milestones,
     - click on the current milestone, click on the link for `closed` issues, check titles.
2. Update Release Notes
   - The file is located in `owlcms4top/src/main/markdown/ReleaseNotes.md` (the file in owlcmstop will be overwritten),
   - Close all Typora instances
3. Refresh the `owlcms4top\src` directory, commit and push

## Automated Releasing

The automated build process takes place on the free tier of Azure DevOps.
The `azure-pipelines.yml` defines the full process for building owlcms and the companion publicresults application, as well as the Heroku and Docker/Kubernetes packaging.

Note that you must provide credentials in the form of variables (for example, by creating a variable group in the project Library).  See src/main/azure-pipelines/variables-releaseRepoCredentials for the $() variables that need to be defined.

You also need to create service connections to your github accounts (for the source repository, and for the additional repositories used for pre-release and release packages), as well as the maven repository used to get the packages (reposilite).



