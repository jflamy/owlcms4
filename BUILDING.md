## Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source.  

This file describes how to build the program without changing it.  If you want to change the code, please see the [Contributing](https://owlcms.github.io/owlcms4/#/Gitpod) documentation for additional information on how to setup a Gitpod or VisualStudio Code environment.

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

When building for the actual release repositories `owlcms-docker` project to build Docker containers using  `mvn package`, once the production build has been done.

### Building official releases

When building official releases, it is assumed that you have been added as a contributor to the Azure Devops project https://dev.azure.com/jflamy/owlcms4 . 

In order to run a build, 

1. Make sure you are on the branch you want to build.  The release number you pick will determine whether the release is sent to the main release repository (if there is no -alpha/-beta/-rc modifier) or to the prereleases repository (if there is a modifier).
   - The project typically uses two branches.  For version 60, there would be `dev60` for the prereleases, and `main60` for the main releases. 
   - All releases, whether maintenance or not, are released first as a prerelease.  So if there is a fix on release 60.0.0 to be called 60.0.1, there would first be a release 60.0.1-rc01.  The results would be pulled back on the dev60 branch, and then merged to the main60 branch (this should be a fast-forward).
   - After building main60, the results are merged back to the dev60 branch.  Sometimes typos are fixed in the release notes, or similar, so we merge back to get all the tags ang everything back in sync.
2. Edit the `azure-pipeline.yaml` file and the `/owlcms4top/src/main/markdown/ReleaseNotes.md` files.  Change the release number in `azure-pipeline.yml` file.
   Use the release number as the commit comment (this is for readability, does not actually affect anything)
   Commit and Push.
3. Go to the pipelines page https://dev.azure.com/jflamy/owlcms4/_build
   Click on the first pipeline listed at the top
4. Use the "Edit" button at the top right.
5. At the left, select the branch you want to build.  This should show you the azure-pipeline file, double-check you are on the correct release.
6. Run the build.  Note that the page that comes up does NOT refresh automatically (it used to, Azure DevOps bug).  To watch the build, click on any of the steps, that second page *does* refresh.
7. Once the build is over, go back to your dev environment and pull.  This will bring back the tag just created.
8. If you built a main release, switch to the corresponding dev release and merge as described above.

Should you need to recreate the pipeline structure and release directories from scratch, the credentials are stored in two locations (depending on who needs them)

- Most credentials used by the Azure Devops tasks are stored as "Service Connections".   These are in the "Project Settings" / "Pipelines" / "Service Connections".  These are the ones actually used to push to github or docker.
- Other credentials are used as variables.  They are defined in a the pipeline "Library" as a "Variable Group". See https://dev.azure.com/jflamy/owlcms4/_library?itemType=VariableGroups .  The same credential may actually have been stored in both locations, for historical reasons.
