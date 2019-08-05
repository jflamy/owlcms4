### Building from source

This is a standard Maven project.  If you so wish, you can build the binaries from this source.  

- Install Java 8 and the support for Maven and Git in your favorite development environment. Eclipse Java IDE includes the M2E and EGit plugins and works fine -- the project includes the settings file for that configuration.
- Clone this repository.
- Running ``mvn package`` inside the owlcms subdirectory should give you a working .jar and .zip in the target directory.

You are welcome to make improvements and correct issues.  If you do, please clone this repository and create a pull request.

### Packaging and Releasing

This will be further automated. This list assumes that you are running Eclipse.

1. Using the GitFlow Eclipse plugin
   1. Start a new release
   2. Push to upstream
2. Use the `Versions_set` launch configuration to set the release numbe
3. Cleanup github
   1. Make sure that all closed issues are closed on github and assigned to the proper milestone
4. Update Release Notes
   1. Descriptive text
   2. Update link to closed issues log
5. Close all Typora instances, save, commit
6. Run the `package exe` launch configuration to build the uberjar, the zip and the owlcms_setup.exe installer
7. Test the installer
   1. Uninstall previous version
   2. Refresh the `/owlcms/target` folder.  
   3. Run `/owlcms/target/owlcms_setup/owlcms_setup.exe` to test the installer
8. Refresh owlcms4top, commit and push
9. GitFlow Finish Release  + Push
10. run `release` launch configuration
11. run `heroku` launch configuration.