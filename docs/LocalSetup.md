In a stand-alone setting, you do not need an Internet connection.  Everything runs locally at the competition site.  <u>One laptop is used to run the software and acts as a *primary</u>*.  If you use more than one display, the other screens will connect through your local WiFi network, or through a router.  See the [Equipment Setup](EquipmentSetup.md) page for details on how various setups can be built, from a single person announcing a club meet to a state championship.

## Installing on a Windows laptop

This step is performed only on the primary laptop.

- Get the current installation file called `owlcms_setup.exe` from the <https://github.com/jflamy/owlcms4/releases> page (see the "Assets" section)

![zip](img\LocalInstall\010_setupexe.png)

- Open the downloaded installer by double-clicking on it. 

  - > **If the installer does not start right away and your computer seems to be working very hard, see this page : [Make Windows Defender Allow Installation](DefenderOff)**

  - The same troubleshooting [page](DefenderOff) also shows what to do if you get a blue window that says `Windows protected your PC`

- The installer will prompt you for an installation location.  The default is usually correct.

  ![020_installLocation](img\LocalInstall\020_installLocation.png)

- Accept all the defaults.  Doing so will create a shortcut on your desktop.

  ![030_desktop](img\LocalInstall\030_desktop.png)

- Double-clicking on the icon will start the server and a browser. See [Initial Startup](#initial-startup) for how to proceed.

- If you just want to use dummy data to practice (which will not touch the actual database), right-click on the icon, double-click on "Open File Location" and then double-click on the `demo-owlcms.exe` file.

## Installing on MacOS or Linux

This step is performed only on the primary laptop.

- Go to the releases location (https://github.com/jflamy/owlcms4/releases) and get the current `zip` file.

- Double-click on the zip file, and extract the files to a directory.  We suggest you use `~/owlcms4` as the unzipped location.

- Make sure you have Java 8 installed. 

  -  For Linux, refer to https://adoptopenjdk.net/releases.html depending on the Linux type you run
  -  For MacOS, see https://adoptopenjdk.net/releases.html#x64_mac

- To start the program, change directory to the location where you unzipped the files and launch Java (should you care, `jar` stands for "Java ARchive").

  ```bash
  cd ~/owlcms4
  java -jar owlcms.jar
  ```
  This will actually start the program and a browser. See [Initial Startup](#initial-startup) for how to proceed.

  If you just want to use dummy data to practice (which will not touch the actual database), use instead:

  ```
  java -DdemoMode=true -jar owlcms.jar
  ```

  

## Initial Startup

When OWLCMS is started on a laptop, two windows are visible:  a command-line window, and an internet browser

![040_starting](img\LocalInstall\040_starting.png)

- The command-line window (typically with a black background) is where the OWLCMS primary web server shows its execution log.  

  All the other displays and screens connect to the primary server.  <u>You can stop the program by clicking on the x</u> or clicking in the window and typing `Control-C`.  The various screens and displays will spin in wait mode until you restart the primary program -- there is normally no need to restart or refresh them.

- The white window is a normal browser.  If you look at the top, you will see two or more lines that tell you how to open more browsers and connect them to the primary server.

  ![060_urls](img\LocalInstall\060_urls.png)

  In this example the other laptops on the network would use the address `http://192.168.4.1:8080/` to communicate with the primary server.  "(wired)" refers to the fact that the primary laptop is connected via an Ethernet wire to its router -- see [Local Access](EquipmentSetup#local-access-over-a-local-network) for discussion.  When available, a wired connection is preferred.

  The address <u>depends on your own specific networking setup</u> and you must use one of the addresses displayed **on your setup.**  If none of the addresses listed work, you will need to refer to the persons that set up the networking at your site and on your laptop.  A "proxy" or a "firewall", or some other technical configuration may be blocking access, or requiring a different address that the server can't discover.

  ## Accessing the Program Files and Configuration

  In order to uninstall owlcms4, to report problems, or to change some program configurations, you may need to access the program directory. In order to do so, right-click on the desktop shortcut and select "Open File Location"

  ![070_openLocation](img\LocalInstall\070_openLocation.png)

  If you do so, you will see the installation directory content:

  ![080_files](img\LocalInstall\080_files.png)

- `owlcms.exe` starts the owlcms server.  `demo-owlcms.exe` does the same, but using fictitious data that is reset anew on every start; this makes it perfect for practicing.

- `unins000.exe` is the unistaller.  It will cleanly uninstall everything (including the database and logs, so be careful)

- `database` contains a file ending in `.db` which contains competition data and is managed using the [H2 database engine](https://www.h2database.com/html/main.html). 

- `logs` contains the execution journal of the program where the full details of what happened are written. If you report bugs, you will be asked to send a copy of the files found in that directory (and possibly a copy of the files in the database folder as well).

- `local` is a directory that is used for translating the screens and documents to other languages, or to add alternate formats for results documents.

- `jre`  contains the Java Runtime Environment

- the file ending in `.jar` is the OWLCMS application in executable format

- the `owlcms.l4j.ini` file is used to override application settings (for example, to force the display language) or technical settings

## Control Access to the Application

Mischevious users can probably figure out your WiFi network password, and gain access to the application. 
To prevent this, you can define an application password for the various technical official consoles.  See the [Access Control Settings](2120AdvancedSystemSettings.md#access-control) section.

## Defining the language

This is done as part of the Pre-Competition setup.  See the [Display and Printing language section](2100PreCompetitionSetup#display-and-printout-language)

