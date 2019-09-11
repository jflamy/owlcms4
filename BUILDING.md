### Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source.

## Pre-requisites

- Install Java 8 and the support for Maven and Git in your favorite development environment. 
  - Eclipse Java IDE includes the M2E and EGit plugins and works fine -- the project includes the settings file for that configuration.
  - The project uses the [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) version management process 
    - There is a [GitFlow Eclipse plugin](https://marketplace.eclipse.org/content/gitflow-nightly) that works well.
    - You may prefer using something like [SourceTree](https://www.sourcetreeapp.com/).
  - The build process uses Git commands and the ability to run them in a shell script; installing [GitHub Desktop](https://desktop.github.com/) is the easiest way. Select the options to add the programs to the execution path.
- In order to build the Windows installer, you need to run on Windows and [Innosetup](http://www.jrsoftware.org/isinfo.php) needs to be installed.
- Clone this repository.


## Building and testing

- Running ``mvn package`` inside the owlcms subdirectory should give you 
  - `target/owlcms.jar` working  uberjar (a .jar file that contains all the dependencies)
  -  `target/owlcms.zip`
- Running `mvn verify` will also give you an executable .exe; for this to work the build has to be on Windows, and you need [Innosetup](http://www.jrsoftware.org/isinfo.php) to be installed.
  - if you are <u>not</u> running on Windows, you will need to disable the `windows_jdk_package` target in owlcms/pom.xml by setting the execution phase to "none" (or empty).
  - To check the Windows installer
    1. Uninstall previous version (open the file location of the shortcut and run uninst000.exe )
    2. Refresh the `/owlcms/target` folder.  
    3. Run `/owlcms/target/owlcms_setup/owlcms_setup.exe` to test the installer

## Preparing a release

1. Cleanup GitHub issue management
   - Make sure that all closed issues are closed on GitHub and assigned to the a milestone that matches the release number.
   - Go to "[Issues/Milestones](Issues/Milestones)"
     - close obsolete milestones,
     - click on the current milestone, click on the link for `closed` issues, check titles.
2. Update Release Notes
   - The file is located in `owlcms4top/ReleaseNotes.md`
   - Close all Typora instances
3. Refresh the `owlcms4top`  project, commit and push

### Automated Releasing

Releases are created on [GitHub](https://help.github.com/en/articles/creating-releases).  
The process used for managing versions is [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).  
A [GitFlow Maven plug-in](https://github.com/aleksandr-m/gitflow-maven-plugin) is used to reduce the number of manual steps.

1. Run `mvn gitflow:release-start` to create the new release.
   - You should be in the `develop`  branch before starting, and have merged all the features you wish so include.
   - This will create the new release branch, and immediately change the version numbers on `develop` to the next SNAPSHOT number.
5. Run `mvn gitflow:release-finish`
   - First, this does a `clean verify` to create the uberjar, the zip for Heroku/Linux/Mac and the .exe for Windows
   - Then it does the merge of the release branch into`master`
   - Finally it creates the GitHub release
6. The GitHub release portion will fail due to a bug in the plugin, but all the real work has been done.
   - go to the target directory
   - start the "git bash" shell
   - run cleanup-release.sh


