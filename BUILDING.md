### Building from source

This is a standard Maven project.  If you so wish, you can build the binaries from this source.
You are welcome to make improvements and correct issues.  If you do, please clone this repository and create a pull request.

- Install Java 8 and the support for Maven and Git in your favorite development environment. 
  - Eclipse Java IDE includes the M2E and EGit plugins and works fine -- the project includes the settings file for that configuration.
  - Eclipse also has a GitFlow plugin at https://marketplace.eclipse.org/content/gitflow-nightly which is useful
- Clone this repository.
- Running ``mvn package`` inside the owlcms subdirectory should give you a working .jar and .zip in the target directory.
- Running `mvn verify` will give you an executable .exe; for this the build has to be on Windows, and you need innosetup to be installed.
  - if you are not running on Windows, you will need to remove the corresponding target in owlcms/pom.xml by setting the execution phase to "none" (or empty).


### Packaging and Releasing

My notes for packaging and releasing

1. Cleanup GitHub issue management
   - Make sure that all closed issues are closed on GitHub and assigned to the proper milestone
   - Go to "Issues/Milestones", close obsolete milestones, take note of the *milestone id* for the current release
4. Update Release Notes
   - Descriptive text
   - Update link to closed issues log to refer to the *milestone id*
   - Close all Typora instances, 
6. Refresh owlcms4top, commit and push
4. Run the `gitflow:release-start` goal (`mvn gitflow:release-start`)
5. Run the `gitflow:release-finish` goal (`mvn gitflow:release-finish`)
   - This does a `clean verify` to create the uberjar, the zip for cloud/linux/mac and the .exe for windows
   - Then it does the merges of the release to master and develop
   - Then it creates the GitHub release
6. The GitHub release portion will fail due to a bug in the plugin, but all the real work has been done.
   - The repository is left on master:  `git push` (Eclipse: Teams / Push to upstream)
   - Switch to develop: `git checkout develop` (Eclipse: Teams / Switch to master)
   - Delete the local release branch that is left over : `git branch -D release/4.x.y`  (Eclipse: Teams / Advanced / Delete Branch)
7. (optional) Double-check the installer
   1. Uninstall previous version
   2. Refresh the `/owlcms/target` folder.  
   3. Run `/owlcms/target/owlcms_setup/owlcms_setup.exe` to test the installer