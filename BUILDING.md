### Building and Packaging

This is a standard Maven project.  If you wish, you can build the binaries from this source.
In order to do so.

- Install Java 8 and the support for Maven and Git in your favorite development environment. 
  - Eclipse Java IDE includes the M2E and EGit plugins and works fine -- the project includes the settings file for that configuration.
  - The project uses [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow) to manage its versions; there is a GitFlow Eclipse plugin at https://marketplace.eclipse.org/content/gitflow-nightly .  You may prefer using something like [](https://www.sourcetreeapp.com/).
- Clone this repository.
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
     - click on the current milestone, click on the link for `closed` issues.  Copy the URL
2. Update Release Notes
   - The file is located in `owlcms4top/ReleaseNotes.md`
   - Update link to the  `Change Log` to refer to the URL for the close issues copied in step 2.
   - Close all Typora instances
3. Refresh the `owlcms4top`  project, commit and push

### Automated Releasing

Releases are created on [GitHub](https://help.github.com/en/articles/creating-releases).  The process used for managing versions is [GitFlow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow).  A [GitFllow Maven plug-in](https://github.com/aleksandr-m/gitflow-maven-plugin) is used to reduce the number of manual steps.

1. Run `mvn gitflow:release-start` to create the new release.
   - You should be in the `develop`  branch before starting, and have merged all the features you wish so include.
   - This will create the new release branch, and immediately change the version numbers on `develop` to the next SNAPSHOT number.
5. Run `mvn gitflow:release-finish`
   - First, this does a `clean verify` to create the uberjar, the zip for Heroku/Linux/Mac and the .exe for Windows
   - Then it does the merge of the release branch into`master`
   - Finally it creates the GitHub release
6. The GitHub release portion will fail due to a bug in the plugin, but all the real work has been done.
   - The local repository is left on master with changes unpushed; changes that occurred in the release branch are also not merged back into `develop`
     - Push all branches upstream (Eclipse: Teams / Push to upstream)
     - Switch to develop (Eclipse: Teams / Switch to master)
     - Merge the local master branch back into develop
     - Push all again
   - Delete the local release branch that is left over (Eclipse: Teams / Advanced / Delete Branch)
 ```bash
 git push --all
 git checkout develop
 git merge master
 git push --all
 git branch -D release/4.x.y
 ```

