## macOS, Linux or RaspberryOS Installation

> Pre-requisite:  You may need to perform a simple install prior to installing owlcms.  See the [Installing Java](#installing-java) section at the bottom of this page to check.

## Installation

- **Get the installation zip archive**: Get the current  **[`owlcms_setup_47.1.1.zip`](https://github.com/owlcms/owlcms4/releases/download/47.1.1/owlcms_setup_47.1.1.zip)** file. Installation files are available as assets at the bottom of each release in the [release repository](https://github.com/owlcms/owlcms4-prerelease/releases/latest) .

  > Tip: [This page](RaspberryInstall) illustrates the sequence of steps on a RaspberryPI and may help you follow along since the process is similar for the other platforms.

- Double-click on the downloaded zip file, and extract the files to a directory. 

  - On a Mac We suggest you use `~/owlcms` as the unzipped location.

- Go to your installation directory where you unzipped owlcms

  ```bash
  java -jar owlcms.jar
  ```
  This will actually start the program. See [Initial Startup](#initial-startup) for how to proceed.

  If you just want to use dummy data to practice (this will not touch your actual database), click on the grey box to copy this command and paste it.

  ```
  java -DmemoryMode=true -DinitialData=LARGEGROUP_DEMO -jar owlcms.jar
  ```


## Initial Startup

When owlcms is started on a laptop, two windows are visible:  a command-line window, and an internet browser

- The command-line window (typically with a black background) is where the OWLCMS primary web server shows its execution log.  

  All the other displays and screens connect to the primary server.  <u>You can stop the program by clicking on the x</u> or clicking in the window and typing `Control-C`.  The various screens and displays will spin in wait mode until you restart the primary program -- there is normally no need to restart or refresh them.

- Depending on the platform, a browser will be opened automatically (or not).  If the browser does not open automatically, navigate to http://localhost:8080

- After the browser opens, if you look at the top, you will see two or more lines that tell you how to open more browsers and connect them to the primary server.

  ![060_urls](img\LocalInstall\060_urls.png)

  In this example the other laptops on the network would use the address `http://192.168.4.1:8080/` to communicate with the primary server.  "(wired)" refers to the fact that the primary laptop is connected via an Ethernet wire to its router -- see [Local Access](EquipmentSetup#local-access-over-a-local-network) for discussion.  When available, a wired connection is preferred.

  The address <u>depends on your own specific networking setup</u> and you must use one of the addresses displayed **on your setup.**  If none of the addresses listed work, you will need to refer to the persons that set up the networking at your site and on your laptop.  A "proxy" or a "firewall", or some other technical configuration may be blocking access, or requiring a different address that the server can't discover.

## Installing Java

Make sure you have a Java17 installed. 

- You can type `java -version` in a Terminal window to see if it is installed, and if so, what version you have.

- On RaspberryOS, Java is already installed.

- For macOS, see [MacOS Java 17](https://adoptium.net/temurin/releases/?os=mac&package=jre&arch=aarch64&version=17) and download the .pkg file. (Temurin is the code name for one of the free Java releases). Double-click the file.

- For Ubuntu and other Debian variants, the following should work.

  ```bash
  sudo apt install default-jre
  ```

- For other Linux distros, see [Linux Java 17](https://adoptium.net/temurin/releases/?os=linux&package=jre&arch=any&version=17) and choose according to the Linux you run