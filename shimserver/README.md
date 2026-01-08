# Shim Server

Problem:

- Vaadin 25 will require Java 21
- The current control panel installs Java 17; and we don't want people to have to install Java by themselves
- People do not update their control panel in spite of the warnings -- some people still run the original versions that don't warn about new versions.

Solution

- ship a Java 17 shim  that will force the update of the control panel -- it will impersonate owlcms.jar

  - If a correct version of the control panel is installed, the shim is not called
    - the real application is named differently and is run directly with the Java 21 version installed by the control panel

  - If the shim is called, this means that an older control panel is calling owlcms.jar as before
    - the shim opens a browser to tell them to update, with the proper links.



Changes required to the build process

- build this as owlcms.jar
- package the real owlcms as owlcms-ovr.jar



Changes required for the control panel

- Centralize the Java and Node versions so that every module does not need to have them, and change the launching process to use these centralized versions
- provide a cleanup checking the installed apps and clean up
- Check the properties in the applications as well if they need a newer version (currently only the main env.properties is checcked for that purpose)