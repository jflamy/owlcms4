In a stand-alone setting, you do not need an Internet connection.  Everything runs locally at the competition site.  <u>One laptop is used to run the software and acts as a *master</u>*.  If you use more than one display, the other screens will connect through your local WiFi network, or through a router.  See the [Equipment Setup](EquipmentSetup.md) page for details on how various setups can be built, from a single person announcing a club meet to a state championship.

## Installing on a Windows laptop

This step is performed only on the master laptop.

- Get the current installation file called `owlcms_setup.exe` from the <https://github.com/jflamy/owlcms4/releases> page (see the "Assets" section)

![zip](img\LocalInstall\010_zip.png)

- Open the downloaded installer by double-clicking on it.

- The installer will prompt you for an installation location.  The default is usually correct.

  ![020_installLocation](img\LocalInstall\020_installLocation.png)

- Accept all the defaults.  Doing so will create a shortcut on your desktop.

  ![030_desktop](img\LocalInstall\030_desktop.png)

- Double-clicking on the icon will start the server and a browser. See [Initial Startup](#initial-startup) for how to proceed.

## Installing on MacOS or Linux

- Go to the releases location (https://github.com/jflamy/owlcms4/releases) and get the current `zip` file.

- Double-click on the zip file, and extract the files to a directory.  We suggest you use `~/owlcms4` as the unzipped location.

- Make sure you have Java 8 installed. 

  -  For Linux, refer to https://openjdk.java.net/install/ depending on the Linux type you run

  - For MacOS, install homebrew (see https://brew.sh/) and then run the following commands

    ```bash
brew tap caskroom/versions
brew cask install java8
    ```

- To start the program, change directory to the location where you unzipped the files and launch java (replace 4.x.y with the actual version number you downloaded)

    ```bash
cd ~/owlcms4
java -jar owlcms-4.x.y.jar
	```
This will actually start the program and a browser. See [Initial Startup](#initial-startup) for how to proceed.

## Initial Startup

When OWLCMS4 is started on a laptop, two windows are visible:  a black command-line window, and a white internet browser

![040_starting](img\LocalInstall\040_starting.png)

- The command-line window (typically with a black background) is the OWLCMS4 master web server.  All the other displays and screens will connect to this server.  You can stop the program by clicking on the x, but if you do so, every single screen and display will spin in wait mode until you restart the program.

- The white window is a normal browser.  If you look at the top, you will see two or more lines that tell you how to open more browsers:

  ![060_urls](img\LocalInstall\060_urls.png)

  In this example the other laptops on the network would use the address `http://192.168.4.1:8080/` to communicate with the master server.  

  The address <u>depends on your own specific networking setup</u> and you must use one of the addresses displayed **on your setup.**  If none of the addresses listed work, you will need to refer to the persons that set up the networking at your site and on your laptop.  A "proxy" or a "firewall", or some other technical configuration may be blocking access, or requiring a different address that the server can't discover.

  ## Accessing the Program Files and Configuration

  In order to uninstall owlcms4, to report problems, or to change some program configurations, you may need to access the program directory. In order to do so, right-click on the desktop shortcut and select "Open File Location"

  ![070_openLocation](img\LocalInstall\070_openLocation.png)

  If you do so, you will see the installation directory content:

  ![080_files](img\LocalInstall\080_files.png)

- `owlcms.exe` starts the owlcms server.  `demo-owlcms.exe` does the same, but using fictitious data that is reset anew on every start; this makes it perfect for practicing.
- The database is created in the directory named `database` 
- Log files are be created in the directory called `logs` . If you report bugs, you will be asked to send a copy of the files found in that directory (and possibly a copy of the files in the database folder as well).
- `bin` and `lib` contain the files to execute Java
- the file ending in `.jar` is the owlcms server proper
- `unins000.exe` will cleanly uninstall everything (including the database and logs, so be careful)