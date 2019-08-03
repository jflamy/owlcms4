### Building from source

This is a standard Maven project.  If you so wish, you can build the binaries from this source.  

- Install Java 8 and the support for Maven and Git in your favorite development environment. Eclipse Java IDE includes the M2E and EGit plugins and works fine -- the project includes the settings file for that configuration.
- Clone this repository.
- Running ``mvn package`` inside the owlcms subdirectory should give you a working .jar and .zip in the target directory.

You are welcome to make improvements and correct issues.  If you do, please clone this repository and create a pull request.

### Packaging and Releasing

This will be further automated. This list assumes that you are running Eclipse.

1. Using the GitFlow plugin
   1. Start a new release
   2. Push to upstream
2. Use the `Versions_set` launch configuration to set the release number
3. Run the `package exe` launch configuration to build the uberjar, the zip and the owlcms_setup.exe installer
4. Test the installer
   1. Uninstall previous version
   2. Refresh the `/owlcms/target` folder.  
   3. Run `/owlcms/target/owlcms_setup/owlcms_setup.exe` to test the installer
5. Cleanup github
   1. Make sure that all closed issues are closed on github and assigned to the proper milestone
   2. Check that the link to the issues log is correct.